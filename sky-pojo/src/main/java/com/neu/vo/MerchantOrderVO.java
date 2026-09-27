package com.neu.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MerchantOrderVO implements Serializable {

    private Long id;
    private String number;
    /** 整单状态 */
    private Integer status;
    /** 当前商户制作状态：1-待确认，2-制作中，3-已出餐 */
    private Integer merchantStatus;
    private BigDecimal merchantAmount;
    private LocalDateTime orderTime;
    private LocalDateTime orderDeliveryTime;
    private String consignee;
    private String phone;
    private String address;
    private String remark;
    private List<MerchantOrderDishVO> dishes;
}
