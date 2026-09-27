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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SkyApplication.class)
@AutoConfigureMockMvc
@Transactional
@Rollback
class OrderPaymentTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtProperties jwtProperties;

    @Test
    void paymentOnlyUpdatesCurrentUsersUnpaidOrderOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 12);
        long userId = insertUser("payment-user-" + suffix);
        long otherUserId = insertUser("payment-other-" + suffix);
        String orderNumber = "PAY-" + suffix;
        String paidOrderNumber = "PAID-" + suffix;
        String otherOrderNumber = "OTHER-" + suffix;
        long orderId = insertOrder(orderNumber, userId, 1);
        insertOrder(paidOrderNumber, userId, 2);
        insertOrder(otherOrderNumber, otherUserId, 1);
        String token = token(userId);

        mockMvc.perform(post("/user/order/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + orderNumber + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/user/order/payment").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/user/order/payment").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + otherOrderNumber + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(post("/user/order/payment").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + paidOrderNumber + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/user/order/payment").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + orderNumber + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.event").value("ORDER_PAID"))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.paymentTime").isNotEmpty());

        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", Integer.class, orderId));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT checkout_time FROM orders WHERE id = ?", java.time.LocalDateTime.class, orderId));

        mockMvc.perform(post("/user/order/payment").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + orderNumber + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    private long insertUser(String username) {
        return insert("""
                INSERT INTO user (openid, username, password, status, create_time, update_time)
                VALUES (?, ?, ?, 1, NOW(), NOW())
                """, "openid-" + username, username, "");
    }

    private long insertOrder(String orderNumber, long userId, int orderStatus) {
        return insert("""
                INSERT INTO orders (`number`, user_id, status, amount, delivery_fee,
                                    order_time, tableware_amount, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, NOW(), 0, NOW(), NOW())
                """, orderNumber, userId, orderStatus, new BigDecimal("20.00"), BigDecimal.ZERO);
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

    private String token(long userId) {
        return JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(),
                Map.of(JwtClaimsConstant.USER_ID, userId));
    }
}
