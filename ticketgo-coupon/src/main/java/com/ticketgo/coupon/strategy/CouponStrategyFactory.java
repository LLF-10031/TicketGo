package com.ticketgo.coupon.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 策略工厂：Spring 注入所有策略后按 type 建立路由表
 */
@Component
public class CouponStrategyFactory {

    private final Map<String, CouponStrategy> strategies;

    public CouponStrategyFactory(List<CouponStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(CouponStrategy::type, Function.identity()));
    }

    public CouponStrategy get(String type) {
        return strategies.get(type);
    }
}
