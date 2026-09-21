package com.neu.mapper;

import com.neu.annotation.AutoFill;
import com.neu.entity.Order;
import com.neu.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface OrderMapper {

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
}
