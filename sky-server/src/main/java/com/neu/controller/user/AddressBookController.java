package com.neu.controller.user;

import com.neu.dto.AddressBookDTO;
import com.neu.entity.AddressBook;
import com.neu.result.Result;
import com.neu.service.AddressBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/addressBook")
@Tag(name = "用户端地址簿相关")
@SecurityRequirement(name = "token")
@Slf4j
public class AddressBookController {

    private final AddressBookService addressBookService;

    public AddressBookController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @PostMapping
    @Operation(summary = "新增收货地址")
    public Result<String> add(@RequestBody AddressBookDTO addressBookDTO) {
        log.info("新增收货地址：{}", addressBookDTO);
        addressBookService.add(addressBookDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "查询收货地址列表")
    public Result<List<AddressBook>> list() {
        return Result.success(addressBookService.list());
    }

    @GetMapping("/default")
    @Operation(summary = "查询默认收货地址")
    public Result<AddressBook> getDefault() {
        return Result.success(addressBookService.getDefault());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询收货地址详情")
    public Result<AddressBook> getById(@PathVariable Long id) {
        return Result.success(addressBookService.getById(id));
    }

    @PutMapping
    @Operation(summary = "修改收货地址")
    public Result<String> update(@RequestBody AddressBookDTO addressBookDTO) {
        log.info("修改收货地址：{}", addressBookDTO);
        addressBookService.update(addressBookDTO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除收货地址")
    public Result<String> delete(@PathVariable Long id) {
        addressBookService.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设置默认收货地址")
    public Result<String> setDefault(@PathVariable Long id) {
        addressBookService.setDefault(id);
        return Result.success();
    }
}
