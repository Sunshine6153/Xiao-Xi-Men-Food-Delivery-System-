package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.dto.MerchantLoginDTO;
import com.sky.entity.Merchant;
import com.sky.exception.MerchantBusinessException;
import com.sky.mapper.MerchantMapper;
import com.sky.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Service
public class MerchantServiceImpl implements MerchantService {

    @Autowired
    private MerchantMapper merchantMapper;

    @Override
    public Merchant login(MerchantLoginDTO merchantLoginDTO) {
        //MyBatis-Plus 专门用来构造 SQL 查询条件的工具
        LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Merchant::getUsername, merchantLoginDTO.getUsername());

        Merchant merchant = merchantMapper.selectOne(queryWrapper);

        if (merchant == null) {
            throw new MerchantBusinessException("商贩账号不存在");
        }
        //将密码进行加密，再与数据库中的密码进行比较
        String encryptedPassword = DigestUtils.md5DigestAsHex(
                merchantLoginDTO.getPassword().getBytes(StandardCharsets.UTF_8)
        );
        if (!encryptedPassword.equals(merchant.getPassword())) {
            throw new MerchantBusinessException("密码错误");
        }

        if (Integer.valueOf(0).equals(merchant.getStatus())) {
            throw new MerchantBusinessException("账号已被禁用");
        }

        return merchant;
    }
}
