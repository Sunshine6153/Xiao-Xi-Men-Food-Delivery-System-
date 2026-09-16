package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.CategoryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("""
        SELECT id, name, sort, status, create_time, update_time
        FROM category
        ORDER BY sort ASC, create_time DESC
        """)
    List<Category> list();

    @Insert("""
        INSERT INTO category
        (name, sort, status, create_time, update_time)
        VALUES
        (#{name}, #{sort}, #{status}, #{createTime}, #{updateTime})
        """)
    void insertCategory(Category category);

    @Update("""
        UPDATE category
        SET name = #{name},
            sort = #{sort},
            update_time = NOW()
        WHERE id = #{id}
        """)
    void updateCategory(CategoryDTO categoryDTO);

    @Update("""
        UPDATE category
        SET status = #{status},
            update_time = NOW()
        WHERE id = #{id}
        """)
    void startOrStop(Integer status, Long id);
}
