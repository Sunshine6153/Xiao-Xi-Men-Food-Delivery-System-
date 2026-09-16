package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class MerchantDTO implements Serializable {
    private Long id;
    private String username;
    private String password;
    private String merchantName;
    private String phone;
    private String location;

    public void setId(Long id) {
    }
}
