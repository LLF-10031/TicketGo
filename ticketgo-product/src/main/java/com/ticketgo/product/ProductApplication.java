package com.ticketgo.product;

import com.ticketgo.common.exception.GlobalExceptionHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 演出产品服务：演出 / 场次 / 票档 + 图片上传
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.ticketgo.product.mapper")
@Import(GlobalExceptionHandler.class)
public class ProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
