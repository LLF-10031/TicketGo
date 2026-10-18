package com.ticketgo.order.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 座位释放 / 置售（与 seat 共用同一库）
 */
public interface SeatMapper {

    /** 释放座位（锁定或已售 → 可售） */
    @Update("update t_seat set status = 0, lock_token = null, lock_expire_at = null, update_time = now() "
            + "where id = #{id} and status in (1, 2)")
    int release(@Param("id") Long id);

    /** 支付成功：锁定 → 已售 */
    @Update("update t_seat set status = 2, lock_token = null, lock_expire_at = null, update_time = now() "
            + "where id = #{id} and status = 1")
    int sell(@Param("id") Long id);
}
