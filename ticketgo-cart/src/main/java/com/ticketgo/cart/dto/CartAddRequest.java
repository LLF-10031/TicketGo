package com.ticketgo.cart.dto;

/**
 * 加入购物车请求
 */
public record CartAddRequest(Long sessionId, Long tierId, Long seatId, Integer quantity) {
}
