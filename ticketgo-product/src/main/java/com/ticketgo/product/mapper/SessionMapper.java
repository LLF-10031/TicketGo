package com.ticketgo.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketgo.product.entity.Session;
import org.apache.ibatis.annotations.Update;

public interface SessionMapper extends BaseMapper<Session> {

    /** 到开票时间：待开票 → 售票中 */
    @Update("update t_session set status = 1, update_time = now() " +
            "where status = 0 and sale_start_time is not null and sale_start_time <= now()")
    int openSales();

    /** 过截止时间：待开票/售票中 → 已结束 */
    @Update("update t_session set status = 3, update_time = now() " +
            "where status in (0, 1) and sale_end_time is not null and sale_end_time < now()")
    int closeSales();
}
