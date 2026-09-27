package com.neu.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderQueryVO implements Serializable {

    private Long id;
    private String number;
    private Long userId;
    private String userName;
    private Long deliveryUserId;
    private String deliveryUserName;
    private Integer status;
    private BigDecimal amount;
    private BigDecimal deliveryFee;
    private LocalDateTime orderTime;
    private LocalDateTime checkoutTime;
    private LocalDateTime orderDeliveryTime;
    private LocalDateTime deliveredTime;
    private Integer tablewareAmount;
    private String consignee;
    private String phone;
    private String address;
    private String remark;
    private List<OrderQueryDishVO> dishes;
}
