package com.ticketgo.coupon.strategy;

import com.ticketgo.coupon.entity.CouponTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 立减券：无门槛直接减 discount_amount
 */
@Component
public class ImmediateStrategy implements CouponStrategy {

    @Override
    public String type() {
        return "immediate";
    }

    @Override
    public boolean applicable(BigDecimal amount, CouponTemplate template) {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public BigDecimal calculate(BigDecimal origin, CouponTemplate template) {
        BigDecimal discount = template.getDiscountAmount() == null ? BigDecimal.ZERO : template.getDiscountAmount();
        BigDecimal result = origin.subtract(discount);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }
}
