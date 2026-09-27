package com.neu.dto;

import lombok.Data;

import java.io.Serializable;


@Data
public class ShoppingCartDTO implements Serializable {

    private Long id;
    private  Long dishId;
    private  String dishFlavor;
    private Integer number;

}
