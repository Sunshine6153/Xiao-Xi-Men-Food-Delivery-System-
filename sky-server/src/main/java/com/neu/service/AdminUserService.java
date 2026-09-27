package com.neu.service;

import com.neu.dto.UserPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.vo.AdminUserVO;

public interface AdminUserService {

    PageResult page(UserPageQueryDTO query);

    AdminUserVO getById(Long id);

    void updateStatus(Integer status, Long id);
}
