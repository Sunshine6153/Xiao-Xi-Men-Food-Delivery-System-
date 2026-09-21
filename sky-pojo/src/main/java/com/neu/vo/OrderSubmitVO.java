package com.neu.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class OrderSubmitVO implements Serializable {
    //订单id
    private Long id;
    //订单编号
    private String orderNumber;
    //订单金额
    private BigDecimal orderAmount;
    //订单时间
    private LocalDateTime orderTime;
}
