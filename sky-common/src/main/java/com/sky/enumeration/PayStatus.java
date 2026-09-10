package com.sky.enumeration;

public enum PayStatus {

    UNPAID(0),
    PAID(1);

    private final int code;

    PayStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
