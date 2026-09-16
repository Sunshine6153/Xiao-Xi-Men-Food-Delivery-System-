package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j

public class ShopController {

    private final MerchantService merchantService;

    public ShopController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @Operation(summary = "获取商户状态")
    @GetMapping("/status/{id}")
    public Result<Integer> getStatus(@PathVariable Long id) {
        return Result.success(merchantService.getStatus(id));
    }
}
