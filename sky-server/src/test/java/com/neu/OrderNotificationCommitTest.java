package com.neu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.websocket.WebSocketServer;
import com.neu.entity.Order;
import com.neu.mapper.OrderMapper;
import com.neu.service.OrderNotificationService;
import com.neu.task.orderTask;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderNotificationCommitTest {
    @Test
    void automaticTasksNotifyOnlyAfterSuccessfulUpdateAndKeepMidnightSchedule() throws Exception {
        List<String> events = new ArrayList<>();
        Order unpaid = Order.builder().id(1L).userId(10L).status(1).build();
        Order delivered = Order.builder().id(2L).userId(10L).status(6).build();
        OrderMapper mapper = (OrderMapper) Proxy.newProxyInstance(OrderMapper.class.getClassLoader(), new Class[]{OrderMapper.class}, (proxy, method, arguments) -> {
            if (method.getName().equals("selectUnpaidAndTimeoutOrder")) return List.of(unpaid);
            if (method.getName().equals("selectDeliveredUnconfirmedOrder")) return List.of(delivered);
            if (method.getName().equals("updateOrderStatusById")) return arguments[0].equals(1L) ? 0 : 1;
            throw new UnsupportedOperationException(method.getName());
        });
        OrderNotificationService notification = new OrderNotificationService() {
            public void notifyUsers(Order order, String event, String content) { events.add(event); }
        };
        orderTask task = new orderTask();
        ReflectionTestUtils.setField(task, "orderMapper", mapper);
        ReflectionTestUtils.setField(task, "orderNotificationService", notification);
        task.processTimeout();
        task.processDelivery();
        assertEquals(List.of("ORDER_COMPLETED"), events);
        assertEquals(7, delivered.getStatus());
        assertEquals("0 0 0 * * ?", orderTask.class.getMethod("processDelivery").getAnnotation(Scheduled.class).cron());
    }

    @Test
    void notificationsWaitForCommitAndDoNotSendOnRollback() {
        AtomicInteger sent = new AtomicInteger();
        WebSocketServer server = new WebSocketServer(new ObjectMapper()) {
            public int sendToUser(Long id, Object message) { return sent.incrementAndGet(); }
        };
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            server.sendToUserAfterCommit(1L, "committed");
            assertEquals(0, sent.get());
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertEquals(1, sent.get());
        } finally {
            TransactionSynchronizationManager.clear();
        }
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            server.sendToUserAfterCommit(1L, "rolled back");
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertEquals(1, sent.get());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}
