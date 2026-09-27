package com.neu.controller.admin;

import com.neu.dto.OrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.result.Result;
import com.neu.service.OrderQueryService;
import com.neu.vo.OrderQueryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员订单查询")
@RestController
@RequestMapping("/admin/platform/order")
public class AdminOrderController {

    private final OrderQueryService orderQueryService;

    public AdminOrderController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @Operation(summary = "分页查询平台订单")
    @SecurityRequirement(name = "token")
    @GetMapping("/page")
    public Result<PageResult> page(OrderPageQueryDTO query) {
        return Result.success(orderQueryService.pageForAdmin(query));
    }

    @Operation(summary = "查询平台订单详情")
    @SecurityRequirement(name = "token")
    @GetMapping("/{id}")
    public Result<OrderQueryVO> getById(@PathVariable Long id) {
        return Result.success(orderQueryService.getForAdmin(id));
    }
}
