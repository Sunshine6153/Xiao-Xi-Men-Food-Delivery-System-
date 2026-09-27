package com.neu.service;

import com.neu.dto.OrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.vo.OrderQueryVO;

public interface DeliveryService {

    PageResult pageAvailable(OrderPageQueryDTO query, Long userId);

    PageResult pageMine(OrderPageQueryDTO query, Long userId);

    OrderQueryVO getById(Long id, Long userId);

    void accept(Long id, Long userId);

    void start(Long id, Long userId);

    void complete(Long id, Long userId);
}
