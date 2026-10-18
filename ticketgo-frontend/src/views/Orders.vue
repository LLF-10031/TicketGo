<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, getOrderList, pay } from '@/api/order'
import { listOf, normalizeOrder, totalOf } from '@/utils/normalize'
import { formatDateTime } from '@/utils/format'
import { orderStatus } from '@/utils/format'
import CountdownTimer from '@/components/CountdownTimer.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const loading = ref(false)
const backendDown = ref(false)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(5)
const status = ref('')

const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'pending' },
  { label: '已支付', value: 'paid' },
  { label: '已取消', value: 'cancelled' },
  { label: '已退款', value: 'refunded' }
]

const sideMenus = [
  { label: '个人中心', path: '/profile' },
  { label: '我的订单', path: '/orders' },
  { label: '我的券包', path: '/my-coupons' },
  { label: '购物车', path: '/cart' },
  { label: '券中心', path: '/coupons' }
]

const demoOrders = [
  { order_id: 9001, order_no: '2026010112300001', status: 'pending', pay_amount: 530, total_amount: 580, coupon_name: '满200减50', create_time: '2026-01-01 12:30', product_name: '周杰伦「嘉年华」演唱会 · 看台票', venue_name: '北京 · 国家体育场', session_time: '2026-06-20 19:30', pay_expire_at: new Date(Date.now() + 13 * 60 * 1000).toISOString() },
  { order_id: 9002, order_no: '2025122811000007', status: 'paid', pay_amount: 280, total_amount: 280, create_time: '2025-12-28 11:00', product_name: '话剧《暗恋桃花源》 · 一楼池座 3排5座', venue_name: '杭州 · 大剧院', session_time: '2026-03-15 19:30' },
  { order_id: 9003, order_no: '2025122019000003', status: 'cancelled', pay_amount: 256, total_amount: 256, create_time: '2025-12-20 19:00', product_name: '沉浸式梵高艺术展 · 成人票 ×2', venue_name: '深圳 · 当代艺术中心', session_time: '2025-12-25' }
]

async function load() {
  loading.value = true
  backendDown.value = false
  try {
    const data = await getOrderList({
      status: status.value || undefined,
      page: page.value,
      page_size: pageSize.value
    })
    const arr = listOf(data)
    if (!arr.length && !status.value) {
      orders.value = demoOrders.map(normalizeOrder)
      total.value = demoOrders.length
    } else {
      orders.value = arr.map(normalizeOrder)
      total.value = totalOf(data, orders.value.length)
    }
  } catch (e) {
    backendDown.value = true
    orders.value = demoOrders.filter((o) => !status.value || o.status === status.value).map(normalizeOrder)
    total.value = orders.value.length
  } finally {
    loading.value = false
  }
}

function switchStatus(v) {
  status.value = v
  page.value = 1
  load()
}

async function onPay(order) {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${order.amount.toFixed(2)}？`, '订单支付', { type: 'info' })
  } catch (e) {
    return
  }
  try {
    await pay({ order_id: order.id, pay_type: 'mock' })
    ElMessage.success('支付成功')
    load()
  } catch (e) {
    ElMessage.error((e && e.message) || '支付失败')
  }
}

async function onCancel(order) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    load()
  } catch (e) {
    ElMessage.error((e && e.message) || '取消失败')
  }
}

function goDetail(order) {
  router.push(`/order/${order.id}`)
}

function goMenu(m) {
  router.push(m.path)
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <div class="order-layout">
      <aside class="side-menu">
        <a
          v-for="m in sideMenus"
          :key="m.label"
          :class="{ active: m.path === '/orders' }"
          @click="goMenu(m)"
          >{{ m.label }}</a
        >
      </aside>

      <main>
        <div class="section-title" style="margin-top: 0"><h2>我的订单</h2></div>

        <div class="status-tabs">
          <span
            v-for="t in tabs"
            :key="t.value"
            class="status-tab"
            :class="{ active: status === t.value }"
            @click="switchStatus(t.value)"
            >{{ t.label }}</span
          >
        </div>

        <el-alert
          v-if="backendDown"
          class="mb-16"
          type="warning"
          :closable="false"
          show-icon
          title="订单服务暂未连接，当前展示演示数据"
        />

        <el-skeleton v-if="loading" :rows="4" animated />

        <EmptyState v-else-if="!orders.length" description="暂无相关订单" icon="📦" action-text="去购票" @action="router.push('/')" />

        <template v-else>
          <div v-for="o in orders" :key="o.id" class="order-card">
            <div class="oc-head">
              <span>订单号：{{ o.orderNo }}</span>
              <span>{{ formatDateTime(o.createdAt) }}</span>
            </div>
            <div class="oc-body">
              <div class="oc-thumb">
                <img v-if="o.cover" :src="o.cover" alt="" />
              </div>
              <div style="flex: 1">
                <div style="font-weight: 600; margin-bottom: 6px">{{ o.title }}</div>
                <div class="muted">
                  {{ o.venue }}<template v-if="o.sessionTime"> · {{ o.sessionTime }}</template>
                  <template v-if="o.seatInfo"> · {{ o.seatInfo }}</template>
                </div>
                <div class="mt-8">
                  <span v-if="o.couponName" class="tag">{{ o.couponName }}</span>
                  实付 <b class="price-strong">¥{{ o.amount.toFixed(2) }}</b>
                </div>
              </div>
              <div style="text-align: right">
                <div class="status-pill" :class="orderStatus(o.rawStatus).cls">
                  {{ orderStatus(o.rawStatus).text }}
                </div>
                <div v-if="o.status === 'pending' && o.expireAt" class="muted mt-8">
                  剩余 <CountdownTimer :target="o.expireAt" finished-text="已超时" @finish="load" />
                </div>
              </div>
            </div>
            <div class="oc-actions">
              <template v-if="o.status === 'pending'">
                <button class="btn btn-ghost btn-sm" @click="onCancel(o)">取消订单</button>
                <button class="btn btn-primary btn-sm" @click="onPay(o)">立即支付</button>
              </template>
              <button class="btn btn-ghost btn-sm" @click="goDetail(o)">查看详情</button>
            </div>
          </div>

          <div v-if="total > pageSize" class="center mt-16">
            <el-pagination
              layout="prev, pager, next"
              background
              :current-page="page"
              :page-size="pageSize"
              :total="total"
              @current-change="(p) => { page = p; load() }"
            />
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.status-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e8d9c5;
  border-radius: var(--radius);
  padding: 6px;
  box-shadow: var(--shadow);
}
.status-tab {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-2);
  cursor: pointer;
  transition: 0.15s;
}
.status-tab:hover {
  background: var(--warm-1);
}
.status-tab.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}
</style>
