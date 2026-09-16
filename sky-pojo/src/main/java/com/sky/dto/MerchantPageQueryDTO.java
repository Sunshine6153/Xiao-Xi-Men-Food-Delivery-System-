package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantPageQueryDTO implements Serializable {
    private Integer page;
    private Integer pageSize;
    //可选
    private String name;
}
