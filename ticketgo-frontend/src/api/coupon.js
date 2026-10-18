import request from './request'

export function getTemplates(params) {
  return request.get('/coupon/templates', { params })
}

export function receiveCoupon(templateId) {
  return request.post(`/coupon/receive/${templateId}`)
}

export function buyCoupon(data) {
  return request.post('/coupon/buy', data)
}

export function getMyCoupons(params) {
  return request.get('/coupon/my', { params })
}

export function getAvailableCoupons(data) {
  return request.post('/coupon/available', data, { silent: true })
}
