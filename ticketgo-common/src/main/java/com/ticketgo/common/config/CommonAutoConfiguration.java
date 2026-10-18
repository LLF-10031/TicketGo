package com.ticketgo.common.config;

import com.ticketgo.common.bloom.RedisBloomFilter;
import com.ticketgo.common.cache.CacheClient;
import com.ticketgo.common.limit.RateLimitAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * common 公共组件自动装配：缓存三件套 / 布隆过滤器 / 限流切面。
 */
@AutoConfiguration
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheClient cacheClient(StringRedisTemplate redisTemplate) {
        return new CacheClient(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisBloomFilter redisBloomFilter(StringRedisTemplate redisTemplate) {
        return new RedisBloomFilter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(StringRedisTemplate redisTemplate) {
        return new RateLimitAspect(redisTemplate);
    }
}
