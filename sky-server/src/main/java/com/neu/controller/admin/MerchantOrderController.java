package com.neu.controller.admin;

import com.neu.constant.JwtClaimsConstant;
import com.neu.dto.MerchantOrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.result.Result;
import com.neu.service.MerchantOrderService;
import com.neu.vo.MerchantOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商户订单管理")
@RestController
@RequestMapping("/admin/order")
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    public MerchantOrderController(MerchantOrderService merchantOrderService) {
        this.merchantOrderService = merchantOrderService;
    }

    @Operation(summary = "分页查询本商户订单")
    @SecurityRequirement(name = "token")
    @GetMapping("/page")
    public Result<PageResult> page(MerchantOrderPageQueryDTO query,
                                   HttpServletRequest request) {
        return Result.success(merchantOrderService.page(query, merchantId(request)));
    }

    @Operation(summary = "查询本商户订单详情")
    @SecurityRequirement(name = "token")
    @GetMapping("/{id}")
    public Result<MerchantOrderVO> getById(@PathVariable Long id,
                                           HttpServletRequest request) {
        return Result.success(merchantOrderService.getById(id, merchantId(request)));
    }

    @Operation(summary = "商户确认接单")
    @SecurityRequirement(name = "token")
    @PutMapping("/confirm/{id}")
    public Result confirm(@PathVariable Long id, HttpServletRequest request) {
        merchantOrderService.confirm(id, merchantId(request));
        return Result.success();
    }

    @Operation(summary = "商户确认出餐")
    @SecurityRequirement(name = "token")
    @PutMapping("/complete/{id}")
    public Result complete(@PathVariable Long id, HttpServletRequest request) {
        merchantOrderService.complete(id, merchantId(request));
        return Result.success();
    }

    private Long merchantId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtClaimsConstant.MERCHANT_ID);
    }
}
