export function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

export function toDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  if (typeof value === 'number') return new Date(value)
  const s = String(value).replace(/-/g, '/').replace('T', ' ').replace(/\.\d+Z?$/, '')
  const d = new Date(s)
  return Number.isNaN(d.getTime()) ? null : d
}

export function formatDateTime(value) {
  const d = toDate(value)
  if (!d) return value ? String(value) : '-'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatDate(value) {
  const d = toDate(value)
  if (!d) return value ? String(value) : '-'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function formatMoney(value) {
  const n = Number(value)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

const ORDER_STATUS_MAP = {
  pending: { text: '待支付', cls: 'pending' },
  unpaid: { text: '待支付', cls: 'pending' },
  0: { text: '待支付', cls: 'pending' },
  paid: { text: '已支付', cls: 'paid' },
  1: { text: '已支付', cls: 'paid' },
  cancelled: { text: '已取消', cls: 'cancelled' },
  canceled: { text: '已取消', cls: 'cancelled' },
  2: { text: '已取消', cls: 'cancelled' },
  refunded: { text: '已退款', cls: 'refunded' },
  3: { text: '已退款', cls: 'refunded' },
  refunding: { text: '退款中', cls: 'refunded' }
}

export function orderStatus(status) {
  const key = status === null || status === undefined ? '' : String(status).toLowerCase()
  return ORDER_STATUS_MAP[key] || { text: status || '未知', cls: 'pending' }
}

export function pick(obj, keys, fallback = undefined) {
  if (!obj) return fallback
  for (const k of keys) {
    if (obj[k] !== undefined && obj[k] !== null && obj[k] !== '') return obj[k]
  }
  return fallback
}
