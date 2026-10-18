package com.ticketgo.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticketgo.common.claim.StockClaimEngine;
import com.ticketgo.common.constant.RedisKeys;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.PageResult;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.order.dto.OrderCreatedMessage;
import com.ticketgo.order.entity.Order;
import com.ticketgo.order.entity.OrderItem;
import com.ticketgo.order.mapper.OrderItemMapper;
import com.ticketgo.order.mapper.OrderMapper;
import com.ticketgo.order.mapper.SeatMapper;
import com.ticketgo.order.mapper.UserCouponMapper;
import com.ticketgo.order.service.OrderService;
import com.ticketgo.order.ws.OrderNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单状态机：待支付(0) → 已支付(1) → 已完成(4) / 已取消(2) / 已退款(3)。
 * 取消/超时执行「三释放」：票档库存 + 座位 + 优惠券，按订单幂等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SeatMapper seatMapper;
    private final UserCouponMapper userCouponMapper;
    private final StockClaimEngine stockClaimEngine;
    private final OrderNotifyService notifyService;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${ticketgo.coupon.base-url:http://127.0.0.1:8087}")
    private String couponBaseUrl;

    @Override
    public PageResult<Order> list(Long userId, Integer status, int page, int pageSize) {
        Page<Order> p = new Page<>(Math.max(page, 1), pageSize <= 0 ? 20 : pageSize);
        var wrapper = Wrappers.<Order>lambdaQuery().eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        orderMapper.selectPage(p, wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    @Override
    public Order detail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    @Override
    public Order cancel(Long id) {
        Order order = detail(id);
        if (order.getStatus() == 1) {
            throw new BizException("已支付订单请走退款流程");
        }
        if (order.getStatus() != 0) {
            return order; // 已取消/已退款，幂等返回
        }
        int rows = orderMapper.markCancelled(id);
        if (rows == 0) {
            return orderMapper.selectById(id);
        }
        release(order);
        notifyService.send(order.getUserId(), "ORDER_STATUS", "CANCELLED");
        log.info("订单取消并三释放 orderNo={}", order.getOrderNo());
        return orderMapper.selectById(id);
    }

    @Override
    public Order refund(Long id) {
        Order order = detail(id);
        if (order.getStatus() != 1) {
            throw new BizException("仅已支付订单可退款");
        }
        order.setStatus(3);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        release(order);
        notifyService.send(order.getUserId(), "ORDER_STATUS", "REFUNDED");
        log.info("订单退款并回补 orderNo={}", order.getOrderNo());
        return orderMapper.selectById(id);
    }

    @Override
    public void markPaid(Long id) {
        Order order = detail(id);
        if (order.getStatus() == 1) {
            return; // 幂等
        }
        int rows = orderMapper.markPaid(id);
        if (rows == 0) {
            return;
        }
        List<OrderItem> items = itemsOf(id);
        for (OrderItem item : items) {
            if (item.getSeatId() != null) {
                seatMapper.sell(item.getSeatId());
            }
        }
        userCouponMapper.useByOrder(id);
        // 券订单支付成功 → 发放用户券（幂等）
        if (order.getOrderType() != null && order.getOrderType() == 2) {
            try {
                restTemplate.postForObject(couponBaseUrl + "/api/coupon/issue/" + id, null, String.class);
                log.info("券订单发券调用成功 orderId={}", id);
            } catch (Exception e) {
                log.warn("券订单发券调用失败 orderId={}：{}", id, e.getMessage());
            }
        }
        notifyService.send(order.getUserId(), "ORDER_STATUS", "PAID");
        log.info("订单支付成功 orderNo={}", order.getOrderNo());
    }

    @Override
    public void createFromMessage(OrderCreatedMessage message) {
        if (message == null || message.orderNo() == null) {
            return;
        }
        Long count = orderMapper.selectCount(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, message.orderNo()));
        if (count != null && count > 0) {
            log.info("订单已存在，幂等跳过 orderNo={}", message.orderNo());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setId(message.orderId() == null ? SnowflakeIdGenerator.generateId() : message.orderId());
        order.setOrderNo(message.orderNo());
        order.setUserId(message.userId());
        order.setOrderType(message.orderType() == null ? 1 : message.orderType());
        order.setSessionId(message.sessionId());
        order.setTotalAmount(message.totalAmount());
        order.setStatus(0);
        order.setPayExpireAt(now.plusMinutes(15));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        if (message.tierId() != null) {
            OrderItem item = new OrderItem();
            item.setId(SnowflakeIdGenerator.generateId());
            item.setOrderId(order.getId());
            item.setTierId(message.tierId());
            item.setSeatId(null);
            item.setPrice(message.totalAmount());
            item.setQuantity(message.quantity() == null ? 1 : message.quantity());
            item.setCreateTime(now);
            orderItemMapper.insert(item);
        }
        notifyService.send(order.getUserId(), "ORDER_STATUS", "CREATED");
        log.info("Kafka 消费落单成功 orderNo={}", order.getOrderNo());
    }

    private List<OrderItem> itemsOf(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
    }

    /** 三释放：票档库存（Redis）+ 座位 + 优惠券 */
    private void release(Order order) {
        for (OrderItem item : itemsOf(order.getId())) {
            if (item.getTierId() != null) {
                int qty = item.getQuantity() == null ? 1 : item.getQuantity();
                stockClaimEngine.restore(RedisKeys.stock(item.getTierId()), qty);
            }
            if (item.getSeatId() != null) {
                seatMapper.release(item.getSeatId());
            }
        }
        userCouponMapper.releaseByOrder(order.getId());
    }
}
