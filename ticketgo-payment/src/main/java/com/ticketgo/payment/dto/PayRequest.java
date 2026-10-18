package com.ticketgo.payment.dto;

/**
 * 发起支付请求
 */
public record PayRequest(Long orderId, String payType) {
}
