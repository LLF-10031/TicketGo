package com.ticketgo.product.scheduler;

import com.ticketgo.product.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 场次状态机：每分钟推进 待开票 → 售票中 → 已结束。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionStatusScheduler {

    private final SessionMapper sessionMapper;

    @Scheduled(fixedDelay = 60000)
    public void advance() {
        int opened = sessionMapper.openSales();
        int closed = sessionMapper.closeSales();
        if (opened > 0 || closed > 0) {
            log.info("场次状态推进：开售 {} 个，结束 {} 个", opened, closed);
        }
    }
}
