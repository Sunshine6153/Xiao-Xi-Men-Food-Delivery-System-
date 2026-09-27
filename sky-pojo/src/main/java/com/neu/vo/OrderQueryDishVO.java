package com.neu.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderQueryDishVO implements Serializable {

    private Long id;
    private Long orderId;
    private Long dishId;
    private Long merchantId;
    private String merchantName;
    private String merchantLocation;
    private Integer status;
    private String name;
    private String dishFlavor;
    private Integer number;
    private BigDecimal amount;
    private String image;
}
