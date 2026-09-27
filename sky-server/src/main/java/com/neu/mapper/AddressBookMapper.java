package com.neu.mapper;

import com.neu.annotation.AutoFill;
import com.neu.entity.AddressBook;
import com.neu.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    @Insert("""
            INSERT INTO address_book
                (user_id, consignee, phone, address, is_default, create_time, update_time)
            VALUES
                (#{userId}, #{consignee}, #{phone}, #{address}, #{isDefault},
                 #{createTime}, #{updateTime})
            """)
    @AutoFill(value = OperationType.INSERT)
    void insert(AddressBook addressBook);

    @Select("""
            SELECT id, user_id, consignee, phone, address, is_default, create_time, update_time
            FROM address_book
            WHERE user_id = #{userId}
            ORDER BY is_default DESC, update_time DESC, id DESC
            """)
    List<AddressBook> listByUserId(Long userId);

    @Select("""
            SELECT id, user_id, consignee, phone, address, is_default, create_time, update_time
            FROM address_book
            WHERE id = #{id} AND user_id = #{userId}
            """)
    AddressBook getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, consignee, phone, address, is_default, create_time, update_time
            FROM address_book
            WHERE user_id = #{userId} AND is_default = 1
            LIMIT 1
            """)
    AddressBook getDefaultByUserId(Long userId);

    @Update("""
            UPDATE address_book
            SET consignee = #{consignee}, phone = #{phone}, address = #{address},
                is_default = #{isDefault}, update_time = #{updateTime}
            WHERE id = #{id} AND user_id = #{userId}
            """)
    @AutoFill(value = OperationType.UPDATE)
    int update(AddressBook addressBook);

    @Update("""
            UPDATE address_book
            SET is_default = 0, update_time = #{updateTime}
            WHERE user_id = #{userId}
            """)
    @AutoFill(value = OperationType.UPDATE)
    int clearDefault(AddressBook addressBook);

    @Update("""
            UPDATE address_book
            SET is_default = 1, update_time = #{updateTime}
            WHERE id = #{id} AND user_id = #{userId}
            """)
    @AutoFill(value = OperationType.UPDATE)
    int setDefault(AddressBook addressBook);

    @Delete("DELETE FROM address_book WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, consignee, phone, address, is_default, create_time, update_time
            FROM address_book
            WHERE id = #{id}
            """)
    AddressBook getById(@Param("id") Long id);
}

