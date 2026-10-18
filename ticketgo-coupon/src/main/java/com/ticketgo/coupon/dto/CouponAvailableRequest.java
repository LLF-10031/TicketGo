package com.ticketgo.coupon.dto;

import java.math.BigDecimal;

/**
 * 下单时查询可用券请求
 */
public record CouponAvailableRequest(Long productId, BigDecimal amount) {
}
