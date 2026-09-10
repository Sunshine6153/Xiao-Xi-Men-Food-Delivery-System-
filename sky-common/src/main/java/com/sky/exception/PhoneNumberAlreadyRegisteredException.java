package com.sky.exception;

public class PhoneNumberAlreadyRegisteredException extends BaseException {

    public PhoneNumberAlreadyRegisteredException() {
        super(409, "该手机号已注册");
    }
}
