package com.neu.controller.user;

import com.neu.dto.OrderSubmitDTO;
import com.neu.result.Result;
import com.neu.vo.OrderSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.neu.service.OrderService;
@Slf4j
@Tag(name = "用户端订单相关")
@RestController("userOderController")
@RequestMapping("/user/dish")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    @Operation(summary = "用户下单")
    public Result<OrderSubmitVO> submitOrder(@RequestBody OrderSubmitDTO orderSubmitDTO) {
        log.info("用户下单：{}", orderSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.SubmitOrder(orderSubmitDTO);
        return Result.success(orderSubmitVO);
    }
    @GetMapping("/reminder/{orderId}")
    @Operation(summary = "用户点餐提醒")
    public Result reminder(@PathVariable Long  orderId) {
        orderService.reminder(orderId);
        log.info("用户点餐提醒：{}", orderId);
        return Result.success();
    }
}
