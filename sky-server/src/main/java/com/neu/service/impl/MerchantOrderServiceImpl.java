package com.neu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.neu.dto.MerchantOrderPageQueryDTO;
import com.neu.entity.Order;
import com.neu.entity.OrderDetail;
import com.neu.exception.MerchantBusinessException;
import com.neu.mapper.OrderDetailMapper;
import com.neu.mapper.OrderMapper;
import com.neu.result.PageResult;
import com.neu.service.MerchantOrderService;
import com.neu.service.OrderNotificationService;
import com.neu.vo.MerchantOrderDishVO;
import com.neu.vo.MerchantOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MerchantOrderServiceImpl implements MerchantOrderService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final OrderNotificationService orderNotificationService;

    public MerchantOrderServiceImpl(OrderMapper orderMapper,
                                    OrderDetailMapper orderDetailMapper,
                                    OrderNotificationService orderNotificationService) {
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
        this.orderNotificationService = orderNotificationService;
    }

    @Override
    public PageResult page(MerchantOrderPageQueryDTO query, Long merchantId) {
        checkStatus(query.getStatus());

        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        PageHelper.startPage(page, pageSize);

        List<MerchantOrderVO> orders = orderMapper.pageMerchantOrders(query, merchantId);
        PageInfo<MerchantOrderVO> pageInfo = new PageInfo<>(orders);
        fillDishes(orders, merchantId);
        return new PageResult(pageInfo.getTotal(), orders);
    }

    @Override
    public MerchantOrderVO getById(Long id, Long merchantId) {
        MerchantOrderVO order = orderMapper.getMerchantOrderById(id, merchantId);
        if (order == null) {
            throw new MerchantBusinessException(404, "订单不存在");
        }
        fillDishes(Collections.singletonList(order), merchantId);
        return order;
    }

    @Override
    @Transactional
    public void confirm(Long id, Long merchantId) {
        MerchantOrderVO merchantOrder = getMerchantOrderForUpdate(id, merchantId);
        if (merchantOrder.getStatus() != Order.PREPARING
                || merchantOrder.getMerchantStatus() != OrderDetail.PENDING_CONFIRMATION) {
            throw new MerchantBusinessException(409, "订单当前状态不可接单");
        }

        orderDetailMapper.updateMerchantStatus(
                id,
                merchantId,
                OrderDetail.PREPARING,
                OrderDetail.PENDING_CONFIRMATION
        );
        orderNotificationService.notifyUsers(orderMapper.getById(id), "MERCHANT_PREPARING", "商户已接单，正在制作餐品");
    }

    @Override
    @Transactional
    public void complete(Long id, Long merchantId) {
        MerchantOrderVO merchantOrder = getMerchantOrderForUpdate(id, merchantId);
        if (merchantOrder.getStatus() != Order.PREPARING
                || merchantOrder.getMerchantStatus() != OrderDetail.PREPARING) {
            throw new MerchantBusinessException(409, "订单当前状态不可确认出餐");
        }

        orderDetailMapper.updateMerchantStatus(
                id,
                merchantId,
                OrderDetail.COMPLETED,
                OrderDetail.PREPARING
        );

        int unfinishedCount = orderDetailMapper.countUnfinishedByOrderId(id);
        if (unfinishedCount == 0) {
            orderMapper.updateOrderStatusById(
                    id,
                    Order.READY_FOR_PICKUP,
                    Order.PREPARING
            );
            orderNotificationService.notifyUsers(orderMapper.getById(id), "ORDER_READY", "所有商户均已出餐，可以前往取餐");
        } else {
            orderNotificationService.notifyUsers(orderMapper.getById(id), "MERCHANT_READY", "已有商户出餐，请查看各店制作进度");
        }
    }

    private MerchantOrderVO getMerchantOrderForUpdate(Long id, Long merchantId) {
        Order order = orderMapper.getByIdForUpdate(id);
        if (order == null) {
            throw new MerchantBusinessException(404, "订单不存在");
        }

        MerchantOrderVO merchantOrder = orderMapper.getMerchantOrderById(id, merchantId);
        if (merchantOrder == null) {
            throw new MerchantBusinessException(403, "无权操作该订单");
        }
        return merchantOrder;
    }

    private void fillDishes(List<MerchantOrderVO> orders, Long merchantId) {
        if (orders.isEmpty()) {
            return;
        }

        List<Long> orderIds = orders.stream().map(MerchantOrderVO::getId).toList();
        Map<Long, List<MerchantOrderDishVO>> dishesByOrderId = orderDetailMapper
                .listMerchantDishes(orderIds, merchantId)
                .stream()
                .collect(Collectors.groupingBy(MerchantOrderDishVO::getOrderId));
        orders.forEach(order -> order.setDishes(
                dishesByOrderId.getOrDefault(order.getId(), Collections.emptyList())));
    }

    private void checkStatus(Integer status) {
        if (status != null
                && status != OrderDetail.PENDING_CONFIRMATION
                && status != OrderDetail.PREPARING
                && status != OrderDetail.COMPLETED) {
            throw new MerchantBusinessException(400, "订单制作状态不正确");
        }
    }
}
