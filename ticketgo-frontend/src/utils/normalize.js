import { pick } from './format'

function asArray(data) {
  if (Array.isArray(data)) return data
  if (!data) return []
  for (const k of ['items', 'list', 'records', 'rows', 'data', 'content']) {
    if (Array.isArray(data[k])) return data[k]
  }
  return []
}

export function listOf(data) {
  return asArray(data)
}

export function totalOf(data, fallback = 0) {
  if (data && typeof data === 'object' && data.total !== undefined) return Number(data.total) || 0
  return fallback
}

const COVER_CLASSES = ['', 'c2', 'c3', 'c4']

export function normalizeProduct(raw, index = 0) {
  if (!raw) return null
  const status = pick(raw, ['status', 'sale_status', 'state'], '')
  const soldOut =
    status === 'soldout' || status === 'sold_out' || status === '售罄' || raw.sold_out === true
  const hot = pick(raw, ['is_hot', 'hot'], false) || status === 'hot'
  const saleStart = pick(raw, ['sale_start_time', 'sale_time', 'open_time', 'saleStartTime'])
  return {
    id: pick(raw, ['id', 'product_id', 'productId']),
    title: pick(raw, ['name', 'title', 'product_name', 'productName'], '未命名演出'),
    venue: pick(raw, ['venue_name', 'venueName', 'venue', 'place'], ''),
    city: pick(raw, ['city', 'city_name', 'cityName'], ''),
    category: pick(raw, ['category', 'category_name', 'categoryName', 'type'], ''),
    price: pick(raw, ['min_price', 'minPrice', 'price', 'starting_price', 'lowest_price'], 0),
    cover: pick(raw, ['cover_url', 'coverUrl', 'cover', 'thumb_url', 'thumbUrl', 'poster_url', 'posterUrl', 'image_url', 'imageUrl'], ''),
    saleStart,
    status,
    soldOut,
    hot,
    coverClass: COVER_CLASSES[index % COVER_CLASSES.length],
    raw
  }
}

export function normalizeSession(raw) {
  if (!raw) return null
  return {
    id: pick(raw, ['id', 'session_id', 'sessionId']),
    time: pick(raw, ['show_time', 'showTime', 'start_time', 'startTime', 'session_time', 'play_time', 'playTime', 'time']),
    venue: pick(raw, ['venue_name', 'venueName', 'venue', 'place'], ''),
    name: pick(raw, ['name', 'session_name', 'sessionName', 'title'], ''),
    status: pick(raw, ['status', 'sale_status', 'state'], ''),
    saleStart: pick(raw, ['sale_start_time', 'saleStartTime', 'sale_time', 'open_time']),
    soldOut: pick(raw, ['sold_out', 'soldOut'], false) === true,
    raw
  }
}

export function normalizeTier(raw) {
  if (!raw) return null
  return {
    id: pick(raw, ['id', 'tier_id', 'tierId']),
    name: pick(raw, ['name', 'tier_name', 'tierName', 'title'], '票档'),
    price: Number(pick(raw, ['price', 'sale_price', 'salePrice', 'amount'], 0)) || 0,
    stock: Number(pick(raw, ['stock', 'remaining', 'remain_stock', 'remainStock', 'available_stock', 'left'], 0)) || 0,
    desc: pick(raw, ['description', 'desc', 'remark'], ''),
    raw
  }
}

const ORDER_STATUS_TEXT = {
  pending: 'pending',
  unpaid: 'pending',
  0: 'pending',
  wait_pay: 'pending',
  paid: 'paid',
  1: 'paid',
  cancelled: 'cancelled',
  canceled: 'cancelled',
  2: 'cancelled',
  refunded: 'refunded',
  refunding: 'refunded',
  3: 'refunded'
}

export function normalizeOrderStatus(status) {
  const key = status === null || status === undefined ? '' : String(status).toLowerCase()
  return ORDER_STATUS_TEXT[key] || 'pending'
}

