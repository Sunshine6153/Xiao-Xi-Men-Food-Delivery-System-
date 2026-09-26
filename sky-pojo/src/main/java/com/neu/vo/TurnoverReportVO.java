package com.neu.vo;

import lombok.Data;

import java.io.Serializable;
@Data
public class TurnoverReportVO implements Serializable {
    // 日期列表
    private String dateList;
    // 收入列表
    private String turnoverList;
}
