package com.neu.service.impl;

import com.neu.mapper.OrderDetailMapper;
import com.neu.mapper.OrderMapper;
import com.neu.mapper.UserMapper;
import com.neu.service.ReportService;
import com.neu.vo.BusinessDataVO;
import com.neu.vo.OrderReportVO;
import com.neu.vo.SalesTop10VO;
import com.neu.vo.TurnoverReportVO;
import com.neu.vo.UserReportVO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class ReportServiceImpl implements ReportService{
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end){
        //存放日期的集合
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.isEqual(end)){
            begin = begin.plusDays(1);
            dates.add(begin);
        }
        //存放收入的集合
        List<BigDecimal> turnovers = new ArrayList<>();
        for (LocalDate date : dates) {
            //统计当天已完成的营业额，订单状态应为已完成
            LocalDateTime beginOfDay = date.atTime(0, 0, 0);
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            Map map = new HashMap<>();
            map.put("begin", beginOfDay);
            map.put("end", endOfDay);
            map.put("status", 7);
            BigDecimal turnover = orderMapper.getTurnover(map);
            if (turnover == null) {
                turnover = BigDecimal.ZERO;
            }
            turnovers.add(turnover);
        }

        String datesStr = StringUtils.join(dates, ",");
        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        turnoverReportVO.setDateList(datesStr);
        turnoverReportVO.setTurnoverList(StringUtils.join(turnovers, ","));
        return turnoverReportVO;
    }

    public UserReportVO getUserReport(LocalDate begin, LocalDate end){
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.isEqual(end)){
            begin = begin.plusDays(1);
            dates.add(begin);
        }

        List<Integer> userCounts = new ArrayList<>();
        List<Integer> newUserCounts = new ArrayList<>();
        for (LocalDate date : dates) {
            LocalDateTime beginOfDay = date.atTime(0, 0, 0);
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            Map map = new HashMap<>();
            map.put("begin", beginOfDay);
            map.put("end", endOfDay);
            Integer userCount = userMapper.getOrderByTime(map);
            Integer newUserCount = userMapper.getNewOrderByTime(map);
            userCounts.add(userCount);
            newUserCounts.add(newUserCount);
        }
        String datesStr = StringUtils.join(dates, ",");
        UserReportVO userReportVO = new UserReportVO();
        userReportVO.setDateList(datesStr);
        userReportVO.setUserCountList(StringUtils.join(userCounts, ","));
        userReportVO.setNewUserCountList(StringUtils.join(newUserCounts, ","));
        return userReportVO;
    }

    public OrderReportVO getOrderReport(LocalDate begin, LocalDate end){
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.isEqual(end)){
            begin = begin.plusDays(1);
            dates.add(begin);
        }

        List<Integer> orderCounts = new ArrayList<>();
        List<Double> orderCompletionRates = new ArrayList<>();
        for (LocalDate date : dates) {
            LocalDateTime beginOfDay = date.atTime(0, 0, 0);
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            Map map = new HashMap<>();
            map.put("begin", beginOfDay);
            map.put("end", endOfDay);
            Integer orderCount = orderMapper.getOrderCount(map);
            map.put("status", 7);
            Integer validOrderCount = orderMapper.getOrderCount(map);
            orderCounts.add(orderCount);
            Double orderCompletionRate = 0.0;
            if (orderCount != 0) {
                orderCompletionRate = validOrderCount.doubleValue() / orderCount;
            }
            orderCompletionRates.add(orderCompletionRate);
        }

        String datesStr = StringUtils.join(dates, ",");
        OrderReportVO orderReportVO = new OrderReportVO();
        orderReportVO.setDateList(datesStr);
        orderReportVO.setOrderCountList(StringUtils.join(orderCounts, ","));
        orderReportVO.setOrderCompletionRateList(StringUtils.join(orderCompletionRates, ","));
        return orderReportVO;
    }

    public SalesTop10VO getSalesTop10(LocalDate begin, LocalDate end){
        List nameList = new ArrayList<>();
        List numberList = new ArrayList<>();
        nameList = orderMapper.getSalesTop10(begin, end);
        numberList = orderDetailMapper.getSalesTop10Number(begin, end);
        SalesTop10VO salesTop10VO = new SalesTop10VO();
        salesTop10VO.setNameList(StringUtils.join(nameList, ","));
        salesTop10VO.setNumberList(StringUtils.join(numberList, ","));
        return salesTop10VO;
    }

    public void exportBusinessData(HttpServletResponse response){
        //查询业务数据
        LocalDate begin = LocalDate.now().minusDays(29);
        LocalDate end = LocalDate.now();
        BusinessDataVO businessDataVO = getBusinessData(begin, end);

        //用POI导入数据
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("template/运营数据报表模板.xlsx");
        if (inputStream == null) {
            throw new RuntimeException("运营数据报表模板不存在");
        }

        try (inputStream; XSSFWorkbook excel = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = excel.getSheet("运营数据");

            //填充统计时间
            sheet.getRow(1).getCell(0).setCellValue("统计时间：" + begin + " 至 " + end);

            //填充概览数据
            sheet.getRow(3).getCell(1).setCellValue(businessDataVO.getTurnover().doubleValue());
            sheet.getRow(3).getCell(3).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet.getRow(4).getCell(1).setCellValue(businessDataVO.getOrderCount());
            sheet.getRow(4).getCell(3).setCellValue(businessDataVO.getUnitPrice().doubleValue());

            //填充明细数据
            int rowIndex = 7;
            LocalDate date = begin;
            while (!date.isAfter(end)) {
                BusinessDataVO dailyBusinessDataVO = getBusinessData(date, date);
                XSSFRow row = sheet.getRow(rowIndex);
                row.getCell(0).setCellValue(java.sql.Date.valueOf(date));
                row.getCell(1).setCellValue(dailyBusinessDataVO.getTurnover().doubleValue());
                row.getCell(2).setCellValue(dailyBusinessDataVO.getOrderCount());
                row.getCell(3).setCellValue(dailyBusinessDataVO.getOrderCompletionRate());
                row.getCell(4).setCellValue(dailyBusinessDataVO.getUnitPrice().doubleValue());
                date = date.plusDays(1);
                rowIndex++;
            }

            //通过输出流在浏览器下载文件
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            excel.write(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("运营数据报表导出失败", e);
        }
    }

    private BusinessDataVO getBusinessData(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = begin.atTime(0, 0, 0);
        LocalDateTime endTime = end.atTime(23, 59, 59);

        Map map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);

        Integer orderCount = orderMapper.getOrderCount(map);
        map.put("status", 7);
        Integer completedOrderCount = orderMapper.getOrderCount(map);
        BigDecimal turnover = orderMapper.getTurnover(map);

        if (turnover == null) {
            turnover = BigDecimal.ZERO;
        }
        if (orderCount == null) {
            orderCount = 0;
        }
        if (completedOrderCount == null) {
            completedOrderCount = 0;
        }

        double orderCompletionRate = 0.0;
        if (orderCount != 0) {
            orderCompletionRate = completedOrderCount.doubleValue() / orderCount;
        }

        BigDecimal unitPrice = BigDecimal.ZERO;
        if (completedOrderCount != 0) {
            unitPrice = turnover.divide(
                    BigDecimal.valueOf(completedOrderCount),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return BusinessDataVO.builder()
                .turnover(turnover)
                .orderCount(orderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .build();
    }
}
