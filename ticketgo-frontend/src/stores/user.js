import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import auth from '@/utils/auth'
import * as userApi from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(auth.getToken())
  const refreshToken = ref(auth.getRefreshToken())
  const profile = ref(auth.getProfile())

  const isLogin = computed(() => !!accessToken.value)
  const displayName = computed(() => {
    const p = profile.value || {}
    return p.nick_name || p.nickName || p.nickname || p.username || p.phone || '用户'
  })
  const avatar = computed(() => {
    const p = profile.value || {}
    return p.avatar_url || p.avatarUrl || p.avatar || ''
  })
  const initial = computed(() => {
    const name = displayName.value || 'U'
    return String(name).slice(-1)
  })

  function setAuth(payload) {
    const p = payload || {}
    const at = p.access_token || p.accessToken || p.token || ''
    const rt = p.refresh_token || p.refreshToken || ''
    const u = p.user || p.user_info || p.userInfo || p.profile || null
    accessToken.value = at
    refreshToken.value = rt
    auth.setToken(at)
    auth.setRefreshToken(rt)
    if (u) {
      profile.value = u
      auth.setProfile(u)
    }
    return at
  }

  async function loginByPassword(phone, password) {
    const data = await userApi.login({ phone, password })
    setAuth(data)
    if (!profile.value) {
      try {
        await fetchProfile()
      } catch (e) {
        /* 后端未实现 profile 时忽略 */
      }
    }
    return data
  }

  async function loginByCode(phone, code) {
    const data = await userApi.login({ phone, code })
    setAuth(data)
    if (!profile.value) {
      try {
        await fetchProfile()
      } catch (e) {
        /* ignore */
      }
    }
    return data
  }

  async function register(data) {
    return userApi.register(data)
  }

  async function fetchProfile() {
    const data = await userApi.getProfile()
    if (data) {
      profile.value = data
      auth.setProfile(data)
    }
    return data
  }

  function updateLocalProfile(patch) {
    profile.value = { ...(profile.value || {}), ...(patch || {}) }
    auth.setProfile(profile.value)
  }

  function logout() {
    userApi.logout().catch(() => {})
    accessToken.value = ''
    refreshToken.value = ''
    profile.value = null
    auth.clearAuth()
  }

  return {
    accessToken,
    refreshToken,
    profile,
    isLogin,
    displayName,
    avatar,
    initial,
    setAuth,
    loginByPassword,
    loginByCode,
    register,
    fetchProfile,
    updateLocalProfile,
    logout
  }
})
