package com.ticketgo.cart;

import com.ticketgo.common.exception.GlobalExceptionHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 购物车服务：加购 / 结算生成订单
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.ticketgo.cart.mapper")
@Import(GlobalExceptionHandler.class)
public class CartApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
