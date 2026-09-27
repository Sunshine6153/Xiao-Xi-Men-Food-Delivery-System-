package com.neu.service;

import com.neu.dto.MerchantDTO;
import com.neu.dto.MerchantLoginDTO;
import com.neu.dto.MerchantProfileDTO;
import com.neu.dto.MerchantPageQueryDTO;
import com.neu.entity.Merchant;
import com.neu.result.PageResult;

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

    Integer getStatus(Long id);

    Merchant getById(Long id);

    void update(MerchantDTO merchantDTO);

    Merchant getProfile(Long id);

    void updateProfile(MerchantProfileDTO merchantProfileDTO, Long id);

    Integer getBusinessStatus(Long id);

    void updateBusinessStatus(Integer status, Long id);
}
