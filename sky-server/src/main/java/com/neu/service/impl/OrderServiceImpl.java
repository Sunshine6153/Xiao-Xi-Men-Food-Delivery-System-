package com.neu.service.impl;

import com.neu.context.BaseContext;
import com.neu.dto.OrderSubmitDTO;
import com.neu.entity.AddressBook;
import com.neu.entity.Order;
import com.neu.entity.OrderDetail;
import com.neu.entity.ShoppingCart;
import com.neu.exception.InformationMissingException;
import com.neu.mapper.AddressBookMapper;
import com.neu.mapper.OrderDetailMapper;
import com.neu.mapper.OrderMapper;
import com.neu.mapper.ShoppingCartMapper;
import com.neu.service.OrderService;
import com.neu.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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


    @Transactional
    public OrderSubmitVO SubmitOrder(OrderSubmitDTO orderSubmitDTO) {
        //检查有无异常
        AddressBook addressBook = addressBookMapper.getById(orderSubmitDTO.getAddressId());
        if(addressBook == null){
            throw new InformationMissingException("地址不存在");
        }
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.listByUserId(BaseContext.getCurrentId());
        if(shoppingCarts == null || shoppingCarts.size() == 0){
            throw new InformationMissingException("购物车为空");
        }
        //向订单表中插入一个数据
        Order order = new Order();
        order.setUserId(BaseContext.getCurrentId());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setStatus(1);
        order.setAmount(orderSubmitDTO.getOrderAmount());
        order.setDeliveryFee(orderSubmitDTO.getDeliveryFee() == null ? BigDecimal.ZERO : orderSubmitDTO.getDeliveryFee());
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
}
