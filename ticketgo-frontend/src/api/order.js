import request from './request'

export function getOrderList(params) {
  return request.get('/order/list', { params })
}

export function getOrder(id) {
  return request.get(`/order/${id}`)
}

export function cancelOrder(id) {
  return request.post(`/order/${id}/cancel`)
}

export function refundOrder(id) {
  return request.post(`/order/${id}/refund`)
}

export function pay(data) {
  return request.post('/payment/pay', data)
}
