package com.aiworkforce.core.exception;
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) { super(message, 401); }
}
