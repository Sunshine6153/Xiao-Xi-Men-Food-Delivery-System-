package com.neu.exception;

public class InformationMissingException extends BaseException {

    public InformationMissingException(String message) {
        super(400, message);
    }
}
