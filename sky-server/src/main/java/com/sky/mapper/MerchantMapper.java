package com.sky.mapper;

import com.sky.dto.MerchantDTO;
import com.sky.entity.Merchant;
import com.sky.dto.MerchantPageQueryDTO;
import java.util.List;

import org.apache.ibatis.annotations.*;

@SuppressWarnings("SqlResolve")
@Mapper
public interface MerchantMapper {

    @Select("""
        SELECT *
        FROM merchant
        WHERE username = #{username}
        LIMIT 1
        """)
    Merchant getByUsername(@Param("username") String username);

    @Insert("""
        INSERT INTO merchant
        (username, password, merchant_name, phone, status, create_time, update_time)
        VALUES
        (#{username}, #{password}, #{merchantName}, #{phone},
         #{status}, #{createTime}, #{updateTime})
        """)
    void insert(Merchant merchant);

    @Select("""
            SELECT *
        FROM merchant
        WHERE (
            :name IS NULL
            OR :name = ''
            OR merchant_name LIKE CONCAT('%', :name, '%')
        )
        ORDER BY create_time DESC;
        """)
    List<Merchant> list(MerchantPageQueryDTO query);

    @Select("""
        SELECT * FROM merchant
        WHERE (
            #{name} IS NULL
            OR #{name} = ''
            OR merchant_name LIKE CONCAT('%', #{name}, '%')
        )
        ORDER BY create_time DESC
        """)
    List<Merchant> pageQuery(MerchantPageQueryDTO merchantPageQueryDTO);

    @Update("""
        UPDATE merchant
        SET status = #{status},
            update_time = NOW()
        WHERE id = #{id}
        """)
    void startOrStop(Merchant merchant);

    @Select("SELECT * FROM merchant WHERE id = #{id}")
    Merchant getById(Long id);

    @Update("""
        UPDATE merchant
        SET username = #{username},
            password = #{password},
            merchant_name = #{merchantName},
            phone = #{phone}
        WHERE id = #{id}
        """)
    void update(MerchantDTO merchantDTO);
}
