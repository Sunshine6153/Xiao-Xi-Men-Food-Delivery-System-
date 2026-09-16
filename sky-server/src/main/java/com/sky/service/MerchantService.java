package com.sky.service;

import com.sky.dto.MerchantDTO;
import com.sky.dto.MerchantLoginDTO;
import com.sky.dto.MerchantPageQueryDTO;
import com.sky.entity.Merchant;
import com.sky.result.PageResult;

public interface MerchantService {

    /**
     * Login with username and password
     * @param merchantLoginDTO Login credentials
     * @return Merchant object
     */
    Merchant login(MerchantLoginDTO merchantLoginDTO);
    void save(MerchantDTO merchantDTO);

    PageResult page(MerchantPageQueryDTO merchantPageQueryDTO);

    void startOrStop(Integer status, Long id);

    Merchant getById(Long id);

    void update(MerchantDTO merchantDTO);
}
