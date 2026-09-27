package com.neu.service;

import com.neu.entity.Order;
import com.neu.mapper.OrderDetailMapper;
import com.neu.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrderNotificationService {

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    public void notifyMerchants(Order order, String event, String content) {
        Map<String, Object> message = message(order, event, content);
        for (Long merchantId : orderDetailMapper.getMerchantIdsByOrderId(order.getId())) {
            webSocketServer.sendToMerchantAfterCommit(merchantId, message);
        }
    }

    public void notifyUsers(Order order, String event, String content) {
        Map<String, Object> message = message(order, event, content);
        webSocketServer.sendToUserAfterCommit(order.getUserId(), message);
        if (order.getDeliveryUserId() != null) {
            webSocketServer.sendToUserAfterCommit(order.getDeliveryUserId(), message);
        }
    }

    private Map<String, Object> message(Order order, String event, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", 3);
        message.put("event", event);
        message.put("orderId", order.getId());
        message.put("status", order.getStatus());
        message.put("content", content);
        return message;
    }
}
