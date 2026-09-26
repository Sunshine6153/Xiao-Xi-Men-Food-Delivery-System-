package com.neu.mapper;

import com.neu.annotation.AutoFill;
import com.neu.dto.MerchantOrderPageQueryDTO;
import com.neu.entity.Order;
import com.neu.enumeration.OperationType;
import com.neu.vo.MerchantOrderVO;
import com.neu.vo.OrderOverviewVO;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    @Select("""
            SELECT SUM(amount) FROM orders
            WHERE order_time >= #{begin} AND order_time <= #{end} AND status = #{status}
            """)
    BigDecimal getTurnover(Map map);

    @Select("""
            <script>
            SELECT COUNT(*) FROM orders
            WHERE order_time &gt;= #{begin} AND order_time &lt;= #{end}
            <if test="status != null">
                AND status = #{status}
            </if>
            </script>
            """)
    Integer getOrderCount(Map map);

    @Insert("""
            INSERT INTO orders
            (`number`, user_id, delivery_user_id, status, amount, delivery_fee,
             order_time, checkout_time, order_delivery_time, delivered_time,
             tableware_amount, consignee, phone, address, remark, create_time, update_time)
            VALUES
            (#{number}, #{userId}, #{deliveryUserId}, #{status}, #{amount}, #{deliveryFee},
             #{orderTime}, #{checkoutTime}, #{orderDeliveryTime}, #{deliveredTime},
             #{tablewareAmount}, #{consignee}, #{phone}, #{address}, #{remark},
             #{createTime}, #{updateTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @AutoFill(OperationType.INSERT)
    void insert(Order order);

    @Select("""
            SELECT * FROM orders
            WHERE `number` = #{orderNumber} AND user_id = #{userId}
            """)
    Order getByNumberAndUserId(@Param("orderNumber") String orderNumber,
                               @Param("userId") Long userId);

    //查询未付款且超时超过15分钟的订单
    @Select("""
            SELECT * FROM orders
            WHERE status = 1 AND order_time < DATE_SUB(NOW(), INTERVAL 15 MINUTE)
            """)
    List<Order> selectUnpaidAndTimeoutOrder();

    @Update("""
            UPDATE orders
            SET status = #{status}
            WHERE id = #{id} AND status = #{expectedStatus}
            """)
    int updateOrderStatusById(@Param("id") Long id,
                              @Param("status") int status,
                              @Param("expectedStatus") int expectedStatus);

    @Select("""
            SELECT * FROM orders
            WHERE status = 6
            """)
    List<Order> selectDeliveredUnconfirmedOrder();

    @Update("""
            UPDATE orders
            SET status = #{status}, checkout_time = #{checkoutTime}
            WHERE id = #{id}
            """)
    void update(Order orderUpdate);

    @Select("""
            SELECT * FROM orders
            WHERE id = #{orderId}
            """)
    Order getById(Long orderId);

    @Select("""
            SELECT od.name
            FROM order_detail od
            JOIN orders o ON od.order_id = o.id
            WHERE o.order_time >= #{begin}
              AND o.order_time < DATE_ADD(#{end}, INTERVAL 1 DAY)
              AND o.status = 7
            GROUP BY od.name
            ORDER BY SUM(od.number) DESC, od.name ASC
            LIMIT 10
            """)
    List<String> getSalesTop10(@Param("begin") LocalDate begin,
                               @Param("end") LocalDate end);

    @Select("""
            SELECT COALESCE(SUM(od.amount * od.number), 0)
            FROM orders o
            INNER JOIN order_detail od ON od.order_id = o.id
            WHERE od.merchant_id = #{merchantId}
              AND o.status = 7
              AND o.order_time >= #{begin}
              AND o.order_time < #{end}
            """)
    BigDecimal getTodayTurnover(@Param("merchantId") Long merchantId,
                                @Param("begin") LocalDateTime begin,
                                @Param("end") LocalDateTime end);

    @Select("""
            SELECT COUNT(DISTINCT o.id)
            FROM orders o
            INNER JOIN order_detail od ON od.order_id = o.id
            WHERE od.merchant_id = #{merchantId}
              AND o.status BETWEEN 3 AND 8
              AND o.order_time >= #{begin}
              AND o.order_time < #{end}
            """)
    Integer getTodayOrderCount(@Param("merchantId") Long merchantId,
                               @Param("begin") LocalDateTime begin,
                               @Param("end") LocalDateTime end);

    @Select("""
            SELECT COUNT(DISTINCT o.id)
            FROM orders o
            INNER JOIN order_detail od ON od.order_id = o.id
            WHERE od.merchant_id = #{merchantId}
              AND o.status = 7
              AND o.order_time >= #{begin}
              AND o.order_time < #{end}
            """)
    Integer getTodayCompletedOrderCount(@Param("merchantId") Long merchantId,
                                        @Param("begin") LocalDateTime begin,
                                        @Param("end") LocalDateTime end);

    @Select("""
            SELECT
              COUNT(DISTINCT CASE WHEN o.status = 3 AND od.status = 1 THEN o.id END) AS pending_preparation_orders,
              COUNT(DISTINCT CASE WHEN o.status = 3 AND od.status = 2 THEN o.id END) AS preparing_orders,
              COUNT(DISTINCT CASE WHEN o.status = 4 THEN o.id END) AS ready_for_pickup_orders,
              COUNT(DISTINCT CASE WHEN o.status = 5 THEN o.id END) AS delivering_orders,
              COUNT(DISTINCT CASE WHEN o.status = 6 THEN o.id END) AS pending_receipt_orders,
              COUNT(DISTINCT CASE WHEN o.status = 7 THEN o.id END) AS completed_orders,
              COUNT(DISTINCT CASE WHEN o.status = 8 THEN o.id END) AS cancelled_orders,
              COUNT(DISTINCT CASE WHEN o.status BETWEEN 3 AND 8 THEN o.id END) AS all_orders
            FROM orders o
            INNER JOIN order_detail od ON od.order_id = o.id
            WHERE od.merchant_id = #{merchantId}
            """)
    OrderOverviewVO getMerchantOrderOverview(Long merchantId);

    @Select("""
            <script>
            SELECT o.id, o.number, o.status,
                   SUM(od.amount * od.number) AS merchant_amount,
                   o.order_time, o.order_delivery_time,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            INNER JOIN order_detail od ON od.order_id = o.id
            WHERE od.merchant_id = #{merchantId}
              AND (
                (o.status = 3 AND EXISTS (
                  SELECT 1 FROM order_detail od2
                  WHERE od2.order_id = o.id
                    AND od2.merchant_id = #{merchantId}
                    AND od2.status = 1
                ))
                OR o.status = 4
              )
            <if test="query.status != null">
              <choose>
                <when test="query.status == 1">
                  AND o.status = 3
                </when>
                <when test="query.status == 4">
                  AND o.status = 4
                </when>
              </choose>
            </if>
            GROUP BY o.id, o.number, o.status, o.order_time, o.order_delivery_time,
                     o.consignee, o.phone, o.address, o.remark
            ORDER BY o.order_time DESC, o.id DESC
            </script>
            """)
    List<MerchantOrderVO> pageMerchantPendingOrders(
            @Param("query") MerchantOrderPageQueryDTO query,
            @Param("merchantId") Long merchantId);
}
