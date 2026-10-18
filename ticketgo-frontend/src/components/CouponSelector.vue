<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getAvailableCoupons, getMyCoupons } from '@/api/coupon'
import { couponStatusText, listOf, normalizeCoupon } from '@/utils/normalize'
import { formatDate } from '@/utils/format'

const props = defineProps({
  modelValue: { type: [Object, String, Number], default: null },
  amount: { type: Number, default: 0 },
  productId: { type: [String, Number], default: null },
  autoLoad: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'change'])

const coupons = ref([])
const loading = ref(false)
const loadError = ref(false)

const selectedId = computed(() => {
  const v = props.modelValue
  if (!v) return null
  return typeof v === 'object' ? v.id : v
})

const usable = computed(() =>
  coupons.value.filter((c) => {
    const st = String(c.status || '').toLowerCase()
    if (['used', 'expired', '1', '2'].includes(st)) return false
    return Number(c.amount) > 0
  })
)

const discount = computed(() => {
  const c = usable.value.find((x) => x.id === selectedId.value)
  if (!c) return 0
  if (c.threshold && props.amount && props.amount < c.threshold) return 0
  return Math.min(c.amount, props.amount || c.amount)
})

function select(coupon) {
  emit('update:modelValue', coupon)
  emit('change', coupon, coupon ? discount.value : 0)
}

function selectNone() {
  emit('update:modelValue', null)
  emit('change', null, 0)
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    let data = null
    if (props.productId && props.amount) {
      data = await getAvailableCoupons({ product_id: props.productId, amount: props.amount })
    }
    let arr = listOf(data)
    if (!arr.length) {
      data = await getMyCoupons({ status: 'unused', page: 1, page_size: 50 })
      arr = listOf(data)
    }
    coupons.value = arr.map(normalizeCoupon)
  } catch (e) {
    loadError.value = true
    coupons.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (props.autoLoad) load()
})
watch(() => props.productId, load)
watch(() => props.amount, load)
</script>

<template>
  <div class="coupon-selector">
    <div class="cs-title">
      <span>优惠券</span>
      <span class="muted">已选抵扣 ¥{{ discount.toFixed(2) }}</span>
    </div>
    <div v-if="loading" class="muted">加载优惠券中…</div>
    <template v-else>
      <div v-if="usable.length || !loadError" class="cs-list">
        <div
          class="cs-item"
          :class="{ active: selectedId === null }"
          @click="selectNone"
        >
          <span>不使用优惠券</span>
        </div>
        <div
          v-for="c in usable"
          :key="c.id"
          class="cs-item"
          :class="{ active: selectedId === c.id, disabled: props.amount && c.threshold && props.amount < c.threshold }"
          @click="select(c)"
        >
          <span class="cs-amount">¥{{ c.amount }}</span>
          <span class="cs-name">{{ c.name }}</span>
          <span class="cs-cond">满{{ c.threshold }}可用</span>
          <span v-if="c.expireAt" class="cs-exp">{{ formatDate(c.expireAt) }} 到期</span>
        </div>
      </div>
      <div v-else class="muted">{{ loadError ? '优惠券服务暂不可用' : '暂无可用优惠券' }}</div>
    </template>
  </div>
</template>

<style scoped>
.coupon-selector {
  border: 1px solid #e8d9c5;
  border-radius: var(--radius);
  padding: 14px 16px;
  background: #fff;
}
.cs-title {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 10px;
}
.cs-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}
.cs-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: 1px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  transition: 0.15s;
}
.cs-item:hover {
  border-color: var(--primary);
  background: var(--warm-1);
}
.cs-item.active {
  border-color: var(--primary);
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}
.cs-item.disabled {
  opacity: 0.5;
}
.cs-amount {
  color: var(--primary);
  font-weight: 700;
  font-size: 15px;
  min-width: 46px;
}
.cs-name {
  flex: 1;
}
.cs-cond,
.cs-exp {
  color: var(--text-3);
  font-size: 12px;
}
</style>
