package com.ticketgo.order.ws;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 推送（多实例）：
 * 本服务不直接推送，而是把消息 publish 到 Redis 频道；
 * 各实例的监听器收到后，若本地持有该用户会话则推送 —— 天然支持多实例广播。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotifyService {

    public static final String WS_CHANNEL = "ws:notify";

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messagingTemplate;

    /** 发布通知到 Redis 频道（多实例广播入口） */
    public void send(Long userId, String type, Object data) {
        if (userId == null) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("type", type);
            payload.put("data", data);
            redis.convertAndSend(WS_CHANNEL, JSONUtil.toJsonStr(payload));
            log.info("WS 广播(Redis) userId={} type={}", userId, type);
        } catch (Exception e) {
            log.warn("WS 广播失败 userId={}：{}", userId, e.getMessage());
        }
    }

    /** 本地投递（由 Redis 监听器调用） */
    public void deliverLocal(Long userId, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notify", payload);
        } catch (Exception e) {
            // 本实例未持有该用户会话时忽略
            log.debug("本地无该用户会话 userId={}", userId);
        }
    }
}
