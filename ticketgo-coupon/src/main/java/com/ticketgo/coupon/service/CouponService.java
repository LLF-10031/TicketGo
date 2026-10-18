package com.ticketgo.coupon.service;

import com.ticketgo.common.result.PageResult;
import com.ticketgo.coupon.dto.AvailableCoupon;
import com.ticketgo.coupon.dto.CouponAvailableRequest;
import com.ticketgo.coupon.dto.CouponBuyRequest;
import com.ticketgo.coupon.entity.CouponTemplate;
import com.ticketgo.coupon.entity.UserCoupon;

import java.util.List;

public interface CouponService {

    List<CouponTemplate> templates(Integer scopeType, Integer status);

    Long receive(Long userId, Long templateId);

    Long buy(Long userId, CouponBuyRequest req);

    PageResult<UserCoupon> my(Long userId, String status, int page, int pageSize);

    List<AvailableCoupon> available(Long userId, CouponAvailableRequest req);

    CouponTemplate createTemplate(CouponTemplate template);

    CouponTemplate updateTemplate(Long id, CouponTemplate template);

    void publish(Long id);
}
