package com.neu.service;

import com.neu.vo.OrderReportVO;
import com.neu.vo.SalesTop10VO;
import com.neu.vo.UserReportVO;
import com.neu.vo.TurnoverReportVO;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end);

    UserReportVO getUserReport(LocalDate begin, LocalDate end);

    OrderReportVO getOrderReport(LocalDate begin, LocalDate end);

    SalesTop10VO getSalesTop10(LocalDate begin, LocalDate end);

    void exportBusinessData(LocalDate begin, LocalDate end, HttpServletResponse response);
}
