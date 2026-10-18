package com.ticketgo.seckill.service;

import com.ticketgo.seckill.dto.SeckillResponse;

public interface SeckillService {

    SeckillResponse seckill(Long userId, Long sessionId, Long tierId);

    void warmUp(Long tierId, long stock);
}
