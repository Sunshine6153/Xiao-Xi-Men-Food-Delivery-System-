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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SkyApplication.class)
@AutoConfigureMockMvc
@Transactional
@Rollback
class MerchantWorkspaceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void workspaceUsesCurrentMerchantDataAndReturnsOnlyOwnDishes() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long merchantId = insert(
                "INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, ?, ?, 'MERCHANT', 1)",
                "workspace-a-" + suffix, "test", "Workspace A");
        long otherMerchantId = insert(
                "INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, ?, ?, 'MERCHANT', 1)",
                "workspace-b-" + suffix, "test", "Workspace B");
        long userId = insert(
                "INSERT INTO user (openid, username, password, status) VALUES (?, ?, '', 1)",
                "workspace-openid-" + suffix, "workspace-user-" + suffix);
        long categoryId = insert(
                "INSERT INTO category (name, sort, status) VALUES (?, 1, 1)",
                "Workspace category " + suffix);
        long ownDishId = insert(
                "INSERT INTO dish (category_id, merchant_id, name, price, status) VALUES (?, ?, ?, ?, 1)",
                categoryId, merchantId, "Own dish", new BigDecimal("10.00"));
        long otherDishId = insert(
                "INSERT INTO dish (category_id, merchant_id, name, price, status) VALUES (?, ?, ?, ?, 1)",
                categoryId, otherMerchantId, "Other dish", new BigDecimal("30.00"));

        long completedOrderId = insertOrder(userId, 7, new BigDecimal("50.00"), LocalDateTime.now());
        insertDetail(completedOrderId, ownDishId, merchantId, "Own completed", 2, new BigDecimal("10.00"), 3);
        insertDetail(completedOrderId, otherDishId, otherMerchantId, "Other completed", 1, new BigDecimal("30.00"), 3);

        long pendingOrderId = insertOrder(userId, 3, new BigDecimal("46.00"), LocalDateTime.now());
        insertDetail(pendingOrderId, ownDishId, merchantId, "Own pending", 2, new BigDecimal("8.00"), 1);
        insertDetail(pendingOrderId, otherDishId, otherMerchantId, "Other pending", 1, new BigDecimal("30.00"), 1);

        long pickupOrderId = insertOrder(userId, 4, new BigDecimal("12.00"), LocalDateTime.now());
        insertDetail(pickupOrderId, ownDishId, merchantId, "Own pickup", 1, new BigDecimal("12.00"), 3);

        long oldCompletedOrderId = insertOrder(userId, 7, new BigDecimal("9.00"), LocalDateTime.now().minusDays(1));
        insertDetail(oldCompletedOrderId, ownDishId, merchantId, "Own old", 1, new BigDecimal("9.00"), 3);

        String merchantToken = token(merchantId, "MERCHANT");
        mockMvc.perform(get("/admin/workspace/businessData")
                        .servletPath("/admin/workspace/businessData").header("token", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.turnover").value(20.00))
                .andExpect(jsonPath("$.data.orderCount").value(3))
                .andExpect(jsonPath("$.data.orderCompletionRate").value(1.0 / 3.0))
                .andExpect(jsonPath("$.data.unitPrice").value(20.00));

        mockMvc.perform(get("/admin/workspace/overviewOrders")
                        .servletPath("/admin/workspace/overviewOrders").header("token", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingPreparationOrders").value(1))
                .andExpect(jsonPath("$.data.readyForPickupOrders").value(1))
                .andExpect(jsonPath("$.data.completedOrders").value(2))
                .andExpect(jsonPath("$.data.allOrders").value(4));

        String pageResponse = mockMvc.perform(get("/admin/workspace/pendingOrders?page=1&pageSize=10")
                        .servletPath("/admin/workspace/pendingOrders").header("token", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn().getResponse().getContentAsString();
        JsonNode records = objectMapper.readTree(pageResponse).path("data").path("records");
        assertEquals(2, records.size());
        for (JsonNode record : records) {
            assertEquals(1, record.path("dishes").size());
            assertTrue(record.path("dishes").get(0).path("name").asText().startsWith("Own "));
        }

        mockMvc.perform(get("/admin/workspace/pendingOrders?page=1&pageSize=10&status=1")
                        .servletPath("/admin/workspace/pendingOrders").header("token", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(pendingOrderId))
                .andExpect(jsonPath("$.data.records[0].merchantAmount").value(16.00));

        mockMvc.perform(get("/admin/workspace/pendingOrders?page=1&pageSize=10&status=2")
                        .servletPath("/admin/workspace/pendingOrders").header("token", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        String today = LocalDate.now().toString();
        mockMvc.perform(get("/admin/workspace/turnover?begin=" + today + "&end=" + today)
                        .servletPath("/admin/workspace/turnover").header("token", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dateList").value(today))
                .andExpect(jsonPath("$.data.turnoverList").value("20.00"));

        mockMvc.perform(get("/admin/workspace/ordersStatistics?begin=" + today + "&end=" + today)
                        .servletPath("/admin/workspace/ordersStatistics").header("token", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dateList").value(today))
                .andExpect(jsonPath("$.data.orderCountList").value("3"))
                .andExpect(jsonPath("$.data.orderCompletionRateList").value(String.valueOf(1.0 / 3.0)));

        mockMvc.perform(get("/admin/workspace/salesTop10?begin=" + today + "&end=" + today)
                        .servletPath("/admin/workspace/salesTop10").header("token", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nameList").value("Own completed"))
                .andExpect(jsonPath("$.data.numberList").value("2"));

        String adminToken = token(merchantId, "ADMIN");
        mockMvc.perform(get("/admin/workspace/businessData")
                        .servletPath("/admin/workspace/businessData").header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/admin/workspace/turnover?begin=" + today + "&end=" + today)
                        .servletPath("/admin/workspace/turnover").header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private long insertOrder(long userId, int status, BigDecimal amount, LocalDateTime orderTime) {
        return insert("""
                        INSERT INTO orders
                        (`number`, user_id, status, amount, delivery_fee, order_time, tableware_amount)
                        VALUES (?, ?, ?, ?, 0, ?, 0)
                        """,
                "workspace-" + UUID.randomUUID(), userId, status, amount, orderTime);
    }

    private void insertDetail(long orderId, long dishId, long merchantId, String name,
                              int number, BigDecimal amount, int status) {
        jdbcTemplate.update("""
                        INSERT INTO order_detail
                        (order_id, dish_id, merchant_id, name, number, amount, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                orderId, dishId, merchantId, name, number, amount, status);
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
