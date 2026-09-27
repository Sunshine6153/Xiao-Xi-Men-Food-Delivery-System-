package com.neu.exception;

public class OrderBusinessException extends BaseException {

    public OrderBusinessException(String message) {
        super(message);
    }

    public OrderBusinessException(Integer code, String message) {
        super(code, message);
    }
}
