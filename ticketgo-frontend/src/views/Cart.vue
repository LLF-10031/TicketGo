<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { checkoutCart } from '@/api/cart'
import { pick } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const cart = useCartStore()
const backendDown = ref(false)
const checkingOut = ref(false)

function itemOf(it) {
  return {
    id: pick(it, ['id', 'cart_item_id', 'cartItemId']),
    title: pick(it, ['product_name', 'productName', 'title', 'name'], '演出门票'),
    venue: pick(it, ['venue_name', 'venueName', 'venue'], ''),
    sessionTime: pick(it, ['session_time', 'sessionTime', 'show_time', 'showTime'], ''),
    tier: pick(it, ['tier_name', 'tierName', 'tier', 'price_name'], ''),
    price: Number(pick(it, ['price', 'unit_price', 'unitPrice'], 0)) || 0,
    quantity: Number(pick(it, ['quantity', 'count', 'num'], 1)) || 1,
    cover: pick(it, ['cover_url', 'coverUrl', 'cover'], ''),
    invalid: pick(it, ['invalid', 'is_invalid'], false) === true || pick(it, ['status'], '') === 'invalid'
  }
}

const items = computed(() => cart.items.map(itemOf))

const allChecked = computed({
  get: () => items.value.length > 0 && cart.selectedIds.length === items.value.length,
  set: (v) => (v ? cart.selectAll() : cart.clearSelection())
})

const totalAmount = computed(() =>
  items.value
    .filter((it) => cart.selectedIds.includes(it.id))
    .reduce((sum, it) => sum + it.price * it.quantity, 0)
)

async function load() {
  backendDown.value = false
  try {
    await cart.load()
  } catch (e) {
    backendDown.value = true
    cart.items = []
  }
}

async function remove(it) {
  try {
    await ElMessageBox.confirm('确定要移除该商品吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await cart.remove(it.id)
    ElMessage.success('已移除')
  } catch (e) {
    ElMessage.error('移除失败')
  }
}

async function checkout() {
  if (!cart.selectedIds.length) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }
  checkingOut.value = true
  try {
    const data = await checkoutCart({ cart_item_ids: cart.selectedIds })
    ElMessage.success('结算成功，请尽快支付')
    const orderId = data && (data.order_id || data.orderId || (Array.isArray(data) && data[0]))
    if (orderId) router.push(`/order/${orderId}`)
    else router.push('/orders')
  } catch (e) {
    ElMessage.error((e && e.message) || '结算失败，请稍后重试')
  } finally {
    checkingOut.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <div class="section-title" style="margin-top: 0"><h2>购物车</h2></div>

    <el-alert
      v-if="backendDown"
      class="mb-16"
      type="warning"
      :closable="false"
      show-icon
      title="购物车服务暂未连接，请确认后端已启动"
    />

    <EmptyState
      v-if="!items.length"
      description="购物车还是空的，去挑一场喜欢的演出吧"
      icon="🛒"
      action-text="去逛逛"
      @action="router.push('/')"
    />

    <template v-else>
      <div class="panel">
        <table>
          <thead>
            <tr>
              <th style="width: 46px">
                <el-checkbox v-model="allChecked" />
              </th>
              <th>演出信息</th>
              <th style="width: 120px">票档</th>
              <th style="width: 110px">单价</th>
              <th style="width: 120px">数量</th>
              <th style="width: 120px">小计</th>
              <th style="width: 90px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="it in items" :key="it.id" :class="{ 'row-invalid': it.invalid }">
              <td>
                <el-checkbox
                  :model-value="cart.selectedIds.includes(it.id)"
                  :disabled="it.invalid"
                  @change="cart.toggleSelect(it.id)"
                />
              </td>
              <td>
                <div class="ci-info">
                  <div class="ci-thumb" :style="it.cover ? '' : 'background: linear-gradient(135deg,#ffd3c2,#ffb199)'">
                    <img v-if="it.cover" :src="it.cover" alt="" />
                  </div>
                  <div>
                    <div style="font-weight: 600">{{ it.title }}</div>
                    <div class="muted">{{ it.venue }}<template v-if="it.sessionTime"> · {{ it.sessionTime }}</template></div>
                    <span v-if="it.invalid" class="tag gray mt-8">已失效</span>
                  </div>
                </div>
              </td>
              <td>{{ it.tier || '-' }}</td>
              <td class="price-strong">¥{{ it.price.toFixed(2) }}</td>
              <td>{{ it.quantity }}</td>
              <td class="price-strong">¥{{ (it.price * it.quantity).toFixed(2) }}</td>
              <td><span class="link" @click="remove(it)">删除</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="checkout-bar">
        <div class="between" style="max-width: 1200px; margin: 0 auto">
          <div>
            已选 <b class="price-strong">{{ cart.selectedIds.length }}</b> 件，合计
            <b class="price-strong" style="font-size: 20px">¥{{ totalAmount.toFixed(2) }}</b>
          </div>
          <button class="btn btn-primary" style="min-width: 160px" :disabled="checkingOut" @click="checkout">
            {{ checkingOut ? '结算中…' : '去结算' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.ci-info {
  display: flex;
  gap: 12px;
  align-items: center;
}
.ci-thumb {
  width: 76px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}
.ci-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.row-invalid {
  opacity: 0.55;
}
.checkout-bar {
  position: sticky;
  bottom: 0;
  margin-top: 18px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  border: 1px solid #e8d9c5;
  border-radius: var(--radius);
  padding: 14px 20px;
  box-shadow: var(--shadow);
}
</style>
