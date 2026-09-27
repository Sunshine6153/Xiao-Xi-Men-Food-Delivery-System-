package com.neu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SkyApplication.class)
@AutoConfigureMockMvc
@Transactional
@Rollback
class ShoppingCartOrderFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void cartQuantitySubmitAndPaymentUseCurrentUserData() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 10);
        long merchantId = insert(
                "INSERT INTO merchant (username, password, merchant_name, location, role, status) VALUES (?, '', ?, ?, 'MERCHANT', 1)",
                "cart-merchant-" + suffix, "Cart Merchant", "一层 01");
        long categoryId = insert(
                "INSERT INTO category (name, sort, status) VALUES (?, 1, 1)",
                "Cart category " + suffix);
        long dishId = insert(
                "INSERT INTO dish (category_id, merchant_id, name, price, image, status) VALUES (?, ?, ?, ?, ?, 1)",
                categoryId, merchantId, "Cart dish", new BigDecimal("12.00"), "/images/cart-test.jpg");
        long userId = insertUser("cart-user-" + suffix);
        long otherUserId = insertUser("cart-other-" + suffix);
        long addressId = insert(
                "INSERT INTO address_book (user_id, consignee, phone, address, is_default) VALUES (?, ?, ?, ?, 1)",
                userId, "测试用户", "13800000000", "第五宿舍 301");
        String token = token(userId);
        String otherToken = token(otherUserId);

        String addBody = "{\"dishId\":" + dishId + ",\"dishFlavor\":\"微辣\"}";
        mockMvc.perform(post("/user/shoppingCart/add").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(addBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/user/shoppingCart/add").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(addBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        String cartResponse = mockMvc.perform(get("/user/shoppingCart/list").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Cart dish"))
                .andExpect(jsonPath("$.data[0].merchantName").value("Cart Merchant"))
                .andExpect(jsonPath("$.data[0].merchantLocation").value("一层 01"))
                .andExpect(jsonPath("$.data[0].number").value(2))
                .andReturn().getResponse().getContentAsString();
        long cartId = objectMapper.readTree(cartResponse).path("data").get(0).path("id").asLong();

        mockMvc.perform(put("/user/shoppingCart/update").header("token", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + cartId + ",\"number\":9}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT number FROM shopping_cart WHERE id = ?", Integer.class, cartId));

        mockMvc.perform(put("/user/shoppingCart/update").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + cartId + ",\"number\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        String submitResponse = mockMvc.perform(post("/user/dish/submit").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId
                                + ",\"remark\":\"送到楼下\",\"tablewareAmount\":0"
                                + ",\"deliveryFee\":3.00,\"orderAmount\":999.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderAmount").value(39.00))
                .andReturn().getResponse().getContentAsString();
        JsonNode order = objectMapper.readTree(submitResponse).path("data");
        long orderId = order.path("id").asLong();
        String orderNumber = order.path("orderNumber").asText();

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shopping_cart WHERE user_id = ?", Integer.class, userId));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT number FROM order_detail WHERE order_id = ?", Integer.class, orderId));

        mockMvc.perform(post("/user/order/payment").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + orderNumber + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.status").value(2));

        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", Integer.class, orderId));

        mockMvc.perform(post("/user/shoppingCart/add").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(addBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        Long newCartId = jdbcTemplate.queryForObject(
                "SELECT id FROM shopping_cart WHERE user_id = ?", Long.class, userId);
        mockMvc.perform(delete("/user/shoppingCart/deleteItem?id=" + newCartId).header("token", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shopping_cart WHERE user_id = ?", Integer.class, userId));

        mockMvc.perform(post("/user/shoppingCart/add").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(addBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/user/shoppingCart/delete").header("token", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shopping_cart WHERE user_id = ?", Integer.class, userId));
    }

    private long insertUser(String username) {
        return insert("INSERT INTO user (openid, username, password, status) VALUES (?, ?, '', 1)",
                "openid-" + username, username);
    }

    private String token(long userId) {
        return JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(),
                Map.of(JwtClaimsConstant.USER_ID, userId));
    }

    private long insert(String sql, Object... values) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
