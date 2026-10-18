package com.ticketgo.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_file")
public class FileRecord {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String bizType;
    private Long bizId;
    private String originalUrl;
    private String displayUrl;
    private String thumbUrl;
    private Integer size;
    private Integer isOrphan;
    private LocalDateTime createTime;
}
