import request from './request'

export function getSeats(sessionId, params) {
  return request.get(`/session/${sessionId}/seats`, { params })
}

export function lockSeats(data) {
  return request.post('/seat/lock', data)
}

export function unlockSeats(data) {
  return request.post('/seat/unlock', data, { silent: true })
}
