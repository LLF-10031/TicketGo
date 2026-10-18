package com.ticketgo.payment.dto;

/**
 * 支付回调请求
 */
public record PayCallbackRequest(Long orderId, String tradeNo, Integer status) {
}
