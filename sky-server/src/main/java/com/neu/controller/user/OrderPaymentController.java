package com.neu.controller.user;

import com.neu.dto.OrderPaymentDTO;
import com.neu.result.Result;
import com.neu.service.OrderService;
import com.neu.vo.OrderPaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "用户端订单支付相关")
@RestController
@RequestMapping("/user/order")
public class OrderPaymentController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/payment")
    @Operation(summary = "模拟支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrderPaymentDTO orderPaymentDTO) {
        log.info("用户模拟支付：{}", orderPaymentDTO);
        return Result.success(orderService.paySuccess(orderPaymentDTO.getOrderNumber()));
    }
}
