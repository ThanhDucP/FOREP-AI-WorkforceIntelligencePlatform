package com.aiworkforce.core.exception;
public class BusinessException extends BaseException {
    public BusinessException(String message) { super(message, 400); }
}
