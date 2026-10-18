package com.ticketgo.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_payment")
public class Payment {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String payType;
    private Integer status; // 0待支付 1成功 2失败 3已退款
    private String tradeNo;
    private LocalDateTime paidAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
