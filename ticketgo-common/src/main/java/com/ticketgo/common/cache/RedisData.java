package com.ticketgo.common.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 逻辑过期缓存包装：数据 + 逻辑过期时间
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisData implements Serializable {
    private LocalDateTime expireTime;
    private Object data;
}
