package com.neu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.neu.dto.UserLoginDTO;
import com.neu.entity.User;
import com.neu.exception.UserNotLoginException;
import com.neu.mapper.UserMapper;
import com.neu.properties.WeChatProperties;
import com.neu.service.UserService;
import com.neu.utils.HttpClientUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private static final String WX_LOGIN =
            "https://api.weixin.qq.com/sns/jscode2session";

    private final UserMapper userMapper;
    private final WeChatProperties weChatProperties;

    public UserServiceImpl(UserMapper userMapper, WeChatProperties weChatProperties) {
        this.userMapper = userMapper;
        this.weChatProperties = weChatProperties;
    }

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenid(userLoginDTO);

        if (openid == null || openid.isBlank()) {
            throw new UserNotLoginException("微信登录失败");
        }

        User user = userMapper.getByOpenid(openid);
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    // 微信登录用户不使用账号密码，但数据库字段要求非空。
                    .password("")
                    .createTime(java.time.LocalDateTime.now())
                    .status(1)
                    .build();
            userMapper.insertUser(user);
        }
        return user;
    }

    public String getOpenid(UserLoginDTO userLoginDTO) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppId());
        map.put("secret", weChatProperties.getAppSecret());
        map.put("js_code", userLoginDTO.getCode());
        map.put("grant_type", "authorization_code");

        String result = HttpClientUtils.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(result);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
