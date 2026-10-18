package com.ticketgo.coupon.dto;

import java.math.BigDecimal;

/**
 * 锁券结果：抵扣后应付金额 + 抵扣额
 */
public record CouponLockResult(BigDecimal payAmount, BigDecimal discount) {
}
