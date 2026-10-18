import axios from 'axios'
import { ElMessage } from 'element-plus'
import auth from '@/utils/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：注入 Authorization 与 X-User-Id
request.interceptors.request.use(
  (config) => {
    const token = auth.getToken()
    if (token) config.headers.Authorization = `Bearer ${token}`
    const userId = auth.getUserId()
    if (userId) config.headers['X-User-Id'] = userId
    return config
  },
  (error) => Promise.reject(error)
)

function handleUnauthorized() {
  auth.clearAuth()
  if (!window.location.pathname.startsWith('/login')) {
    ElMessage.warning('登录已过期，请重新登录')
    window.location.href = '/login'
  }
}

// 响应拦截：统一处理 code !== 200
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (Number(body.code) === 200) return body.data
      if (Number(body.code) === 401) {
        handleUnauthorized()
      } else if (!response.config.silent) {
        ElMessage.error(body.message || '请求失败')
      }
      return Promise.reject(Object.assign(new Error(body.message || '请求失败'), { code: body.code }))
    }
    // 非统一响应体（如直接返回数组/对象）原样返回
    return body
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    const body = error.response && error.response.data
    const msg =
      (body && (body.message || body.error)) ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '') ||
      '网络异常，请确认后端服务已启动'
    if (!error.config || !error.config.silent) {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request
