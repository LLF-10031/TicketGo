package com.ticketgo.seckill.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.seckill.entity.MessageOutbox;
import com.ticketgo.seckill.mapper.MessageOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 本地消息表投递器：
 * - 定时扫描 status=0 的消息投递 Kafka；
 * - 失败按指数退避（30s ×2^n，封顶 10 分钟）持续重试；
 * - 超过最大重试次数 → 置 status=2（死信），打 ERROR 告警，等待人工重投。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final MessageOutboxMapper messageOutboxMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int BATCH_SIZE = 20;
    private static final int BASE_DELAY_SECONDS = 30;
    private static final int MAX_DELAY_SECONDS = 600;
    private static final int DEFAULT_MAX_RETRY = 10;

    @Scheduled(fixedDelay = 3000)
    public void dispatch() {
        List<MessageOutbox> list = messageOutboxMapper.selectList(Wrappers.<MessageOutbox>lambdaQuery()
                .eq(MessageOutbox::getStatus, 0)
                .le(MessageOutbox::getNextRetryAt, LocalDateTime.now())
                .orderByAsc(MessageOutbox::getCreateTime)
                .last("limit " + BATCH_SIZE));
        for (MessageOutbox outbox : list) {
            try {
                kafkaTemplate.send(outbox.getTopic(), outbox.getBizKey(), outbox.getMessageBody())
                        .get(3, TimeUnit.SECONDS);
                outbox.setStatus(1);
                outbox.setUpdateTime(LocalDateTime.now());
                messageOutboxMapper.updateById(outbox);
                log.info("outbox 投递成功 bizKey={} topic={}", outbox.getBizKey(), outbox.getTopic());
            } catch (Exception e) {
                int retry = outbox.getRetryCount() == null ? 0 : outbox.getRetryCount() + 1;
                int maxRetry = outbox.getMaxRetry() == null ? DEFAULT_MAX_RETRY : outbox.getMaxRetry();
                outbox.setRetryCount(retry);
                outbox.setUpdateTime(LocalDateTime.now());
                if (retry >= maxRetry) {
                    // 进入死信：不再自动重投，等待人工重投接口
                    outbox.setStatus(2);
                    messageOutboxMapper.updateById(outbox);
                    log.error("【outbox 死信告警】消息超过最大重试次数，已转死信待人工处理 bizKey={} topic={} retry={} err={}",
                            outbox.getBizKey(), outbox.getTopic(), retry, e.getMessage());
                } else {
                    long delay = Math.min((long) BASE_DELAY_SECONDS << Math.min(retry - 1, 5), MAX_DELAY_SECONDS);
                    outbox.setNextRetryAt(LocalDateTime.now().plusSeconds(delay));
                    messageOutboxMapper.updateById(outbox);
                    log.warn("outbox 投递失败，{}s 后重试 bizKey={} retry={}/{}：{}",
                            delay, outbox.getBizKey(), retry, maxRetry, e.getMessage());
                }
            }
        }
    }
}
