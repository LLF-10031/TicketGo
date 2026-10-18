package com.ticketgo.coupon.dto;

import java.math.BigDecimal;

/**
 * 下单锁券请求
 */
public record CouponLockRequest(Long userCouponId, Long orderId, Long userId,
                                BigDecimal amount, Long productId) {
}
