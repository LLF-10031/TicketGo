package com.ticketgo.order.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.Result;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.order.entity.Order;
import com.ticketgo.order.entity.OrderItem;
import com.ticketgo.order.mapper.OrderItemMapper;
import com.ticketgo.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电子票：仅已支付订单可出票。
 */
@RestController
@RequiredArgsConstructor
public class OrderTicketController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @GetMapping("/api/order/{id}/ticket")
    public Result<Map<String, Object>> ticket(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BizException("订单未支付，暂无电子票");
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, id));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("userId", order.getUserId());
        data.put("sessionId", order.getSessionId());
        data.put("status", order.getStatus());
        data.put("qrContent", "ticketgo://ticket/" + order.getOrderNo());
        data.put("items", items);
        return Result.ok(data);
    }
}
