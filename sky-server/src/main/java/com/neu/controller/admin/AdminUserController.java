package com.neu.controller.admin;

import com.neu.dto.UserPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.result.Result;
import com.neu.service.AdminUserService;
import com.neu.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员用户管理")
@RestController
@RequestMapping("/admin/user")
@SecurityRequirement(name = "token")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "用户分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(UserPageQueryDTO query) {
        return Result.success(adminUserService.page(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> getById(@PathVariable Long id) {
        return Result.success(adminUserService.getById(id));
    }

    @Operation(summary = "启用或禁用用户")
    @PostMapping("/status/{status}")
    public Result<String> updateStatus(@PathVariable Integer status,
                                       @RequestParam Long id) {
        adminUserService.updateStatus(status, id);
        return Result.success();
    }
}
