package com.neu.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantOrderPageQueryDTO implements Serializable {

    private Integer page;
    private Integer pageSize;
    private String number;
    /** 商户制作状态：1-待确认，2-制作中，3-已出餐 */
    private Integer status;
}
