package com.neu.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessDataVO implements Serializable {

    private BigDecimal turnover;
    private Integer orderCount;
    private Double orderCompletionRate;
    private BigDecimal unitPrice;
}
