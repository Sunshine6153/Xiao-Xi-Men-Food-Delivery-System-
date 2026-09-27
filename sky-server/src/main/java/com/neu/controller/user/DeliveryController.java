package com.neu.controller.user;

import com.neu.context.BaseContext;
import com.neu.dto.OrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.result.Result;
import com.neu.service.DeliveryService;
import com.neu.vo.OrderQueryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端帮带配送")
@RestController
@RequestMapping("/user/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Operation(summary = "分页查询待接帮带订单")
    @GetMapping("/available")
    public Result<PageResult> available(OrderPageQueryDTO query) {
        return Result.success(deliveryService.pageAvailable(
                query, BaseContext.getCurrentId()
        ));
    }

    @Operation(summary = "分页查询我的帮带订单")
    @GetMapping("/mine")
    public Result<PageResult> mine(OrderPageQueryDTO query) {
        return Result.success(deliveryService.pageMine(
                query, BaseContext.getCurrentId()
        ));
    }

    @Operation(summary = "查询帮带订单详情")
    @GetMapping("/{id}")
    public Result<OrderQueryVO> getById(@PathVariable Long id) {
        return Result.success(deliveryService.getById(
                id, BaseContext.getCurrentId()
        ));
    }

    @Operation(summary = "接取帮带订单")
    @PutMapping("/accept/{id}")
    public Result accept(@PathVariable Long id) {
        deliveryService.accept(id, BaseContext.getCurrentId());
        return Result.success();
    }

    @Operation(summary = "开始配送")
    @PutMapping("/start/{id}")
    public Result start(@PathVariable Long id) {
        deliveryService.start(id, BaseContext.getCurrentId());
        return Result.success();
    }

    @Operation(summary = "确认送达")
    @PutMapping("/complete/{id}")
    public Result complete(@PathVariable Long id) {
        deliveryService.complete(id, BaseContext.getCurrentId());
        return Result.success();
    }
}
