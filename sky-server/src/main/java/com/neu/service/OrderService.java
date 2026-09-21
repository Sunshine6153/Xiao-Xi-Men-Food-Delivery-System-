package com.neu.service;

import com.neu.dto.OrderSubmitDTO;
import com.neu.vo.OrderSubmitVO;

public interface OrderService {
   public OrderSubmitVO SubmitOrder(OrderSubmitDTO orderSubmitDTO);
}
