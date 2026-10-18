package com.ticketgo.coupon.dto;

import java.math.BigDecimal;

/**
 * 可用券及其抵扣后金额
 */
public record AvailableCoupon(Long userCouponId, Long templateId, String name, String type,
                              BigDecimal discount, BigDecimal payAmount) {
}
