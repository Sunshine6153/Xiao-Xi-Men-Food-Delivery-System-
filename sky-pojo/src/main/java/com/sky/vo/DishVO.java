package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DishVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long categoryId;
    private String name;
    private String image;
    private String description;
    private Integer status;
    private BigDecimal price;
    private Integer copies;
    private Integer sort;
    private String categoryName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
