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
@TableName("orders")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待支付 */
    public static final int PENDING_PAYMENT = 1;
    /** 待接单 */
    public static final int PENDING_ACCEPTANCE = 2;
    /** 制作中 */
    public static final int PREPARING = 3;
    /** 待取餐 */
    public static final int READY_FOR_PICKUP = 4;
    /** 配送中 */
    public static final int DELIVERING = 5;
    /** 待确认收餐 */
    public static final int PENDING_RECEIPT = 6;
    /** 已完成 */
    public static final int COMPLETED = 7;
    /** 已取消 */
    public static final int CANCELLED = 8;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String number;
    private Long deliveryUserId;
    /**
     * 订单状态：
     * 1-待支付，2-待接单，3-制作中，4-待取餐，
     * 5-配送中，6-待确认收餐，7-已完成，8-已取消。
     */
    private Integer status;
    private BigDecimal amount;
    private BigDecimal deliveryFee;
    private LocalDateTime orderTime;
    private LocalDateTime checkoutTime;
    private LocalDateTime orderDeliveryTime;
    private LocalDateTime deliveredTime;
    private Integer tablewareAmount;
    private String consignee;
    private String phone;
    private String address;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
