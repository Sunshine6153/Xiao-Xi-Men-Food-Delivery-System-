package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.dto.MerchantDTO;
import com.sky.dto.MerchantLoginDTO;
import com.sky.entity.Merchant;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.MerchantMapper;
import com.sky.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.Date;

@Service
public class MerchantServiceImpl implements MerchantService {

    @Autowired
    private MerchantMapper merchantMapper;

    @Override
    public Merchant login(MerchantLoginDTO merchantLoginDTO) {
        String username = merchantLoginDTO.getUsername();
        String password = merchantLoginDTO.getPassword();

        Merchant merchant = merchantMapper.getByUsername(username);

        if (merchant == null) {
            throw new AccountNotFoundException("商贩账号不存在");
        }
        //将密码进行加密，再与数据库中的密码进行比较
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(merchant.getPassword())) {
            throw new PasswordErrorException("密码错误");
        }

        if (Integer.valueOf(0).equals(merchant.getStatus())) {
            throw new AccountLockedException("账号已被禁用");
        }

        return merchant;
    }

    public void save(MerchantDTO merchantDTO) {

        Merchant merchant = Merchant.builder()
                .username(merchantDTO.getUsername())
                .password(merchantDTO.getPassword())
                .merchantName(merchantDTO.getMerchantName())
                .phone(merchantDTO.getPhone())
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        merchantMapper.insert(merchant);
    }
}