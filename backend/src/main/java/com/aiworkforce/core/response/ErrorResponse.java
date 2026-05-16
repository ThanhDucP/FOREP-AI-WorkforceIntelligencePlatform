package com.aiworkforce.core.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private boolean success;
    private String message;
    private List<String> errors;
    
    public static ErrorResponse error(String message, List<String> errors) {
        return ErrorResponse.builder().success(false).message(message).errors(errors).build();
    }
}
