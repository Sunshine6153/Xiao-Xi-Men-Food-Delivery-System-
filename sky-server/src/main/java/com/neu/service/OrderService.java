package com.neu.service;

import com.neu.dto.OrderSubmitDTO;
import com.neu.vo.OrderPaymentVO;
import com.neu.vo.OrderSubmitVO;

public interface OrderService {
   OrderSubmitVO SubmitOrder(OrderSubmitDTO orderSubmitDTO);

   OrderPaymentVO paySuccess(String orderNumber);

    void reminder(Long orderId);
}
