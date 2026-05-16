package com.aiworkforce.core.exception;
import lombok.Getter;
import java.util.List;
@Getter
public class ValidationException extends BaseException {
    private final List<String> errors;
    public ValidationException(String message, List<String> errors) {
        super(message, 400);
        this.errors = errors;
    }
}
