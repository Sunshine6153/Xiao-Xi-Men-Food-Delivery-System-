package com.sky.vo;

import com.sky.entity.ShoppingCart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCartVO extends ShoppingCart {

    private static final long serialVersionUID = 1L;

    private String categoryName;
    private Integer dishCopies;
}
