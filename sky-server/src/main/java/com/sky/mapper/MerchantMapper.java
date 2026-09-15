package com.sky.mapper;

import com.sky.entity.Merchant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
