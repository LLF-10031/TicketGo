package com.ticketgo.common.constant;

/**
 * Redis Key 统一管理（禁止业务代码硬编码 key）
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 验证码 */
    public static final String CAPTCHA = "captcha:";
    /** 短信验证码 */
    public static final String SMS_CODE = "sms:code:";
    /** 登录用户 */
    public static final String LOGIN_TOKEN = "login:token:";
    /** 刷新Token */
    public static final String REFRESH_TOKEN = "refresh:token:";
    /** Token 黑名单 */
    public static final String TOKEN_BLACKLIST = "token:blacklist:";

    /** 票档库存 */
    public static final String STOCK = "stock:tier:";
    /** 秒杀已购用户集合（一人一单） */
    public static final String SECKILL_USERS = "seckill:users:";
    /** 券库存 */
    public static final String COUPON_STOCK = "stock:coupon:";
    /** 用户已领券数 */
    public static final String COUPON_USER_COUNT = "coupon:user:count:";

    /** 座位锁 */
    public static final String SEAT_LOCK = "seat:lock:";
    /** 缓存：演出详情 */
    public static final String CACHE_PRODUCT = "cache:product:";
    /** 限流滑动窗口 */
    public static final String RATE_LIMIT = "rate:limit:";

    public static String stock(Long tierId) {
        return STOCK + tierId;
    }

    public static String seckillUsers(Long tierId) {
        return SECKILL_USERS + tierId;
    }

    public static String couponStock(Long templateId) {
        return COUPON_STOCK + templateId;
    }

    public static String seatLock(Long sessionId, Long seatId) {
        return SEAT_LOCK + sessionId + ":" + seatId;
    }
}
