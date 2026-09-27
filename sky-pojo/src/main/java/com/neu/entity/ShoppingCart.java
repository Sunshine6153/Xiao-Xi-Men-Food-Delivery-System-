package com.neu.entity;

import com.baomidou.mybatisplus.annotation.*;
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
@TableName("shopping_cart")
public class ShoppingCart implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long dishId;
    private Long merchantId;
    private String dishFlavor;
    @TableField(exist = false)
    private String name;
    @TableField(exist = false)
    private BigDecimal price;
    private Integer number;
    @TableField(exist = false)
    private String image;
    @TableField(exist = false)
    private String merchantName;
    @TableField(exist = false)
    private String merchantLocation;
    private LocalDateTime createTime;
}
