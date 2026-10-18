package com.ticketgo.notification.controller;

import com.ticketgo.common.result.PageResult;
import com.ticketgo.common.result.Result;
import com.ticketgo.notification.entity.Notification;
import com.ticketgo.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知接口：站内信发送/列表/已读。
 */
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/api/notification/send")
    public Result<Long> send(@RequestBody SendRequest req) {
        return Result.ok(notificationService.send(req.userId(), req.type(), req.title(), req.content()));
    }

    @GetMapping("/api/notification/list")
    public Result<PageResult<Notification>> list(@RequestHeader("X-User-Id") Long userId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return Result.ok(notificationService.list(userId, page, pageSize));
    }

    @PostMapping("/api/notification/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }

    public record SendRequest(Long userId, String type, String title, String content) {
    }
}
