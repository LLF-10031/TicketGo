<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProduct, getSessions, getTiers } from '@/api/product'
import { listOf, normalizeProduct, normalizeSession, normalizeTier } from '@/utils/normalize'
import { formatDateTime, toDate } from '@/utils/format'
import CountdownTimer from '@/components/CountdownTimer.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const backendDown = ref(false)
const product = ref(null)
const sessions = ref([])
const tiers = ref([])
const activeSessionId = ref(null)
const activeTierId = ref(null)
const tiersLoading = ref(false)

const activeSession = computed(() => sessions.value.find((s) => s.id === activeSessionId.value) || null)
const activeTier = computed(() => tiers.value.find((t) => t.id === activeTierId.value) || null)

const saleStartDate = computed(() => toDate(product.value && product.value.saleStart))
const notOnSale = computed(() => {
  if (!saleStartDate.value) return false
  return saleStartDate.value.getTime() > Date.now()
})
const soldOut = computed(() => !!(product.value && product.value.soldOut))

const demoProduct = {
  id: route.params.id,
  name: '周杰伦「嘉年华」世界巡回演唱会 · 北京站',
  city: '北京',
  venue_name: '国家体育场（鸟巢）',
  min_price: 580,
  category: '演唱会',
  sale_start_time: '2027-01-01 20:00'
}
const demoSessions = [
  { id: 101, show_time: '2026-06-20 19:30', venue_name: '国家体育场（鸟巢）' },
  { id: 102, show_time: '2026-06-21 19:30', venue_name: '国家体育场（鸟巢）' },
  { id: 103, show_time: '2026-06-22 19:30', venue_name: '国家体育场（鸟巢）' }
]
const demoTiers = [
  { id: 201, name: '看台票', price: 580, stock: 999, description: '余票充足' },
  { id: 202, name: '内场票', price: 980, stock: 30, description: '仅剩 30 张' },
  { id: 203, name: 'VIP 票', price: 1580, stock: 10, description: '含周边礼包' }
]

async function loadTiers(sessionId) {
  if (!sessionId) {
    tiers.value = []
    return
  }
  tiersLoading.value = true
  try {
    const data = await getTiers(sessionId)
    const arr = listOf(data)
    tiers.value = arr.length ? arr.map(normalizeTier) : demoTiers.map(normalizeTier)
  } catch (e) {
    tiers.value = demoTiers.map(normalizeTier)
  } finally {
    if (tiers.value.length && !tiers.value.some((t) => t.id === activeTierId.value)) {
      activeTierId.value = tiers.value[0].id
    }
    tiersLoading.value = false
  }
}

async function loadSessions(productId) {
  try {
    const data = await getSessions(productId)
    const arr = listOf(data)
    sessions.value = arr.length ? arr.map(normalizeSession) : demoSessions.map(normalizeSession)
  } catch (e) {
    sessions.value = demoSessions.map(normalizeSession)
  }
  if (sessions.value.length) {
    activeSessionId.value = sessions.value[0].id
    await loadTiers(activeSessionId.value)
  }
}

async function load() {
  loading.value = true
  try {
    const data = await getProduct(route.params.id)
    product.value = data ? normalizeProduct(data) : normalizeProduct(demoProduct)
  } catch (e) {
    backendDown.value = true
    product.value = normalizeProduct(demoProduct)
  }
  await loadSessions(route.params.id)
  loading.value = false
}

function selectSession(s) {
  if (activeSessionId.value === s.id) return
  activeSessionId.value = s.id
  activeTierId.value = null
  loadTiers(s.id)
}

function goSeckill() {
  if (!activeSessionId.value) return null
  router.push({
    path: `/seckill/${activeSessionId.value}`,
    query: activeTierId.value ? { tierId: activeTierId.value } : {}
  })
}

function goSeat() {
  if (!activeSessionId.value) return
  router.push({
    path: `/seat/${activeSessionId.value}`,
    query: activeTierId.value ? { tierId: activeTierId.value } : {}
  })
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="product">
      <el-alert
        v-if="backendDown"
        class="mb-16"
        type="warning"
        :closable="false"
        show-icon
        title="后端服务暂未连接，当前展示演示数据"
      />

      <div class="detail-top">
        <div class="detail-poster">
          <img v-if="product.cover" :src="product.cover" :alt="product.title" />
        </div>
        <div class="detail-info">
          <h1>{{ product.title }}</h1>
          <div class="meta">
            <div>📍 {{ product.city ? product.city + ' · ' : '' }}{{ product.venue || '场馆待定' }}</div>
            <div v-if="activeSession">🕐 {{ formatDateTime(activeSession.time) }}</div>
            <div v-if="product.saleStart">🎫 开票时间：{{ formatDateTime(product.saleStart) }}</div>
            <div class="mt-8">
              <span v-if="soldOut" class="tag gray">已售罄</span>
              <span v-else-if="notOnSale" class="tag">即将开票</span>
              <span v-else class="tag">热卖中</span>
              <span class="tag gray" style="margin-left: 6px">实名购票</span>
            </div>
          </div>
          <div v-if="notOnSale" class="mt-16">
            <div class="muted mb-8">距离开票还有</div>
            <CountdownTimer :target="product.saleStart" class="price-strong" style="font-size: 24px" />
          </div>
        </div>
      </div>

      <div class="section-title"><h2>选择场次与票档</h2></div>

      <div v-if="sessions.length" class="row gap-8" style="flex-wrap: wrap; margin-bottom: 16px">
        <span
          v-for="s in sessions"
          :key="s.id"
          class="tag"
          :class="{ gray: s.id !== activeSessionId }"
          style="cursor: pointer; padding: 7px 14px"
          @click="selectSession(s)"
        >
          {{ formatDateTime(s.time) }}
        </span>
      </div>
      <EmptyState v-else description="暂无可售场次" icon="📅" />

      <div v-if="tiersLoading" class="muted">票档加载中…</div>
      <template v-else>
        <div
          v-for="t in tiers"
          :key="t.id"
          class="tier-row"
          :class="{ active: t.id === activeTierId }"
          @click="activeTierId = t.id"
        >
          <div>
            <b>{{ t.name }}</b>
            <span class="tag" :class="{ gray: !t.stock }" style="margin-left: 8px">
              {{ t.stock > 10 ? '余票充足' : t.stock > 0 ? `仅剩 ${t.stock} 张` : '已售罄' }}
            </span>
            <span v-if="t.desc" class="muted" style="margin-left: 8px">{{ t.desc }}</span>
          </div>
          <div class="price">¥{{ t.price }}</div>
        </div>
        <EmptyState v-if="!tiers.length" description="暂无票档信息" icon="🎫" />
      </template>

      <div class="buybar">
        <button class="btn btn-primary" style="flex: 1" :disabled="soldOut || notOnSale" @click="goSeckill">
          免选座抢票
        </button>
        <button class="btn btn-ghost" style="flex: 1" :disabled="soldOut || notOnSale" @click="goSeat">
          选座购票
        </button>
      </div>
    </template>

    <EmptyState v-else description="演出不存在或已下架" icon="🎭" action-text="返回首页" @action="router.push('/')" />
  </div>
</template>
