package com.neu.service;

import com.neu.dto.MerchantOrderPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.vo.MerchantOrderVO;

public interface MerchantOrderService {

    PageResult page(MerchantOrderPageQueryDTO query, Long merchantId);

    MerchantOrderVO getById(Long id, Long merchantId);

    void confirm(Long id, Long merchantId);

    void complete(Long id, Long merchantId);
}
