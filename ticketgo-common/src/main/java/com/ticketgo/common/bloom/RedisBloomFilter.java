package com.ticketgo.common.bloom;

import cn.hutool.core.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis 位图的布隆过滤器（防缓存穿透）。
 * 使用多个哈希函数映射到位图，contains 返回"可能存在/一定不存在"。
 */
@RequiredArgsConstructor
public class RedisBloomFilter {

    private final StringRedisTemplate redis;
    /** 误判率越低、位图越大 */
    private long capacity = 1_000_000L;
    private double fpp = 0.01;

    /** 位图位数 */
    private long numBits() {
        // m = -n*ln(p) / (ln2)^2
        return (long) Math.ceil(-capacity * Math.log(fpp) / (Math.log(2) * Math.log(2)));
    }

    /** 哈希函数个数 */
    private int numHashes() {
        return Math.max(1, (int) Math.round((numBits() / (double) capacity) * Math.log(2)));
    }

    private long[] hashes(String value) {
        long m = numBits();
        int k = numHashes();
        long h1 = HashUtil.murmur32(value.getBytes());
        long h2 = HashUtil.djbHash(value);
        long[] positions = new long[k];
        for (int i = 0; i < k; i++) {
            long combined = Math.abs(h1 + i * h2 + (long) i * i);
            positions[i] = combined % m;
        }
        return positions;
    }

    public void add(String key, String value) {
        for (long pos : hashes(value)) {
            redis.opsForValue().setBit(key, pos, true);
        }
    }

    public boolean contains(String key, String value) {
        for (long pos : hashes(value)) {
            Boolean bit = redis.opsForValue().getBit(key, pos);
            if (bit == null || !bit) {
                return false;
            }
        }
        return true;
    }

    public void reset(String key) {
        redis.delete(key);
    }
}
