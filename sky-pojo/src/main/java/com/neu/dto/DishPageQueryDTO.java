package com.neu.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DishPageQueryDTO implements Serializable {

    private Integer page;
    private Integer pageSize;
    private Long merchantId;
    private String name;
    private Long categoryId;
    private Integer status;
}
