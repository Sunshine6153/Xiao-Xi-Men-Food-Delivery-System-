package com.neu.controller.admin;

import com.neu.constant.JwtClaimsConstant;
import com.neu.dto.MerchantOrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.result.Result;
import com.neu.service.WorkspaceService;
import com.neu.vo.BusinessDataVO;
import com.neu.vo.OrderReportVO;
import com.neu.vo.OrderOverviewVO;
import com.neu.vo.SalesTop10VO;
import com.neu.vo.TurnoverReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "商户工作台")
@RestController
@RequestMapping("/admin/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Operation(summary = "查询商户今日经营数据")
    @SecurityRequirement(name = "token")
    @GetMapping("/businessData")
    public Result<BusinessDataVO> businessData(HttpServletRequest request) {
        return Result.success(workspaceService.getBusinessData(merchantId(request)));
    }

    @Operation(summary = "查询商户各状态订单数量")
    @SecurityRequirement(name = "token")
    @GetMapping("/overviewOrders")
    public Result<OrderOverviewVO> overviewOrders(HttpServletRequest request) {
        return Result.success(workspaceService.getOrderOverview(merchantId(request)));
    }

    @Operation(summary = "分页查询待制作和待取餐订单详情")
    @SecurityRequirement(name = "token")
    @GetMapping("/pendingOrders")
    public Result<PageResult> pendingOrders(MerchantOrderPageQueryDTO query,
                                            HttpServletRequest request) {
        return Result.success(workspaceService.pagePendingOrders(query, merchantId(request)));
    }

    @Operation(summary = "查询商户营业额趋势")
    @SecurityRequirement(name = "token")
    @GetMapping("/turnover")
    public Result<TurnoverReportVO> turnover(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            HttpServletRequest request) {
        return Result.success(workspaceService.getTurnoverReport(begin, end, merchantId(request)));
    }

    @Operation(summary = "查询商户订单数量和成交率")
    @SecurityRequirement(name = "token")
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            HttpServletRequest request) {
        return Result.success(workspaceService.getOrderReport(begin, end, merchantId(request)));
    }

    @Operation(summary = "查询商户菜品销量TOP10")
    @SecurityRequirement(name = "token")
    @GetMapping("/salesTop10")
    public Result<SalesTop10VO> salesTop10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            HttpServletRequest request) {
        return Result.success(workspaceService.getSalesTop10(begin, end, merchantId(request)));
    }

    private Long merchantId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtClaimsConstant.MERCHANT_ID);
    }
}
