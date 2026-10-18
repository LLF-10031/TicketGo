package com.ticketgo.coupon.controller;

import com.ticketgo.common.limit.RateLimit;
import com.ticketgo.common.result.PageResult;
import com.ticketgo.common.result.Result;
import com.ticketgo.coupon.dto.AvailableCoupon;
import com.ticketgo.coupon.dto.CouponAvailableRequest;
import com.ticketgo.coupon.dto.CouponBuyRequest;
import com.ticketgo.coupon.entity.CouponTemplate;
import com.ticketgo.coupon.entity.UserCoupon;
import com.ticketgo.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券（C 端）
 */
@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/api/coupon/templates")
    public Result<List<CouponTemplate>> templates(@RequestParam(name = "scope_type", required = false) Integer scopeType,
                                                  @RequestParam(required = false) Integer status) {
        return Result.ok(couponService.templates(scopeType, status));
    }

    @RateLimit(prefix = "coupon:receive", window = 1, limit = 1)
    @PostMapping("/api/coupon/receive/{templateId}")
    public Result<Long> receive(@RequestHeader("X-User-Id") Long userId, @PathVariable Long templateId) {
        return Result.ok(couponService.receive(userId, templateId));
    }

    @PostMapping("/api/coupon/buy")
    public Result<Long> buy(@RequestHeader("X-User-Id") Long userId, @RequestBody CouponBuyRequest req) {
        return Result.ok(couponService.buy(userId, req));
    }

    @GetMapping("/api/coupon/my")
    public Result<PageResult<UserCoupon>> my(@RequestHeader("X-User-Id") Long userId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return Result.ok(couponService.my(userId, status, page, pageSize));
    }

    @PostMapping("/api/coupon/available")
    public Result<List<AvailableCoupon>> available(@RequestHeader("X-User-Id") Long userId,
                                                   @RequestBody CouponAvailableRequest req) {
        return Result.ok(couponService.available(userId, req));
    }
}
