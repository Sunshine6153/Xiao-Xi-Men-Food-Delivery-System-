package com.neu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper extends BaseMapper<DishFlavor> {

    @Insert("""
        INSERT INTO dish_flavor (dish_id, name, value)
        VALUES (#{dishId}, #{name}, #{value})
        """)
    void insertFlavor(DishFlavor flavor);

    @Select("""
        SELECT id, dish_id, name, value
        FROM dish_flavor
        WHERE dish_id = #{dishId}
        ORDER BY id ASC
        """)
    List<DishFlavor> listByDishId(Long dishId);

    @Delete("DELETE FROM dish_flavor WHERE dish_id = #{dishId}")
    void deleteByDishId(Long dishId);
}
