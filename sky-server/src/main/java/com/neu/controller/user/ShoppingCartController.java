package com.neu.controller.user;

import com.neu.dto.ShoppingCartDTO;
import com.neu.entity.ShoppingCart;
import com.neu.result.Result;
import com.neu.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
@Tag(name = "用户端购物车相关")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @RequestMapping("/add")
    @Operation(summary = "添加购物车")
    public Result<?> addShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车");
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @RequestMapping("/list")
    @Operation(summary = "查看购物车列表")
    public Result<List<ShoppingCart>> listShoppingCart() {
        log.info("查看购物车列表");
        List<ShoppingCart> shoppingCartList = shoppingCartService.listShoppingCart();
        return Result.success(shoppingCartList);
    }

    @RequestMapping("/update")
    @Operation(summary = "修改购物车商品数量")
    public Result<?> updateShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("修改购物车商品数量：{}", shoppingCartDTO);
        shoppingCartService.updateShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @RequestMapping("/deleteItem")
    @Operation(summary = "删除购物车单项商品")
    public Result<?> deleteShoppingCartItem(@RequestParam Long id) {
        log.info("删除购物车单项商品：{}", id);
        shoppingCartService.deleteShoppingCartItem(id);
        return Result.success();
    }

    @RequestMapping("/delete")
    @Operation(summary = "清空购物车")
    public Result<?> deleteShoppingCart() {
        log.info("清空购物车");
        shoppingCartService.deleteShoppingCart();
        return Result.success();
    }
}
