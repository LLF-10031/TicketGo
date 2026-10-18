package com.ticketgo.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketgo.payment.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper extends BaseMapper<Order> {

    /** 待支付 → 已支付（条件更新保证幂等） */
    @Update("update t_order set status = 1, paid_at = now(), update_time = now() where id = #{id} and status = 0")
    int markPaid(@Param("id") Long id);
}
