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
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String number;
    private Integer status;
    private BigDecimal amount;
    private LocalDateTime orderTime;
    private String userName;
    private String phone;
    private String address;
    private String remark;
    private String consignee;
}
