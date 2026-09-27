package com.neu;

import com.neu.constant.JwtClaimsConstant;
import com.neu.controller.admin.AuthController;
import com.neu.controller.admin.AdminUserController;
import com.neu.controller.admin.CategoryController;
import com.neu.controller.admin.DishController;
import com.neu.controller.admin.MerchantController;
import com.neu.handler.GlobalExceptionHandle;
import com.neu.interceptor.JwtTokenAdminInterceptor;
import com.neu.interceptor.JwtTokenUserInterceptor;
import com.neu.properties.JwtProperties;
import com.neu.properties.LocalFileProperties;
import com.neu.config.WebMvcConfiguration;
import com.neu.service.impl.CategoryServiceImpl;
import com.neu.service.impl.AdminUserServiceImpl;
import com.neu.service.impl.DishServiceImpl;
import com.neu.service.impl.MerchantServiceImpl;
import com.neu.utils.JwtUtil;
import com.neu.websocket.WebSocketServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminPermissionBoundaryTest.TestApplication.class)
@AutoConfigureMockMvc
@Transactional
@Rollback
class AdminPermissionBoundaryTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties({JwtProperties.class, LocalFileProperties.class})
    @MapperScan("com.neu.mapper")
    @Import({WebMvcConfiguration.class, JwtTokenAdminInterceptor.class,
            JwtTokenUserInterceptor.class, GlobalExceptionHandle.class,
            CategoryController.class, DishController.class, MerchantController.class,
            AuthController.class, AdminUserController.class, CategoryServiceImpl.class,
            AdminUserServiceImpl.class, DishServiceImpl.class,
            MerchantServiceImpl.class, WebSocketServer.class, UnknownAdminController.class})
    static class TestApplication {
        @Bean("redisTemplate")
        @Primary
        RedisTemplate<String, Object> noOpRedisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>() {
                @Override
                public Set<String> keys(String pattern) {
                    return Collections.emptySet();
                }

                @Override
                public Long delete(Collection<String> keys) {
                    return 0L;
                }
            };
            template.setConnectionFactory(connectionFactory);
            return template;
        }
    }

    @RestController
    static class UnknownAdminController {
        @GetMapping("/admin/unknown")
        String unknown() {
            return "should never be reached";
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @Timeout(30)
    void anonymousMerchantAndAdminHaveOnlyTheirAllowedRoutes() throws Exception {
        String merchantToken = token(createAccount("MERCHANT"), "MERCHANT");
        String adminToken = token(createAccount("ADMIN"), "ADMIN");

        mockMvc.perform(get("/admin/category/list"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(post("/admin/merchant/page"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/admin/category/list").header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/admin/category/list").header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        String testCategory = "Permission test " + UUID.randomUUID();
        String body = "{\"name\":\"" + testCategory + "\",\"sort\":1}";
        mockMvc.perform(post("/admin/category").header("token", merchantToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        assertEquals(0, categoryCount(testCategory));
        mockMvc.perform(post("/admin/category").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(1, categoryCount(testCategory));
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM category WHERE name = ?", Long.class, testCategory);

        String updatedCategory = testCategory + " updated";
        String updateBody = "{\"id\":" + categoryId + ",\"name\":\"" + updatedCategory + "\",\"sort\":2}";
        mockMvc.perform(put("/admin/category").header("token", merchantToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(put("/admin/category").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(1, categoryCount(updatedCategory));

        String statusPath = "/admin/category/status/0?id=" + categoryId;
        mockMvc.perform(post(statusPath).header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post(statusPath).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT status FROM category WHERE id = ?", Integer.class, categoryId));

        String deletePath = "/admin/category/" + categoryId;
        mockMvc.perform(delete(deletePath).header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(delete(deletePath).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0, categoryCount(updatedCategory));

        mockMvc.perform(post("/admin/merchant/page").header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/admin/merchant/page").header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        String testUsername = "permission-merchant-" + UUID.randomUUID().toString().substring(0, 12);
        String merchantBody = "{\"username\":\"" + testUsername
                + "\",\"password\":\"test\",\"merchantName\":\"Permission test merchant\""
                + ",\"phone\":\"13800000000\",\"location\":\"Test location\"}";
        mockMvc.perform(post("/admin/merchant/save").header("token", merchantToken)
                        .contentType(MediaType.APPLICATION_JSON).content(merchantBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchant WHERE username = ?", Integer.class, testUsername));
        mockMvc.perform(post("/admin/merchant/save").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(merchantBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals("MERCHANT", jdbcTemplate.queryForObject("SELECT role FROM merchant WHERE username = ?", String.class, testUsername));
        Long testMerchantId = jdbcTemplate.queryForObject("SELECT id FROM merchant WHERE username = ?", Long.class, testUsername);
        String originalPassword = DigestUtils.md5DigestAsHex("test".getBytes(StandardCharsets.UTF_8));
        assertEquals(originalPassword, jdbcTemplate.queryForObject("SELECT password FROM merchant WHERE id = ?", String.class, testMerchantId));
        assertEquals("Test location", jdbcTemplate.queryForObject("SELECT location FROM merchant WHERE id = ?", String.class, testMerchantId));

        mockMvc.perform(get("/admin/merchant/" + testMerchantId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.location").value("Test location"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        String updateWithoutPassword = "{\"id\":" + testMerchantId + ",\"username\":\"" + testUsername
                + "\",\"password\":\"\",\"merchantName\":\"Permission merchant updated\""
                + ",\"phone\":\"13900000000\",\"location\":\"Updated location\"}";
        mockMvc.perform(post("/admin/merchant/update").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateWithoutPassword))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(originalPassword, jdbcTemplate.queryForObject("SELECT password FROM merchant WHERE id = ?", String.class, testMerchantId));
        assertEquals("Updated location", jdbcTemplate.queryForObject("SELECT location FROM merchant WHERE id = ?", String.class, testMerchantId));

        String updateWithPassword = "{\"id\":" + testMerchantId + ",\"username\":\"" + testUsername
                + "\",\"password\":\"new-password\",\"merchantName\":\"Permission merchant updated\""
                + ",\"phone\":\"13900000000\",\"location\":\"Updated location\"}";
        mockMvc.perform(post("/admin/merchant/update").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateWithPassword))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(DigestUtils.md5DigestAsHex("new-password".getBytes(StandardCharsets.UTF_8)),
                jdbcTemplate.queryForObject("SELECT password FROM merchant WHERE id = ?", String.class, testMerchantId));

        mockMvc.perform(post("/admin/merchant/save").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(merchantBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));
        mockMvc.perform(post("/admin/merchant/update").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"missing-id\",\"merchantName\":\"Missing id\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(get("/admin/dish/page?page=1&pageSize=2").header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/admin/dish").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/admin/user/page?page=1&pageSize=2").header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/admin/dish/clearCache?pattern=*").header("token", merchantToken))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/admin/auth/me").header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("MERCHANT"));
        mockMvc.perform(get("/admin/auth/me").header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("ADMIN"));
        mockMvc.perform(get("/admin/unknown").header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/admin/unknown").header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Timeout(30)
    void merchantCannotReadOrChangeAnotherMerchantsDish() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 12);
        long merchantId = insert("INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, ?, ?, ?, ?)",
                "permission-a-" + suffix, "test", "Permission A", "MERCHANT", 1);
        long otherMerchantId = insert("INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, ?, ?, ?, ?)",
                "permission-b-" + suffix, "test", "Permission B", "MERCHANT", 1);
        long protectedAdminId = insert("INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, ?, ?, ?, ?)",
                "permission-admin-" + suffix, "test", "Protected Admin", "ADMIN", 1);
        long categoryId = insert("INSERT INTO category (name, sort, status) VALUES (?, ?, ?)",
                "Permission category " + suffix, 1, 1);
        long ownDishId = insert("INSERT INTO dish (category_id, merchant_id, name, price, status) VALUES (?, ?, ?, ?, ?)",
                categoryId, merchantId, "Own dish", new BigDecimal("10.00"), 1);
        long otherDishId = insert("INSERT INTO dish (category_id, merchant_id, name, price, status) VALUES (?, ?, ?, ?, ?)",
                categoryId, otherMerchantId, "Other dish", new BigDecimal("12.00"), 1);
        long userId = insert("""
                        INSERT INTO user (openid, username, password, status, create_time, update_time)
                        VALUES (?, ?, ?, ?, NOW(), NOW())
                        """,
                "permission-openid-" + suffix, "permission-user-" + suffix, "", 1);
        String merchantToken = token(merchantId, "MERCHANT");
        String adminToken = token(createAccount("ADMIN"), "ADMIN");

        mockMvc.perform(delete("/admin/category/" + categoryId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("分类下存在菜品，不能删除"));
        assertEquals(1, categoryCount("Permission category " + suffix));

        String merchantPageResponse = mockMvc.perform(post("/admin/merchant/page?page=1&pageSize=100").header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode record : objectMapper.readTree(merchantPageResponse).path("data").path("records")) {
            assertEquals("MERCHANT", record.path("role").asText());
        }
        mockMvc.perform(get("/admin/merchant/" + protectedAdminId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/admin/merchant/status/0?id=" + protectedAdminId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/admin/merchant/update").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + protectedAdminId + ",\"username\":\"permission-admin-" + suffix
                                + "\",\"merchantName\":\"Attempted admin edit\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        assertEquals("Protected Admin", jdbcTemplate.queryForObject("SELECT merchant_name FROM merchant WHERE id = ?", String.class, protectedAdminId));

        mockMvc.perform(get("/admin/dish/" + ownDishId).header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/admin/dish/" + otherDishId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.merchantId").value(otherMerchantId));
        mockMvc.perform(get("/admin/dish/page?page=1&pageSize=100&merchantId=" + otherMerchantId
                        + "&categoryId=" + categoryId + "&status=1").header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(otherDishId));
        String pageResponse = mockMvc.perform(get("/admin/dish/page?page=1&pageSize=100&merchantId=" + otherMerchantId)
                        .header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        JsonNode records = objectMapper.readTree(pageResponse).path("data").path("records");
        assertTrue(records.isArray());
        assertTrue(records.size() > 0);
        for (JsonNode record : records) {
            assertEquals(merchantId, record.path("merchantId").asLong());
        }
        mockMvc.perform(post("/admin/dish/status/0?id=" + ownDishId).header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT status FROM dish WHERE id = ?", Integer.class, ownDishId));
        mockMvc.perform(get("/admin/dish/" + otherDishId).header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(post("/admin/dish/status/0?id=" + otherDishId).header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(delete("/admin/dish/" + otherDishId).header("token", merchantToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(put("/admin/dish").header("token", merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + otherDishId + ",\"categoryId\":" + categoryId
                                + ",\"name\":\"Attempted edit\",\"price\":12.00}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT status FROM dish WHERE id = ?", Integer.class, otherDishId));
        assertEquals("Other dish", jdbcTemplate.queryForObject("SELECT name FROM dish WHERE id = ?", String.class, otherDishId));

        mockMvc.perform(post("/admin/dish/status/0?id=" + otherDishId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT status FROM dish WHERE id = ?", Integer.class, otherDishId));
        mockMvc.perform(delete("/admin/dish/" + otherDishId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/admin/user/page?page=1&pageSize=10&keyword=" + suffix + "&status=1")
                        .header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(userId))
                .andExpect(jsonPath("$.data.records[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].openid").doesNotExist());
        mockMvc.perform(get("/admin/user/" + userId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.openid").doesNotExist());
        mockMvc.perform(post("/admin/user/status/0?id=" + userId).header("token", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT status FROM user WHERE id = ?", Integer.class, userId));
    }

    private String token(long id, String role) {
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(),
                Map.of(JwtClaimsConstant.MERCHANT_ID, id, JwtClaimsConstant.ROLE, role));
    }

    private long createAccount(String role) {
        return insert("INSERT INTO merchant (username, password, merchant_name, role, status) VALUES (?, ?, ?, ?, ?)",
                "permission-" + UUID.randomUUID(), "test", "Permission account", role, 1);
    }

    private MockHttpServletRequestBuilder get(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .servletPath(url.split("\\?", 2)[0]);
    }

    private MockHttpServletRequestBuilder post(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .servletPath(url.split("\\?", 2)[0]);
    }

    private MockHttpServletRequestBuilder put(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .servletPath(url.split("\\?", 2)[0]);
    }

    private MockHttpServletRequestBuilder delete(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url)
                .servletPath(url.split("\\?", 2)[0]);
    }

    private int categoryCount(String name) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category WHERE name = ?", Integer.class, name);
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
