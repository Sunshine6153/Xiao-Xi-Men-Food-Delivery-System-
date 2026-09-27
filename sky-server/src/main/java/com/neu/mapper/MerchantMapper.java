package com.neu.mapper;

import com.neu.annotation.AutoFill;
import com.neu.entity.Merchant;
import com.neu.dto.MerchantPageQueryDTO;
import java.util.List;

import com.neu.enumeration.OperationType;
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
        (username, password, merchant_name, phone, location, role, status, business_status, create_time, update_time)
        VALUES
        (#{username}, #{password}, #{merchantName}, #{phone}, #{location}, #{role},
         #{status}, COALESCE(#{businessStatus}, 1), #{createTime}, #{updateTime})
        """)
    @AutoFill(value = OperationType.INSERT)
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
        SELECT id, username, merchant_name, phone, location, role, status, business_status, create_time, update_time
        FROM merchant
        WHERE role = 'MERCHANT'
        AND (
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
            update_time = #{updateTime}
        WHERE id = #{id}
        """)
    @AutoFill(value = OperationType.UPDATE)
    void startOrStop(Merchant merchant);

    @Select("SELECT status FROM merchant WHERE id = #{id}")
    Integer getStatus(Long id);

    @Select("SELECT business_status FROM merchant WHERE id = #{id}")
    Integer getBusinessStatus(Long id);

    @Select("""
        SELECT id, username, merchant_name, phone, location, role, status, business_status, create_time, update_time
        FROM merchant
        WHERE id = #{id}
        """)
    Merchant getById(Long id);

    @Update("""
        UPDATE merchant
        SET username = #{username},
            password = COALESCE(#{password}, password),
            merchant_name = #{merchantName},
            phone = #{phone},
            location = #{location},
            update_time = #{updateTime}
        WHERE id = #{id}
        """)
    @AutoFill(value = OperationType.UPDATE)
    void update(Merchant merchant);

    @Update("""
        UPDATE merchant
        SET merchant_name = #{merchantName},
            phone = #{phone},
            location = #{location},
            update_time = #{updateTime}
        WHERE id = #{id}
        """)
    @AutoFill(value = OperationType.UPDATE)
    int updateProfile(Merchant merchant);

    @Update("""
        UPDATE merchant
        SET business_status = #{businessStatus},
            update_time = #{updateTime}
        WHERE id = #{id}
        """)
    @AutoFill(value = OperationType.UPDATE)
    int updateBusinessStatus(Merchant merchant);
}
