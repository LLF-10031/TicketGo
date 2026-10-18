<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSeats, lockSeats } from '@/api/seat'
import { getTiers } from '@/api/product'
import { listOf, normalizeSeat, normalizeTier, seatStatusOf } from '@/utils/normalize'
import SeatMap from '@/components/SeatMap.vue'
import CouponSelector from '@/components/CouponSelector.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const locking = ref(false)
const backendDown = ref(false)
const seats = ref([])
const tiers = ref([])
const selected = ref([])
const coupon = ref(null)
const lockRemain = ref(0)
let lockTimer = null

const tierId = ref(route.query.tierId || null)

const selectedSeats = computed(() =>
  seats.value.filter((s) => selected.value.includes(s.id))
)

const totalAmount = computed(() =>
  selectedSeats.value.reduce((sum, s) => {
    const t = tiers.value.find((x) => String(x.id) === String(s.tierId))
    return sum + (s.price || (t && t.price) || 0)
  }, 0)
)

const discount = computed(() => {
  const c = coupon.value
  if (!c) return 0
  if (c.threshold && totalAmount.value < c.threshold) return 0
  return Math.min(Number(c.amount) || 0, totalAmount.value)
})

const payAmount = computed(() => Math.max(0, totalAmount.value - discount.value))

function buildDemoSeats() {
  const rows = ['A', 'B', 'C', 'D', 'E', 'F']
  const out = []
  rows.forEach((r, ri) => {
    for (let c = 1; c <= 12; c += 1) {
      const sold = (ri * 12 + c) % 7 === 0
      out.push(
        normalizeSeat({
          seat_id: `${r}${c}`,
          row_no: r,
          col_no: c,
          tier_id: ri < 2 ? 201 : ri < 4 ? 202 : 203,
          price: ri < 2 ? 580 : ri < 4 ? 980 : 1580,
          status: sold ? 'sold' : ri === 5 && c === 6 ? 'locked' : 'available'
        })
      )
    }
  })
  return out
}

async function load() {
  loading.value = true
  backendDown.value = false
  try {
    const data = await getSeats(route.params.sessionId, tierId.value ? { tier_id: tierId.value } : {})
    const arr = listOf(data)
    seats.value = arr.length ? arr.map(normalizeSeat) : buildDemoSeats()
  } catch (e) {
    backendDown.value = true
    seats.value = buildDemoSeats()
  }
  try {
    const t = await getTiers(route.params.sessionId)
    const arr = listOf(t)
    tiers.value = arr.length
      ? arr.map(normalizeTier)
      : [201, 202, 203].map((id, i) =>
          normalizeTier({ id, name: ['看台票', '内场票', 'VIP 票'][i], price: [580, 980, 1580][i] })
        )
  } catch (e) {
    tiers.value = [201, 202, 203].map((id, i) =>
      normalizeTier({ id, name: ['看台票', '内场票', 'VIP 票'][i], price: [580, 980, 1580][i] })
    )
  }
  loading.value = false
}

function onConflict(seat, reason) {
  if (reason === 'max') ElMessage.warning('最多选择 6 个座位')
  else if (reason === 'sold') ElMessage.warning('该座位已售出')
  else if (reason === 'locked') ElMessage.warning('该座位已被锁定，请选择其他座位')
}

function startLockCountdown(seconds) {
  lockRemain.value = seconds
  if (lockTimer) clearInterval(lockTimer)
  lockTimer = setInterval(() => {
    lockRemain.value -= 1
    if (lockRemain.value <= 0) {
      clearInterval(lockTimer)
      lockTimer = null
      selected.value = []
      ElMessage.warning('锁座已超时，请重新选择')
      load()
    }
  }, 1000)
}

async function checkout() {
  if (!selected.value.length) {
    ElMessage.warning('请先选择座位')
    return
  }
  locking.value = true
  try {
    const data = await lockSeats({
      session_id: route.params.sessionId,
      seat_ids: selected.value,
      tier_id: tierId.value,
      expire_seconds: 300
    })
    ElMessage.success('锁座成功，请在 5 分钟内完成下单')
    startLockCountdown(300)
    const orderId = data && (data.order_id || data.orderId)
    if (orderId) {
      router.push(`/order/${orderId}`)
    } else {
      router.push('/orders')
    }
  } catch (e) {
    ElMessage.error((e && e.message) || '锁座失败，座位可能已被抢占')
    await load()
    selected.value = []
  } finally {
    locking.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => lockTimer && clearInterval(lockTimer))
</script>

<template>
  <div class="container page">
    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else>
      <el-alert
        v-if="backendDown"
        class="mb-16"
        type="warning"
        :closable="false"
        show-icon
        title="后端服务暂未连接，当前展示演示座位图"
      />

      <div class="seat-layout">
        <div>
          <div class="section-title" style="margin-top: 0"><h2>选择座位</h2></div>
          <SeatMap v-model="selected" :seats="seats" :max-select="6" @conflict="onConflict" />
        </div>

        <aside class="panel seat-summary">
          <h3 style="font-size: 16px; margin-bottom: 12px">订单摘要</h3>
          <div v-if="selectedSeats.length" class="selected-list">
            <div v-for="s in selectedSeats" :key="s.id" class="between selected-item">
              <span>{{ s.area }} {{ s.row }}排{{ s.col }}座</span>
              <span class="price-strong">¥{{ s.price }}</span>
            </div>
          </div>
          <div v-else class="muted">尚未选择座位（最多 6 个）</div>

          <div v-if="lockRemain > 0" class="lock-tip">锁座剩余 {{ Math.floor(lockRemain / 60) }}:{{ String(lockRemain % 60).padStart(2, '0') }}</div>

          <div class="mt-16">
            <CouponSelector v-model="coupon" :amount="totalAmount" />
          </div>

          <div class="summary-total mt-16">
            <div class="between"><span>票款</span><span>¥{{ totalAmount.toFixed(2) }}</span></div>
            <div class="between"><span>优惠</span><span class="price-strong">-¥{{ discount.toFixed(2) }}</span></div>
            <div class="between total-line"><span>应付</span><span class="price-strong">¥{{ payAmount.toFixed(2) }}</span></div>
          </div>

          <button
            class="btn btn-primary btn-block mt-16"
            :disabled="locking || !selected.length"
            @click="checkout"
          >
            {{ locking ? '锁座中…' : '锁定座位并下单' }}
          </button>
        </aside>
      </div>

      <div class="seat-tiers mt-16">
        <span v-for="(t, i) in tiers" :key="t.id" class="tag" :class="{ gray: i > 0 }" style="margin-right: 8px">
          {{ t.name }} ¥{{ t.price }}
        </span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.seat-layout {
  display: grid;
  grid-template-columns: 1fr 330px;
  gap: 20px;
  align-items: start;
}
.selected-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 220px;
  overflow-y: auto;
}
.selected-item {
  font-size: 13px;
  padding: 6px 10px;
  background: var(--warm-1);
  border-radius: 8px;
}
.lock-tip {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fff1e6;
  color: #ff7a1a;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
}
.summary-total {
  border-top: 1px dashed var(--border);
  padding-top: 12px;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.total-line {
  font-size: 16px;
  font-weight: 700;
}
@media (max-width: 900px) {
  .seat-layout {
    grid-template-columns: 1fr;
  }
}
</style>
