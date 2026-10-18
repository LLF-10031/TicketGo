package com.ticketgo.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_ticket_tier")
public class TicketTier {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long sessionId;
    private String name;
    private BigDecimal price;
    private Integer totalStock;
    private Integer soldStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
