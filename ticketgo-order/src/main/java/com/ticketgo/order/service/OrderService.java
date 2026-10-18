package com.ticketgo.order.service;

import com.ticketgo.common.result.PageResult;
import com.ticketgo.order.dto.OrderCreatedMessage;
import com.ticketgo.order.entity.Order;

public interface OrderService {

    PageResult<Order> list(Long userId, Integer status, int page, int pageSize);

    Order detail(Long id);

    Order cancel(Long id);

    Order refund(Long id);

    void markPaid(Long id);

    void createFromMessage(OrderCreatedMessage message);
}
