package com.sky.exception;

public class MerchantBusinessException extends BaseException {

    public MerchantBusinessException(String message) {
        super(message);
    }

    public MerchantBusinessException(Integer code, String message) {
        super(code, message);
    }
}
