package com.ticketgo.order.ws;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 订阅 Redis 频道，收到广播后在本实例投递给对应用户的 WebSocket 会话。
 */
@Slf4j
public class RedisNotifyListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    public RedisNotifyListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            JSONObject json = JSONUtil.parseObj(body);
            Long userId = json.getLong("userId");
            if (userId == null) {
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = JSONUtil.toBean(body, Map.class);
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notify", map);
            log.info("WS 本地投递 userId={} type={}", userId, json.getStr("type"));
        } catch (Exception e) {
            log.warn("Redis 通知投递失败：{}", e.getMessage());
        }
    }
}
