package com.neu.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DishDTO implements Serializable {

    private Long id;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private String image;
    private String description;
    private List<DishFlavorDTO> flavors;
}
