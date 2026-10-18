package com.ticketgo.seckill.dto;

import java.math.BigDecimal;

/**
 * 落单消息体（Kafka topic=order-created）
 */
public record OrderCreatedMessage(Long orderId, String orderNo, Long userId, Integer orderType,
                                  Long sessionId, Long tierId, BigDecimal totalAmount, Integer quantity) {
}
