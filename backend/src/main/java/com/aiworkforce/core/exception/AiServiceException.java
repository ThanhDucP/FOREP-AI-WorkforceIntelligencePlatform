package com.aiworkforce.core.exception;

/**
 * Exception được throw khi không thể kết nối hoặc gọi dịch vụ AI (Gemini API).
 * <p>
 * Các trường hợp sử dụng:
 * <ul>
 *   <li>API key chưa được cấu hình hoặc không hợp lệ</li>
 *   <li>Gemini API trả về lỗi HTTP (400, 401, 429, 500, ...)</li>
 *   <li>Timeout hoặc mất kết nối mạng</li>
 *   <li>Gemini API trả về response rỗng hoặc không parse được</li>
 * </ul>
 */
public class AiServiceException extends BaseException {

    public AiServiceException(String message) {
        super(message, 503); // 503 Service Unavailable
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, 503);
        initCause(cause);
    }
}
