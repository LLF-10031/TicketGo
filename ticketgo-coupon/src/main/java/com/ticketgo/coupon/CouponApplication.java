package com.ticketgo.coupon;

import com.ticketgo.common.exception.GlobalExceptionHandler;
import com.ticketgo.common.limit.RateLimitAspect;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 优惠券服务：策略模式（满减/折扣/立减）+ 通用领取引擎
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.ticketgo.coupon.mapper")
@Import({GlobalExceptionHandler.class, RateLimitAspect.class})
public class CouponApplication {
    public static void main(String[] args) {
        SpringApplication.run(CouponApplication.class, args);
    }
}
