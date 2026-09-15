package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class MerchantDTO implements Serializable {
    private String username;
    private String password;
    private String merchantName;
    private String phone;
    private String location;
}
