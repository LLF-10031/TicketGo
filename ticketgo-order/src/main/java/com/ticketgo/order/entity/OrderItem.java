package com.ticketgo.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order_item")
public class OrderItem {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long orderId;
    private Long tierId;
    private Long seatId;
    private BigDecimal price;
    private Integer quantity;
    private LocalDateTime createTime;
}
