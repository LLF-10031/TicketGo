package com.ticketgo.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_coupon")
public class UserCoupon {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private Long templateId;
    private Integer status; // 0未用 1锁定 2已用 3已释放 4已过期
    private Long sourceOrderId;
    private Long lockOrderId;
    private LocalDateTime receivedAt;
    private LocalDateTime expireAt;
    private LocalDateTime usedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
