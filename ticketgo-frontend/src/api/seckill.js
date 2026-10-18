import request from './request'

export function seckill(sessionId, tierId) {
  return request.post(`/seckill/${sessionId}/${tierId}`, {}, { silent: true })
}
