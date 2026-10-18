package com.ticketgo.order.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.order.entity.Order;
import com.ticketgo.order.mapper.OrderMapper;
import com.ticketgo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时扫单：每 30s 扫描待支付且已超时订单 → 取消并三释放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 30000)
    public void scanExpired() {
        List<Order> expired = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, 0)
                .isNotNull(Order::getPayExpireAt)
                .lt(Order::getPayExpireAt, LocalDateTime.now())
                .orderByAsc(Order::getPayExpireAt)
                .last("limit 100"));
        for (Order order : expired) {
            try {
                orderService.cancel(order.getId());
            } catch (Exception e) {
                log.warn("超时订单取消失败 orderNo={}：{}", order.getOrderNo(), e.getMessage());
            }
        }
        if (!expired.isEmpty()) {
            log.info("超时扫单处理 {} 笔", expired.size());
        }
    }
}
