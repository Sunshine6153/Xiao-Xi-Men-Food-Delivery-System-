package com.neu.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class MerchantOrderDishVO implements Serializable {

    private Long id;
    private Long orderId;
    private Long dishId;
    private String name;
    private String dishFlavor;
    private Integer number;
    private BigDecimal amount;
    private String image;
    private Integer status;
}
