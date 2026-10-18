package com.ticketgo.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 通知服务（占位）：当前仅提供健康检查，后续接入站内信/短信/WS 广播
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.ticketgo.notification.mapper")
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
