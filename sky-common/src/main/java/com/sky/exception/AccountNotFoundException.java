package com.sky.exception;

public class AccountNotFoundException extends BaseException {

    public AccountNotFoundException() {
        super(404, "账号不存在");
    }

    public AccountNotFoundException(String message) {
        super(404, message);
    }
}
