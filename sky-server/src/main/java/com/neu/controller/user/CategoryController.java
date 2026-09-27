package com.neu.controller.user;

import com.neu.entity.Category;
import com.neu.result.Result;
import com.neu.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户端菜品分类相关")
@RestController("userCategoryController")
@RequestMapping("/user/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "获取启用的菜品分类")
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listForUser());
    }
}
