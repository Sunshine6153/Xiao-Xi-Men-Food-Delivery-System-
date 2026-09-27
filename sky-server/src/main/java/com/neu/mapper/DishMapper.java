package com.neu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.annotation.AutoFill;
import com.neu.dto.DishPageQueryDTO;
import com.neu.entity.Dish;
import com.neu.enumeration.OperationType;
import com.neu.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    @Select("SELECT COUNT(*) FROM dish WHERE category_id = #{categoryId}")
    int countByCategoryId(Long categoryId);

    @Select("""
        <script>
        SELECT d.id, d.category_id, d.merchant_id, d.name, d.price,
               d.image, d.description, d.status, d.create_time, d.update_time,
               c.name AS category_name,
               m.merchant_name, m.location AS merchant_location
        FROM dish d
        INNER JOIN category c ON c.id = d.category_id
        INNER JOIN merchant m ON m.id = d.merchant_id
        <where>
            <if test="merchantId != null">
                AND d.merchant_id = #{merchantId}
            </if>
            <if test="name != null and name != ''">
                AND d.name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="categoryId != null">
                AND d.category_id = #{categoryId}
            </if>
            <if test="status != null">
                AND d.status = #{status}
            </if>
        </where>
        ORDER BY d.create_time DESC
        </script>
        """)
    List<DishVO> pageQuery(DishPageQueryDTO query);

    @Select("""
        SELECT d.id, d.category_id, d.merchant_id, d.name, d.price,
               d.image, d.description, d.status, d.create_time, d.update_time,
               c.name AS category_name,
               m.merchant_name, m.location AS merchant_location
        FROM dish d
        INNER JOIN category c ON c.id = d.category_id AND c.status = 1
        INNER JOIN merchant m ON m.id = d.merchant_id AND m.status = 1 AND m.business_status = 1
        WHERE d.status = 1
          AND (#{categoryId} IS NULL OR d.category_id = #{categoryId})
        ORDER BY c.sort ASC, d.create_time DESC
        """)
    List<DishVO> listForUser(Long categoryId);

    @Select("""
        SELECT d.id, d.category_id, d.merchant_id, d.name, d.price,
               d.image, d.description, d.status, d.create_time, d.update_time,
               c.name AS category_name,
               m.merchant_name, m.location AS merchant_location
        FROM dish d
        INNER JOIN category c ON c.id = d.category_id AND c.status = 1
        INNER JOIN merchant m ON m.id = d.merchant_id AND m.status = 1 AND m.business_status = 1
        WHERE d.id = #{id}
          AND d.status = 1
        """)
    DishVO getByIdForUser(Long id);

    @Select("""
        SELECT id, category_id, merchant_id, name, price, image,
               description, status, create_time, update_time
        FROM dish
        WHERE id = #{id}
        """)
    Dish getById(Long id);

    @Select("""
        SELECT d.id, d.category_id, d.merchant_id, d.name, d.price,
               d.image, d.description, d.status, d.create_time, d.update_time,
               c.name AS category_name
        FROM dish d
        INNER JOIN category c ON c.id = d.category_id
        WHERE d.id = #{id}
          AND d.merchant_id = #{merchantId}
        """)
    DishVO getByIdForMerchant(Dish dish);

    @Select("""
        SELECT d.id, d.category_id, d.merchant_id, d.name, d.price,
               d.image, d.description, d.status, d.create_time, d.update_time,
               c.name AS category_name,
               m.merchant_name, m.location AS merchant_location
        FROM dish d
        INNER JOIN category c ON c.id = d.category_id
        INNER JOIN merchant m ON m.id = d.merchant_id
        WHERE d.id = #{id}
        """)
    DishVO getByIdForAdmin(Long id);

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

    @Update("""
        UPDATE dish
        SET status = #{status},
            update_time = #{updateTime}
        WHERE id = #{id}
        """)
    @AutoFill(OperationType.UPDATE)
    int updateStatusForAdmin(Dish dish);

    @Delete("""
        DELETE FROM dish
        WHERE id = #{id}
          AND merchant_id = #{merchantId}
        """)
    int deleteDish(Dish dish);
}
