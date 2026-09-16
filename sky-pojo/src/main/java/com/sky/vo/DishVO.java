package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.sky.entity.DishFlavor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DishVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long categoryId;
    private Long merchantId;
    private String name;
    private String image;
    private String description;
    private Integer status;
    private BigDecimal price;
    private String categoryName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<DishFlavor> flavors;
}
