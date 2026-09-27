package com.neu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressBookDTO implements Serializable {
    private Long id;
    private String consignee;
    private String phone;
    private String address;
    private Integer isDefault;
}
