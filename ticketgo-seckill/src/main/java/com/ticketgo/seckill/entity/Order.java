package com.ticketgo.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String orderNo;
    private Long userId;
    private Integer orderType; // 1票订单 2券订单
    private Long sessionId;
    private BigDecimal totalAmount;
    private Integer status; // 0待支付 1已支付 2已取消 3已退款 4已完成
    private LocalDateTime payExpireAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
