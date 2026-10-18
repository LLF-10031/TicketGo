package com.ticketgo.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_reminder")
public class Reminder {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private Long productId;
    private Long sessionId;
    private LocalDateTime remindAt;
    private Integer status; // 0待提醒 1已提醒 2已取消
    private LocalDateTime createTime;
}
