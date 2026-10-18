package com.ticketgo.seckill.dto;

/**
 * 秒杀结果
 */
public record SeckillResponse(Long orderId, String orderNo, String message) {
}
