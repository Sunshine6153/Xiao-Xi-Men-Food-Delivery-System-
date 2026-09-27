package com.neu.task;

import com.neu.entity.Order;
import com.neu.mapper.OrderMapper;
import com.neu.service.OrderNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class orderTask {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderNotificationService orderNotificationService;
    // 每分钟执行一次，处理订单超时任务
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeout() {
        log.info("处理订单超时任务");

        List<Order> timeoutOrders = orderMapper.selectUnpaidAndTimeoutOrder();
        for (Order order : timeoutOrders) {
            log.info("处理订单超时任务，订单编号：{}", order.getNumber());
            if (orderMapper.updateOrderStatusById(order.getId(), 8, 1) > 0) {
                order.setStatus(8);
                orderNotificationService.notifyUsers(order, "ORDER_CANCELLED", "订单支付超时，已自动取消");
            }
        }

    }

    //每天零点完成已送达但仍待确认收餐的订单
    @Scheduled(cron = "0 0 0 * * ?")
    public void processDelivery() {
        log.info("处理未确认收餐订单自动完成任务");
        List<Order> deliveryOrders = orderMapper.selectDeliveredUnconfirmedOrder();
        for (Order order : deliveryOrders) {
            log.info("自动完成未确认收餐订单，订单编号：{}", order.getNumber());
            if (orderMapper.updateOrderStatusById(order.getId(), 7, 6) > 0) {
                order.setStatus(7);
                orderNotificationService.notifyUsers(order, "ORDER_COMPLETED", "订单已在零点自动完成");
            }
        }
    }
}
