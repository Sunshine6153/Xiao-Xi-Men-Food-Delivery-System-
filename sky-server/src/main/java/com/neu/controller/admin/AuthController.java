package com.neu.controller.admin;

import com.neu.constant.JwtClaimsConstant;
import com.neu.result.Result;
import com.neu.vo.AccountIdentityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@Tag(name = "管理端身份验证")
public class AuthController {

    @GetMapping("/me")
    @Operation(summary = "验证当前账号身份")
    @SecurityRequirement(name = "token")
    public Result<AccountIdentityVO> me(HttpServletRequest request) {
        Long id = (Long) request.getAttribute(JwtClaimsConstant.MERCHANT_ID);
        String role = (String) request.getAttribute(JwtClaimsConstant.ROLE);
        return Result.success(new AccountIdentityVO(id, role));
    }
}
