package com.ticketgo.coupon.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.coupon.entity.CouponTemplate;
import com.ticketgo.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：优惠券模板
 */
@RestController
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping("/api/admin/coupon-template")
    public Result<CouponTemplate> create(@RequestBody CouponTemplate template) {
        return Result.ok(couponService.createTemplate(template));
    }

    @PutMapping("/api/admin/coupon-template/{id}")
    public Result<CouponTemplate> update(@PathVariable Long id, @RequestBody CouponTemplate template) {
        return Result.ok(couponService.updateTemplate(id, template));
    }

    @PostMapping("/api/admin/coupon-template/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        couponService.publish(id);
        return Result.ok();
    }
}
