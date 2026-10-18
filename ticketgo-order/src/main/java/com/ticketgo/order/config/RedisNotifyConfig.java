package com.ticketgo.order.config;

import com.ticketgo.order.ws.OrderNotifyService;
import com.ticketgo.order.ws.RedisNotifyListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Redis Pub/Sub 配置：为 WebSocket 多实例广播提供跨实例通知能力。
 */
@Configuration
public class RedisNotifyConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory,
                                                                       SimpMessagingTemplate messagingTemplate) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(new RedisNotifyListener(messagingTemplate),
                new ChannelTopic(OrderNotifyService.WS_CHANNEL));
        return container;
    }
}
