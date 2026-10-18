package com.ticketgo.common.result;

import lombok.Getter;

/**
 * 统一响应码
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    FAIL(500, "操作失败"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),

    // 业务码
    STOCK_EMPTY(1001, "已售罄"),
    ALREADY_BOUGHT(1002, "您已参与过，请勿重复抢购"),
    NOT_ON_SALE(1003, "未开票或已结束"),
    RATE_LIMIT(1004, "操作过于频繁，请稍后再试"),
    COUPON_UNAVAILABLE(1005, "优惠券不可用"),
    SEAT_LOCKED(1006, "座位已被选"),
    ORDER_EXPIRED(1007, "订单已超时"),
    PAY_FAIL(1008, "支付失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
