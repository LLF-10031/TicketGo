package com.ticketgo.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_coupon_template")
public class CouponTemplate {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String name;
    private String type; // full_reduce/discount/immediate
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscount;
    private Integer scopeType; // 0全部 1指定演出
    private String scopeProductIds;
    private Integer validType; // 0固定区间 1领取后N天
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Integer validDays;
    private Integer stockType; // 0限量 1不限量
    private Integer totalQty;
    private Integer perUserLimit;
    private Integer acquireType; // 0领取 1购买
    private BigDecimal price;
    private String imageUrl;
    private Integer status; // 0草稿 1已上架 2已结束
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
