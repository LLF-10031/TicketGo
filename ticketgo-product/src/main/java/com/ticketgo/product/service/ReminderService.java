package com.ticketgo.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.product.entity.Reminder;
import com.ticketgo.product.entity.Session;
import com.ticketgo.product.mapper.ReminderMapper;
import com.ticketgo.product.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开票提醒：订阅 / 取消 / 到点通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderMapper reminderMapper;
    private final SessionMapper sessionMapper;
    private final RestTemplate restTemplate;

    @Value("${ticketgo.notification.base-url:http://127.0.0.1:8089}")
    private String notificationBaseUrl;

    public Long subscribe(Long userId, Long productId, Long sessionId) {
        if (userId == null || sessionId == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException("场次不存在");
        }
        Long exists = reminderMapper.selectCount(Wrappers.<Reminder>lambdaQuery()
                .eq(Reminder::getUserId, userId)
                .eq(Reminder::getSessionId, sessionId)
                .eq(Reminder::getStatus, 0));
        if (exists != null && exists > 0) {
            throw new BizException("您已订阅该场次提醒");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime remindAt = now.plusMinutes(1);
        if (session.getSaleStartTime() != null && session.getSaleStartTime().isAfter(now)) {
            remindAt = session.getSaleStartTime().minusMinutes(10);
            if (remindAt.isBefore(now)) {
                remindAt = now.plusMinutes(1);
            }
        }
        Reminder reminder = new Reminder();
        reminder.setId(SnowflakeIdGenerator.generateId());
        reminder.setUserId(userId);
        reminder.setProductId(productId != null ? productId : session.getProductId());
        reminder.setSessionId(sessionId);
        reminder.setRemindAt(remindAt);
        reminder.setStatus(0);
        reminder.setCreateTime(now);
        reminderMapper.insert(reminder);
        return reminder.getId();
    }

    public void cancel(Long userId, Long id) {
        Reminder reminder = reminderMapper.selectById(id);
        if (reminder == null || !reminder.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        reminder.setStatus(2);
        reminderMapper.updateById(reminder);
    }

    public List<Reminder> my(Long userId) {
        return reminderMapper.selectList(Wrappers.<Reminder>lambdaQuery()
                .eq(Reminder::getUserId, userId)
                .orderByDesc(Reminder::getCreateTime));
    }

    /** 定时调用：到点提醒 */
    public void dispatchDue() {
        List<Reminder> due = reminderMapper.selectList(Wrappers.<Reminder>lambdaQuery()
                .eq(Reminder::getStatus, 0)
                .le(Reminder::getRemindAt, LocalDateTime.now())
                .last("limit 50"));
        for (Reminder reminder : due) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("userId", reminder.getUserId());
                body.put("type", "REMINDER");
                body.put("title", "开票提醒");
                body.put("content", "您关注的演出即将开票，快去抢票吧！");
                restTemplate.postForObject(notificationBaseUrl + "/api/notification/send", body, String.class);
                reminder.setStatus(1);
                reminderMapper.updateById(reminder);
                log.info("开票提醒已发送 userId={} sessionId={}", reminder.getUserId(), reminder.getSessionId());
            } catch (Exception e) {
                log.warn("开票提醒发送失败 id={}：{}", reminder.getId(), e.getMessage());
            }
        }
    }
}
