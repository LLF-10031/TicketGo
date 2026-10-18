package com.ticketgo.seat.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.seat.dto.SeatLockRequest;
import com.ticketgo.seat.dto.SeatUnlockRequest;
import com.ticketgo.seat.entity.Seat;
import com.ticketgo.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 座位图 / 锁座 / 释放
 */
@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/api/session/{id}/seats")
    public Result<List<Seat>> seats(@PathVariable Long id,
                                    @RequestParam(name = "tier_id", required = false) Long tierId) {
        return Result.ok(seatService.listBySession(id, tierId));
    }

    @PostMapping("/api/seat/lock")
    public Result<String> lock(@RequestBody SeatLockRequest req) {
        return Result.ok(seatService.lock(req));
    }

    @PostMapping("/api/seat/unlock")
    public Result<Void> unlock(@RequestBody SeatUnlockRequest req) {
        seatService.unlock(req);
        return Result.ok();
    }
}
