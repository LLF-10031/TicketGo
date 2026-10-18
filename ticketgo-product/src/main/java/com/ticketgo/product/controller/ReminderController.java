package com.ticketgo.product.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.product.entity.Reminder;
import com.ticketgo.product.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开票提醒接口
 */
@RestController
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping("/api/reminder")
    public Result<Long> subscribe(@RequestHeader("X-User-Id") Long userId, @RequestBody SubscribeRequest req) {
        return Result.ok(reminderService.subscribe(userId, req.productId(), req.sessionId()));
    }

    @DeleteMapping("/api/reminder/{id}")
    public Result<Void> cancel(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        reminderService.cancel(userId, id);
        return Result.ok();
    }

    @GetMapping("/api/reminder/my")
    public Result<List<Reminder>> my(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(reminderService.my(userId));
    }

    public record SubscribeRequest(Long productId, Long sessionId) {
    }
}
