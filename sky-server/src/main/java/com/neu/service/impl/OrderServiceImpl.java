package com.neu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.neu.context.BaseContext;
import com.neu.dto.OrderSubmitDTO;
import com.neu.entity.AddressBook;
import com.neu.entity.Order;
import com.neu.entity.OrderDetail;
import com.neu.entity.ShoppingCart;
import com.neu.exception.InformationMissingException;
import com.neu.exception.OrderBusinessException;
import com.neu.mapper.AddressBookMapper;
import com.neu.mapper.OrderDetailMapper;
import com.neu.mapper.OrderMapper;
import com.neu.mapper.ShoppingCartMapper;
import com.neu.service.OrderService;
import com.neu.service.OrderNotificationService;
import com.neu.vo.OrderSubmitVO;
import com.neu.vo.OrderPaymentVO;
import com.neu.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private OrderNotificationService orderNotificationService;


    @Transactional
    public OrderSubmitVO SubmitOrder(OrderSubmitDTO orderSubmitDTO) {
        //检查有无异常
        AddressBook addressBook = addressBookMapper.getByIdAndUserId(
                orderSubmitDTO.getAddressId(), BaseContext.getCurrentId()
        );
        if(addressBook == null){
            throw new InformationMissingException("地址不存在");
        }
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.listByUserId(BaseContext.getCurrentId());
        if(shoppingCarts == null || shoppingCarts.size() == 0){
            throw new InformationMissingException("购物车为空");
        }
        if (shoppingCartMapper.countUnavailableByUserId(BaseContext.getCurrentId()) > 0) {
            throw new OrderBusinessException(409, "购物车中存在已下架菜品或暂停营业的商户");
        }
        BigDecimal deliveryFee = orderSubmitDTO.getDeliveryFee() == null
                ? BigDecimal.ZERO : orderSubmitDTO.getDeliveryFee();
        if (deliveryFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderBusinessException(400, "配送费不能小于0");
        }
        BigDecimal dishAmount = BigDecimal.ZERO;
        for (ShoppingCart shoppingCart : shoppingCarts) {
            if (shoppingCart.getNumber() == null || shoppingCart.getNumber() <= 0
                    || shoppingCart.getPrice() == null
                    || shoppingCart.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new OrderBusinessException(409, "购物车商品数量或价格无效");
            }
            dishAmount = dishAmount.add(
                    shoppingCart.getPrice().multiply(BigDecimal.valueOf(shoppingCart.getNumber()))
            );
        }
        //向订单表中插入一个数据
        Order order = new Order();
        order.setUserId(BaseContext.getCurrentId());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setStatus(1);
        order.setAmount(dishAmount.add(deliveryFee));
        order.setDeliveryFee(deliveryFee);
        order.setOrderTime(LocalDateTime.now());
        order.setOrderDeliveryTime(orderSubmitDTO.getOrderDeliveryTime());
        order.setTablewareAmount(orderSubmitDTO.getTablewareAmount() == null ? 0 : orderSubmitDTO.getTablewareAmount());
        order.setConsignee(addressBook.getConsignee());
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getAddress());
        order.setRemark(orderSubmitDTO.getRemark());
        orderMapper.insert(order);
        //像订单明细表插入多个数据
        List<OrderDetail> orderDetails = new ArrayList<>();

        for(ShoppingCart shoppingCart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetail.setAmount(shoppingCart.getPrice());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);
        //清空购物车
        shoppingCartMapper.deleteShoppingCartByUserId(BaseContext.getCurrentId());

        //封装返回结果
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setId(order.getId());
        orderSubmitVO.setOrderNumber(order.getNumber());
        orderSubmitVO.setOrderAmount(order.getAmount());
        orderSubmitVO.setOrderTime(order.getOrderTime());
        return orderSubmitVO;
    }

    @Transactional
    public OrderPaymentVO paySuccess(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new InformationMissingException("订单号不能为空");
        }

        Long userId = BaseContext.getCurrentId();
        // 根据订单号和用户Id查询订单
        Order order = orderMapper.getByNumberAndUserId(orderNumber, userId);
        if (order == null) {
            throw new OrderBusinessException(404, "订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new OrderBusinessException(409, "订单当前状态不可支付");
        }
        LocalDateTime paymentTime = LocalDateTime.now();
        Order orderUpdate = Order.builder().
                id(order.getId())
                .status(2)
                .checkoutTime(paymentTime)
                .build();
        orderMapper.update(orderUpdate);

        //通过websocket推送消息
        Map<String, Object> message = new HashMap<>();
        message.put("type", 1);
        message.put("event", "ORDER_PAID");
        message.put("status", 2);
        message.put("orderId", order.getId());
        message.put("content", "订单支付成功"+orderNumber);
        String json = JSON.toJSONString(message);
        List<Long> merchantIds =
                orderDetailMapper.getMerchantIdsByOrderId(order.getId());
        for (Long merchantId : merchantIds) {
            webSocketServer.sendToMerchantAfterCommit(merchantId, json);
        }
        webSocketServer.sendToUserAfterCommit(userId, json);

        //封装返回结果
        OrderPaymentVO orderPaymentVO = new OrderPaymentVO();
        orderPaymentVO.setEvent("ORDER_PAID");
        orderPaymentVO.setOrderId(order.getId());
        orderPaymentVO.setOrderNumber(order.getNumber());
        orderPaymentVO.setStatus(2);
        orderPaymentVO.setPaymentTime(paymentTime);
        return orderPaymentVO;
    }

    public void reminder(Long orderId) {
        Long userId = BaseContext.getCurrentId();
        Order order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new OrderBusinessException(403, "无权限");
        }
        if (order.getStatus() < Order.PENDING_ACCEPTANCE
                || order.getStatus() > Order.DELIVERING) {
            throw new OrderBusinessException(409, "订单当前状态不可催单");
        }
        Map message = new HashMap();
        message.put("type", 2);
        message.put("event", "ORDER_REMINDER");
        message.put("orderId", orderId);
        message.put("content", "用户催单，请及时处理");
        String json = JSON.toJSONString(message);
        List<Long> merchantIds =
                orderDetailMapper.getMerchantIdsByOrderId(orderId);

        for (Long merchantId : merchantIds) {
            webSocketServer.sendToMerchant(merchantId, json);
        }
    }

    @Override
    @Transactional
    public void cancel(Long orderId) {
        Order order = orderMapper.getByIdForUpdate(orderId);
        if (order == null) {
            throw new OrderBusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(403, "无权限操作该订单");
        }
        if (order.getStatus() != Order.PENDING_PAYMENT
                && order.getStatus() != Order.PENDING_ACCEPTANCE) {
            throw new OrderBusinessException(409, "订单当前状态不可取消");
        }
        int rows = orderMapper.updateOrderStatusById(
                orderId, Order.CANCELLED, order.getStatus()
        );
        if (rows == 0) {
            throw new OrderBusinessException(409, "订单状态已发生变化");
        }
        order.setStatus(Order.CANCELLED);
        orderNotificationService.notifyUsers(order, "ORDER_CANCELLED", "订单已取消");
    }

    @Override
    @Transactional
    public void confirmReceipt(Long orderId) {
        Order order = orderMapper.getByIdForUpdate(orderId);
        if (order == null) {
            throw new OrderBusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(403, "无权限操作该订单");
        }
        if (order.getStatus() != Order.PENDING_RECEIPT) {
            throw new OrderBusinessException(409, "订单当前状态不可确认收餐");
        }
        int rows = orderMapper.updateOrderStatusById(
                orderId, Order.COMPLETED, Order.PENDING_RECEIPT
        );
        if (rows == 0) {
            throw new OrderBusinessException(409, "订单状态已发生变化");
        }
        order.setStatus(Order.COMPLETED);
        orderNotificationService.notifyUsers(order, "ORDER_COMPLETED", "用户已确认收餐，订单完成");
    }
}
