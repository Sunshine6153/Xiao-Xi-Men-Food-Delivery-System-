package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.MerchantDTO;
import com.sky.dto.MerchantLoginDTO;
import com.sky.entity.Merchant;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.MerchantService;
import com.sky.utils.JwtUtil;
import com.sky.vo.MerchantLoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "商户登录相关")
@RestController
@RequestMapping("/admin/merchant")
@Slf4j

public class MerchantController {

    private final MerchantService merchantService;
    private final JwtProperties jwtProperties;

    public MerchantController(MerchantService merchantService, JwtProperties jwtProperties) {
        this.merchantService = merchantService;
        this.jwtProperties = jwtProperties;
    }
    @Operation(summary = "商户登录")
    @PostMapping("/login")
    public Result<MerchantLoginVO> login(@RequestBody MerchantLoginDTO merchantLoginDTO) {
        // 1. 使用数据层实体完成账号和密码校验
        Merchant merchant = merchantService.login(merchantLoginDTO);
        // 2. 使用 JWT 配置和商户实体生成登录令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.MERCHANT_ID, merchant.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );

        // 3. 使用登录响应 VO 返回商户信息和令牌
        MerchantLoginVO merchantLoginVO = MerchantLoginVO.builder()
                .id(merchant.getId())
                .name(merchant.getMerchantName())
                .token(token)
                .build();

        return Result.success(merchantLoginVO);

    }
    @Operation(summary = "商户退出登录")
    @SecurityRequirement(name = "token")
    @PostMapping("/logout")
    public Result<String> logout(@RequestBody Map<String, Object> request) {
        return Result.success();
    }

    @Operation(summary = "新增员工")
    @SecurityRequirement(name = "token")//要求添加令牌
    @PostMapping("/save")
    public Result<String> save(@RequestBody MerchantDTO merchantDTO) {
        merchantService.save(merchantDTO);
        log.info("新增员工：{}", merchantDTO);
        return Result.success();
    }
}
