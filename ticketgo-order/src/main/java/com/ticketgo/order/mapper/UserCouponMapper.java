package com.ticketgo.order.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户券释放 / 核销（与 coupon 共用同一库）
 */
public interface UserCouponMapper {

    /** 锁定 → 未用（订单取消/超时释放） */
    @Update("update t_user_coupon set status = 0, lock_order_id = null, update_time = now() "
            + "where lock_order_id = #{orderId} and status = 1")
    int releaseByOrder(@Param("orderId") Long orderId);

    /** 锁定 → 已用（支付成功核销） */
    @Update("update t_user_coupon set status = 2, used_at = now(), update_time = now() "
            + "where lock_order_id = #{orderId} and status = 1")
    int useByOrder(@Param("orderId") Long orderId);
}
