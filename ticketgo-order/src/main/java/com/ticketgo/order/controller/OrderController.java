package com.ticketgo.order.controller;

import com.ticketgo.common.result.PageResult;
import com.ticketgo.common.result.Result;
import com.ticketgo.order.entity.Order;
import com.ticketgo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口
 */
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/api/order/list")
    public Result<PageResult<Order>> list(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return Result.ok(orderService.list(userId, status, page, pageSize));
    }

    @GetMapping("/api/order/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @PostMapping("/api/order/{id}/cancel")
    public Result<Order> cancel(@PathVariable Long id) {
        return Result.ok(orderService.cancel(id));
    }

    @PostMapping("/api/order/{id}/refund")
    public Result<Order> refund(@PathVariable Long id) {
        return Result.ok(orderService.refund(id));
    }

    /** 内部接口：支付服务回调后置为已支付 */
    @PostMapping("/api/order/{id}/mark-paid")
    public Result<Void> markPaid(@PathVariable Long id) {
        orderService.markPaid(id);
        return Result.ok();
    }
}
