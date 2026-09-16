package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    @Select("""
        <script>
        SELECT d.id, d.category_id, d.merchant_id, d.name, d.price,
               d.image, d.description, d.status, d.create_time, d.update_time,
               c.name AS category_name
        FROM dish d
        INNER JOIN category c ON c.id = d.category_id
        <where>
            d.merchant_id = #{merchantId}
            <if test="query.name != null and query.name != ''">
                AND d.name LIKE CONCAT('%', #{query.name}, '%')
            </if>
            <if test="query.categoryId != null">
                AND d.category_id = #{query.categoryId}
            </if>
            <if test="query.status != null">
                AND d.status = #{query.status}
            </if>
        </where>
        ORDER BY d.create_time DESC
        </script>
        """)
    List<DishVO> pageQuery(@Param("merchantId") Long merchantId,
                           @Param("query") DishPageQueryDTO query);

    @Select("""
        SELECT d.id, d.category_id, d.merchant_id, d.name, d.price,
               d.image, d.description, d.status, d.create_time, d.update_time,
               c.name AS category_name
        FROM dish d
        INNER JOIN category c ON c.id = d.category_id
        WHERE d.id = #{id}
          AND d.merchant_id = #{merchantId}
        """)
    DishVO getById(@Param("id") Long id, @Param("merchantId") Long merchantId);

    @Insert("""
        INSERT INTO dish
        (name, category_id, merchant_id, price, image, description,
         status, create_time, update_time)
        VALUES
        (#{name}, #{categoryId}, #{merchantId}, #{price}, #{image},
         #{description}, #{status}, #{createTime}, #{updateTime})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @AutoFill(OperationType.INSERT)
    void insertDish(Dish dish);

    @Update("""
        UPDATE dish
        SET name = #{name},
            category_id = #{categoryId},
            price = #{price},
            image = #{image},
            description = #{description},
            update_time = #{updateTime}
        WHERE id = #{id}
          AND merchant_id = #{merchantId}
        """)
    @AutoFill(OperationType.UPDATE)
    int updateDish(Dish dish);

    @Update("""
        UPDATE dish
        SET status = #{status},
            update_time = #{updateTime}
        WHERE id = #{id}
          AND merchant_id = #{merchantId}
        """)
    @AutoFill(OperationType.UPDATE)
    int updateStatus(Dish dish);
}
