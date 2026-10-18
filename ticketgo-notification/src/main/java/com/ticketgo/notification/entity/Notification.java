package com.ticketgo.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_notification")
public class Notification {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private String type;      // ORDER / COUPON / SYSTEM
    private String title;
    private String content;
    private Integer isRead;   // 0未读 1已读
    private LocalDateTime createTime;
}
