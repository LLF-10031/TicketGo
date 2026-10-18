import request from './request'

export function getCaptcha() {
  return request.get('/captcha', { silent: true })
}

export function sendSms(data) {
  return request.post('/sms/send', data)
}

export function login(data) {
  return request.post('/user/login', data, { silent: true })
}

export function register(data) {
  return request.post('/user/register', data, { silent: true })
}

export function getProfile() {
  return request.get('/user/profile')
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export function updateAvatar(avatarUrl) {
  return request.put('/user/avatar', { avatar_url: avatarUrl })
}

export function updatePassword(data) {
  return request.put('/user/password', data)
}

export function logout() {
  return request.post('/user/logout', {}, { silent: true })
}
