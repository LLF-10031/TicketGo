<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSession, getTiers } from '@/api/product'
import { seckill } from '@/api/seckill'
import { normalizeSession, normalizeTier, normalizeProduct, listOf } from '@/utils/normalize'
import { formatDateTime, toDate } from '@/utils/format'
import CountdownTimer from '@/components/CountdownTimer.vue'
import CouponSelector from '@/components/CouponSelector.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const backendDown = ref(false)
const session = ref(null)
const tiers = ref([])
const tierId = ref(route.query.tierId || null)
const coupon = ref(null)

const dialogVisible = ref(false)
const dialogType = ref('success')
const dialogTitle = ref('')
const dialogMessage = ref('')
const resultOrderId = ref(null)

const tier = computed(() => {
  if (tierId.value) {
    const t = tiers.value.find((x) => String(x.id) === String(tierId.value))
    if (t) return t
  }
  return tiers.value[0] || null
})

const saleStart = computed(() => (session.value && session.value.saleStart) || null)
const notOnSale = computed(() => {
  const d = toDate(saleStart.value)
  return d ? d.getTime() > Date.now() : false
})
const soldOut = computed(() => {
  const t = tier.value
  return !!((session.value && session.value.soldOut) || (t && t.stock <= 0))
})

const buttonText = computed(() => {
  if (submitting.value) return '排队中…'
  if (notOnSale.value) return '未开票'
  if (soldOut.value) return '已抢完'
  return '立即抢票'
})

function showResult(type, title, message, orderId) {
  dialogType.value = type
  dialogTitle.value = title
  dialogMessage.value = message
  resultOrderId.value = orderId || null
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  try {
    const s = await getSession(route.params.sessionId)
    session.value = s ? normalizeSession(s) : null
  } catch (e) {
    backendDown.value = true
    session.value = null
  }
  if (!session.value) {
    session.value = normalizeSession({
      id: route.params.sessionId,
      show_time: '2026-06-20 19:30',
      venue_name: '国家体育场（鸟巢）'
    })
  }
  try {
    const data = await getTiers(route.params.sessionId)
    const arr = listOf(data)
    tiers.value = arr.length
      ? arr.map(normalizeTier)
      : [normalizeTier({ id: tierId.value || 201, name: '看台票', price: 580, stock: 999 })].filter((t) => t.id)
  } catch (e) {
    tiers.value = [normalizeTier({ id: tierId.value || 201, name: '看台票', price: 580, stock: 999 })].filter((t) => t.id)
  }
  if (!tierId.value && tiers.value.length) tierId.value = tiers.value[0].id
  loading.value = false
}

async function doSeckill() {
  if (submitting.value || !tier.value) return
  if (notOnSale.value) {
    ElMessage.warning('尚未开票，请耐心等待')
    return
  }
  submitting.value = true
  try {
    const data = await seckill(route.params.sessionId, tier.value.id)
    const ok = data && (data.success === true || data.order_id || data.orderId)
    const orderId = data && (data.order_id || data.orderId)
    if (ok) {
      showResult('success', '抢票成功', '已为你锁定门票，请在 15 分钟内完成支付', orderId)
    } else {
      showResult('fail', '抢票失败', (data && data.message) || '当前排队人数过多，请稍后重试')
    }
  } catch (e) {
    const code = e && e.code
    let msg = (e && e.message) || '抢票失败，请稍后重试'
    if (code === 1001) msg = '已售罄，下次再来吧'
    if (code === 1002) msg = '您已参与过，请勿重复抢购'
    if (code === 1003) msg = '未开票或已结束'
    showResult('fail', '抢票失败', msg)
  } finally {
    submitting.value = false
  }
}

function goOrder() {
  dialogVisible.value = false
  if (resultOrderId.value) router.push(`/order/${resultOrderId.value}`)
  else router.push('/orders')
}

onMounted(load)
</script>

<template>
  <div class="container page" style="max-width: 760px">
    <el-skeleton v-if="loading" :rows="5" animated />

    <template v-else>
      <el-alert
        v-if="backendDown"
        class="mb-16"
        type="warning"
        :closable="false"
        show-icon
        title="后端服务暂未连接，抢票功能可能不可用"
      />

      <div class="panel center">
        <div class="tag mb-8" style="display: inline-block">限时秒杀</div>
        <h2 style="font-size: 22px; margin-bottom: 6px">
          {{ (session && session.name) || '演出秒杀专场' }}
        </h2>
        <div class="muted mb-16">
          {{ session ? formatDateTime(session.time) : '-' }}
          <template v-if="session && session.venue"> · {{ session.venue }}</template>
        </div>

        <div v-if="notOnSale" class="mb-16">
          <div class="muted mb-8">距开票还有</div>
          <CountdownTimer
            :target="saleStart"
            style="font-size: 30px; font-weight: 800; color: var(--primary)"
            @finish="load"
          />
        </div>

        <div class="sku-box">
          <div class="between">
            <span>{{ tier ? tier.name : '票档' }}</span>
            <span class="price-strong" style="font-size: 22px">¥{{ tier ? tier.price : '-' }}</span>
          </div>
          <div class="muted mt-8">
            库存：{{ tier ? (tier.stock > 10 ? '充足' : tier.stock + ' 张') : '-' }}
          </div>
        </div>

        <button
          class="btn btn-primary btn-block mt-24"
          style="height: 54px; font-size: 18px; border-radius: 999px"
          :disabled="submitting || notOnSale || soldOut"
          @click="doSeckill"
        >
          {{ buttonText }}
        </button>

        <div class="muted mt-16">提示：抢票采用异步落单，成功后请尽快完成支付</div>
      </div>

      <div class="mt-16">
        <CouponSelector v-model="coupon" :amount="tier ? tier.price : 0" />
      </div>
    </template>

    <el-dialog v-model="dialogVisible" width="360px" :show-close="false" align-center>
      <div class="center">
        <div style="font-size: 46px">{{ dialogType === 'success' ? '🎉' : '😢' }}</div>
        <h3 class="mt-8">{{ dialogTitle }}</h3>
        <p class="muted mt-8">{{ dialogMessage }}</p>
      </div>
      <template #footer>
        <div class="center">
          <button class="btn btn-primary" style="min-width: 140px" @click="goOrder">
            {{ dialogType === 'success' ? '去支付' : '知道了' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sku-box {
  background: var(--warm-1);
  border: 1px solid #e8d9c5;
  border-radius: 12px;
  padding: 16px 18px;
  text-align: left;
}
</style>
