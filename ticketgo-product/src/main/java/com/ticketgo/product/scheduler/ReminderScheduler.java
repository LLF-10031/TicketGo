package com.ticketgo.product.scheduler;

import com.ticketgo.product.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 开票提醒调度：每分钟扫描到点的提醒并发送站内信。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    @Scheduled(fixedDelay = 60000)
    public void dispatch() {
        reminderService.dispatchDue();
    }
}
