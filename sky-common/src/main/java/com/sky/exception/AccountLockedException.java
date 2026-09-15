package com.sky.exception;

public class AccountLockedException extends BaseException {

    public AccountLockedException() {
        super(403, "账号被锁定");
    }

    public AccountLockedException(String message) {
        super(403, message);
    }
}
