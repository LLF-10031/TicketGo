package com.ticketgo.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_session")
public class Session {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long productId;
    private String venue;
    private LocalDateTime showTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Integer seatMode; // 0免选座 1选座
    private Integer status;   // 0待开票 1售票中 2售罄 3已结束
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
