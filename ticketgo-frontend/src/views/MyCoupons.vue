<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCoupons } from '@/api/coupon'
import { couponStatusText, listOf, normalizeCoupon } from '@/utils/normalize'
import { formatDate } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const loading = ref(true)
const backendDown = ref(false)
const coupons = ref([])
const status = ref('unused')

const tabs = [
  { label: '未使用', value: 'unused' },
  { label: '已使用', value: 'used' },
  { label: '已过期', value: 'expired' }
]

const demo = {
  unused: [
    { user_coupon_id: 11, name: '满 200 减 50', discount_amount: 50, min_amount: 200, scope_type: '全场通用', expire_at: '2026-12-31', status: 'unused' },
    { user_coupon_id: 12, name: '新人立减券', discount_amount: 30, min_amount: 100, scope_type: '全场通用', expire_at: '2026-12-31', status: 'unused' }
  ],
  used: [
    { user_coupon_id: 21, name: '话剧歌剧 80 元券', discount_amount: 80, min_amount: 300, scope_type: '指定分类', expire_at: '2026-08-01', status: 'used' }
  ],
  expired: [
    { user_coupon_id: 31, name: '展览休闲立减 20', discount_amount: 20, min_amount: 80, scope_type: '指定分类', expire_at: '2026-01-01', status: 'expired' }
  ]
}

async function load() {
  loading.value = true
  backendDown.value = false
  try {
    const data = await getMyCoupons({ status: status.value, page: 1, page_size: 50 })
    const arr = listOf(data)
    coupons.value = arr.length ? arr.map(normalizeCoupon) : demo[status.value].map(normalizeCoupon)
  } catch (e) {
    backendDown.value = true
    coupons.value = demo[status.value].map(normalizeCoupon)
  } finally {
    loading.value = false
  }
}

function switchTab(v) {
  status.value = v
  load()
}
</script>

<template>
  <div class="container page">
    <div class="order-layout">
      <aside class="side-menu">
        <a @click="router.push('/profile')">个人中心</a>
        <a @click="router.push('/orders')">我的订单</a>
        <a class="active">我的券包</a>
        <a @click="router.push('/cart')">购物车</a>
        <a @click="router.push('/coupons')">券中心</a>
      </aside>

      <main>
        <div class="section-title" style="margin-top: 0"><h2>我的券包</h2></div>

        <div class="status-tabs">
          <span
            v-for="t in tabs"
            :key="t.value"
            class="status-tab"
            :class="{ active: status === t.value }"
            @click="switchTab(t.value)"
            >{{ t.label }}</span
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

        <el-skeleton v-if="loading" :rows="4" animated />

        <EmptyState v-else-if="!coupons.length" description="这里还没有优惠券" icon="🎟️" action-text="去券中心" @action="router.push('/coupons')" />

        <div v-else class="coupon-grid">
          <div
            v-for="c in coupons"
            :key="c.id"
            class="coupon-card"
            :class="{ disabled: status !== 'unused' }"
          >
            <div class="cc-left" :class="{ 'coupon-empty-left': status !== 'unused' }">
              <div class="cc-amount"><small>¥</small>{{ c.amount }}</div>
              <div class="cc-condition">满 {{ c.threshold }} 元可用</div>
            </div>
            <div class="cc-right">
              <div>
                <div class="cc-name">{{ c.name }}</div>
                <div class="cc-scope">{{ c.scope }}</div>
                <div class="cc-expire">
                  <template v-if="c.expireAt">有效期至 {{ formatDate(c.expireAt) }}</template>
                  <template v-else>长期有效</template>
                </div>
              </div>
              <div class="cc-action between">
                <span class="tag" :class="couponStatusText(c.status).cls">
                  {{ couponStatusText(c.status).text }}
                </span>
                <button v-if="status === 'unused'" class="btn btn-ghost btn-sm" @click="router.push('/')">
                  去使用
                </button>
              </div>
            </div>
          </div>
        </div>
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
