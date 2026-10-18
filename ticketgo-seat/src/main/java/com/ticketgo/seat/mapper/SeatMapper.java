package com.ticketgo.seat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketgo.seat.entity.Seat;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SeatMapper extends BaseMapper<Seat> {

    /** 锁座（仅当可售时生效） */
    @Update("update t_seat set status = 1, lock_token = #{token}, lock_expire_at = #{expireAt}, update_time = now() "
            + "where id = #{id} and status = 0")
    int lockSeat(@Param("id") Long id, @Param("token") String token, @Param("expireAt") LocalDateTime expireAt);

    /** 释放锁座（仅释放本人持有且处于锁定态） */
    @Update("update t_seat set status = 0, lock_token = null, lock_expire_at = null, update_time = now() "
            + "where id = #{id} and status = 1 and lock_token = #{token}")
    int unlockSeat(@Param("id") Long id, @Param("token") String token);

    /** 支付成功后置已售 */
    @Update("update t_seat set status = 2, lock_token = null, lock_expire_at = null, update_time = now() where id = #{id}")
    int sellSeat(@Param("id") Long id);
}
