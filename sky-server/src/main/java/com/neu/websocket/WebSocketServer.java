package com.neu.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.constant.AccountRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketServer extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public WebSocketServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientKey = clientKey(session);
        if (clientKey == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("缺少客户端身份"));
            return;
        }
        sessions.computeIfAbsent(clientKey, key -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket 客户端已连接：{}，当前连接数：{}", clientKey, connectionCount());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            send(session, "pong");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket 连接异常：{}", exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session);
        log.info("WebSocket 客户端已断开，当前连接数：{}", connectionCount());
    }

    public int sendToUser(Long userId, Object message) {
        return sendToClient(clientKey("USER", userId), message);
    }

    public int sendToMerchant(Long merchantId, Object message) {
        return sendToClient(clientKey(AccountRole.MERCHANT, merchantId), message);
    }

    public int sendToAdmin(Long adminId, Object message) {
        return sendToClient(clientKey(AccountRole.ADMIN, adminId), message);
    }

    // 状态更新提交后再通知，客户端收到消息即可查询到最新数据。
    public void sendToUserAfterCommit(Long userId, Object message) {
        afterCommit(() -> sendToUser(userId, message));
    }

    public void sendToMerchantAfterCommit(Long merchantId, Object message) {
        afterCommit(() -> sendToMerchant(merchantId, message));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runNotification(action);
                }
            });
        } else {
            runNotification(action);
        }
    }

    private void runNotification(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("订单消息发送失败：{}", e.getMessage());
        }
    }

    public void closeUserConnections(Long userId) {
        closeConnections(clientKey("USER", userId));
    }

    public void closeMerchantConnections(Long merchantId) {
        closeConnections(clientKey(AccountRole.MERCHANT, merchantId));
    }

    private void closeConnections(String key) {
        Set<WebSocketSession> clientSessions = sessions.remove(key);
        if (clientSessions == null) return;
        for (WebSocketSession session : clientSessions) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("账号已被禁用"));
            } catch (IOException e) {
                log.warn("关闭账号连接失败：{}", e.getMessage());
            }
        }
    }

    public int broadcast(Object message) {
        String payload = serialize(message);
        return sessions.values().stream()
                .mapToInt(clientSessions -> send(clientSessions, payload))
                .sum();
    }

    public int connectionCount() {
        return sessions.values().stream().mapToInt(Set::size).sum();
    }

    private int sendToClient(String key, Object message) {
        Set<WebSocketSession> clientSessions = sessions.get(key);
        if (clientSessions == null || clientSessions.isEmpty()) {
            return 0;
        }
        return send(clientSessions, serialize(message));
    }

    private int send(Set<WebSocketSession> clientSessions, String payload) {
        int sent = 0;
        for (WebSocketSession session : clientSessions) {
            if (!session.isOpen()) {
                clientSessions.remove(session);
                continue;
            }
            try {
                send(session, payload);
                sent++;
            } catch (IOException e) {
                log.warn("WebSocket 消息发送失败：{}", e.getMessage());
            }
        }
        return sent;
    }

    private void send(WebSocketSession session, String payload) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(payload));
        }
    }

    private String serialize(Object message) {
        if (message instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("WebSocket 消息序列化失败", e);
        }
    }

    private String clientKey(WebSocketSession session) {
        Object value = session.getAttributes().get(WebSocketHandshakeInterceptor.CLIENT_KEY);
        return value == null ? null : value.toString();
    }

    private String clientKey(String clientType, Long clientId) {
        return clientType + ":" + clientId;
    }

    private void remove(WebSocketSession session) {
        String key = clientKey(session);
        if (key == null) {
            return;
        }
        Set<WebSocketSession> clientSessions = sessions.get(key);
        if (clientSessions == null) {
            return;
        }
        clientSessions.remove(session);
        if (clientSessions.isEmpty()) {
            sessions.remove(key, clientSessions);
        }
    }
}
