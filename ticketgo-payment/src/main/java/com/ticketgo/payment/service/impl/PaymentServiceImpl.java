package com.ticketgo.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.payment.dto.PayCallbackRequest;
import com.ticketgo.payment.dto.PayRequest;
import com.ticketgo.payment.entity.Order;
import com.ticketgo.payment.entity.Payment;
import com.ticketgo.payment.mapper.OrderMapper;
import com.ticketgo.payment.mapper.PaymentMapper;
import com.ticketgo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * Mock 支付：写支付单；回调时通知订单服务置为已支付（失败则直接改库兜底）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final RestTemplate restTemplate;

    @Value("${ticketgo.order.base-url:http://127.0.0.1:8085}")
    private String orderBaseUrl;

    @Override
    @io.seata.spring.annotation.GlobalTransactional(name = "ticketgo-pay", rollbackFor = Exception.class)
    public Payment pay(PayRequest req) {
        if (req == null || req.orderId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        Order order = orderMapper.selectById(req.orderId());
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        Payment exist = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, req.orderId())
                .last("limit 1"));
        if (exist != null) {
            return exist; // 幂等
        }
        LocalDateTime now = LocalDateTime.now();
        Payment payment = new Payment();
        payment.setId(SnowflakeIdGenerator.generateId());
        payment.setOrderId(req.orderId());
        payment.setAmount(order.getTotalAmount());
        payment.setPayType(StringUtils.hasText(req.payType()) ? req.payType() : "MOCK");
        payment.setStatus(1); // Mock 直接成功
        payment.setTradeNo("MOCK" + IdUtil.fastSimpleUUID());
        payment.setPaidAt(now);
        payment.setCreateTime(now);
        payment.setUpdateTime(now);
        paymentMapper.insert(payment);
        log.info("Mock 支付成功 orderNo={} tradeNo={}", order.getOrderNo(), payment.getTradeNo());
        // Mock 支付即刻成功，直接通知订单服务置为已支付（真实场景由支付渠道异步回调）
        markOrderPaid(req.orderId());
        return payment;
    }

    @Override
    public void callback(PayCallbackRequest req) {
        if (req == null || req.orderId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, req.orderId())
                .last("limit 1"));
        if (payment == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        int status = req.status() == null ? 1 : req.status();
        payment.setTradeNo(StringUtils.hasText(req.tradeNo()) ? req.tradeNo() : payment.getTradeNo());
        payment.setStatus(status);
        if (status == 1) {
            payment.setPaidAt(LocalDateTime.now());
        }
        payment.setUpdateTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        if (status == 1) {
            markOrderPaid(req.orderId());
        }
        log.info("支付回调处理完成 orderId={} status={}", req.orderId(), status);
    }

    /** 优先调订单服务内部接口，失败则直接改库兜底 */
    private void markOrderPaid(Long orderId) {
        try {
            restTemplate.postForObject(orderBaseUrl + "/api/order/" + orderId + "/mark-paid", null, String.class);
            log.info("调用订单服务置为已支付成功 orderId={}", orderId);
        } catch (Exception e) {
            log.warn("调用订单服务失败，直接改库兜底 orderId={}：{}", orderId, e.getMessage());
            orderMapper.markPaid(orderId);
        }
    }
}
