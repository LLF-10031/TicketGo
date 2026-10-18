package com.ticketgo.coupon.strategy;

import com.ticketgo.coupon.entity.CouponTemplate;

import java.math.BigDecimal;

/**
 * 优惠券策略：新增券种只需实现本接口，符合开闭原则。
 */
public interface CouponStrategy {

    /** 券类型：full_reduce / discount / immediate */
    String type();

    /** 门槛与范围校验 */
    boolean applicable(BigDecimal amount, CouponTemplate template);

    /** 计算抵扣后金额 */
    BigDecimal calculate(BigDecimal origin, CouponTemplate template);
}
