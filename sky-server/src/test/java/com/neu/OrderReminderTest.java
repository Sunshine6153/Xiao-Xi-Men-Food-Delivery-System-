package com.neu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.constant.AccountRole;
import com.neu.constant.JwtClaimsConstant;
import com.neu.properties.JwtProperties;
import com.neu.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SkyApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderReminderTest {

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @Timeout(30)
    void ownerCanRemindRelatedMerchantButOtherUserCannot() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 12);
        Long merchantId = null, categoryId = null, dishId = null;
        Long ownerId = null, otherUserId = null, orderId = null;
        WebSocket webSocket = null;

        try {
            merchantId = insert("""
                    INSERT INTO merchant (username, password, merchant_name, role, status)
                    VALUES (?, 'test', 'Reminder merchant', ?, 1)
                    """, "reminder-merchant-" + suffix, AccountRole.MERCHANT);
            categoryId = insert("INSERT INTO category (name, sort, status) VALUES (?, 1, 1)",
                    "Reminder category " + suffix);
            dishId = insert("""
                    INSERT INTO dish (category_id, merchant_id, name, price, status)
                    VALUES (?, ?, 'Reminder dish', 10, 1)
                    """, categoryId, merchantId);
            ownerId = insertUser("reminder-owner-" + suffix);
            otherUserId = insertUser("reminder-other-" + suffix);
            String orderNumber = "REMINDER-" + suffix;
            orderId = insert("""
                    INSERT INTO orders (`number`, user_id, status, amount, delivery_fee,
                                        order_time, checkout_time, tableware_amount,
                                        create_time, update_time)
                    VALUES (?, ?, 2, 10, 0, NOW(), NOW(), 0, NOW(), NOW())
                    """, orderNumber, ownerId);
            jdbcTemplate.update("""
                    INSERT INTO order_detail
                    (order_id, dish_id, merchant_id, name, number, amount, status,
                     create_time, update_time)
                    VALUES (?, ?, ?, 'Reminder dish', 1, 10, 1, NOW(), NOW())
                    """, orderId, dishId, merchantId);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)).build();
            String merchantToken = adminToken(merchantId);
            HttpResponse<String> ticketResponse = client.send(
                    HttpRequest.newBuilder(httpUri("/admin/websocket/ticket"))
                            .header("token", merchantToken)
                            .POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            String ticket = objectMapper.readTree(ticketResponse.body()).path("data").asText();
            assertFalse(ticket.isBlank());

            MessageListener listener = new MessageListener();
            webSocket = client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws?ticket=" + ticket), listener)
                    .get(5, TimeUnit.SECONDS);

            HttpResponse<String> forbiddenResponse = client.send(
                    HttpRequest.newBuilder(httpUri("/user/dish/reminder/" + orderId))
                            .header("token", userToken(otherUserId)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(403, objectMapper.readTree(forbiddenResponse.body()).path("code").asInt());
            assertFalse(listener.message().isDone());

            HttpResponse<String> reminderResponse = client.send(
                    HttpRequest.newBuilder(httpUri("/user/dish/reminder/" + orderId))
                            .header("token", userToken(ownerId)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, objectMapper.readTree(reminderResponse.body()).path("code").asInt());

            JsonNode notification = objectMapper.readTree(
                    listener.message().get(5, TimeUnit.SECONDS));
            assertEquals(2, notification.path("type").asInt());
            assertEquals(orderId.longValue(), notification.path("orderId").asLong());
            assertTrue(notification.path("content").asText().contains("催单"));
        } finally {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").get(3, TimeUnit.SECONDS);
            }
            if (orderId != null) {
                jdbcTemplate.update("DELETE FROM order_detail WHERE order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
            }
            if (dishId != null) jdbcTemplate.update("DELETE FROM dish WHERE id = ?", dishId);
            if (categoryId != null) jdbcTemplate.update("DELETE FROM category WHERE id = ?", categoryId);
            if (ownerId != null) jdbcTemplate.update("DELETE FROM user WHERE id = ?", ownerId);
            if (otherUserId != null) jdbcTemplate.update("DELETE FROM user WHERE id = ?", otherUserId);
            if (merchantId != null) jdbcTemplate.update("DELETE FROM merchant WHERE id = ?", merchantId);
        }
    }

    private long insertUser(String username) {
        return insert("""
                INSERT INTO user (openid, username, password, status, create_time, update_time)
                VALUES (?, ?, '', 1, NOW(), NOW())
                """, "openid-" + username, username);
    }

    private String adminToken(long merchantId) {
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(),
                Map.of(JwtClaimsConstant.MERCHANT_ID, merchantId,
                        JwtClaimsConstant.ROLE, AccountRole.MERCHANT));
    }

    private String userToken(long userId) {
        return JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(),
                Map.of(JwtClaimsConstant.USER_ID, userId));
    }

    private URI httpUri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
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

    private static class MessageListener implements WebSocket.Listener {
        private final StringBuilder text = new StringBuilder();
        private final CompletableFuture<String> message = new CompletableFuture<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            text.append(data);
            if (last) message.complete(text.toString());
            webSocket.request(1);
            return null;
        }

        CompletableFuture<String> message() {
            return message;
        }
    }
}
