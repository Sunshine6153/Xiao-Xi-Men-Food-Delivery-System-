package com.neu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class OrderSubmitDTO implements Serializable {

    private Long addressId;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime orderDeliveryTime;

    private Integer DeliveryStatus;

    private Integer TablewareAmount;

    private BigDecimal DeliveryFee;

    private BigDecimal OrderAmount;
}
