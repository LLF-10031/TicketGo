<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, getOrder, pay, refundOrder } from '@/api/order'
import { normalizeOrder } from '@/utils/normalize'
import { formatDateTime, orderStatus } from '@/utils/format'
import CountdownTimer from '@/components/CountdownTimer.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const backendDown = ref(false)
const order = ref(null)
const paying = ref(false)

const statusInfo = computed(() => orderStatus(order.value && order.value.rawStatus))

const qrMatrix = computed(() => {
  const code = String((order.value && (order.value.ticketCode || order.value.orderNo)) || 'TICKETGO')
  const size = 21
  let seed = 0
  for (let i = 0; i < code.length; i += 1) seed = (seed * 31 + code.charCodeAt(i)) % 2147483647
  const rand = () => {
    seed = (seed * 1103515245 + 12345) % 2147483648
    return seed / 2147483648
  }
  const m = []
  for (let y = 0; y < size; y += 1) {
    const row = []
    for (let x = 0; x < size; x += 1) row.push(rand() > 0.52)
    m.push(row)
  }
  const finder = (ox, oy) => {
    for (let y = 0; y < 7; y += 1)
      for (let x = 0; x < 7; x += 1) {
        const edge = x === 0 || x === 6 || y === 0 || y === 6
        const core = x >= 2 && x <= 4 && y >= 2 && y <= 4
        m[oy + y][ox + x] = edge || core
      }
  }
  finder(0, 0)
  finder(size - 7, 0)
  finder(0, size - 7)
  return m
})

async function load() {
  loading.value = true
  backendDown.value = false
  try {
    const data = await getOrder(route.params.id)
    order.value = data ? normalizeOrder(data) : null
  } catch (e) {
    backendDown.value = true
    order.value = normalizeOrder({
      order_id: route.params.id,
      order_no: String(route.params.id),
      status: 'pending',
      pay_amount: 530,
      total_amount: 580,
      discount_amount: 50,
      coupon_name: '满200减50',
      create_time: new Date().toISOString(),
      pay_expire_at: new Date(Date.now() + 14 * 60 * 1000).toISOString(),
      product_name: '周杰伦「嘉年华」演唱会 · 看台票',
      venue_name: '北京 · 国家体育场',
      session_time: '2026-06-20 19:30',
      ticket_code: 'TG-' + route.params.id + '-A1B2C3'
    })
  } finally {
    loading.value = false
  }
}

