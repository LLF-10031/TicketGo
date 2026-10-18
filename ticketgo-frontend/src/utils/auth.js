const TOKEN_KEY = 'tg_access_token'
const REFRESH_KEY = 'tg_refresh_token'
const PROFILE_KEY = 'tg_profile'
const USER_ID_KEY = 'tg_user_id'

function read(key) {
  try {
    return localStorage.getItem(key)
  } catch (e) {
    return null
  }
}

function write(key, value) {
  try {
    if (value === null || value === undefined || value === '') localStorage.removeItem(key)
    else localStorage.setItem(key, value)
  } catch (e) {
    /* ignore quota / privacy mode errors */
  }
}

export function getToken() {
  return read(TOKEN_KEY) || ''
}
export function setToken(token) {
  write(TOKEN_KEY, token)
}
export function getRefreshToken() {
  return read(REFRESH_KEY) || ''
}
export function setRefreshToken(token) {
  write(REFRESH_KEY, token)
}
export function getUserId() {
  return read(USER_ID_KEY) || ''
}
export function setUserId(id) {
  write(USER_ID_KEY, id)
}
export function getProfile() {
  const raw = read(PROFILE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}
export function setProfile(profile) {
  if (!profile) {
    write(PROFILE_KEY, '')
    write(USER_ID_KEY, '')
    return
  }
  write(PROFILE_KEY, JSON.stringify(profile))
  const id = profile.id || profile.user_id || profile.userId
  if (id) setUserId(id)
}

export function clearAuth() {
  write(TOKEN_KEY, '')
  write(REFRESH_KEY, '')
  write(PROFILE_KEY, '')
  write(USER_ID_KEY, '')
}

export default {
  getToken,
  setToken,
  getRefreshToken,
  setRefreshToken,
  getUserId,
  setUserId,
  getProfile,
  setProfile,
  clearAuth
}
