package com.ticketgo.seckill.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.common.limit.RateLimit;
import com.ticketgo.common.result.Result;
import com.ticketgo.seckill.dto.SeckillResponse;
import com.ticketgo.seckill.entity.MessageOutbox;
import com.ticketgo.seckill.mapper.MessageOutboxMapper;
import com.ticketgo.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 秒杀接口
 */
@RestController
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final MessageOutboxMapper messageOutboxMapper;

    @RateLimit(prefix = "seckill", window = 1, limit = 1)
    @PostMapping("/api/seckill/{sessionId}/{tierId}")
    public Result<SeckillResponse> seckill(@RequestHeader("X-User-Id") Long userId,
                                           @PathVariable Long sessionId,
                                           @PathVariable Long tierId) {
        return Result.ok(seckillService.seckill(userId, sessionId, tierId));
    }

    @PostMapping("/api/seckill/warmup/{tierId}")
    public Result<Void> warmUp(@PathVariable Long tierId, @RequestParam long stock) {
        seckillService.warmUp(tierId, stock);
        return Result.ok();
    }

    /** 人工重投单条死信消息 */
    @PostMapping("/api/seckill/outbox/retry/{id}")
    public Result<Void> retryOutbox(@PathVariable Long id) {
        MessageOutbox o = messageOutboxMapper.selectById(id);
        if (o != null) {
            o.setStatus(0);
            o.setRetryCount(0);
            o.setNextRetryAt(LocalDateTime.now());
            o.setUpdateTime(LocalDateTime.now());
            messageOutboxMapper.updateById(o);
        }
        return Result.ok();
    }

    /** 一键重投所有死信消息（status=2） */
    @PostMapping("/api/seckill/outbox/retry-dead")
    public Result<Integer> retryDead() {
        int n = messageOutboxMapper.update(null, Wrappers.<MessageOutbox>lambdaUpdate()
                .set(MessageOutbox::getStatus, 0)
                .set(MessageOutbox::getRetryCount, 0)
                .set(MessageOutbox::getNextRetryAt, LocalDateTime.now())
                .eq(MessageOutbox::getStatus, 2));
        return Result.ok(n);
    }
}
