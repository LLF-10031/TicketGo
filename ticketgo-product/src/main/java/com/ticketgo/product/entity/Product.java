package com.ticketgo.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_product")
public class Product {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String name;
    private String category;
    private String description;
    private String poster;
    private Integer status; // 0草稿 1已上架 2售罄 3已结束
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
