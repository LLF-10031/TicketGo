package com.ticketgo.seat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_seat")
public class Seat {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long sessionId;
    private Long tierId;
    private String rowNo;
    private String colNo;
    private Integer x;
    private Integer y;
    private Integer status; // 0可售 1锁定 2已售
    private String lockToken;
    private LocalDateTime lockExpireAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
