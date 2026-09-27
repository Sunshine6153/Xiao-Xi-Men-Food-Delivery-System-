package com.neu.controller.admin;

import com.neu.dto.CategoryDTO;
import com.neu.entity.Category;
import com.neu.result.Result;
import com.neu.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜品分类相关")
@RestController
@RequestMapping("/admin/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishController dishController;

    @Operation(summary = "分类列表")
    @SecurityRequirement(name = "token")
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.list());
    }

    @Operation(summary = "分类详情")
    @SecurityRequirement(name = "token")
    @GetMapping("/{id}")
    public Result<Category> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @Operation(summary = "新增分类")
    @SecurityRequirement(name = "token")
    @PostMapping
    public Result<String> save(@RequestBody CategoryDTO categoryDTO) {
        categoryService.save(categoryDTO);
        dishController.clearCache("dish_*");
        log.info("新增菜品分类：{}", categoryDTO);
        return Result.success();
    }

    @Operation(summary = "修改分类")
    @SecurityRequirement(name = "token")
    @PutMapping
    public Result<String> update(@RequestBody CategoryDTO categoryDTO) {
        categoryService.update(categoryDTO);
        dishController.clearCache("dish_*");
        log.info("修改菜品分类：{}", categoryDTO);
        return Result.success();
    }

    @Operation(summary = "启用或禁用分类")
    @SecurityRequirement(name = "token")
    @PostMapping("/status/{status}")
    public Result<String> startOrStop(@PathVariable Integer status,
                                      @RequestParam Long id) {
        log.info("菜品分类{}状态：{}", id, status);
        categoryService.startOrStop(status, id);
        dishController.clearCache("dish_*");
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        categoryService.delete(id);
        dishController.clearCache("dish_*");
        return Result.success();
    }
}
