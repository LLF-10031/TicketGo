package com.ticketgo.coupon.dto;

/**
 * 购买不限量券请求
 */
public record CouponBuyRequest(Long templateId, Integer quantity) {
}
