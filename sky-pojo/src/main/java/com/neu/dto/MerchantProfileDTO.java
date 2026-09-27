package com.neu.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantProfileDTO implements Serializable {

    private String merchantName;
    private String phone;
    private String location;
}
