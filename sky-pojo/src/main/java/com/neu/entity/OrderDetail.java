package com.neu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("order_detail")
public class OrderDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待商户确认 */
    public static final int PENDING_CONFIRMATION = 1;
    /** 制作中 */
    public static final int PREPARING = 2;
    /** 已出餐 */
    public static final int COMPLETED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long dishId;
    private Long merchantId;
    private String name;
    private String dishFlavor;
    private Integer number;
    private BigDecimal amount;
    private String image;
    /** 商户制作状态：1-待确认，2-制作中，3-已出餐 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
