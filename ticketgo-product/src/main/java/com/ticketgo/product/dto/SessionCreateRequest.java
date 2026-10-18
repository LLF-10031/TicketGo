package com.ticketgo.product.dto;

import java.time.LocalDateTime;

/**
 * 管理端创建场次请求
 */
public record SessionCreateRequest(Long productId, String venue, LocalDateTime showTime,
                                   LocalDateTime saleStartTime, LocalDateTime saleEndTime, Integer seatMode) {
}
