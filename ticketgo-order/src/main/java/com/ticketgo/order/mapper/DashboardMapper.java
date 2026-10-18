package com.ticketgo.order.mapper;

import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 管理端看板统计
 */
public interface DashboardMapper {

    @Select("SELECT COUNT(*) FROM t_order")
    long totalOrders();

    @Select("SELECT COUNT(*) FROM t_order WHERE status = 1")
    long paidOrders();

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM t_order WHERE status = 1")
    BigDecimal totalRevenue();

    @Select("SELECT COUNT(*) FROM t_user_coupon WHERE status = 2")
    long usedCoupons();

    @Select("SELECT COALESCE(SUM(sold_stock), 0) FROM t_ticket_tier")
    long soldTickets();
}
