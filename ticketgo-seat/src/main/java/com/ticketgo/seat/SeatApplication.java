package com.ticketgo.seat;

import com.ticketgo.common.exception.GlobalExceptionHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 座位服务：座位图 / 锁座 / 释放
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.ticketgo.seat.mapper")
@Import(GlobalExceptionHandler.class)
public class SeatApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeatApplication.class, args);
    }
}