async function onPay() {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${order.value.amount.toFixed(2)}？`, '订单支付', { type: 'info' })
  } catch (e) {
    return
  }
  paying.value = true
  try {
    await pay({ order_id: order.value.id, pay_type: 'mock' })
    ElMessage.success('支付成功')
    await load()
  } catch (e) {
    ElMessage.error((e && e.message) || '支付失败')
  } finally {
    paying.value = false
  }
}

async function onCancel() {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    load()
  } catch (e) {
    ElMessage.error((e && e.message) || '取消失败')
  }
}

async function onRefund() {
  try {
    await ElMessageBox.confirm('确定要申请退款吗？', '退款申请', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await refundOrder(order.value.id)
    ElMessage.success('退款申请已提交')
    load()
  } catch (e) {
    ElMessage.error((e && e.message) || '退款申请失败')
  }
}
</script>

<template>
  <div class="container page" style="max-width: 860px">
    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="order">
      <el-alert
        v-if="backendDown"
        class="mb-16"
        type="warning"
        :closable="false"
        show-icon
        title="订单服务暂未连接，当前展示演示数据"
      />

      <div class="section-title" style="margin-top: 0">
        <h2>订单详情</h2>
        <a class="more" @click="router.push('/orders')">返回订单列表 ›</a>
      </div>

      <div class="panel mb-16 order-head-panel">
        <div class="between">
          <div>
            <div class="status-pill" :class="statusInfo.cls">{{ statusInfo.text }}</div>
            <div class="muted mt-8">订单号：{{ order.orderNo }}</div>
            <div class="muted">下单时间：{{ formatDateTime(order.createdAt) }}</div>
          </div>
          <div v-if="order.status === 'pending'" style="text-align: right">
            <div class="muted">请在以下时间内完成支付</div>
            <CountdownTimer
              :target="order.expireAt"
              style="font-size: 26px; font-weight: 800; color: var(--primary)"
              finished-text="订单已超时"
              @finish="load"
            />
          </div>
        </div>
      </div>

      <div v-if="order.status === 'paid'" class="panel mb-16 center eticket">
        <div class="muted mb-8">电子票 · 入场凭证</div>
        <svg class="qr" viewBox="0 0 21 21" width="180" height="180">
          <rect x="0" y="0" width="21" height="21" fill="#fff" />
          <template v-for="(row, y) in qrMatrix" :key="y">
            <rect
              v-for="(cell, x) in row"
              :key="x"
              :x="x"
              :y="y"
              width="1"
              height="1"
              :fill="cell ? '#1a1a1a' : '#fff'"
            />
          </template>
        </svg>
        <div class="mt-8" style="font-weight: 700; letter-spacing: 2px">
          {{ order.ticketCode || order.orderNo }}
        </div>
        <div class="muted mt-8">请于演出当天出示此二维码检票入场</div>
      </div>

      <div class="panel mb-16">
        <h3 style="font-size: 16px; margin-bottom: 14px">商品信息</h3>
        <div class="oc-body" style="display: flex; gap: 14px">
          <div class="oc-thumb">
            <img v-if="order.cover" :src="order.cover" alt="" />
          </div>
          <div style="flex: 1">
            <div style="font-weight: 600">{{ order.title }}</div>
            <div class="muted mt-8">
              {{ order.venue }}<template v-if="order.sessionTime"> · {{ order.sessionTime }}</template>
            </div>
            <div v-if="order.seatInfo" class="muted">座位：{{ order.seatInfo }}</div>
            <div class="muted">数量：{{ order.quantity }} 张</div>
          </div>
        </div>
      </div>

      <div class="panel mb-16">
        <h3 style="font-size: 16px; margin-bottom: 14px">金额明细</h3>
        <div class="between mt-8"><span class="muted">商品总额</span><span>¥{{ order.originAmount.toFixed(2) }}</span></div>
        <div class="between mt-8">
          <span class="muted">优惠券<template v-if="order.couponName">（{{ order.couponName }}）</template></span>
          <span class="price-strong">-¥{{ order.discount.toFixed(2) }}</span>
        </div>
        <div class="between mt-8" style="font-size: 18px; font-weight: 700">
          <span>实付金额</span><span class="price-strong">¥{{ order.amount.toFixed(2) }}</span>
        </div>
      </div>

      <div class="panel center actions-bar">
        <template v-if="order.status === 'pending'">
          <button class="btn btn-ghost" @click="onCancel">取消订单</button>
          <button class="btn btn-primary" :disabled="paying" @click="onPay">
            {{ paying ? '支付中…' : '立即支付' }}
          </button>
        </template>
        <template v-else-if="order.status === 'paid'">
          <button class="btn btn-ghost" @click="onRefund">申请退款</button>
          <button class="btn btn-primary" @click="router.push('/')">继续购票</button>
        </template>
        <template v-else>
          <button class="btn btn-primary" @click="router.push('/')">继续购票</button>
        </template>
      </div>
    </template>

    <EmptyState v-else description="订单不存在" icon="🧾" action-text="返回订单列表" @action="router.push('/orders')" />
  </div>
</template>

<style scoped>
.order-head-panel {
  border-left: 4px solid var(--primary);
}
.eticket {
  background: linear-gradient(180deg, #fffdfa, #fff8ee);
}
.qr {
  border: 8px solid #fff;
  box-shadow: 0 4px 16px rgba(120, 90, 60, 0.14);
  border-radius: 8px;
}
.actions-bar {
  display: flex;
  justify-content: center;
  gap: 14px;
}
.oc-thumb {
  width: 96px;
  height: 74px;
  border-radius: 10px;
  overflow: hidden;
  background: linear-gradient(135deg, #ffd3c2, #ffb199);
  flex-shrink: 0;
}
.oc-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
