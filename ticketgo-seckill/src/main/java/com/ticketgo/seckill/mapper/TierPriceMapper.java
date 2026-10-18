package com.ticketgo.seckill.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 票档价格只读查询（与 product 共用同一库）
 */
public interface TierPriceMapper {

    @Select("select price from t_ticket_tier where id = #{id}")
    BigDecimal selectPrice(@Param("id") Long tierId);
}
