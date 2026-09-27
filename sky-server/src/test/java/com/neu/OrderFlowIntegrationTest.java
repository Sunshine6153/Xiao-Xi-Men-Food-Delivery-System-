package com.neu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.constant.JwtClaimsConstant;
import com.neu.controller.WebSocketTicketController;
import com.neu.controller.admin.MerchantOrderController;
import com.neu.controller.user.DeliveryController;
import com.neu.controller.user.OrderController;
import com.neu.controller.user.OrderPaymentController;
import com.neu.controller.user.UserOrderQueryController;
import com.neu.config.WebSocketConfiguration;
import com.neu.properties.JwtProperties;
import com.neu.properties.WebSocketProperties;
import com.neu.service.OrderNotificationService;
import com.neu.service.impl.DeliveryServiceImpl;
import com.neu.service.impl.MerchantOrderServiceImpl;
import com.neu.service.impl.OrderQueryServiceImpl;
import com.neu.service.impl.OrderServiceImpl;
import com.neu.utils.JwtUtil;
import com.neu.websocket.WebSocketHandshakeInterceptor;
import com.neu.websocket.WebSocketTicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.HttpMethod.*;

// 使用真实 MySQL、HTTP 控制器和 WebSocket；只删除本测试创建的记录。
@SpringBootTest(classes = {AdminPermissionBoundaryTest.TestApplication.class,
        OrderFlowIntegrationTest.FlowConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {
    @TestConfiguration
    @EnableConfigurationProperties(WebSocketProperties.class)
    @Import({WebSocketConfiguration.class, WebSocketHandshakeInterceptor.class, WebSocketTicketService.class,
            WebSocketTicketController.class, OrderNotificationService.class, OrderController.class,
            OrderPaymentController.class, UserOrderQueryController.class, DeliveryController.class,
            MerchantOrderController.class, OrderServiceImpl.class, OrderQueryServiceImpl.class,
            DeliveryServiceImpl.class, MerchantOrderServiceImpl.class})
    static class FlowConfiguration {}

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired JwtProperties jwt;
    @Autowired RedisTemplate redisTemplate;
    @LocalServerPort int port;
    final List<Long> users = new ArrayList<>();
    final List<Long> merchants = new ArrayList<>();
    final List<Long> dishes = new ArrayList<>();
    final List<Long> orders = new ArrayList<>();
    final List<WebSocket> sockets = new ArrayList<>();

    @Test
    @Timeout(60)
    void multiMerchantOrderNotificationsAndDisabledTokens() throws Exception {
        try {
            long buyer = user();
            long runner = user();
            long m1 = merchant("MERCHANT", "北门一号窗口");
            long m2 = merchant("MERCHANT", "西门二号窗口");
            long admin = merchant("ADMIN", "");
            long outsider = merchant("MERCHANT", "其他商户");
            long d1 = dish(m1);
            long d2 = dish(m2);
            String number = "notify-" + UUID.randomUUID();
            long order = insert("INSERT INTO orders (`number`, user_id, status, amount, delivery_fee, address) VALUES (?, ?, 1, 23, 3, ?)", number, buyer, "测试宿舍");
            orders.add(order);
            detail(order, d1, m1);
            detail(order, d2, m2);
            String buyerToken = token(buyer, "USER");
            String runnerToken = token(runner, "USER");
            String merchant1 = token(m1, "MERCHANT");
            String merchant2 = token(m2, "MERCHANT");
            String adminToken = token(admin, "ADMIN");
            Messages buyerMessages = connect(buyerToken, false);
            Messages runnerMessages = connect(runnerToken, false);
            Messages m1Messages = connect(merchant1, true);
            Messages m2Messages = connect(merchant2, true);
            Messages outsiderMessages = connect(token(outsider, "MERCHANT"), true);

            call(POST, "/user/order/payment", buyerToken, Map.of("orderNumber", number), 200);
            expect(buyerMessages, "ORDER_PAID"); expect(m1Messages, "ORDER_PAID"); expect(m2Messages, "ORDER_PAID");
            assertEquals(2, state(order));
            call(POST, "/user/order/payment", buyerToken, Map.of("orderNumber", number), 409);
            call(PUT, "/user/delivery/accept/" + order, buyerToken, null, 409);
            call(GET, "/user/dish/reminder/" + order, buyerToken, null, 200);
            expect(m1Messages, "ORDER_REMINDER"); expect(m2Messages, "ORDER_REMINDER");
            call(PUT, "/user/delivery/accept/" + order, runnerToken, null, 200);
            expect(m1Messages, "ORDER_ACCEPTED"); expect(m2Messages, "ORDER_ACCEPTED");
            expect(buyerMessages, "ORDER_ACCEPTED"); expect(runnerMessages, "ORDER_ACCEPTED");
            call(PUT, "/user/order/cancel/" + order, buyerToken, null, 409);
            call(PUT, "/user/delivery/start/" + order, runnerToken, null, 409);
            call(PUT, "/admin/order/confirm/" + order, merchant1, null, 200);
            expect(buyerMessages, "MERCHANT_PREPARING"); expect(runnerMessages, "MERCHANT_PREPARING");
            call(PUT, "/admin/order/complete/" + order, merchant1, null, 200);
            expect(buyerMessages, "MERCHANT_READY"); expect(runnerMessages, "MERCHANT_READY");
            assertEquals(3, state(order));
            JsonNode progress = call(GET, "/user/delivery/" + order, runnerToken, null, 200).path("data").path("dishes");
            assertEquals(2, progress.size());
            assertEquals("北门一号窗口", progress.get(0).path("merchantLocation").asText());
            assertEquals(3, progress.get(0).path("status").asInt());
            assertEquals(1, progress.get(1).path("status").asInt());
            call(PUT, "/admin/order/confirm/" + order, merchant2, null, 200);
            expect(buyerMessages, "MERCHANT_PREPARING"); expect(runnerMessages, "MERCHANT_PREPARING");
            call(PUT, "/admin/order/complete/" + order, merchant2, null, 200);
            expect(buyerMessages, "ORDER_READY"); expect(runnerMessages, "ORDER_READY");
            assertEquals(4, state(order));
            call(PUT, "/user/delivery/start/" + order, runnerToken, null, 200);
            expect(buyerMessages, "ORDER_DELIVERING"); expect(runnerMessages, "ORDER_DELIVERING");
            call(PUT, "/user/delivery/complete/" + order, runnerToken, null, 200);
            expect(buyerMessages, "ORDER_DELIVERED"); expect(runnerMessages, "ORDER_DELIVERED");
            call(PUT, "/user/order/confirm/" + order, buyerToken, null, 200);
            expect(buyerMessages, "ORDER_COMPLETED"); expect(runnerMessages, "ORDER_COMPLETED");
            assertEquals(7, state(order));
            call(GET, "/user/dish/reminder/" + order, buyerToken, null, 409);
            assertNull(outsiderMessages.messages.poll());
            call(GET, "/user/order/page?page=1&pageSize=1", buyerToken, null, 200);

            // 暂停营业仍可操作订单；账号禁用则旧 token 和已有通知连接失效。
            call(PUT, "/admin/merchant/business-status/0", merchant1, null, 200);
            call(GET, "/admin/auth/me", merchant1, null, 200);
            call(POST, "/admin/user/status/0?id=" + buyer, adminToken, null, 200);
            assertEquals(1008, buyerMessages.closed.poll(3, TimeUnit.SECONDS));
            call(GET, "/user/order/page", buyerToken, null, 401);
            call(POST, "/user/websocket/ticket", buyerToken, null, 401);
            call(POST, "/admin/merchant/status/0?id=" + m1, adminToken, null, 200);
            assertEquals(1008, m1Messages.closed.poll(3, TimeUnit.SECONDS));
            call(GET, "/admin/auth/me", merchant1, null, 401);
        } finally {
            sockets.forEach(WebSocket::abort);
            for (long id : orders) { jdbc.update("DELETE FROM order_detail WHERE order_id = ?", id); jdbc.update("DELETE FROM orders WHERE id = ?", id); }
            for (long id : dishes) jdbc.update("DELETE FROM dish WHERE id = ?", id);
            for (long id : merchants) {
                jdbc.update("DELETE FROM merchant WHERE id = ?", id);
                redisTemplate.delete("merchant_status:" + id);
                redisTemplate.delete("merchant_business_status:" + id);
            }
            for (long id : users) jdbc.update("DELETE FROM user WHERE id = ?", id);
        }
    }

    private JsonNode call(org.springframework.http.HttpMethod method, String path, String token, Object body, int code) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, path).servletPath(path.split("\\?", 2)[0]).header("token", token);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return json.readTree(mvc.perform(builder).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private Messages connect(String token, boolean merchant) throws Exception {
        String ticket = call(POST, merchant ? "/admin/websocket/ticket" : "/user/websocket/ticket", token, null, 200).path("data").asText();
        Messages listener = new Messages();
        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder().header("Origin", "http://localhost:" + port)
                .buildAsync(URI.create("ws://localhost:" + port + "/ws?ticket=" + ticket), listener).get(5, TimeUnit.SECONDS);
        sockets.add(socket);
        return listener;
    }
    private void expect(Messages listener, String event) throws Exception {
        String message = listener.messages.poll(3, TimeUnit.SECONDS);
        assertNotNull(message, "未收到事件 " + event);
        assertEquals(event, json.readTree(message).path("event").asText());
    }
    private int state(long id) { return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", Integer.class, id); }
    private long user() { long id = insert("INSERT INTO user (openid, username, password, status) VALUES (?, ?, '', 1)", UUID.randomUUID().toString(), "notify-" + UUID.randomUUID()); users.add(id); return id; }
    private long merchant(String role, String location) { long id = insert("INSERT INTO merchant (username, password, merchant_name, role, status, location) VALUES (?, 'test', ?, ?, 1, ?)", "notify-" + UUID.randomUUID(), "测试商户", role, location); merchants.add(id); return id; }
    private long dish(long merchant) { long id = insert("INSERT INTO dish (name, merchant_id, price, status) VALUES ('测试餐品', ?, 10, 1)", merchant); dishes.add(id); return id; }
    private void detail(long order, long dish, long merchant) { jdbc.update("INSERT INTO order_detail (order_id, dish_id, merchant_id, name, number, amount, status) VALUES (?, ?, ?, '测试餐品', 1, 10, 1)", order, dish, merchant); }
    private String token(long id, String role) {
        if ("USER".equals(role)) return JwtUtil.createJWT(jwt.getUserSecretKey(), jwt.getUserTtl(), Map.of(JwtClaimsConstant.USER_ID, id));
        return JwtUtil.createJWT(jwt.getAdminSecretKey(), jwt.getAdminTtl(), Map.of(JwtClaimsConstant.MERCHANT_ID, id, JwtClaimsConstant.ROLE, role));
    }
    private long insert(String sql, Object... values) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        }, key);
        return key.getKey().longValue();
    }
    static class Messages implements WebSocket.Listener {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final BlockingQueue<Integer> closed = new LinkedBlockingQueue<>();
        final StringBuilder text = new StringBuilder();
        public void onOpen(WebSocket socket) { socket.request(1); }
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            text.append(data);
            if (last) { messages.add(text.toString()); text.setLength(0); }
            socket.request(1);
            return null;
        }
        public CompletionStage<?> onClose(WebSocket socket, int status, String reason) { closed.add(status); return null; }
    }
}
