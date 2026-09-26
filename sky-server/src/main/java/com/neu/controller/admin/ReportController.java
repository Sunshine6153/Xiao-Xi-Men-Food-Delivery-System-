package com.neu.controller.admin;

import com.neu.result.Result;
import com.neu.service.ReportService;
import com.neu.vo.OrderReportVO;
import com.neu.vo.SalesTop10VO;
import com.neu.vo.TurnoverReportVO;
import com.neu.vo.UserReportVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Tag(name = "ReportController", description = "报表管理")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;
    @GetMapping("/turnover")
    @Tag(name = "turnoverReport", description = "营业额报表")
    public Result<TurnoverReportVO> turnoverReport(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("开始统计营业额，begin: {}, end: {}", begin, end);
        TurnoverReportVO turnoverReportVO = reportService.getTurnoverReport(begin, end);
        return Result.success(turnoverReportVO);
    }

    @GetMapping("/userreport")
    @Tag(name = "userReport", description = "用户报表")
    public Result<UserReportVO> userReport(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("开始统计用户数，begin: {}, end: {}", begin, end);
        UserReportVO userReportVO = reportService.getUserReport(begin, end);
        return Result.success(userReportVO);
    }

    @GetMapping("/ordersStatistics")
    @Tag(name = "ordersStatistics", description = "订单统计报表")
    public Result<OrderReportVO> ordersStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("开始统计订单数，begin: {}, end: {}", begin, end);
        OrderReportVO orderReportVO = reportService.getOrderReport(begin, end);
        return Result.success(orderReportVO);
    }

    @GetMapping("/salesTop10")
    @Tag(name = "salesTop10", description = "销售TOP10报表")
    public Result<SalesTop10VO> salesTop10(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("开始统计销售TOP10，begin: {}, end: {}", begin, end);
        SalesTop10VO salesTop10VO = reportService.getSalesTop10(begin, end);
        return Result.success(salesTop10VO);
    }


    @GetMapping("/export")
    @Tag(name = "export", description = "导出报表")
    public void export(HttpServletResponse response) {
        reportService.exportBusinessData(response);
    }
}
