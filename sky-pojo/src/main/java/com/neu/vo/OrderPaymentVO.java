package com.neu.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrderPaymentVO implements Serializable {

    private String event;
    private Long orderId;
    private String orderNumber;
    private Integer status;
    private LocalDateTime paymentTime;
}
