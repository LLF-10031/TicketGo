<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const categories = [
  { label: '首页', path: '/', category: '' },
  { label: '演唱会', path: '/', category: '演唱会' },
  { label: '话剧歌剧', path: '/', category: '话剧歌剧' },
  { label: '体育赛事', path: '/', category: '体育赛事' },
  { label: '展览休闲', path: '/', category: '展览休闲' },
  { label: '我的订单', path: '/orders' },
  { label: '券中心', path: '/coupons' }
]

const currentCategory = computed(() => route.query.category || '')

function isActive(item) {
  if (item.path === '/orders') return route.path === '/orders'
  if (item.path === '/coupons') return route.path === '/coupons'
  if (item.path !== '/') return false
  if (route.path !== '/') return false
  return item.category === currentCategory.value
}

function go(item) {
  if (item.category) router.push({ path: '/', query: { category: item.category } })
  else router.push(item.path)
}

function onSearch() {
  router.push({ path: '/', query: keyword.value ? { keyword: keyword.value } : {} })
}

function goPath(path) {
  router.push(path)
}

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="header">
    <div class="header-inner">
      <div class="logo" @click="goPath('/')"><span class="dot"></span>TicketGo</div>
      <div class="search">
        <input
          v-model="keyword"
          placeholder="搜索演出、艺人、场馆"
          @keyup.enter="onSearch"
        />
        <button @click="onSearch">搜索</button>
      </div>
      <div class="header-right">
        <span class="icon" @click="goPath('/orders')">我的订单</span>
        <span class="icon" @click="goPath('/coupons')">优惠券</span>
        <template v-if="userStore.isLogin">
          <div class="avatar-wrap">
            <div class="avatar">
              <img v-if="userStore.avatar" :src="userStore.avatar" alt="avatar" />
              <span v-else>{{ userStore.initial }}</span>
            </div>
            <div class="dropdown">
              <div class="dropdown-head">
                <div class="avatar">
                  <img v-if="userStore.avatar" :src="userStore.avatar" alt="avatar" />
                  <span v-else>{{ userStore.initial }}</span>
                </div>
                <div>
                  <div class="name">{{ userStore.displayName }}</div>
                  <div class="sub">欢迎回来</div>
                </div>
              </div>
              <a @click="goPath('/profile')">个人中心</a>
              <a @click="goPath('/orders')">我的订单</a>
              <a @click="goPath('/my-coupons')">我的券包</a>
              <a @click="goPath('/cart')">购物车</a>
              <a class="logout" @click="onLogout">退出登录</a>
            </div>
          </div>
        </template>
        <a v-else class="login-link" @click="goPath('/login')">登录 / 注册</a>
      </div>
    </div>
  </header>
  <nav class="nav">
    <div class="nav-inner">
      <a
        v-for="item in categories"
        :key="item.label"
        :class="{ active: isActive(item) }"
        @click="go(item)"
        >{{ item.label }}</a
      >
    </div>
  </nav>
</template>
