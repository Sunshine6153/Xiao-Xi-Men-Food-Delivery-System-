package com.neu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

    @Select("SELECT * FROM shopping_cart " +
            "WHERE user_id = IFNULL(#{userId}, user_id) " +
            "AND dish_id = IFNULL(#{dishId}, dish_id) " +
            "AND dish_flavor <=> #{dishFlavor}")
    List<ShoppingCart> listSameDish(ShoppingCart shoppingCart);

    @Select("SELECT sc.*, d.name, d.price, d.image, " +
            "m.merchant_name AS merchant_name, m.location AS merchant_location " +
            "FROM shopping_cart sc LEFT JOIN dish d ON sc.dish_id = d.id " +
            "LEFT JOIN merchant m ON sc.merchant_id = m.id " +
            "WHERE sc.user_id = #{userId}")
    List<ShoppingCart> listByUserId(Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM shopping_cart sc
            LEFT JOIN dish d ON d.id = sc.dish_id
            LEFT JOIN category c ON c.id = d.category_id
            LEFT JOIN merchant m ON m.id = d.merchant_id
            WHERE sc.user_id = #{userId}
              AND (d.id IS NULL OR c.id IS NULL OR m.id IS NULL
                   OR COALESCE(d.status, 0) != 1 OR COALESCE(c.status, 0) != 1
                   OR COALESCE(m.status, 0) != 1 OR COALESCE(m.business_status, 0) != 1)
            """)
    int countUnavailableByUserId(Long userId);

    @Update("UPDATE shopping_cart SET number = #{number} WHERE id = #{id}")
    void updateNumberById(ShoppingCart existShoppingCart);

    @Update("UPDATE shopping_cart SET number = #{number} WHERE id = #{id} AND user_id = #{userId}")
    void updateNumber(@Param("id") Long id,
                      @Param("number") Integer number,
                      @Param("userId") Long userId);

    @Delete("DELETE FROM shopping_cart WHERE id = #{id} AND user_id = #{userId}")
    void deleteShoppingCartItem(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM shopping_cart WHERE user_id = #{userId}")
    void deleteShoppingCartByUserId(Long userId);
}
