package com.ticketgo.common.limit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户/IP 维度滑动窗口限流注解。
 * 用法：@RateLimit(prefix = "seckill", window = 1, limit = 1)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流 key 前缀 */
    String prefix();

    /** 窗口大小（秒） */
    int window() default 1;

    /** 窗口内最大请求数 */
    int limit() default 1;

    /** 限流维度 */
    Dimension dimension() default Dimension.USER;

    /** 限流后提示 */
    String message() default "操作过于频繁，请稍后再试";

    enum Dimension {
        USER, IP
    }
}
