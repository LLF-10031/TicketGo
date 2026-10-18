package com.ticketgo.seat.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.seat.entity.Seat;
import com.ticketgo.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：座位图批量生成 / 查看
 */
@RestController
@RequiredArgsConstructor
public class AdminSeatController {

    private final SeatService seatService;

    @PostMapping("/api/admin/session/{sessionId}/seats/generate")
    public Result<Integer> generate(@PathVariable Long sessionId,
                                    @RequestParam Long tierId,
                                    @RequestParam(defaultValue = "10") int rows,
                                    @RequestParam(defaultValue = "10") int cols) {
        return Result.ok(seatService.generate(sessionId, tierId, rows, cols));
    }

    @GetMapping("/api/admin/session/{sessionId}/seats")
    public Result<List<Seat>> list(@PathVariable Long sessionId,
                                   @RequestParam(name = "tier_id", required = false) Long tierId) {
        return Result.ok(seatService.listBySession(sessionId, tierId));
    }
}
