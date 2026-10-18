package com.ticketgo.notification.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticketgo.common.result.PageResult;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.notification.entity.Notification;
import com.ticketgo.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 站内信 + Mock 短信。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /** 发送站内信（并模拟短信落日志） */
    public Long send(Long userId, String type, String title, String content) {
        Notification n = new Notification();
        n.setId(SnowflakeIdGenerator.generateId());
        n.setUserId(userId);
        n.setType(type == null ? "SYSTEM" : type);
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);
        log.info("【站内信】userId={} type={} title={}", userId, n.getType(), title);
        log.info("【Mock短信】userId={} 内容={}", userId, content);
        return n.getId();
    }

    public PageResult<Notification> list(Long userId, int page, int pageSize) {
        Page<Notification> p = new Page<>(Math.max(page, 1), pageSize <= 0 ? 20 : pageSize);
        notificationMapper.selectPage(p, Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime));
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public void markRead(Long id) {
        Notification n = new Notification();
        n.setId(id);
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }
}
