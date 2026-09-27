package com.neu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.entity.OrderDetail;
import com.neu.vo.MerchantOrderDishVO;
import com.neu.vo.OrderQueryDishVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    @Insert("""
            <script>
            INSERT INTO order_detail
            (order_id, dish_id, merchant_id, name, dish_flavor, number, amount, image)
            VALUES
            <foreach collection="orderDetails" item="orderDetail" separator=",">
                (#{orderDetail.orderId}, #{orderDetail.dishId}, #{orderDetail.merchantId},
                 #{orderDetail.name}, #{orderDetail.dishFlavor}, #{orderDetail.number},
                 #{orderDetail.amount}, #{orderDetail.image})
            </foreach>
            </script>
            """)
    void insertBatch(@Param("orderDetails") List<OrderDetail> orderDetails);

    @Select("""
        SELECT DISTINCT merchant_id
        FROM order_detail
        WHERE order_id = #{orderId}
        """)
    List<Long> getMerchantIdsByOrderId(@Param("orderId") Long orderId);

    @Select("""
            SELECT SUM(od.number)
            FROM order_detail od
            JOIN orders o ON od.order_id = o.id
            WHERE o.order_time >= #{begin}
              AND o.order_time < DATE_ADD(#{end}, INTERVAL 1 DAY)
              AND o.status = 7
            GROUP BY od.name
            ORDER BY SUM(od.number) DESC, od.name ASC
            LIMIT 10
            """)
    List<Integer> getSalesTop10Number(@Param("begin") LocalDate begin,
                                      @Param("end") LocalDate end);

    @Select("""
            SELECT SUM(od.number)
            FROM order_detail od
            INNER JOIN orders o ON od.order_id = o.id
            WHERE od.merchant_id = #{merchantId}
              AND o.order_time >= #{begin}
              AND o.order_time < DATE_ADD(#{end}, INTERVAL 1 DAY)
              AND o.status = 7
            GROUP BY od.name
            ORDER BY SUM(od.number) DESC, od.name ASC
            LIMIT 10
            """)
    List<Integer> getMerchantSalesTop10Number(@Param("merchantId") Long merchantId,
                                              @Param("begin") LocalDate begin,
                                              @Param("end") LocalDate end);

    @Select("""
            <script>
            SELECT id, order_id, dish_id, name, dish_flavor,
                   number, amount, image, status
            FROM order_detail
            WHERE merchant_id = #{merchantId}
              AND order_id IN
              <foreach collection="orderIds" item="orderId" open="(" separator="," close=")">
                #{orderId}
              </foreach>
            ORDER BY order_id DESC, id ASC
            </script>
            """)
    List<MerchantOrderDishVO> listMerchantDishes(
            @Param("orderIds") List<Long> orderIds,
            @Param("merchantId") Long merchantId);

    @Update("""
            UPDATE order_detail
            SET status = #{status}
            WHERE order_id = #{orderId}
              AND merchant_id = #{merchantId}
              AND status = #{expectedStatus}
            """)
    int updateMerchantStatus(@Param("orderId") Long orderId,
                             @Param("merchantId") Long merchantId,
                             @Param("status") Integer status,
                             @Param("expectedStatus") Integer expectedStatus);

    @Select("""
            SELECT COUNT(*)
            FROM order_detail
            WHERE order_id = #{orderId}
              AND status != 3
            """)
    int countUnfinishedByOrderId(Long orderId);

    @Select("""
            <script>
            SELECT od.id, od.order_id, od.dish_id, od.merchant_id,
                   m.merchant_name, m.location AS merchant_location, od.status,
                   od.name, od.dish_flavor,
                   od.number, od.amount, od.image
            FROM order_detail od
            LEFT JOIN merchant m ON m.id = od.merchant_id
            WHERE od.order_id IN
            <foreach collection="orderIds" item="orderId" open="(" separator="," close=")">
                #{orderId}
            </foreach>
            ORDER BY od.order_id DESC, od.merchant_id ASC, od.id ASC
            </script>
            """)
    List<OrderQueryDishVO> listOrderDishes(@Param("orderIds") List<Long> orderIds);
}
