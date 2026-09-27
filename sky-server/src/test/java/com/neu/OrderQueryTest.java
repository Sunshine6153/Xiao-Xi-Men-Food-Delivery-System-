package com.neu;

import com.neu.constant.JwtClaimsConstant;
import com.neu.properties.JwtProperties;
import com.neu.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SkyApplication.class)
@AutoConfigureMockMvc
@Transactional
@Rollback
class OrderQueryTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtProperties jwtProperties;

    @Test
    void adminSeesPlatformOrderAndUserSeesOnlyOwnOrder() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long adminId = insert(
                "INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, '', 'Query admin', 'ADMIN', 1)",
                "query-admin-" + suffix);
        long merchantA = insert(
                "INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, '', 'Query merchant A', 'MERCHANT', 1)",
                "query-a-" + suffix);
        long merchantB = insert(
                "INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, '', 'Query merchant B', 'MERCHANT', 1)",
                "query-b-" + suffix);
        long userA = insert(
                "INSERT INTO user (openid, username, name, password, status) VALUES (?, ?, 'Query user A', '', 1)",
                "query-open-a-" + suffix, "query-user-a-" + suffix);
        long userB = insert(
                "INSERT INTO user (openid, username, name, password, status) VALUES (?, ?, 'Query user B', '', 1)",
                "query-open-b-" + suffix, "query-user-b-" + suffix);
        long categoryId = insert(
                "INSERT INTO category (name, sort, status) VALUES (?, 1, 1)",
                "Query category " + suffix);
        long dishA = insert(
                "INSERT INTO dish (category_id, merchant_id, name, price, status) VALUES (?, ?, 'Query dish A', 10, 1)",
                categoryId, merchantA);
        long dishB = insert(
                "INSERT INTO dish (category_id, merchant_id, name, price, status) VALUES (?, ?, 'Query dish B', 20, 1)",
                categoryId, merchantB);
        String numberA = "query-order-a-" + suffix;
        long orderA = insert("""
                        INSERT INTO orders
                        (`number`, user_id, status, amount, delivery_fee, order_time,
                         checkout_time, tableware_amount, consignee, phone, address, remark)
                        VALUES (?, ?, 3, 33, 3, NOW(), NOW(), 1, 'Receiver A', '13800000001', 'Address A', 'Remark A')
                        """,
                numberA, userA);
        insertDetail(orderA, dishA, merchantA, "Query dish A", 1, 10);
        insertDetail(orderA, dishB, merchantB, "Query dish B", 1, 20);
        long orderB = insert("""
                        INSERT INTO orders
                        (`number`, user_id, status, amount, delivery_fee, order_time,
                         tableware_amount, consignee, phone, address)
                        VALUES (?, ?, 2, 20, 0, NOW(), 0, 'Receiver B', '13800000002', 'Address B')
                        """,
                "query-order-b-" + suffix, userB);
        insertDetail(orderB, dishB, merchantB, "Query dish B", 1, 20);

        String adminToken = adminToken(adminId, "ADMIN");
        mockMvc.perform(get("/admin/platform/order/page?page=1&pageSize=10&number=" + suffix + "&status=3")
                        .servletPath("/admin/platform/order/page").header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].number").value(numberA))
                .andExpect(jsonPath("$.data.records[0].dishes.length()").value(2));

        mockMvc.perform(get("/admin/platform/order/" + orderA)
                        .servletPath("/admin/platform/order/" + orderA).header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("Query user A"))
                .andExpect(jsonPath("$.data.dishes[0].merchantName").isNotEmpty())
                .andExpect(jsonPath("$.data.dishes[1].merchantName").isNotEmpty());

        String userTokenA = userToken(userA);
        mockMvc.perform(get("/user/order/page?page=1&pageSize=10")
                        .servletPath("/user/order/page").header("token", userTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(orderA))
                .andExpect(jsonPath("$.data.records[0].dishes.length()").value(2));

        mockMvc.perform(get("/user/order/" + orderB)
                        .servletPath("/user/order/" + orderB).header("token", userTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/admin/platform/order/page?page=1&pageSize=10")
                        .servletPath("/admin/platform/order/page")
                        .header("token", adminToken(merchantA, "MERCHANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private void insertDetail(long orderId, long dishId, long merchantId,
                              String name, int number, int amount) {
        jdbcTemplate.update("""
                        INSERT INTO order_detail
                        (order_id, dish_id, merchant_id, name, number, amount, status)
                        VALUES (?, ?, ?, ?, ?, ?, 1)
                        """,
                orderId, dishId, merchantId, name, number, amount);
    }

    private String adminToken(long id, String role) {
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(),
                Map.of(JwtClaimsConstant.MERCHANT_ID, id, JwtClaimsConstant.ROLE, role));
    }

    private String userToken(long id) {
        return JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(),
                Map.of(JwtClaimsConstant.USER_ID, id));
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
