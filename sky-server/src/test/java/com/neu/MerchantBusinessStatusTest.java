package com.neu;

import com.neu.constant.JwtClaimsConstant;
import com.neu.properties.JwtProperties;
import com.neu.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SkyApplication.class)
@AutoConfigureMockMvc
@Transactional
@Rollback
class MerchantBusinessStatusTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Test
    void pauseKeepsLoginAvailableAndInvalidatesMenuCacheAndBlocksCheckout() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "business-m-" + suffix;
        String password = DigestUtils.md5DigestAsHex("test".getBytes(StandardCharsets.UTF_8));
        long merchantId = insert("INSERT INTO merchant (username,password,merchant_name,role,status) VALUES (?,?,'Profile merchant','MERCHANT',1)", username, password);
        long userId = insert("INSERT INTO user (openid,username,password,status) VALUES (?,?,'',1)", "business-open-" + suffix, "business-user-" + suffix);
        long categoryId = insert("INSERT INTO category (name,sort,status) VALUES (?,1,1)", "Business category " + suffix);
        long dishId = insert("INSERT INTO dish (category_id,merchant_id,name,price,status) VALUES (?,?,'Business dish',12,1)", categoryId, merchantId);
        long addressId = insert("INSERT INTO address_book (user_id,consignee,phone,address) VALUES (?,'Test receiver','13800000000','第五宿舍')", userId);
        String merchantToken = JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), Map.of(JwtClaimsConstant.MERCHANT_ID, merchantId, JwtClaimsConstant.ROLE, "MERCHANT"));
        String userToken = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), Map.of(JwtClaimsConstant.USER_ID, userId));

        try {
            mockMvc.perform(get("/admin/merchant/profile").servletPath("/admin/merchant/profile").header("token", merchantToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.businessStatus").value(1))
                    .andExpect(jsonPath("$.data.password").doesNotExist());
            mockMvc.perform(put("/admin/merchant/profile").servletPath("/admin/merchant/profile").header("token", merchantToken)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"merchantName\":\"Updated profile\",\"phone\":\"13800000000\",\"location\":\"一食堂\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
            menu(categoryId, userToken, 1);
            String addBody = "{\"dishId\":" + dishId + "}";
            mockMvc.perform(post("/user/shoppingCart/add").header("token", userToken).contentType(MediaType.APPLICATION_JSON).content(addBody))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

            businessStatus(merchantToken, 0);
            mockMvc.perform(get("/user/shop/status/" + merchantId))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(0));
            assertEquals(1, jdbcTemplate.queryForObject("SELECT status FROM merchant WHERE id=?", Integer.class, merchantId));
            assertEquals("Updated profile", jdbcTemplate.queryForObject("SELECT merchant_name FROM merchant WHERE id=?", String.class, merchantId));
            menu(categoryId, userToken, 0);
            mockMvc.perform(post("/admin/merchant/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"test\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").isNotEmpty());
            mockMvc.perform(post("/user/shoppingCart/add").header("token", userToken).contentType(MediaType.APPLICATION_JSON).content(addBody))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));
            mockMvc.perform(post("/user/dish/submit").header("token", userToken).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"addressId\":" + addressId + ",\"deliveryFee\":2,\"orderAmount\":14}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));
            assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?", Integer.class, userId));
            assertEquals(1, jdbcTemplate.queryForObject("SELECT number FROM shopping_cart WHERE user_id=?", Integer.class, userId));

            businessStatus(merchantToken, 1);
            menu(categoryId, userToken, 1);
            mockMvc.perform(post("/user/shoppingCart/add").header("token", userToken).contentType(MediaType.APPLICATION_JSON).content(addBody))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
            assertEquals(2, jdbcTemplate.queryForObject("SELECT number FROM shopping_cart WHERE user_id=?", Integer.class, userId));
        } finally {
            stringRedisTemplate.delete(List.of("merchant_status:" + merchantId, "merchant_business_status:" + merchantId, "dish_" + categoryId));
        }
    }

    private void businessStatus(String token, int status) throws Exception {
        String path = "/admin/merchant/business-status/" + status;
        mockMvc.perform(put(path).servletPath(path).header("token", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    private void menu(long categoryId, String token, int count) throws Exception {
        mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)).header("token", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(count));
    }

    private long insert(String sql, Object... values) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
