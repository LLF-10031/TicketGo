package com.ticketgo.order.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.order.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端看板：订单 / 收入 / 券核销 / 售票
 */
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardMapper dashboardMapper;

    @GetMapping("/api/admin/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalOrders", dashboardMapper.totalOrders());
        data.put("paidOrders", dashboardMapper.paidOrders());
        data.put("totalRevenue", dashboardMapper.totalRevenue() == null ? BigDecimal.ZERO : dashboardMapper.totalRevenue());
        data.put("usedCoupons", dashboardMapper.usedCoupons());
        data.put("soldTickets", dashboardMapper.soldTickets());
        return Result.ok(data);
    }
}
