package com.sky.entity;

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
@TableName("orders")
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String number;
    private Integer status;
    private Long addressBookId;
    private String phone;
    private BigDecimal amount;
    private LocalDateTime orderTime;
    private LocalDateTime checkoutTime;
    private LocalDateTime confirmTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime completionTime;
    private String remark;
    private String username;
    private String consignee;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;
}