export function normalizeOrder(raw) {
  if (!raw) return null
  const items = asArray(pick(raw, ['items', 'order_items', 'orderItems', 'details'], []))
  const first = items[0] || {}
  return {
    id: pick(raw, ['id', 'order_id', 'orderId']),
    orderNo: pick(raw, ['order_no', 'orderNo', 'id', 'order_id', 'orderId'], ''),
    status: normalizeOrderStatus(pick(raw, ['status', 'order_status', 'orderStatus'])),
    rawStatus: pick(raw, ['status', 'order_status', 'orderStatus']),
    amount: Number(pick(raw, ['pay_amount', 'payAmount', 'total_amount', 'totalAmount', 'amount'], 0)) || 0,
    originAmount: Number(pick(raw, ['total_amount', 'totalAmount', 'origin_amount', 'originAmount', 'amount'], 0)) || 0,
    discount: Number(pick(raw, ['discount_amount', 'discountAmount', 'coupon_amount', 'couponAmount'], 0)) || 0,
    couponName: pick(raw, ['coupon_name', 'couponName', 'coupon'], ''),
    createdAt: pick(raw, ['create_time', 'createTime', 'created_at', 'createdAt']),
    expireAt: pick(raw, ['pay_expire_at', 'payExpireAt', 'expire_at', 'expireAt', 'expire_time', 'expireTime']),
    payTime: pick(raw, ['pay_time', 'payTime', 'paid_at', 'paidAt']),
    title: pick(raw, ['product_name', 'productName', 'show_name', 'showName', 'title'], pick(first, ['product_name', 'name', 'title'], '演出门票')),
    venue: pick(raw, ['venue_name', 'venueName', 'venue'], pick(first, ['venue_name', 'venue'], '')),
    sessionTime: pick(raw, ['session_time', 'sessionTime', 'show_time', 'showTime'], pick(first, ['show_time', 'session_time', 'time'], '')),
    seatInfo: pick(raw, ['seat_info', 'seatInfo', 'seats', 'seat'], pick(first, ['seat_info', 'seat_name', 'seat'], '')),
    ticketCode: pick(raw, ['ticket_code', 'ticketCode', 'verify_code', 'verifyCode', 'ticket_no']),
    quantity: Number(pick(raw, ['quantity', 'count'], first.quantity || 1)) || 1,
    cover: pick(raw, ['cover_url', 'coverUrl', 'cover'], pick(first, ['cover_url', 'cover'], '')),
    items,
    raw
  }
}

export function normalizeCoupon(raw) {
  if (!raw) return null
  const status = pick(raw, ['status', 'use_status', 'useStatus'], 'unused')
  return {
    id: pick(raw, ['id', 'user_coupon_id', 'userCouponId', 'coupon_id', 'couponId']),
    templateId: pick(raw, ['template_id', 'templateId', 'coupon_template_id']),
    name: pick(raw, ['name', 'coupon_name', 'couponName', 'title', 'template_name'], '优惠券'),
    amount: Number(pick(raw, ['discount_amount', 'discountAmount', 'amount', 'value', 'face_value'], 0)) || 0,
    threshold: Number(pick(raw, ['threshold', 'min_amount', 'minAmount', 'condition_amount', 'conditionAmount', 'use_threshold'], 0)) || 0,
    scope: pick(raw, ['scope_type', 'scopeType', 'scope', 'scope_name', 'scopeName', 'category'], '全部演出'),
    expireAt: pick(raw, ['expire_at', 'expireAt', 'end_time', 'endTime', 'valid_end', 'validEnd', 'expire_time']),
    startAt: pick(raw, ['start_time', 'startTime', 'valid_start', 'validStart']),
    status: String(status),
    stock: pick(raw, ['stock', 'remaining', 'remain_stock']),
    limitType: pick(raw, ['limit_type', 'limitType', 'type'], ''),
    received: pick(raw, ['received', 'is_received', 'isReceived'], false),
    raw
  }
}

export function couponStatusText(status) {
  const s = String(status || '').toLowerCase()
  if (['used', '1', '已使用'].includes(s)) return { text: '已使用', cls: 'gray' }
  if (['expired', '2', '已过期'].includes(s)) return { text: '已过期', cls: 'gray' }
  return { text: '未使用', cls: '' }
}

export function normalizeSeat(raw) {
  if (!raw) return null
  const status = pick(raw, ['status', 'seat_status', 'seatStatus', 'state'], 'available')
  return {
    id: pick(raw, ['id', 'seat_id', 'seatId']),
    row: pick(raw, ['row_no', 'rowNo', 'row', 'row_name', 'rowName'], ''),
    col: pick(raw, ['col_no', 'colNo', 'col', 'seat_no', 'seatNo', 'col_name'], ''),
    x: Number(pick(raw, ['x', 'pos_x', 'posX', 'coordinate_x'], 0)) || 0,
    y: Number(pick(raw, ['y', 'pos_y', 'posY', 'coordinate_y'], 0)) || 0,
    area: pick(raw, ['area', 'area_name', 'zone', 'section'], ''),
    tierId: pick(raw, ['tier_id', 'tierId', 'price_tier_id']),
    price: Number(pick(raw, ['price', 'amount'], 0)) || 0,
    status: String(status).toLowerCase(),
    raw
  }
}

export function seatStatusOf(seat) {
  const s = seat.status
  if (['available', 'free', '0', 'normal', 'on'].includes(s)) return 'available'
  if (['sold', '1', 'occupied'].includes(s)) return 'sold'
  if (['locked', '2', 'lock'].includes(s)) return 'locked'
  return 'available'
}
