package com.neu.controller.user;

import com.neu.context.BaseContext;
import com.neu.dto.OrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.result.Result;
import com.neu.service.OrderQueryService;
import com.neu.service.OrderService;
import com.neu.vo.OrderQueryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@Tag(name = "用户端订单查询")
@RestController
@RequestMapping("/user/order")
public class UserOrderQueryController {

    private final OrderQueryService orderQueryService;
    private final OrderService orderService;

    public UserOrderQueryController(OrderQueryService orderQueryService,
                                    OrderService orderService) {
        this.orderQueryService = orderQueryService;
        this.orderService = orderService;
    }

    @Operation(summary = "分页查询我的订单")
    @GetMapping("/page")
    public Result<PageResult> page(OrderPageQueryDTO query) {
        return Result.success(orderQueryService.pageForUser(
                query, BaseContext.getCurrentId()
        ));
    }

    @Operation(summary = "查询我的订单详情")
    @GetMapping("/{id}")
    public Result<OrderQueryVO> getById(@PathVariable Long id) {
        return Result.success(orderQueryService.getForUser(
                id, BaseContext.getCurrentId()
        ));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    @Operation(summary = "确认收餐")
    @PutMapping("/confirm/{id}")
    public Result confirmReceipt(@PathVariable Long id) {
        orderService.confirmReceipt(id);
        return Result.success();
    }
}
