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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SkyApplication.class)
@AutoConfigureMockMvc
@Transactional
@Rollback
class MerchantOrderManagementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void merchantsManageOnlyTheirPartAndLastMerchantAdvancesWholeOrder() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long merchantA = insert(
                "INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, '', ?, 'MERCHANT', 1)",
                "order-a-" + suffix, "Order merchant A");
        long merchantB = insert(
                "INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, '', ?, 'MERCHANT', 1)",
                "order-b-" + suffix, "Order merchant B");
        long merchantC = insert(
                "INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, '', ?, 'MERCHANT', 1)",
                "order-c-" + suffix, "Order merchant C");
        long userId = insert(
                "INSERT INTO user (openid, username, password, status) VALUES (?, ?, '', 1)",
                "order-openid-" + suffix, "order-user-" + suffix);
        long categoryId = insert(
                "INSERT INTO category (name, sort, status) VALUES (?, 1, 1)",
                "Order category " + suffix);
        long dishA = insert(
                "INSERT INTO dish (category_id, merchant_id, name, price, status) VALUES (?, ?, ?, 10, 1)",
                categoryId, merchantA, "Dish A");
        long dishB = insert(
                "INSERT INTO dish (category_id, merchant_id, name, price, status) VALUES (?, ?, ?, 20, 1)",
                categoryId, merchantB, "Dish B");
        String orderNumber = "merchant-order-" + suffix;
        long orderId = insert("""
                        INSERT INTO orders
                        (`number`, user_id, status, amount, delivery_fee, order_time, tableware_amount,
                         consignee, phone, address, remark)
                        VALUES (?, ?, 3, 40, 0, NOW(), 0, 'Test user', '13800000000', 'Test address', 'Less spicy')
                        """,
                orderNumber, userId);
        insertDetail(orderId, dishA, merchantA, "Dish A", 2, new BigDecimal("10.00"));
        insertDetail(orderId, dishB, merchantB, "Dish B", 1, new BigDecimal("20.00"));

        String tokenA = token(merchantA, "MERCHANT");
        String tokenB = token(merchantB, "MERCHANT");
        String tokenC = token(merchantC, "MERCHANT");

        String pageResponse = mockMvc.perform(get("/admin/order/page?page=1&pageSize=10&status=1&number=" + suffix)
                        .servletPath("/admin/order/page").header("token", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].merchantStatus").value(1))
                .andExpect(jsonPath("$.data.records[0].merchantAmount").value(20.00))
                .andReturn().getResponse().getContentAsString();
        JsonNode dishes = objectMapper.readTree(pageResponse)
                .path("data").path("records").get(0).path("dishes");
        assertEquals(1, dishes.size());
        assertEquals("Dish A", dishes.get(0).path("name").asText());

        mockMvc.perform(get("/admin/order/" + orderId)
                        .servletPath("/admin/order/" + orderId).header("token", tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(put("/admin/order/confirm/" + orderId)
                        .servletPath("/admin/order/confirm/" + orderId).header("token", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(2, detailStatus(orderId, merchantA));

        mockMvc.perform(put("/admin/order/complete/" + orderId)
                        .servletPath("/admin/order/complete/" + orderId).header("token", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(3, detailStatus(orderId, merchantA));
        assertEquals(1, detailStatus(orderId, merchantB));
        assertEquals(3, orderStatus(orderId));

        mockMvc.perform(put("/admin/order/confirm/" + orderId)
                        .servletPath("/admin/order/confirm/" + orderId).header("token", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(put("/admin/order/complete/" + orderId)
                        .servletPath("/admin/order/complete/" + orderId).header("token", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(3, detailStatus(orderId, merchantB));
        assertEquals(4, orderStatus(orderId));

        mockMvc.perform(put("/admin/order/complete/" + orderId)
                        .servletPath("/admin/order/complete/" + orderId).header("token", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(get("/admin/order/page?page=1&pageSize=10")
                        .servletPath("/admin/order/page").header("token", token(merchantA, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private void insertDetail(long orderId, long dishId, long merchantId,
                              String name, int number, BigDecimal amount) {
        jdbcTemplate.update("""
                        INSERT INTO order_detail
                        (order_id, dish_id, merchant_id, name, number, amount, status)
                        VALUES (?, ?, ?, ?, ?, ?, 1)
                        """,
                orderId, dishId, merchantId, name, number, amount);
    }

    private int detailStatus(long orderId, long merchantId) {
        return jdbcTemplate.queryForObject(
                "SELECT MIN(status) FROM order_detail WHERE order_id = ? AND merchant_id = ?",
                Integer.class, orderId, merchantId);
    }

    private int orderStatus(long orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", Integer.class, orderId);
    }

    private String token(long merchantId, String role) {
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(),
                Map.of(JwtClaimsConstant.MERCHANT_ID, merchantId, JwtClaimsConstant.ROLE, role));
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
