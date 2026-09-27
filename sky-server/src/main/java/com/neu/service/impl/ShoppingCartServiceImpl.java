package com.neu.service.impl;

import com.neu.context.BaseContext;
import com.neu.dto.ShoppingCartDTO;
import com.neu.entity.ShoppingCart;
import com.neu.exception.OrderBusinessException;
import com.neu.mapper.DishMapper;
import com.neu.mapper.ShoppingCartMapper;
import com.neu.service.ShoppingCartService;
import com.neu.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        DishVO dish = dishMapper.getByIdForUser(shoppingCartDTO.getDishId());
        if (dish == null) {
            throw new OrderBusinessException(409, "菜品已下架或商户暂停营业");
        }
       //判断添加商品在购物车内是否存在
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> shoppingCartList = shoppingCartMapper.listSameDish(shoppingCart);
        //若存在只需要数量+1
        if(shoppingCartList != null && !shoppingCartList.isEmpty()){
            ShoppingCart existShoppingCart = shoppingCartList.get(0);
            existShoppingCart.setNumber(existShoppingCart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(existShoppingCart);
        }else {
            shoppingCart.setName(dish.getName());
            shoppingCart.setPrice(dish.getPrice());
            shoppingCart.setImage(dish.getImage());

            shoppingCart.setMerchantId(dish.getMerchantId());
            shoppingCart.setNumber(1);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(java.time.LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> listShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        return shoppingCartMapper.listByUserId(userId);
    }

    @Override
    public void updateShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        if (shoppingCartDTO.getNumber() == null || shoppingCartDTO.getNumber() <= 0) {
            shoppingCartMapper.deleteShoppingCartItem(shoppingCartDTO.getId(), userId);
            return;
        }
        shoppingCartMapper.updateNumber(
                shoppingCartDTO.getId(), shoppingCartDTO.getNumber(), userId
        );
    }

    @Override
    public void deleteShoppingCartItem(Long id) {
        shoppingCartMapper.deleteShoppingCartItem(id, BaseContext.getCurrentId());
    }

    @Override
    public void deleteShoppingCart() {
        shoppingCartMapper.deleteShoppingCartByUserId(BaseContext.getCurrentId());
    }
}
