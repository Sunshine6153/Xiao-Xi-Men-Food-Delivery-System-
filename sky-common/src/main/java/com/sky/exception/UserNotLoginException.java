package com.sky.exception;

public class UserNotLoginException extends BaseException {

    public UserNotLoginException() {
        super(401, "用户未登录");
    }
}
