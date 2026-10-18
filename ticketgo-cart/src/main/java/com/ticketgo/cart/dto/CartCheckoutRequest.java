package com.ticketgo.cart.dto;

import java.util.List;

/**
 * 购物车结算请求（可选使用优惠券）
 */
public record CartCheckoutRequest(List<Long> cartItemIds, Long userCouponId, Long productId) {
}
