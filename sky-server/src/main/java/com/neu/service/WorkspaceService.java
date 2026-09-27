package com.neu.service;

import com.neu.dto.MerchantOrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.vo.BusinessDataVO;
import com.neu.vo.OrderReportVO;
import com.neu.vo.OrderOverviewVO;
import com.neu.vo.SalesTop10VO;
import com.neu.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface WorkspaceService {

    BusinessDataVO getBusinessData(Long merchantId);

    OrderOverviewVO getOrderOverview(Long merchantId);

    PageResult pagePendingOrders(MerchantOrderPageQueryDTO query, Long merchantId);

    TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end, Long merchantId);

    OrderReportVO getOrderReport(LocalDate begin, LocalDate end, Long merchantId);

    SalesTop10VO getSalesTop10(LocalDate begin, LocalDate end, Long merchantId);
}
