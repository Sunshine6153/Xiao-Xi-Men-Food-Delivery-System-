package com.sky.constant;

public enum Status {

    DISABLE(0),
    ENABLE(1);

    private final int code;

    Status(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
