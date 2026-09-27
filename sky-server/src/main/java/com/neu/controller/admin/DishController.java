package com.neu.controller.admin;

import com.neu.constant.AccountRole;
import com.neu.constant.JwtClaimsConstant;
import com.neu.dto.DishDTO;
import com.neu.dto.DishPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.result.Result;
import com.neu.service.DishService;
import com.neu.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

@Tag(name = "菜品相关")
@RestController
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Operation(summary = "菜品分页查询")
    @SecurityRequirement(name = "token")
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO query,
                                   HttpServletRequest request) {
        return Result.success(dishService.page(query, merchantScope(request)));
    }

    @Operation(summary = "菜品详情")
    @SecurityRequirement(name = "token")
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id,
                                  HttpServletRequest request) {
        return Result.success(dishService.getById(id, merchantScope(request)));
    }

    @Operation(summary = "新增菜品")
    @SecurityRequirement(name = "token")
    @PostMapping
    public Result<String> save(@RequestBody DishDTO dishDTO,
                               HttpServletRequest request) {
        dishService.save(dishDTO, merchantId(request));
        clearCache("dish_*");
        log.info("新增菜品：{}", dishDTO);
        return Result.success();
    }

    @Operation(summary = "修改菜品")
    @SecurityRequirement(name = "token")
    @PutMapping
    public Result<String> update(@RequestBody DishDTO dishDTO,
                                 HttpServletRequest request) {
        dishService.update(dishDTO, merchantId(request));
        clearCache("dish_*");
        log.info("修改菜品：{}", dishDTO);
        return Result.success();
    }

    @Operation(summary = "启用或禁用菜品")
    @SecurityRequirement(name = "token")
    @PostMapping("/status/{status}")
    public Result<String> updateStatus(@PathVariable Integer status,
                                       @RequestParam Long id,
                                       HttpServletRequest request) {
        dishService.updateStatus(status, id, merchantScope(request));
        clearCache("dish_*");
        return Result.success();
    }

    @Operation(summary = "删除菜品")
    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id,
                                 HttpServletRequest request) {
        dishService.delete(id, merchantId(request));
        clearCache("dish_*");
        return Result.success();
    }

    public void clearCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

    @Operation(summary = "上传菜品图片")
    @SecurityRequirement(name = "token")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(dishService.uploadImage(file));
    }

    private Long merchantId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtClaimsConstant.MERCHANT_ID);
    }

    private Long merchantScope(HttpServletRequest request) {
        String role = (String) request.getAttribute(JwtClaimsConstant.ROLE);
        return AccountRole.ADMIN.equals(role) ? null : merchantId(request);
    }
}
