<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductList } from '@/api/product'
import { listOf, normalizeProduct, totalOf } from '@/utils/normalize'
import ShowCard from '@/components/ShowCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const backendDown = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(8)
const category = ref('')
const keyword = ref('')

const entries = [
  { ic: '🎤', t: '演唱会', category: '演唱会' },
  { ic: '🎭', t: '话剧歌剧', category: '话剧歌剧' },
  { ic: '🏀', t: '体育赛事', category: '体育赛事' },
  { ic: '🖼️', t: '展览休闲', category: '展览休闲' },
  { ic: '⚡', t: '今日开票', category: '' },
  { ic: '🎟️', t: '券中心', path: '/coupons' }
]

const demoData = [
  { id: 1, name: '周杰伦「嘉年华」世界巡回演唱会', city: '北京', venue_name: '国家体育场', min_price: 580, status: 'hot' },
  { id: 2, name: '五月天「诺亚方舟」巡回演唱会', city: '上海', venue_name: '梅赛德斯奔驰文化中心', min_price: 380, sale_start_time: '2026-10-01 20:00' },
  { id: 3, name: '话剧《暗恋桃花源》经典重演', city: '杭州', venue_name: '杭州大剧院', min_price: 280, category: '话剧歌剧' },
  { id: 4, name: 'CBA 总决赛 G5 现场观赛', city: '广州', venue_name: '广州体育馆', min_price: 199, status: 'soldout', category: '体育赛事' },
  { id: 5, name: '音乐节 2026 春季阵容公布', city: '成都', venue_name: '露天音乐公园', min_price: 320, status: 'hot' },
  { id: 6, name: '沉浸式梵高艺术展', city: '深圳', venue_name: '当代艺术中心', min_price: 128, category: '展览休闲' },
  { id: 7, name: '林俊杰「圣所」巡回演唱会', city: '南京', venue_name: '奥体中心', min_price: 480, sale_start_time: '2026-11-01 20:00' },
  { id: 8, name: '《茶馆》北京人艺经典版', city: '北京', venue_name: '首都剧场', min_price: 260, category: '话剧歌剧' }
]

async function fetchList() {
  loading.value = true
  backendDown.value = false
  try {
    const data = await getProductList({
      category: category.value || undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      page_size: pageSize.value
    })
    const arr = listOf(data)
    if (!arr.length && page.value === 1 && !category.value && !keyword.value) {
      // 后端返回空 → 展示演示数据，避免空白
      list.value = demoData.map((d, i) => normalizeProduct(d, i))
      total.value = demoData.length
    } else {
      list.value = arr.map((d, i) => normalizeProduct(d, i))
      total.value = totalOf(data, list.value.length)
    }
  } catch (e) {
    backendDown.value = true
    list.value = demoData.map((d, i) => normalizeProduct(d, i))
    total.value = demoData.length
  } finally {
    loading.value = false
  }
}

function goEntry(item) {
  if (item.path) {
    router.push(item.path)
    return
  }
  category.value = item.category
  page.value = 1
  router.push({ path: '/', query: item.category ? { category: item.category } : {} })
}

function onPageChange(p) {
  page.value = p
  fetchList()
}

function resetCategory() {
  category.value = ''
  router.push({ path: '/', query: keyword.value ? { keyword: keyword.value } : {} })
}

watch(
  () => route.query,
  (q) => {
    category.value = q.category || ''
    keyword.value = q.keyword || ''
    page.value = 1
    fetchList()
  },
  { immediate: false }
)

onMounted(() => {
  category.value = route.query.category || ''
  keyword.value = route.query.keyword || ''
  fetchList()
})
</script>

<template>
  <div class="container page">
    <section class="banner">
      <div>
        <h1>2026 全国巡回演唱会</h1>
        <p>开票倒计时 · 立即预约，抢先锁定好位置</p>
        <button class="btn-light" @click="router.push('/product/1')">查看详情</button>
      </div>
    </section>

    <div class="entries">
      <div v-for="e in entries" :key="e.t" class="entry" @click="goEntry(e)">
        <div class="ic">{{ e.ic }}</div>
        <div class="t">{{ e.t }}</div>
      </div>
    </div>

    <div class="section-title">
      <h2>
        {{ category ? category : keyword ? `“${keyword}” 的搜索结果` : '热门演出' }}
      </h2>
      <a v-if="category || keyword" class="more" @click="resetCategory">返回全部 ›</a>
    </div>

    <el-alert
      v-if="backendDown"
      class="mb-16"
      type="warning"
      :closable="false"
      show-icon
      title="后端服务暂未连接（http://localhost:8080），当前展示演示数据"
    />

    <div v-if="loading" class="grid">
      <div v-for="n in 8" :key="n" class="card">
        <div class="cover" style="background: linear-gradient(135deg, #f3e7db, #ece0d3)"></div>
        <div class="card-body">
          <el-skeleton :rows="2" animated />
        </div>
      </div>
    </div>

    <EmptyState
      v-else-if="!list.length"
      description="没有找到相关演出，换个关键词试试吧"
      icon="🔍"
    />

    <div v-else class="grid">
      <ShowCard v-for="p in list" :key="p.id" :product="p" />
    </div>

    <div v-if="!loading && total > pageSize" class="center mt-24">
      <el-pagination
        layout="prev, pager, next"
        background
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>
