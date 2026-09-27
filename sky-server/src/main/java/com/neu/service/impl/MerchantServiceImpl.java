package com.neu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.neu.dto.MerchantDTO;
import com.neu.constant.AccountRole;
import com.neu.dto.MerchantLoginDTO;
import com.neu.dto.MerchantPageQueryDTO;
import com.neu.dto.MerchantProfileDTO;
import com.neu.entity.Merchant;
import com.neu.exception.AccountLockedException;
import com.neu.exception.AccountNotFoundException;
import com.neu.exception.PasswordErrorException;
import com.neu.exception.InformationMissingException;
import com.neu.exception.MerchantBusinessException;
import com.neu.mapper.MerchantMapper;
import com.neu.result.PageResult;
import com.neu.service.MerchantService;
import com.neu.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Service
public class MerchantServiceImpl implements MerchantService {

    private static final String STATUS_KEY_PREFIX = "merchant_status:";
    private static final String BUSINESS_STATUS_KEY_PREFIX = "merchant_business_status:";

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public Merchant login(MerchantLoginDTO merchantLoginDTO) {
        String username = merchantLoginDTO.getUsername();
        String password = merchantLoginDTO.getPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new PasswordErrorException("账号或密码不能为空");
        }

        Merchant merchant = merchantMapper.getByUsername(username);

        if (merchant == null) {
            throw new AccountNotFoundException("商贩账号不存在");
        }
        //将密码进行加密，再与数据库中的密码进行比较
        password = encodePassword(password);
        if (!password.equals(merchant.getPassword())) {
            throw new PasswordErrorException("密码错误");
        }

        if (Integer.valueOf(0).equals(getStatus(merchant.getId()))) {
            throw new AccountLockedException("账号已被禁用");
        }

        if (!AccountRole.ADMIN.equals(merchant.getRole())
                && !AccountRole.MERCHANT.equals(merchant.getRole())) {
            throw new AccountLockedException("账号角色无效");
        }

