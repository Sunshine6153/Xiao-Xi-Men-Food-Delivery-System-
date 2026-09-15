package com.sky.exception;

public class PasswordErrorException extends BaseException {

    public PasswordErrorException() {
        super(401, "密码错误");
    }

    public PasswordErrorException(String message) {
        super(401, message);
    }
}
