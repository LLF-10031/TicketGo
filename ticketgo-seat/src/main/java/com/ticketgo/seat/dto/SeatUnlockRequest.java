package com.ticketgo.seat.dto;

import java.util.List;

/**
 * 释放锁座请求
 */
public record SeatUnlockRequest(Long sessionId, List<Long> seatIds, String lockToken) {
}
