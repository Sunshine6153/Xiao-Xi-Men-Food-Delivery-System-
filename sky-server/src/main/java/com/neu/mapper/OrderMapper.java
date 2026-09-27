package com.neu.mapper;

import com.neu.annotation.AutoFill;
import com.neu.dto.MerchantOrderPageQueryDTO;
import com.neu.dto.OrderPageQueryDTO;
import com.neu.entity.Order;
import com.neu.enumeration.OperationType;
import com.neu.vo.MerchantOrderVO;
import com.neu.vo.OrderOverviewVO;
import com.neu.vo.OrderQueryVO;
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
            FOR UPDATE
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
            SELECT * FROM orders
            WHERE id = #{orderId}
            FOR UPDATE
            """)
    Order getByIdForUpdate(Long orderId);

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
              AND o.order_time <= #{end}
            """)
    BigDecimal getMerchantTurnover(@Param("merchantId") Long merchantId,
                                   @Param("begin") LocalDateTime begin,
                                   @Param("end") LocalDateTime end);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT o.id)
            FROM orders o
            INNER JOIN order_detail od ON od.order_id = o.id
            WHERE od.merchant_id = #{merchantId}
              AND o.order_time &gt;= #{begin}
              AND o.order_time &lt;= #{end}
            <choose>
                <when test="status != null">
                    AND o.status = #{status}
                </when>
                <otherwise>
                    AND o.status BETWEEN 3 AND 8
                </otherwise>
            </choose>
            </script>
            """)
    Integer getMerchantOrderCount(@Param("merchantId") Long merchantId,
                                  @Param("begin") LocalDateTime begin,
                                  @Param("end") LocalDateTime end,
                                  @Param("status") Integer status);

    @Select("""
            SELECT od.name
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
    List<String> getMerchantSalesTop10(@Param("merchantId") Long merchantId,
                                       @Param("begin") LocalDate begin,
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
              COUNT(DISTINCT CASE WHEN o.status BETWEEN 3 AND 7 AND od.status = 3 THEN o.id END) AS completed_preparation_orders,
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
                   MIN(od.status) AS merchant_status,
                   SUM(od.amount * od.number) AS merchant_amount,
                   o.order_time, o.order_delivery_time,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            INNER JOIN order_detail od ON od.order_id = o.id
            WHERE od.merchant_id = #{merchantId}
              AND o.status BETWEEN 3 AND 7
            <if test="query.number != null and query.number != ''">
              AND o.number LIKE CONCAT('%', #{query.number}, '%')
            </if>
            GROUP BY o.id, o.number, o.status, o.order_time, o.order_delivery_time,
                     o.consignee, o.phone, o.address, o.remark
            <if test="query.status != null">
              HAVING MIN(od.status) = #{query.status}
            </if>
            ORDER BY o.order_time DESC, o.id DESC
            </script>
            """)
    List<MerchantOrderVO> pageMerchantOrders(
            @Param("query") MerchantOrderPageQueryDTO query,
            @Param("merchantId") Long merchantId);

    @Select("""
            SELECT o.id, o.number, o.status,
                   MIN(od.status) AS merchant_status,
                   SUM(od.amount * od.number) AS merchant_amount,
                   o.order_time, o.order_delivery_time,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            INNER JOIN order_detail od ON od.order_id = o.id
            WHERE o.id = #{orderId}
              AND od.merchant_id = #{merchantId}
              AND o.status BETWEEN 3 AND 7
            GROUP BY o.id, o.number, o.status, o.order_time, o.order_delivery_time,
                     o.consignee, o.phone, o.address, o.remark
            """)
    MerchantOrderVO getMerchantOrderById(@Param("orderId") Long orderId,
                                         @Param("merchantId") Long merchantId);

    @Select("""
            <script>
            SELECT o.id, o.number, o.user_id, COALESCE(u.name, u.username) AS user_name,
                   o.delivery_user_id, COALESCE(du.name, du.username) AS delivery_user_name,
                   o.status, o.amount, o.delivery_fee, o.order_time, o.checkout_time,
                   o.order_delivery_time, o.delivered_time, o.tableware_amount,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            INNER JOIN user u ON u.id = o.user_id
            LEFT JOIN user du ON du.id = o.delivery_user_id
            WHERE 1 = 1
            <if test="query.number != null and query.number != ''">
              AND o.number LIKE CONCAT('%', #{query.number}, '%')
            </if>
            <if test="query.status != null">
              AND o.status = #{query.status}
            </if>
            <if test="query.phone != null and query.phone != ''">
              AND o.phone LIKE CONCAT('%', #{query.phone}, '%')
            </if>
            <if test="query.begin != null">
              AND o.order_time &gt;= #{query.begin}
            </if>
            <if test="query.end != null">
              AND o.order_time &lt; DATE_ADD(#{query.end}, INTERVAL 1 DAY)
            </if>
            ORDER BY o.order_time DESC, o.id DESC
            </script>
            """)
    List<OrderQueryVO> pageAdminOrders(@Param("query") OrderPageQueryDTO query);

    @Select("""
            <script>
            SELECT o.id, o.number, o.user_id, COALESCE(u.name, u.username) AS user_name,
                   o.delivery_user_id, COALESCE(du.name, du.username) AS delivery_user_name,
                   o.status, o.amount, o.delivery_fee, o.order_time, o.checkout_time,
                   o.order_delivery_time, o.delivered_time, o.tableware_amount,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            INNER JOIN user u ON u.id = o.user_id
            LEFT JOIN user du ON du.id = o.delivery_user_id
            WHERE o.user_id = #{userId}
            <if test="query.status != null">
              AND o.status = #{query.status}
            </if>
            ORDER BY o.order_time DESC, o.id DESC
            </script>
            """)
    List<OrderQueryVO> pageUserOrders(@Param("query") OrderPageQueryDTO query,
                                      @Param("userId") Long userId);

    @Select("""
            SELECT o.id, o.number, o.user_id, COALESCE(u.name, u.username) AS user_name,
                   o.delivery_user_id, COALESCE(du.name, du.username) AS delivery_user_name,
                   o.status, o.amount, o.delivery_fee, o.order_time, o.checkout_time,
                   o.order_delivery_time, o.delivered_time, o.tableware_amount,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            INNER JOIN user u ON u.id = o.user_id
            LEFT JOIN user du ON du.id = o.delivery_user_id
            WHERE o.id = #{orderId}
              AND (#{userId} IS NULL OR o.user_id = #{userId})
            """)
    OrderQueryVO getOrderQueryById(@Param("orderId") Long orderId,
                                   @Param("userId") Long userId);

    @Select("""
            SELECT o.id, o.number, o.user_id, o.delivery_user_id, o.status,
                   o.amount, o.delivery_fee, o.order_time, o.checkout_time,
                   o.order_delivery_time, o.delivered_time, o.tableware_amount,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            WHERE o.status = 2
              AND o.delivery_user_id IS NULL
              AND o.user_id != #{userId}
            ORDER BY o.order_time ASC, o.id ASC
            """)
    List<OrderQueryVO> pageAvailableDeliveryOrders(Long userId);

    @Select("""
            SELECT o.id, o.number, o.user_id, o.delivery_user_id, o.status,
                   o.amount, o.delivery_fee, o.order_time, o.checkout_time,
                   o.order_delivery_time, o.delivered_time, o.tableware_amount,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            WHERE o.delivery_user_id = #{userId}
            ORDER BY o.order_time DESC, o.id DESC
            """)
    List<OrderQueryVO> pageMyDeliveryOrders(Long userId);

    @Select("""
            SELECT o.id, o.number, o.user_id, o.delivery_user_id, o.status,
                   o.amount, o.delivery_fee, o.order_time, o.checkout_time,
                   o.order_delivery_time, o.delivered_time, o.tableware_amount,
                   o.consignee, o.phone, o.address, o.remark
            FROM orders o
            WHERE o.id = #{orderId}
              AND ((o.status = 2 AND o.delivery_user_id IS NULL AND o.user_id != #{userId})
                   OR o.delivery_user_id = #{userId})
            """)
    OrderQueryVO getDeliveryOrderById(@Param("orderId") Long orderId,
                                      @Param("userId") Long userId);

    @Update("""
            UPDATE orders
            SET delivery_user_id = #{userId}, status = 3
            WHERE id = #{orderId}
              AND status = 2
              AND delivery_user_id IS NULL
              AND user_id != #{userId}
            """)
    int acceptDeliveryOrder(@Param("orderId") Long orderId,
                            @Param("userId") Long userId);

    @Update("""
            UPDATE orders
            SET status = 5
            WHERE id = #{orderId}
              AND delivery_user_id = #{userId}
              AND status = 4
            """)
    int startDeliveryOrder(@Param("orderId") Long orderId,
                           @Param("userId") Long userId);

    @Update("""
            UPDATE orders
            SET status = 6, delivered_time = #{deliveredTime}
            WHERE id = #{orderId}
              AND delivery_user_id = #{userId}
              AND status = 5
            """)
    int completeDeliveryOrder(@Param("orderId") Long orderId,
                              @Param("userId") Long userId,
                              @Param("deliveredTime") LocalDateTime deliveredTime);

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
