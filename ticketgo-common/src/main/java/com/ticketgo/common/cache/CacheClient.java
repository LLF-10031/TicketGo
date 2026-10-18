package com.ticketgo.common.cache;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存三件套工具：
 * - 防穿透：布隆过滤器（见 RedisBloomFilter）+ 缓存空对象
 * - 防击穿：逻辑过期 + 双重检查 + 互斥锁 + 异步重建
 * - 防雪崩：随机 TTL
 */
@Slf4j
@RequiredArgsConstructor
public class CacheClient {

    private final StringRedisTemplate redis;

    private static final long NULL_TTL_SECONDS = 60;

    /** 普通写入（带随机 TTL，防雪崩） */
    public void set(String key, Object value, long ttlSeconds) {
        long ttl = ttlSeconds + RandomUtil.randomLong(0, ttlSeconds / 10 + 1);
        redis.opsForValue().set(key, JSONUtil.toJsonStr(value), ttl, TimeUnit.SECONDS);
    }

    /** 写入逻辑过期缓存（TTL 用随机值，防雪崩；逻辑过期时间用于防击穿） */
    public void setWithLogicalExpire(String key, Object value, long logicalExpireSeconds) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(logicalExpireSeconds));
        long ttl = logicalExpireSeconds * 2 + RandomUtil.randomLong(0, 300);
        redis.opsForValue().set(key, JSONUtil.toJsonStr(redisData), ttl, TimeUnit.SECONDS);
    }

    /**
     * 防穿透（缓存空对象 + 随机 TTL）
     */
    public <T> T getWithPassThrough(String keyPrefix, Object id, Class<T> type, long ttlSeconds, Function<Object, T> loader) {
        String key = keyPrefix + id;
        String json = redis.opsForValue().get(key);
        if (json != null) {
            return json.isEmpty() ? null : JSONUtil.toBean(json, type);
        }
        T value = loader.apply(id);
        if (value == null) {
            // 缓存空对象，短 TTL
            redis.opsForValue().set(key, "", NULL_TTL_SECONDS, TimeUnit.SECONDS);
            return null;
        }
        set(key, value, ttlSeconds);
        return value;
    }

    /**
     * 防击穿（逻辑过期 + 互斥锁 + 异步重建）
     */
    public <T> T getWithLogicalExpire(String keyPrefix, Object id, Class<T> type, long logicalExpireSeconds,
                                      Function<Object, T> loader, ExecutorService rebuildExecutor) {
        String key = keyPrefix + id;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            // 热点数据未命中，走穿透逻辑
            return getWithPassThrough(keyPrefix, id, type, logicalExpireSeconds, loader);
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        T data = JSONUtil.toBean(JSONUtil.toJsonStr(redisData.getData()), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime != null && expireTime.isAfter(LocalDateTime.now())) {
            return data;
        }
        // 已逻辑过期：尝试获取互斥锁重建
        String lockKey = "lock:" + key;
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS));
        if (locked) {
            rebuildExecutor.submit(() -> {
                try {
                    // 双重检查
                    String again = redis.opsForValue().get(key);
                    if (again != null) {
                        RedisData againData = JSONUtil.toBean(again, RedisData.class);
                        if (againData.getExpireTime() != null && againData.getExpireTime().isAfter(LocalDateTime.now())) {
                            return;
                        }
                    }
                    T fresh = loader.apply(id);
                    if (fresh != null) {
                        setWithLogicalExpire(key, fresh, logicalExpireSeconds);
                    }
                } catch (Exception e) {
                    log.warn("缓存异步重建失败 key={}：{}", key, e.getMessage());
                } finally {
                    redis.delete(lockKey);
                }
            });
        }
        // 直接返回旧数据
        return data;
    }

    /** 删除缓存（更新/删除时用） */
    public void delete(String key) {
        redis.delete(key);
    }
}
