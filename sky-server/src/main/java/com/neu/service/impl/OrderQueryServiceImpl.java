package com.neu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.neu.dto.OrderPageQueryDTO;
import com.neu.exception.OrderBusinessException;
import com.neu.mapper.OrderDetailMapper;
import com.neu.mapper.OrderMapper;
import com.neu.result.PageResult;
import com.neu.service.OrderQueryService;
import com.neu.vo.OrderQueryDishVO;
import com.neu.vo.OrderQueryVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;

    public OrderQueryServiceImpl(OrderMapper orderMapper,
                                 OrderDetailMapper orderDetailMapper) {
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
    }

    @Override
    public PageResult pageForAdmin(OrderPageQueryDTO query) {
        checkQuery(query);
        startPage(query);
        List<OrderQueryVO> orders = orderMapper.pageAdminOrders(query);
        PageInfo<OrderQueryVO> pageInfo = new PageInfo<>(orders);
        fillDishes(orders);
        return new PageResult(pageInfo.getTotal(), orders);
    }

    @Override
    public OrderQueryVO getForAdmin(Long id) {
        return getById(id, null);
    }

    @Override
    public PageResult pageForUser(OrderPageQueryDTO query, Long userId) {
        checkQuery(query);
        startPage(query);
        List<OrderQueryVO> orders = orderMapper.pageUserOrders(query, userId);
        PageInfo<OrderQueryVO> pageInfo = new PageInfo<>(orders);
        fillDishes(orders);
        return new PageResult(pageInfo.getTotal(), orders);
    }

    @Override
    public OrderQueryVO getForUser(Long id, Long userId) {
        return getById(id, userId);
    }

    private OrderQueryVO getById(Long id, Long userId) {
        OrderQueryVO order = orderMapper.getOrderQueryById(id, userId);
        if (order == null) {
            throw new OrderBusinessException(404, "订单不存在");
        }
        fillDishes(Collections.singletonList(order));
        return order;
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

    private void checkQuery(OrderPageQueryDTO query) {
        if (query.getStatus() != null
                && (query.getStatus() < 1 || query.getStatus() > 8)) {
            throw new OrderBusinessException(400, "订单状态不正确");
        }
        if (query.getBegin() != null && query.getEnd() != null
                && query.getBegin().isAfter(query.getEnd())) {
            throw new OrderBusinessException(400, "开始日期不能晚于结束日期");
        }
    }
}
