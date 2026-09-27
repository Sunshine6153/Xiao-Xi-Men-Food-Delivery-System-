package com.neu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.neu.dto.MerchantOrderPageQueryDTO;
import com.neu.entity.Order;
import com.neu.exception.MerchantBusinessException;
import com.neu.mapper.OrderDetailMapper;
import com.neu.mapper.OrderMapper;
import com.neu.result.PageResult;
import com.neu.service.WorkspaceService;
import com.neu.vo.BusinessDataVO;
import com.neu.vo.MerchantOrderDishVO;
import com.neu.vo.MerchantOrderVO;
import com.neu.vo.OrderReportVO;
import com.neu.vo.OrderOverviewVO;
import com.neu.vo.SalesTop10VO;
import com.neu.vo.TurnoverReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;

    public WorkspaceServiceImpl(OrderMapper orderMapper,
                                OrderDetailMapper orderDetailMapper) {
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
    }

    @Override
    public BusinessDataVO getBusinessData(Long merchantId) {
        LocalDateTime begin = LocalDate.now().atStartOfDay();
        LocalDateTime end = begin.plusDays(1);

        BigDecimal turnover = orderMapper.getTodayTurnover(merchantId, begin, end);
        Integer orderCount = orderMapper.getTodayOrderCount(merchantId, begin, end);
        Integer completedOrderCount = orderMapper.getTodayCompletedOrderCount(merchantId, begin, end);

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

    @Override
    public OrderOverviewVO getOrderOverview(Long merchantId) {
        return orderMapper.getMerchantOrderOverview(merchantId);
    }

    @Override
    public PageResult pagePendingOrders(MerchantOrderPageQueryDTO query, Long merchantId) {
        if (query.getStatus() != null
                && query.getStatus() != 1
                && query.getStatus() != Order.READY_FOR_PICKUP) {
            throw new MerchantBusinessException(400, "只能查询待制作或待取餐订单");
        }

        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        PageHelper.startPage(page, pageSize);

        List<MerchantOrderVO> orders = orderMapper.pageMerchantPendingOrders(query, merchantId);
        PageInfo<MerchantOrderVO> pageInfo = new PageInfo<>(orders);
        if (orders.isEmpty()) {
            return new PageResult(pageInfo.getTotal(), Collections.emptyList());
        }

        List<Long> orderIds = orders.stream().map(MerchantOrderVO::getId).toList();
        Map<Long, List<MerchantOrderDishVO>> dishesByOrderId = orderDetailMapper
                .listMerchantDishes(orderIds, merchantId)
                .stream()
                .collect(Collectors.groupingBy(MerchantOrderDishVO::getOrderId));
        orders.forEach(order -> order.setDishes(
                dishesByOrderId.getOrDefault(order.getId(), Collections.emptyList())));

        return new PageResult(pageInfo.getTotal(), orders);
    }

    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end, Long merchantId) {
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.isEqual(end)) {
            begin = begin.plusDays(1);
            dates.add(begin);
        }

        List<BigDecimal> turnovers = new ArrayList<>();
        for (LocalDate date : dates) {
            LocalDateTime beginOfDay = date.atTime(0, 0, 0);
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            BigDecimal turnover = orderMapper.getMerchantTurnover(
                    merchantId, beginOfDay, endOfDay
            );
            if (turnover == null) {
                turnover = BigDecimal.ZERO;
            }
            turnovers.add(turnover);
        }

        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        turnoverReportVO.setDateList(StringUtils.join(dates, ","));
        turnoverReportVO.setTurnoverList(StringUtils.join(turnovers, ","));
        return turnoverReportVO;
    }

    @Override
    public OrderReportVO getOrderReport(LocalDate begin, LocalDate end, Long merchantId) {
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.isEqual(end)) {
            begin = begin.plusDays(1);
            dates.add(begin);
        }

        List<Integer> orderCounts = new ArrayList<>();
        List<Double> orderCompletionRates = new ArrayList<>();
        for (LocalDate date : dates) {
            LocalDateTime beginOfDay = date.atTime(0, 0, 0);
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            Integer orderCount = orderMapper.getMerchantOrderCount(
                    merchantId, beginOfDay, endOfDay, null
            );
            Integer completedOrderCount = orderMapper.getMerchantOrderCount(
                    merchantId, beginOfDay, endOfDay, Order.COMPLETED
            );
            orderCounts.add(orderCount);

            double orderCompletionRate = 0.0;
            if (orderCount != 0) {
                orderCompletionRate = completedOrderCount.doubleValue() / orderCount;
            }
            orderCompletionRates.add(orderCompletionRate);
        }

        OrderReportVO orderReportVO = new OrderReportVO();
        orderReportVO.setDateList(StringUtils.join(dates, ","));
        orderReportVO.setOrderCountList(StringUtils.join(orderCounts, ","));
        orderReportVO.setOrderCompletionRateList(StringUtils.join(orderCompletionRates, ","));
        return orderReportVO;
    }

    @Override
    public SalesTop10VO getSalesTop10(LocalDate begin, LocalDate end, Long merchantId) {
        List<String> nameList = orderMapper.getMerchantSalesTop10(merchantId, begin, end);
        List<Integer> numberList = orderDetailMapper.getMerchantSalesTop10Number(
                merchantId, begin, end
        );

        SalesTop10VO salesTop10VO = new SalesTop10VO();
        salesTop10VO.setNameList(StringUtils.join(nameList, ","));
        salesTop10VO.setNumberList(StringUtils.join(numberList, ","));
        return salesTop10VO;
    }
}
