package com.ticketgo.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message_outbox")
public class MessageOutbox {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String bizKey;
    private String topic;
    private String messageBody;
    private Integer status; // 0待发送 1已发送 2失败
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
