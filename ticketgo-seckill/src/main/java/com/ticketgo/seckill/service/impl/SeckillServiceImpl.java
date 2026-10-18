package com.ticketgo.seckill.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.ticketgo.common.claim.ClaimResult;
import com.ticketgo.common.claim.StockClaimEngine;
import com.ticketgo.common.constant.RedisKeys;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.seckill.dto.OrderCreatedMessage;
import com.ticketgo.seckill.dto.SeckillResponse;
import com.ticketgo.seckill.entity.MessageOutbox;
import com.ticketgo.seckill.entity.Order;
import com.ticketgo.seckill.entity.OrderItem;
import com.ticketgo.seckill.mapper.MessageOutboxMapper;
import com.ticketgo.seckill.mapper.OrderItemMapper;
import com.ticketgo.seckill.mapper.OrderMapper;
import com.ticketgo.seckill.mapper.TierPriceMapper;
import com.ticketgo.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀核心：Lua 原子扣库存 + 一人一单 → 同事务写订单 + 本地消息表 → 定时投递 Kafka。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final StockClaimEngine stockClaimEngine;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MessageOutboxMapper messageOutboxMapper;
    private final TierPriceMapper tierPriceMapper;
    private final RestTemplate restTemplate;

    @Value("${ticketgo.kafka.order-topic:order-created}")
    private String orderTopic;

    @Value("${ticketgo.coupon.base-url:http://127.0.0.1:8087}")
    private String couponBaseUrl;

    private static final int PAY_EXPIRE_MINUTES = 15;
    private static final int PER_USER_LIMIT = 1;

    @Override
    public SeckillResponse seckill(Long userId, Long sessionId, Long tierId, Long userCouponId, Long productId) {
        if (userId == null || sessionId == null || tierId == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        // ① Redis Lua 原子裁决：扣库存 + 一人一单
        ClaimResult claim = stockClaimEngine.claim(
                RedisKeys.stock(tierId),
                RedisKeys.seckillUsers(tierId),
                userId,
                PER_USER_LIMIT);
        if (!claim.isSuccess()) {
            throw new BizException(toResultCode(claim));
        }

        BigDecimal price = tierPriceMapper.selectPrice(tierId);
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        long orderId = SnowflakeIdGenerator.generateId();
        BigDecimal payAmount = price;

        // ①.5 可选：锁券并计算抵扣（失败则回补库存）
        if (userCouponId != null) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("userCouponId", userCouponId);
                body.put("orderId", orderId);
                body.put("userId", userId);
                body.put("amount", price);
                body.put("productId", productId);
                @SuppressWarnings("unchecked")
                Map<String, Object> resp = restTemplate.postForObject(couponBaseUrl + "/api/coupon/lock", body, Map.class);
                Object code = resp == null ? null : resp.get("code");
                Object data = resp == null ? null : resp.get("data");
                if (code != null && "200".equals(String.valueOf(code)) && data instanceof Map<?, ?> d) {
                    payAmount = new BigDecimal(String.valueOf(d.get("payAmount")));
                } else {
                    restoreStock(tierId, userId);
                    throw new BizException("优惠券不可用，请重新选择");
                }
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.warn("锁券失败 tierId={} couponId={}: {}", tierId, userCouponId, e.getMessage());
                restoreStock(tierId, userId);
                throw new BizException("优惠券服务不可用，请稍后重试");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // ② 同库事务：订单 + 明细 + 本地消息表
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNo("TG" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6));
        order.setUserId(userId);
        order.setOrderType(1);
        order.setSessionId(sessionId);
        order.setTotalAmount(payAmount);
        order.setStatus(0);
        order.setPayExpireAt(now.plusMinutes(PAY_EXPIRE_MINUTES));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setId(SnowflakeIdGenerator.generateId());
        item.setOrderId(order.getId());
        item.setTierId(tierId);
        item.setSeatId(null);
        item.setPrice(price);
        item.setQuantity(1);
        item.setCreateTime(now);
        orderItemMapper.insert(item);

        OrderCreatedMessage message = new OrderCreatedMessage(order.getId(), order.getOrderNo(), userId,
                1, sessionId, tierId, price, 1);
        MessageOutbox outbox = new MessageOutbox();
        outbox.setId(SnowflakeIdGenerator.generateId());
        outbox.setBizKey(order.getOrderNo());
        outbox.setTopic(orderTopic);
        outbox.setMessageBody(JSONUtil.toJsonStr(message));
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        outbox.setMaxRetry(5);
        outbox.setNextRetryAt(now);
        outbox.setCreateTime(now);
        outbox.setUpdateTime(now);
        messageOutboxMapper.insert(outbox);

        log.info("秒杀成功 userId={} tierId={} orderNo={}", userId, tierId, order.getOrderNo());
        return new SeckillResponse(order.getId(), order.getOrderNo(), claim.getMessage());
    }

    @Override
    public void warmUp(Long tierId, long stock) {
        stockClaimEngine.warmUp(RedisKeys.stock(tierId), stock);
        log.info("库存预热 tierId={} stock={}", tierId, stock);
    }

    private void restoreStock(Long tierId, Long userId) {
        stockClaimEngine.restore(RedisKeys.stock(tierId), 1);
        stockClaimEngine.releaseUser(RedisKeys.seckillUsers(tierId), userId, 1);
    }

    private ResultCode toResultCode(ClaimResult claim) {
        return switch (claim) {
            case STOCK_EMPTY -> ResultCode.STOCK_EMPTY;
            case LIMIT_REACHED -> ResultCode.ALREADY_BOUGHT;
            case NOT_WARMED -> ResultCode.NOT_ON_SALE;
            default -> ResultCode.FAIL;
        };
    }
}
