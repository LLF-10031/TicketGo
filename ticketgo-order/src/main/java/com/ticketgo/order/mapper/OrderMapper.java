package com.ticketgo.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketgo.order.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper extends BaseMapper<Order> {

    /** 待支付 → 已支付（条件更新保证幂等） */
    @Update("update t_order set status = 1, paid_at = now(), update_time = now() where id = #{id} and status = 0")
    int markPaid(@Param("id") Long id);

    /** 待支付 → 已取消（条件更新保证幂等） */
    @Update("update t_order set status = 2, cancelled_at = now(), update_time = now() where id = #{id} and status = 0")
    int markCancelled(@Param("id") Long id);
}
