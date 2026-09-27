package com.neu.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderPaymentDTO implements Serializable {

    private String orderNumber;
}
