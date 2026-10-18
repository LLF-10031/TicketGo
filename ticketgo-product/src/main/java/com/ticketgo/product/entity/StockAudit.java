package com.ticketgo.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_stock_audit")
public class StockAudit {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String targetType;   // tier / coupon_template
    private Long targetId;
    private Integer beforeQty;
    private Integer afterQty;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
}
