package com.neu.service.impl;

import com.neu.context.BaseContext;
import com.neu.dto.AddressBookDTO;
import com.neu.entity.AddressBook;
import com.neu.exception.AddressBookBusinessException;
import com.neu.mapper.AddressBookMapper;
import com.neu.service.AddressBookService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {

    private final AddressBookMapper addressBookMapper;

    public AddressBookServiceImpl(AddressBookMapper addressBookMapper) {
        this.addressBookMapper = addressBookMapper;
    }

    @Override
    @Transactional
    public void add(AddressBookDTO addressBookDTO) {
        validate(addressBookDTO);
        Long userId = currentUserId();
        List<AddressBook> addresses = addressBookMapper.listByUserId(userId);

        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookDTO, addressBook);
        addressBook.setUserId(userId);
        addressBook.setIsDefault(addressBookDTO.getIsDefault() != null
                && addressBookDTO.getIsDefault() == 1 ? 1 : 0);

        if (addresses.isEmpty()) {
            addressBook.setIsDefault(1);
        } else if (addressBook.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressBookMapper.insert(addressBook);
    }

    @Override
    public List<AddressBook> list() {
        return addressBookMapper.listByUserId(currentUserId());
    }

    @Override
    public AddressBook getById(Long id) {
        return findOwnedAddress(id);
    }

    @Override
    @Transactional
    public void update(AddressBookDTO addressBookDTO) {
        if (addressBookDTO == null || addressBookDTO.getId() == null) {
            throw new AddressBookBusinessException("地址 ID 不能为空");
        }
        validate(addressBookDTO);
        Long userId = currentUserId();
        AddressBook oldAddress = findOwnedAddress(addressBookDTO.getId());

        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookDTO, addressBook);
        addressBook.setUserId(userId);
        int requestedDefault = addressBookDTO.getIsDefault() != null
                && addressBookDTO.getIsDefault() == 1 ? 1 : 0;
        // 默认地址不能被修改成普通地址，避免用户没有默认地址。
        addressBook.setIsDefault(oldAddress.getIsDefault() == 1 && requestedDefault == 0
                ? 1 : requestedDefault);

        if (addressBook.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressBookMapper.update(addressBook);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        AddressBook addressBook = findOwnedAddress(id);
        Long userId = currentUserId();
        addressBookMapper.deleteByIdAndUserId(id, userId);

        if (addressBook.getIsDefault() == 1) {
            List<AddressBook> remaining = addressBookMapper.listByUserId(userId);
            if (!remaining.isEmpty()) {
                AddressBook nextDefault = remaining.get(0);
                clearDefault(userId);
                addressBookMapper.setDefault(nextDefault);
            }
        }
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        AddressBook addressBook = findOwnedAddress(id);
        Long userId = currentUserId();
        clearDefault(userId);
        addressBook.setUserId(userId);
        addressBookMapper.setDefault(addressBook);
    }

    @Override
    public AddressBook getDefault() {
        AddressBook addressBook = addressBookMapper.getDefaultByUserId(currentUserId());
        if (addressBook == null) {
            throw new AddressBookBusinessException("默认地址不存在");
        }
        return addressBook;
    }

    private void clearDefault(Long userId) {
        AddressBook condition = new AddressBook();
        condition.setUserId(userId);
        addressBookMapper.clearDefault(condition);
    }

    private AddressBook findOwnedAddress(Long id) {
        if (id == null) {
            throw new AddressBookBusinessException("地址 ID 不能为空");
        }
        AddressBook addressBook = addressBookMapper.getByIdAndUserId(id, currentUserId());
        if (addressBook == null) {
            throw new AddressBookBusinessException("地址不存在");
        }
        return addressBook;
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new AddressBookBusinessException("用户未登录");
        }
        return userId;
    }

    private void validate(AddressBookDTO addressBookDTO) {
        if (addressBookDTO == null) {
            throw new AddressBookBusinessException("地址信息不能为空");
        }
        if (!StringUtils.hasText(addressBookDTO.getConsignee())) {
            throw new AddressBookBusinessException("收货人不能为空");
        }
        if (!StringUtils.hasText(addressBookDTO.getPhone())) {
            throw new AddressBookBusinessException("联系电话不能为空");
        }
        if (!addressBookDTO.getPhone().matches("^1\\d{10}$")) {
            throw new AddressBookBusinessException("联系电话格式不正确");
        }
        if (!StringUtils.hasText(addressBookDTO.getAddress())) {
            throw new AddressBookBusinessException("收货地址不能为空");
        }
    }
}
