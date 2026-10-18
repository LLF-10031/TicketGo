package com.ticketgo.coupon.strategy;

import com.ticketgo.coupon.entity.CouponTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 满减券：满 threshold_amount 减 discount_amount
 */
@Component
public class FullReduceStrategy implements CouponStrategy {

    @Override
    public String type() {
        return "full_reduce";
    }

    @Override
    public boolean applicable(BigDecimal amount, CouponTemplate template) {
        BigDecimal threshold = template.getThresholdAmount() == null ? BigDecimal.ZERO : template.getThresholdAmount();
        return amount.compareTo(threshold) >= 0;
    }

    @Override
    public BigDecimal calculate(BigDecimal origin, CouponTemplate template) {
        BigDecimal discount = template.getDiscountAmount() == null ? BigDecimal.ZERO : template.getDiscountAmount();
        BigDecimal result = origin.subtract(discount);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }
}