        return merchant;
    }

    public void save(MerchantDTO merchantDTO) {
        validateRequiredFields(merchantDTO, true);
        ensureUsernameAvailable(merchantDTO.getUsername(), null);

        Merchant merchant = Merchant.builder()
                .username(merchantDTO.getUsername())
                .password(encodePassword(merchantDTO.getPassword()))
                .merchantName(merchantDTO.getMerchantName())
                .phone(merchantDTO.getPhone())
                .location(merchantDTO.getLocation())
                .status(1)
                .businessStatus(1)
                .role(AccountRole.MERCHANT)
                .build();
        merchantMapper.insert(merchant);
    }

    public PageResult page(MerchantPageQueryDTO merchantPageQueryDTO) {
        int page = merchantPageQueryDTO.getPage() == null ? 1 : merchantPageQueryDTO.getPage();
        int pageSize = merchantPageQueryDTO.getPageSize() == null ? 10 : merchantPageQueryDTO.getPageSize();
        PageHelper.startPage(page, pageSize);

        List<Merchant> merchantList = merchantMapper.pageQuery(merchantPageQueryDTO);
        PageInfo<Merchant> pageInfo = new PageInfo<>(merchantList);
        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    public void startOrStop(Integer status, Long id) {
        if (status == null || (status != 0 && status != 1)) {
            throw new MerchantBusinessException(400, "商户状态只能为启用或禁用");
        }
        requireManagedMerchant(id);
        Merchant merchant = Merchant.builder().id(id).status(status).build();
        merchantMapper.startOrStop(merchant);
        stringRedisTemplate.opsForValue().set(statusKey(id), status.toString());
        if (status == 0) {
            webSocketServer.closeMerchantConnections(id);
        }
    }

    @Override
    public Integer getStatus(Long id) {
        String cachedStatus = stringRedisTemplate.opsForValue().get(statusKey(id));
        if (cachedStatus != null) {
            return Integer.valueOf(cachedStatus);
        }

        Integer status = merchantMapper.getStatus(id);
        if (status != null) {
            stringRedisTemplate.opsForValue().set(statusKey(id), status.toString());
        }
        return status;
    }

    private String statusKey(Long id) {
        return STATUS_KEY_PREFIX + id;
    }

    public Merchant getById(Long id) {
        return requireManagedMerchant(id);
    }

    public void update(MerchantDTO merchantDTO) {
        validateRequiredFields(merchantDTO, false);
        requireManagedMerchant(merchantDTO.getId());
        ensureUsernameAvailable(merchantDTO.getUsername(), merchantDTO.getId());
        String password = StringUtils.hasText(merchantDTO.getPassword())
                ? encodePassword(merchantDTO.getPassword())
                : null;
        Merchant merchant = Merchant.builder()
                .id(merchantDTO.getId())
                .username(merchantDTO.getUsername())
                .password(password)
                .merchantName(merchantDTO.getMerchantName())
                .phone(merchantDTO.getPhone())
                .location(merchantDTO.getLocation())
                .build();
        merchantMapper.update(merchant);
    }

    @Override
    public Merchant getProfile(Long id) {
        return requireManagedMerchant(id);
    }

    @Override
    public void updateProfile(MerchantProfileDTO merchantProfileDTO, Long id) {
        if (merchantProfileDTO == null
                || !StringUtils.hasText(merchantProfileDTO.getMerchantName())) {
            throw new InformationMissingException("商户名称不能为空");
        }
        if (merchantProfileDTO.getMerchantName().length() > 128
                || (merchantProfileDTO.getPhone() != null
                && merchantProfileDTO.getPhone().length() > 32)
                || (merchantProfileDTO.getLocation() != null
                && merchantProfileDTO.getLocation().length() > 256)) {
            throw new MerchantBusinessException(400, "商户信息长度超出限制");
        }
        requireManagedMerchant(id);
        Merchant merchant = Merchant.builder()
                .id(id)
                .merchantName(merchantProfileDTO.getMerchantName())
                .phone(merchantProfileDTO.getPhone())
                .location(merchantProfileDTO.getLocation())
                .build();
        merchantMapper.updateProfile(merchant);
    }

    @Override
    public Integer getBusinessStatus(Long id) {
        String cachedStatus = stringRedisTemplate.opsForValue().get(businessStatusKey(id));
        if (cachedStatus != null) {
            return Integer.valueOf(cachedStatus);
        }
        Integer status = merchantMapper.getBusinessStatus(id);
        if (status != null) {
            stringRedisTemplate.opsForValue().set(businessStatusKey(id), status.toString());
        }
        return status;
    }

    @Override
    public void updateBusinessStatus(Integer status, Long id) {
        if (status == null || (status != 0 && status != 1)) {
            throw new MerchantBusinessException(400, "营业状态只能为营业或暂停营业");
        }
        requireManagedMerchant(id);
        Merchant merchant = Merchant.builder()
                .id(id)
                .businessStatus(status)
                .build();
        merchantMapper.updateBusinessStatus(merchant);
        stringRedisTemplate.opsForValue().set(businessStatusKey(id), status.toString());
    }

    private String businessStatusKey(Long id) {
        return BUSINESS_STATUS_KEY_PREFIX + id;
    }

    private void validateRequiredFields(MerchantDTO merchantDTO, boolean creating) {
        if (merchantDTO == null) {
            throw new InformationMissingException("商户信息不能为空");
        }
        if (!creating && merchantDTO.getId() == null) {
            throw new InformationMissingException("商户ID不能为空");
        }
        if (!StringUtils.hasText(merchantDTO.getUsername())) {
            throw new InformationMissingException("商户账号不能为空");
        }
        if (creating && !StringUtils.hasText(merchantDTO.getPassword())) {
            throw new InformationMissingException("商户密码不能为空");
        }
        if (!StringUtils.hasText(merchantDTO.getMerchantName())) {
            throw new InformationMissingException("商户名称不能为空");
        }
        if (merchantDTO.getUsername().length() > 64
                || (merchantDTO.getPassword() != null && merchantDTO.getPassword().length() > 128)
                || merchantDTO.getMerchantName().length() > 128
                || (merchantDTO.getPhone() != null && merchantDTO.getPhone().length() > 32)
                || (merchantDTO.getLocation() != null && merchantDTO.getLocation().length() > 256)) {
            throw new MerchantBusinessException(400, "商户信息长度超出限制");
        }
    }

    private void ensureUsernameAvailable(String username, Long currentId) {
        Merchant existing = merchantMapper.getByUsername(username);
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new MerchantBusinessException(409, "商户账号已存在");
        }
    }

    private Merchant requireMerchant(Long id) {
        if (id == null) {
            throw new InformationMissingException("商户ID不能为空");
        }
        Merchant merchant = merchantMapper.getById(id);
        if (merchant == null) {
            throw new MerchantBusinessException(404, "商户不存在");
        }
        return merchant;
    }

    private Merchant requireManagedMerchant(Long id) {
        Merchant merchant = requireMerchant(id);
        if (!AccountRole.MERCHANT.equals(merchant.getRole())) {
            throw new MerchantBusinessException(403, "只能管理商户账号");
        }
        return merchant;
    }

    private String encodePassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
