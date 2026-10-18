package com.ticketgo.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_cart")
public class Cart {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long tierId;
    private Long seatId;
    private Integer quantity;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
