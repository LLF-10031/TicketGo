package com.ticketgo.seckill;

import com.ticketgo.common.exception.GlobalExceptionHandler;
import com.ticketgo.common.limit.RateLimitAspect;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀服务：Redis Lua 扣库存 + 本地消息表 + Kafka
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.ticketgo.seckill.mapper")
@Import({GlobalExceptionHandler.class, RateLimitAspect.class})
public class SeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
