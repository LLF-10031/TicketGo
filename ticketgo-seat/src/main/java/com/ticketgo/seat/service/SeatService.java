package com.ticketgo.seat.service;

import com.ticketgo.seat.dto.SeatLockRequest;
import com.ticketgo.seat.dto.SeatUnlockRequest;
import com.ticketgo.seat.entity.Seat;

import java.util.List;

public interface SeatService {

    List<Seat> listBySession(Long sessionId, Long tierId);

    String lock(SeatLockRequest req);

    void unlock(SeatUnlockRequest req);

    /** 管理端：按 排×列 批量生成座位 */
    int generate(Long sessionId, Long tierId, int rows, int cols);
}
