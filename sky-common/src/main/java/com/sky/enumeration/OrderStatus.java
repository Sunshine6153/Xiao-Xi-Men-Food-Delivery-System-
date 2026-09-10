package com.sky.enumeration;

public enum OrderStatus {

    PENDING(1),
    CONFIRMED(2),
    DELIVERING(3),
    COMPLETED(4),
    CANCELLED(5);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
