package com.ticketgo.common.limit;

import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

/**
 * 滑动窗口限流切面
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redis;

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    static {
        SCRIPT.setLocation(new ClassPathResource("lua/rate_limit.lua"));
        SCRIPT.setResultType(Long.class);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String identity = resolveIdentity(rateLimit.dimension());
        String key = "rate:limit:" + rateLimit.prefix() + ":" + identity;
        long now = System.currentTimeMillis();
        long windowMs = rateLimit.window() * 1000L;
        Long allowed = redis.execute(SCRIPT, Collections.singletonList(key),
                String.valueOf(now), String.valueOf(windowMs), String.valueOf(rateLimit.limit()));
        if (allowed == null || allowed == 0L) {
            throw new BizException(ResultCode.RATE_LIMIT.getCode(), rateLimit.message());
        }
        return pjp.proceed();
    }

    private String resolveIdentity(RateLimit.Dimension dimension) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String userId = null;
        String ip = "unknown";
        if (attrs != null && attrs.getRequest() != null) {
            userId = attrs.getRequest().getHeader("X-User-Id");
            ip = attrs.getRequest().getRemoteAddr();
        }
        if (dimension == RateLimit.Dimension.IP) {
            return ip;
        }
        return userId != null ? userId : ip;
    }
}
