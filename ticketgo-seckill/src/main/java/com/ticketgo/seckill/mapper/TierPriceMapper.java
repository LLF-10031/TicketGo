package com.ticketgo.seckill.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 票档只读查询 + 已售回写（与 product 共用同一库）
 */
public interface TierPriceMapper {

    @Select("select price from t_ticket_tier where id = #{id}")
    BigDecimal selectPrice(@Param("id") Long tierId);

    /** 秒杀成功后回写已售数量（每单 +1） */
    @Update("update t_ticket_tier set sold_stock = sold_stock + 1 where id = #{id}")
    int increaseSold(@Param("id") Long tierId);
}
