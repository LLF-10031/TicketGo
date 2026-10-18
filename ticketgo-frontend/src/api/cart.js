import request from './request'

export function getCart() {
  return request.get('/cart')
}

export function addToCart(data) {
  return request.post('/cart/add', data)
}

export function removeCartItem(id) {
  return request.delete(`/cart/${id}`)
}

export function checkoutCart(data) {
  return request.post('/cart/checkout', data)
}
