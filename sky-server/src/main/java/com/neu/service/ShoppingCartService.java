package com.neu.service;

import com.neu.dto.ShoppingCartDTO;
import com.neu.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> listShoppingCart();

    void deleteShoppingCartByUserId(Long userId);
}
