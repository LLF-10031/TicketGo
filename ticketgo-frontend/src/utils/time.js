import axios from 'axios'

// 以服务端时间为准：读取任意接口响应头的 Date 计算本地时钟偏移
let offset = 0
let synced = false

export async function syncServerTime() {
  try {
    const res = await axios.get('/api/product/list', {
      params: { page: 1, page_size: 1 },
      timeout: 4000
    })
    const dateHeader = res.headers && (res.headers.date || res.headers.Date)
    if (dateHeader) {
      const server = new Date(dateHeader).getTime()
      if (!Number.isNaN(server)) {
        offset = server - Date.now()
        synced = true
      }
    }
  } catch (e) {
    // 后端未就绪时保持本地时间
  }
  return offset
}

export function serverNow() {
  return Date.now() + offset
}

export function hasSynced() {
  return synced
}
