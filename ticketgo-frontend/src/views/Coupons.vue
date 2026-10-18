<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { buyCoupon, getTemplates, receiveCoupon } from '@/api/coupon'
import { listOf, normalizeCoupon } from '@/utils/normalize'
import { formatDate } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(true)
const backendDown = ref(false)
const coupons = ref([])
const scope = ref('')

const scopes = [
  { label: '全部', value: '' },
  { label: '全场通用', value: 'all' },
  { label: '指定演出', value: 'product' },
  { label: '指定分类', value: 'category' }
]

const demo = [
  { template_id: 1, name: '新人立减券', discount_amount: 30, min_amount: 100, scope_type: '全场通用', end_time: '2026-12-31', limit_type: 'limited', stock: 100 },
  { template_id: 2, name: '满 200 减 50', discount_amount: 50, min_amount: 200, scope_type: '全场通用', end_time: '2026-12-31', limit_type: 'limited', stock: 50 },
  { template_id: 3, name: '演唱会专享 100 元券', discount_amount: 100, min_amount: 600, scope_type: '指定分类', end_time: '2026-10-01', limit_type: 'buy', price: 9.9 },
  { template_id: 4, name: '满 500 减 120', discount_amount: 120, min_amount: 500, scope_type: '全场通用', end_time: '2026-11-30', limit_type: 'limited', stock: 0 },
  { template_id: 5, name: '话剧歌剧 80 元券', discount_amount: 80, min_amount: 300, scope_type: '指定分类', end_time: '2026-09-30', limit_type: 'buy', price: 5 },
  { template_id: 6, name: '展览休闲立减 20', discount_amount: 20, min_amount: 80, scope_type: '指定分类', end_time: '2026-12-15', limit_type: 'limited', stock: 200 }
]

async function load() {
  loading.value = true
  backendDown.value = false
  try {
    const data = await getTemplates({ scope_type: scope.value || undefined })
    const arr = listOf(data)
    coupons.value = arr.length ? arr.map(normalizeCoupon) : demo.map(normalizeCoupon)
  } catch (e) {
    backendDown.value = true
    coupons.value = demo.map(normalizeCoupon)
  } finally {
    loading.value = false
  }
}

function isBuyable(c) {
  return ['buy', 'purchase', 'unlimited'].includes(String(c.limitType).toLowerCase())
}

function disabled(c) {
  if (isBuyable(c)) return false
  if (c.stock !== undefined && c.stock !== null && Number(c.stock) <= 0) return true
  return c.received === true
}

async function onReceive(c) {
  try {
    await receiveCoupon(c.templateId || c.id)
    ElMessage.success('领取成功，可在「我的券包」查看')
    load()
  } catch (e) {
    ElMessage.error((e && e.message) || '领取失败')
  }
}

async function onBuy(c) {
  try {
    await buyCoupon({ template_id: c.templateId || c.id, quantity: 1 })
    ElMessage.success('购买成功')
    load()
  } catch (e) {
    ElMessage.error((e && e.message) || '购买失败，请稍后重试')
  }
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <div class="section-title" style="margin-top: 0">
      <h2>券中心</h2>
      <a class="more" @click="$router.push('/my-coupons')">我的券包 ›</a>
    </div>

    <div class="status-tabs">
      <span
        v-for="s in scopes"
        :key="s.value"
        class="status-tab"
        :class="{ active: scope === s.value }"
        @click="scope = s.value; load()"
        >{{ s.label }}</span
      >
    </div>

    <el-alert
      v-if="backendDown"
      class="mb-16"
      type="warning"
      :closable="false"
      show-icon
      title="优惠券服务暂未连接，当前展示演示数据"
    />

    <el-skeleton v-if="loading" :rows="5" animated />

    <EmptyState v-else-if="!coupons.length" description="暂无可领取的优惠券" icon="🎟️" />

    <div v-else class="coupon-grid">
      <div v-for="c in coupons" :key="c.id" class="coupon-card" :class="{ disabled: disabled(c) }">
        <div class="cc-left">
          <div class="cc-amount"><small>¥</small>{{ c.amount }}</div>
          <div class="cc-condition">满 {{ c.threshold }} 元可用</div>
        </div>
        <div class="cc-right">
          <div>
            <div class="cc-name">{{ c.name }}</div>
            <div class="cc-scope">适用范围：{{ c.scope }}</div>
            <div class="cc-expire">
              <template v-if="c.expireAt">有效期至 {{ formatDate(c.expireAt) }}</template>
              <template v-else>长期有效</template>
            </div>
            <div v-if="!isBuyable(c) && c.stock !== undefined" class="cc-expire">
              剩余 {{ c.stock }} 张
            </div>
          </div>
          <div class="cc-action">
            <button
              v-if="isBuyable(c)"
              class="btn btn-primary btn-sm btn-block"
              @click="onBuy(c)"
            >
              购买
            </button>
            <button
              v-else
              class="btn btn-ghost btn-sm btn-block"
              :disabled="disabled(c)"
              @click="onReceive(c)"
            >
              {{ disabled(c) ? (c.received ? '已领取' : '已领完') : '立即领取' }}
            </button>
          </div>
        </div>
      </div>
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
  width: fit-content;
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
