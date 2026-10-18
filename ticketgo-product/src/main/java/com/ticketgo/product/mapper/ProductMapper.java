package com.ticketgo.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketgo.product.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProductMapper extends BaseMapper<Product> {

    /** 热度排序：按该演出所有票档已售数量之和倒序 */
    @Select("<script>" +
            "select p.* from t_product p " +
            "left join t_session s on s.product_id = p.id " +
            "left join t_ticket_tier t on t.session_id = s.id " +
            "where 1=1 " +
            "<if test='category != null and category != \"\"'> and p.category = #{category}</if> " +
            "<if test='status != null'> and p.status = #{status}</if> " +
            "<if test='keyword != null and keyword != \"\"'> and p.name like concat('%', #{keyword}, '%')</if> " +
            "group by p.id " +
            "order by coalesce(sum(t.sold_stock), 0) desc, p.create_time desc " +
            "limit #{offset}, #{size}" +
            "</script>")
    List<Product> selectHot(@Param("category") String category,
                            @Param("status") Integer status,
                            @Param("keyword") String keyword,
                            @Param("offset") int offset,
                            @Param("size") int size);

    @Select("<script>" +
            "select count(distinct p.id) from t_product p where 1=1 " +
            "<if test='category != null and category != \"\"'> and p.category = #{category}</if> " +
            "<if test='status != null'> and p.status = #{status}</if> " +
            "<if test='keyword != null and keyword != \"\"'> and p.name like concat('%', #{keyword}, '%')</if> " +
            "</script>")
    long countFiltered(@Param("category") String category,
                       @Param("status") Integer status,
                       @Param("keyword") String keyword);
}
