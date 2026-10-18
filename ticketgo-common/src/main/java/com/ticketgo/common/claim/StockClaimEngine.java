package com.ticketgo.common.claim;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * 通用领取引擎：票秒杀 与 券领取 复用同一套「原子扣减 + 用户限购」。
 */
@RequiredArgsConstructor
public class StockClaimEngine {

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    static {
        SCRIPT.setLocation(new ClassPathResource("lua/stock_claim.lua"));
        SCRIPT.setResultType(Long.class);
    }

    /**
     * 原子领取：扣减库存 + 记录用户已领数
     *
     * @param stockKey      库存 key
     * @param usersHashKey  用户已领 Hash key
     * @param userId        用户 ID
     * @param perUserLimit  每人限购数
     */
    public ClaimResult claim(String stockKey, String usersHashKey, Long userId, int perUserLimit) {
        Long result = redisTemplate.execute(
                SCRIPT,
                List.of(stockKey, usersHashKey),
                String.valueOf(userId),
                String.valueOf(perUserLimit)
        );
        return ClaimResult.of(result);
    }

    /** 预热库存 */
    public void warmUp(String stockKey, long stock) {
        redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
    }

    /** 回补库存（订单取消/超时） */
    public void restore(String stockKey, long count) {
        redisTemplate.opsForValue().increment(stockKey, count);
    }

    /** 回退用户已领数（释放限购额度） */
    public void releaseUser(String usersHashKey, Long userId, long count) {
        redisTemplate.opsForHash().increment(usersHashKey, String.valueOf(userId), -count);
    }
}
