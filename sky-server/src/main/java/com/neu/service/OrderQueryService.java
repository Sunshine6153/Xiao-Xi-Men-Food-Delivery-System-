package com.neu.service;

import com.neu.dto.OrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.vo.OrderQueryVO;

public interface OrderQueryService {

    PageResult pageForAdmin(OrderPageQueryDTO query);

    OrderQueryVO getForAdmin(Long id);

    PageResult pageForUser(OrderPageQueryDTO query, Long userId);

    OrderQueryVO getForUser(Long id, Long userId);
}
