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

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SkyApplication.class)
@AutoConfigureMockMvc
@Transactional
@Rollback
class OrderLifecycleTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void deliveryAndBuyerActionsFollowTheApprovedStatusFlow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long merchantA = insertMerchant("lifecycle-a-" + suffix);
        long merchantB = insertMerchant("lifecycle-b-" + suffix);
        long ownerId = insertUser("owner-" + suffix);
        long runnerId = insertUser("runner-" + suffix);
        long otherRunnerId = insertUser("other-runner-" + suffix);
        long categoryId = insert("INSERT INTO category (name,sort,status) VALUES (?,1,1)", "Lifecycle category " + suffix);
        long dishA = insertDish(categoryId, merchantA);
        long dishB = insertDish(categoryId, merchantB);
        long orderId = insertOrder(ownerId, "lifecycle-order-" + suffix, 2);
        insertDetail(orderId, dishA, merchantA);
        insertDetail(orderId, dishB, merchantB);

        String ownerToken = userToken(ownerId);
        String runnerToken = userToken(runnerId);
        String otherRunnerToken = userToken(otherRunnerId);

        mockMvc.perform(get("/user/delivery/available"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
        JsonNode ownerAvailable = page("/user/delivery/available", ownerToken);
        assertFalse(containsOrder(ownerAvailable, orderId));
        JsonNode runnerAvailable = page("/user/delivery/available", runnerToken);
        assertTrue(containsOrder(runnerAvailable, orderId));
        mockMvc.perform(get("/user/delivery/" + orderId).header("token", runnerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.dishes.length()").value(2));
        action("/user/delivery/accept/" + orderId, ownerToken, 409);
        action("/user/delivery/accept/" + orderId, runnerToken, 200);
        assertEquals(3, orderStatus(orderId));
        assertEquals(runnerId, jdbcTemplate.queryForObject("SELECT delivery_user_id FROM orders WHERE id=?", Long.class, orderId));
        action("/user/delivery/accept/" + orderId, otherRunnerToken, 409);
        action("/user/order/cancel/" + orderId, ownerToken, 409);
        action("/user/delivery/start/" + orderId, runnerToken, 409);
        assertTrue(containsOrder(page("/user/delivery/mine", runnerToken), orderId));

        action("/admin/order/confirm/" + orderId, merchantToken(merchantA), 200);
        action("/admin/order/complete/" + orderId, merchantToken(merchantA), 200);
        assertEquals(3, orderStatus(orderId));
        action("/admin/order/confirm/" + orderId, merchantToken(merchantB), 200);
        action("/admin/order/complete/" + orderId, merchantToken(merchantB), 200);
        assertEquals(4, orderStatus(orderId));

        action("/user/delivery/start/" + orderId, otherRunnerToken, 409);
        action("/user/delivery/start/" + orderId, runnerToken, 200);
        assertEquals(5, orderStatus(orderId));
        action("/user/order/confirm/" + orderId, ownerToken, 409);
        action("/user/delivery/complete/" + orderId, otherRunnerToken, 409);
        action("/user/delivery/complete/" + orderId, runnerToken, 200);
        assertEquals(6, orderStatus(orderId));
        assertNotNull(jdbcTemplate.queryForObject("SELECT delivered_time FROM orders WHERE id=?", java.sql.Timestamp.class, orderId));
        action("/user/order/confirm/" + orderId, runnerToken, 403);
        action("/user/order/confirm/" + orderId, ownerToken, 200);
        assertEquals(7, orderStatus(orderId));
        action("/user/order/confirm/" + orderId, ownerToken, 409);
    }

    @Test
    void ownerCanCancelUnpaidAndPaidUnacceptedOrdersOnly() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long ownerId = insertUser("cancel-owner-" + suffix);
        long otherId = insertUser("cancel-other-" + suffix);
        for (int orderStatus : new int[]{1, 2}) {
            long orderId = insertOrder(ownerId, "cancel-" + orderStatus + "-" + suffix, orderStatus);
            action("/user/order/cancel/" + orderId, userToken(otherId), 403);
            action("/user/order/cancel/" + orderId, userToken(ownerId), 200);
            assertEquals(8, orderStatus(orderId));
            action("/user/order/cancel/" + orderId, userToken(ownerId), 409);
            action("/user/delivery/accept/" + orderId, userToken(otherId), 409);
        }
    }

    private void action(String path, String token, int code) throws Exception {
        mockMvc.perform(put(path).servletPath(path).header("token", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code));
    }

    private JsonNode page(String path, String token) throws Exception {
        String body = mockMvc.perform(get(path).header("token", token).param("page", "1").param("pageSize", "1000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("records");
    }

    private boolean containsOrder(JsonNode records, long orderId) {
        for (JsonNode record : records) if (record.path("id").asLong() == orderId) return true;
        return false;
    }

    private long insertMerchant(String username) {
        return insert("INSERT INTO merchant (username,password,merchant_name,role,status) VALUES (?,'','Lifecycle merchant','MERCHANT',1)", username);
    }

    private long insertUser(String username) {
        return insert("INSERT INTO user (openid,username,password,status) VALUES (?,?, '',1)", "openid-" + username, username);
    }

    private long insertDish(long categoryId, long merchantId) {
        return insert("INSERT INTO dish (category_id,merchant_id,name,price,status) VALUES (?,?,'Lifecycle dish',12,1)", categoryId, merchantId);
    }

    private long insertOrder(long userId, String number, int status) {
        return insert("INSERT INTO orders (`number`,user_id,status,amount,delivery_fee,order_time,tableware_amount,address) VALUES (?,?,?,26,2,NOW(),0,'第五宿舍')", number, userId, status);
    }

    private void insertDetail(long orderId, long dishId, long merchantId) {
        jdbcTemplate.update("INSERT INTO order_detail (order_id,dish_id,merchant_id,name,number,amount,status) VALUES (?,?,?,'Lifecycle dish',1,12,1)", orderId, dishId, merchantId);
    }

    private int orderStatus(long id) {
        return jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id=?", Integer.class, id);
    }

    private String userToken(long userId) {
        return JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), Map.of(JwtClaimsConstant.USER_ID, userId));
    }

    private String merchantToken(long merchantId) {
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), Map.of(JwtClaimsConstant.MERCHANT_ID, merchantId, JwtClaimsConstant.ROLE, "MERCHANT"));
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
