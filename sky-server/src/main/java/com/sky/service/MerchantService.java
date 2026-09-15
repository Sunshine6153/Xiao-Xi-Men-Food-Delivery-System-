package com.sky.service;

import com.sky.dto.MerchantDTO;
import com.sky.dto.MerchantLoginDTO;
import com.sky.entity.Merchant;

public interface MerchantService {

    /**
     * Login with username and password
     * @param merchantLoginDTO Login credentials
     * @return Merchant object
     */
    Merchant login(MerchantLoginDTO merchantLoginDTO);
    void save(MerchantDTO merchantDTO);
}
