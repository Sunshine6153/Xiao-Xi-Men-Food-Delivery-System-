package com.neu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.neu.dto.UserPageQueryDTO;
import com.neu.exception.MerchantBusinessException;
import com.neu.mapper.UserMapper;
import com.neu.result.PageResult;
import com.neu.service.AdminUserService;
import com.neu.websocket.WebSocketServer;
import com.neu.vo.AdminUserVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final WebSocketServer webSocketServer;

    public AdminUserServiceImpl(UserMapper userMapper, WebSocketServer webSocketServer) {
        this.userMapper = userMapper;
        this.webSocketServer = webSocketServer;
    }

    @Override
    public PageResult page(UserPageQueryDTO query) {
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        PageHelper.startPage(page, pageSize);

        List<AdminUserVO> users = userMapper.pageQuery(query);
        PageInfo<AdminUserVO> pageInfo = new PageInfo<>(users);
        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public AdminUserVO getById(Long id) {
        AdminUserVO user = userMapper.getAdminById(id);
        if (user == null) {
            throw new MerchantBusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        if (status == null || (status != 0 && status != 1)) {
            throw new MerchantBusinessException(400, "用户状态只能为启用或禁用");
        }
        if (userMapper.updateStatus(status, id) == 0) {
            throw new MerchantBusinessException(404, "用户不存在");
        }
        if (status == 0) {
            webSocketServer.closeUserConnections(id);
        }
    }
}
