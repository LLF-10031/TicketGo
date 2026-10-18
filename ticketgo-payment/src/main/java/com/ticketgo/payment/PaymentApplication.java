package com.ticketgo.payment;

import com.ticketgo.common.exception.GlobalExceptionHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 支付服务（Mock）
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.ticketgo.payment.mapper")
@Import(GlobalExceptionHandler.class)
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
