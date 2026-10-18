package com.ticketgo.seat.dto;

import java.util.List;

/**
 * 锁座请求
 */
public record SeatLockRequest(Long sessionId, List<Long> seatIds, Integer expireSeconds) {
}
