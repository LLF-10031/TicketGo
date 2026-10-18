package com.ticketgo.coupon.strategy;

import com.ticketgo.coupon.entity.CouponTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 折扣券：discount_rate（如 0.9 = 9 折），可用 max_discount 封顶
 */
@Component
public class DiscountStrategy implements CouponStrategy {

    @Override
    public String type() {
        return "discount";
    }

    @Override
    public boolean applicable(BigDecimal amount, CouponTemplate template) {
        BigDecimal threshold = template.getThresholdAmount() == null ? BigDecimal.ZERO : template.getThresholdAmount();
        return amount.compareTo(threshold) >= 0;
    }

    @Override
    public BigDecimal calculate(BigDecimal origin, CouponTemplate template) {
        BigDecimal rate = template.getDiscountRate() == null ? BigDecimal.ONE : template.getDiscountRate();
        BigDecimal discount = origin.multiply(BigDecimal.ONE.subtract(rate));
        if (template.getMaxDiscount() != null && discount.compareTo(template.getMaxDiscount()) > 0) {
            discount = template.getMaxDiscount();
        }
        BigDecimal result = origin.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }
}
