package com.ticketgo.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketgo.order.dto.OrderCreatedMessage;
import com.ticketgo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀落单消费者：topic=order-created，group=order-group，按 order_no 幂等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${ticketgo.kafka.order-topic:order-created}", groupId = "order-group")
    public void onMessage(String message) {
        log.info("收到落单消息：{}", message);
        try {
            // 用 Jackson 反序列化 record（Hutool 的 toBean 不支持 record 构造绑定）
            OrderCreatedMessage msg = objectMapper.readValue(message, OrderCreatedMessage.class);
            orderService.createFromMessage(msg);
        } catch (Exception e) {
            log.error("落单消息处理失败：{}", message, e);
            throw new RuntimeException(e);
        }
    }
}
