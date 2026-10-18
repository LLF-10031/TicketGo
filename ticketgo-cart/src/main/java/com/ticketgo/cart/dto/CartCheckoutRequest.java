package com.ticketgo.cart.dto;

import java.util.List;

/**
 * 购物车结算请求
 */
public record CartCheckoutRequest(List<Long> cartItemIds) {
}
