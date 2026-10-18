import request from './request'

export function getProductList(params) {
  return request.get('/product/list', { params, silent: true })
}

export function getProduct(id) {
  return request.get(`/product/${id}`)
}

export function getSessions(productId) {
  return request.get(`/product/${productId}/sessions`)
}

export function getSession(sessionId) {
  return request.get(`/session/${sessionId}`)
}

export function getTiers(sessionId) {
  return request.get(`/session/${sessionId}/tiers`)
}
