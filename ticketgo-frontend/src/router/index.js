import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { blank: true, title: '登录 / 注册' }
  },
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/product/:id',
    name: 'product-detail',
    component: () => import('@/views/ProductDetail.vue'),
    meta: { title: '演出详情' }
  },
  {
    path: '/seckill/:sessionId',
    name: 'seckill',
    component: () => import('@/views/Seckill.vue'),
    meta: { title: '秒杀抢票' }
  },
  {
    path: '/seat/:sessionId',
    name: 'seat',
    component: () => import('@/views/SeatSelect.vue'),
    meta: { title: '选座购票', requireAuth: true }
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('@/views/Cart.vue'),
    meta: { title: '购物车', requireAuth: true }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/views/Orders.vue'),
    meta: { title: '我的订单', requireAuth: true }
  },
  {
    path: '/order/:id',
    name: 'order-detail',
    component: () => import('@/views/OrderDetail.vue'),
    meta: { title: '订单详情', requireAuth: true }
  },
  {
    path: '/coupons',
    name: 'coupons',
    component: () => import('@/views/Coupons.vue'),
    meta: { title: '券中心' }
  },
  {
    path: '/my-coupons',
    name: 'my-coupons',
    component: () => import('@/views/MyCoupons.vue'),
    meta: { title: '我的券包', requireAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心', requireAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · TicketGo` : 'TicketGo · 演出票务平台'
  const userStore = useUserStore()
  if (to.meta.requireAuth && !userStore.isLogin) {
    ElMessage.warning('请先登录')
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
