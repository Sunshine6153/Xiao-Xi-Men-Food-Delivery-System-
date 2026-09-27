package com.neu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.neu.dto.OrderPageQueryDTO;
import com.neu.entity.Order;
import com.neu.exception.OrderBusinessException;
import com.neu.mapper.OrderDetailMapper;
import com.neu.mapper.OrderMapper;
import com.neu.result.PageResult;
import com.neu.service.DeliveryService;
import com.neu.service.OrderNotificationService;
import com.neu.vo.OrderQueryDishVO;
import com.neu.vo.OrderQueryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final OrderNotificationService orderNotificationService;

    public DeliveryServiceImpl(OrderMapper orderMapper,
                               OrderDetailMapper orderDetailMapper,
                               OrderNotificationService orderNotificationService) {
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
        this.orderNotificationService = orderNotificationService;
    }

    @Override
    public PageResult pageAvailable(OrderPageQueryDTO query, Long userId) {
        startPage(query);
        List<OrderQueryVO> orders = orderMapper.pageAvailableDeliveryOrders(userId);
        PageInfo<OrderQueryVO> pageInfo = new PageInfo<>(orders);
        fillDishes(orders);
        return new PageResult(pageInfo.getTotal(), orders);
    }

    @Override
    public PageResult pageMine(OrderPageQueryDTO query, Long userId) {
        startPage(query);
        List<OrderQueryVO> orders = orderMapper.pageMyDeliveryOrders(userId);
        PageInfo<OrderQueryVO> pageInfo = new PageInfo<>(orders);
        fillDishes(orders);
        return new PageResult(pageInfo.getTotal(), orders);
    }

    @Override
    public OrderQueryVO getById(Long id, Long userId) {
        OrderQueryVO order = orderMapper.getDeliveryOrderById(id, userId);
        if (order == null) {
            throw new OrderBusinessException(404, "帮带订单不存在");
        }
        fillDishes(Collections.singletonList(order));
        return order;
    }

    @Override
    @Transactional
    public void accept(Long id, Long userId) {
        int rows = orderMapper.acceptDeliveryOrder(id, userId);
        if (rows == 0) {
            throw new OrderBusinessException(409, "订单已被接取或不可接取");
        }
        Order order = orderMapper.getById(id);
        orderNotificationService.notifyMerchants(order, "ORDER_ACCEPTED", "订单已有同学接取，请及时接单制作");
        orderNotificationService.notifyUsers(order, "ORDER_ACCEPTED", "订单已被接取，等待商户制作");
    }

    @Override
    @Transactional
    public void start(Long id, Long userId) {
        int rows = orderMapper.startDeliveryOrder(id, userId);
        if (rows == 0) {
            throw new OrderBusinessException(409, "订单当前状态不可开始配送");
        }
        orderNotificationService.notifyUsers(orderMapper.getById(id), "ORDER_DELIVERING", "餐品已取走，正在配送");
    }

    @Override
    @Transactional
    public void complete(Long id, Long userId) {
        int rows = orderMapper.completeDeliveryOrder(id, userId, LocalDateTime.now());
        if (rows == 0) {
            throw new OrderBusinessException(409, "订单当前状态不可确认送达");
        }
        orderNotificationService.notifyUsers(orderMapper.getById(id), "ORDER_DELIVERED", "餐品已送达，请确认收餐");
    }

    private void startPage(OrderPageQueryDTO query) {
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        PageHelper.startPage(page, pageSize);
    }

    private void fillDishes(List<OrderQueryVO> orders) {
        if (orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream().map(OrderQueryVO::getId).toList();
        Map<Long, List<OrderQueryDishVO>> dishesByOrderId = orderDetailMapper
                .listOrderDishes(orderIds)
                .stream()
                .collect(Collectors.groupingBy(OrderQueryDishVO::getOrderId));
        orders.forEach(order -> order.setDishes(
                dishesByOrderId.getOrDefault(order.getId(), Collections.emptyList())));
    }
}
