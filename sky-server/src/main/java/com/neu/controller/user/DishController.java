package com.neu.controller.user;

import com.neu.result.Result;
import com.neu.service.DishService;
import com.neu.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户端菜品相关")
@RestController("userDishController")
@RequestMapping("/user/dish")
public class DishController {

    private final DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @Operation(summary = "获取用户端上架菜品列表")
    @GetMapping("/list")
    @SuppressWarnings("unchecked")
    public Result<List<DishVO>> list(@RequestParam(required = false) Long categoryId) {
        // 构造 Redis 中的 key，按分类缓存菜品列表。
        String key = "dish_" + categoryId;
        List<DishVO> dishList = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (dishList != null && !dishList.isEmpty()) {
            return Result.success(dishList);
        }
        // 如果不存在，则从数据库中查询并缓存到 Redis 中。
        dishList = dishService.listForUser(categoryId);
        redisTemplate.opsForValue().set(key, dishList);
        return Result.success(dishList);
    }

    @Operation(summary = "获取用户端菜品详情")
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        return Result.success(dishService.getByIdForUser(id));
    }

}
