package com.neu.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DishFlavorDTO implements Serializable {

    private String name;
    private String value;
}
