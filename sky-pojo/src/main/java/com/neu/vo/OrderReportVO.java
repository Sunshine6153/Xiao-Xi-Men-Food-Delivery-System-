package com.neu.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderReportVO implements Serializable {
    private String dateList;
    private String orderCountList;
    private String orderCompletionRateList;
}
