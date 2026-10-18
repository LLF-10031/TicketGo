package com.ticketgo.seckill.config;

import com.ticketgo.common.claim.StockClaimEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * 秒杀服务 Bean：通用领取引擎 + RestTemplate（调用券服务）
 */
@Configuration
public class BeanConfig {

    @Bean
    public StockClaimEngine stockClaimEngine(StringRedisTemplate redisTemplate) {
        return new StockClaimEngine(redisTemplate);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
