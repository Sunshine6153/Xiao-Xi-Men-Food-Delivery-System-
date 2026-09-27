package com.neu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.entity.User;
import com.neu.dto.UserPageQueryDTO;
import com.neu.annotation.AutoFill;
import com.neu.enumeration.OperationType;
import com.neu.vo.AdminUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE openid = #{openid} LIMIT 1")
    User getByOpenid(@Param("openid") String openid);

    @Select("SELECT status FROM user WHERE id = #{id}")
    Integer getStatus(Long id);

    @Insert("""
            INSERT INTO user (openid, username, password, status, create_time, update_time)
            VALUES (#{openid}, #{openid}, #{password}, #{status}, #{createTime}, #{updateTime})
            """)
    @AutoFill(value = OperationType.INSERT)
    void insertUser(User user);

    @Select("""
            <script>
            SELECT id, username, name, phone, status, create_time, update_time
            FROM user
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (username LIKE CONCAT('%', #{keyword}, '%')
                         OR name LIKE CONCAT('%', #{keyword}, '%')
                         OR phone LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="status != null">
                    AND status = #{status}
                </if>
            </where>
            ORDER BY create_time DESC, id DESC
            </script>
            """)
    List<AdminUserVO> pageQuery(UserPageQueryDTO query);

    @Select("""
            SELECT id, username, name, phone, status, create_time, update_time
            FROM user
            WHERE id = #{id}
            """)
    AdminUserVO getAdminById(Long id);

    @Update("""
            UPDATE user
            SET status = #{status}, update_time = NOW()
            WHERE id = #{id}
            """)
    int updateStatus(@Param("status") Integer status, @Param("id") Long id);

    @Select("""
            SELECT COUNT(*) FROM user
            WHERE create_time <= #{end}
            """)
    Integer getOrderByTime(Map map);

    @Select("""
            SELECT COUNT(*) FROM user
            WHERE create_time >= #{begin} AND create_time < #{end}
            """)
    Integer getNewOrderByTime(Map map);
}
