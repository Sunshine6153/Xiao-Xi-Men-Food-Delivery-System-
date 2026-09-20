package com.neu.service;

import com.neu.dto.UserLoginDTO;
import com.neu.entity.User;

public interface UserService {
    User wxLogin(UserLoginDTO userLoginDTO);
}
