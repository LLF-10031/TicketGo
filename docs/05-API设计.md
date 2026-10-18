# TicketGo — 演出/赛事票务抢票平台 — API 设计

> 版本：v0.1（草稿）
> 状态：基于已定决策，`[TBD]` 标记待定项

---

## 一、通用约定

| 项 | 约定 |
|---|---|
| 基础路径 | 网关统一前缀，各服务按 `/api/{service}/...` 分发 |
| 响应格式 | `{success: bool, data: any, error?: {code, message}}` |
| 鉴权 | JWT 双Token（Access Token 15min + Refresh Token 7d），请求头 `Authorization: Bearer {token}` |
| 分页 | `?page=1&page_size=20`，响应 `{total, items}` |
| 统一错误码 | `[TBD]` |

---

## 二、网关路由

| 路径前缀 | 目标服务 |
|---|---|
| `/api/user/**` | user-service |
| `/api/product/**` | product-service |
| `/api/seat/**` | seat-service |
| `/api/cart/**` | cart-service |
| `/api/seckill/**` | seckill-service |
| `/api/order/**` | order-service |
| `/api/payment/**` | payment-service |

---

## 三、用户服务（user-service）

### `GET /api/captcha` 获取图形验证码

Response: `{captcha_id, image_base64}`（Hutool 生成，答案存 Redis TTL 5min）

### `POST /api/sms/send` 发送短信验证码（Mock）

Body: `{phone, captcha_id, captcha_input}`（校验图形验证码，通过才发送；Mock 写 Redis + 日志）

### `POST /api/user/login` 登录

Body: `{phone, password}` 或 `{phone, code}`（密码 / 短信验证码二选一）

### `POST /api/user/register` 注册

Body: `{phone, password?, nick_name?}`

### `POST /api/user/refresh` 刷新 Token

Header: `refreshToken`

### `POST /api/user/logout` 登出

### `GET /api/user/profile` 获取用户信息

### `PUT /api/user/profile` 修改用户信息（昵称等）

### `PUT /api/user/avatar` 修改头像

Body: `{avatar_url}`（先经 `/api/file/upload` 上传，biz_type=avatar）

### `PUT /api/user/password` 修改密码

Body: `{old_password, new_password}`（验旧密码 + BCrypt 重哈希）

---

## 四、演出产品服务（product-service）

### `GET /api/product/list` 演出列表

Query: `?category=&status=&keyword=&page=1&page_size=20`

### `GET /api/product/{id}` 演出详情

### `GET /api/product/{id}/sessions` 场次列表

### `GET /api/session/{id}` 场次详情（含票档列表）

### `GET /api/session/{id}/tiers` 票档列表

---

## 五、座位服务（seat-service）

### `GET /api/session/{sessionId}/seats` 座位图

Query: `?tier_id=` 可选

### `POST /api/seat/lock` 锁座

Body: `{session_id, seat_ids, expire_seconds}`

### `POST /api/seat/unlock` 释放锁座

Body: `{session_id, seat_ids, lock_token}`

---

## 六、购物车服务（cart-service）

### `GET /api/cart` 购物车列表

### `POST /api/cart/add` 添加到购物车

Body: `{session_id, tier_id, seat_id?, quantity}`

### `DELETE /api/cart/{id}` 删除购物车项

### `POST /api/cart/checkout` 购物车结算

Body: `{cart_item_ids}`

---

## 七、秒杀服务（seckill-service）

### `POST /api/seckill/{sessionId}/{tierId}` 发起秒杀抢票

Header: `Authorization: Bearer {token}`

Response: `{success, order_id?, message}`

> 抢票成功返回 `order_id`，失败返回错误信息。订单通过 Kafka 异步落库，稍后可通过订单查询确认。

---

## 八、订单服务（order-service）

### `GET /api/order/list` 订单列表

Query: `?status=&page=1&page_size=20`

### `GET /api/order/{id}` 订单详情

### `POST /api/order/{id}/cancel` 取消订单（待支付状态）

### `POST /api/order/{id}/refund` 申请退款

---

## 九、支付服务（payment-service）

### `POST /api/payment/pay` 发起支付（Mock）

Body: `{order_id, pay_type}`

### `POST /api/payment/callback` 支付回调（Mock）

Body: `{order_id, trade_no, status}`

---

## 九·补 优惠券服务（coupon-service）

### `GET /api/coupon/templates` 券中心列表

Query: `?scope_type=&status=`

### `POST /api/coupon/receive/{templateId}` 领取限量券

Response: `{success, user_coupon_id?}`

### `POST /api/coupon/buy` 购买不限量券

Body: `{template_id, quantity}` → 生成券订单

### `GET /api/coupon/my` 我的券包

Query: `?status=unused&page=1`

### `POST /api/coupon/available` 下单时查询可用券

Body: `{product_id, amount}` → 返回可用券列表 + 各自抵扣后金额

---

## 九·补2 图片/文件服务

### `POST /api/file/upload` 上传图片

Body: `multipart/form-data`（file, biz_type）

Response: `{original_url, display_url, thumb_url}`

### `DELETE /api/file/{id}` 删除图片

---

## 九·补3 管理端接口（B端，API 不画页面）

### 演出/场次/票档
- `POST /api/admin/product` 创建演出
- `PUT /api/admin/product/{id}` 修改演出
- `POST /api/admin/product/{id}/publish` 上架
- `POST /api/admin/session` 创建场次
- `POST /api/admin/session/{id}/tier` 配置票档
- `PUT /api/admin/tier/{id}/stock` 调整库存（记审计）

### 优惠券
- `POST /api/admin/coupon-template` 创建券模板（类型/门槛/范围/有效期/限量类型/每人限购数）
- `PUT /api/admin/coupon-template/{id}` 修改
- `POST /api/admin/coupon-template/{id}/publish` 上架

### 订单
- `GET /api/admin/order/list` 订单查询
- `POST /api/admin/order/{id}/refund` 退款审核

### 看板
- `GET /api/admin/dashboard` 销量/售罄/券核销率

---

## 十、`[TBD]` 待定与补充

| 项 | 说明 |
|---|---|
| 秒杀 Lua 脚本接口 | 待 T1 详细设计时定签 |
| 幂等与去重参数 | 待 T1 详细设计时定 |
| 错误码表 | 统一错误码定义 |
| 响应体详细 JSON Schema | 每个接口的 request/response 样例 |
| 鉴权错误码 | 401/403 错误码定义 |
| 管理端权限 | 是否做角色（admin/运营），待定 |