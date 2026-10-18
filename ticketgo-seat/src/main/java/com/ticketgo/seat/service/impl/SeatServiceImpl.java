package com.ticketgo.seat.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.common.constant.RedisKeys;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.seat.dto.SeatLockRequest;
import com.ticketgo.seat.dto.SeatUnlockRequest;
import com.ticketgo.seat.entity.Seat;
import com.ticketgo.seat.mapper.SeatMapper;
import com.ticketgo.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatMapper seatMapper;
    private final StringRedisTemplate redisTemplate;

    private static final long DEFAULT_TTL_SECONDS = 300L;

    @Override
    public List<Seat> listBySession(Long sessionId, Long tierId) {
        var wrapper = Wrappers.<Seat>lambdaQuery().eq(Seat::getSessionId, sessionId);
        if (tierId != null) {
            wrapper.eq(Seat::getTierId, tierId);
        }
        wrapper.orderByAsc(Seat::getRowNo).orderByAsc(Seat::getColNo);
        return seatMapper.selectList(wrapper);
    }

    @Override
    public String lock(SeatLockRequest req) {
        if (req == null || req.sessionId() == null || CollectionUtils.isEmpty(req.seatIds())) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        long ttl = req.expireSeconds() == null || req.expireSeconds() <= 0 ? DEFAULT_TTL_SECONDS : req.expireSeconds();
        String token = IdUtil.fastSimpleUUID();
        List<String> acquiredKeys = new ArrayList<>();
        try {
            for (Long seatId : req.seatIds()) {
                String key = RedisKeys.seatLock(req.sessionId(), seatId);
                Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl, TimeUnit.SECONDS);
                if (!Boolean.TRUE.equals(ok)) {
                    throw new BizException(ResultCode.SEAT_LOCKED);
                }
                acquiredKeys.add(key);
            }
        } catch (BizException e) {
            // 任一失败则回滚已抢到的锁
            acquiredKeys.forEach(redisTemplate::delete);
            throw e;
        }
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(ttl);
        for (Long seatId : req.seatIds()) {
            seatMapper.lockSeat(seatId, token, expireAt);
        }
        log.info("锁座成功 sessionId={} seats={} token={}", req.sessionId(), req.seatIds(), token);
        return token;
    }

    @Override
    public void unlock(SeatUnlockRequest req) {
        if (req == null || req.sessionId() == null || CollectionUtils.isEmpty(req.seatIds())
                || !StringUtils.hasText(req.lockToken())) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        for (Long seatId : req.seatIds()) {
            String key = RedisKeys.seatLock(req.sessionId(), seatId);
            String value = redisTemplate.opsForValue().get(key);
            if (req.lockToken().equals(value)) {
                redisTemplate.delete(key);
                seatMapper.unlockSeat(seatId, req.lockToken());
            }
        }
        log.info("释放锁座 sessionId={} seats={}", req.sessionId(), req.seatIds());
    }
}
