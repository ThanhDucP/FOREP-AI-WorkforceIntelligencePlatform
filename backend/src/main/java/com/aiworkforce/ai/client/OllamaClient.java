package com.aiworkforce.ai.client;

import com.aiworkforce.ai.config.AiProperties;
import com.aiworkforce.core.exception.AiServiceException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

/**
 * Client AI dùng để gọi dịch vụ phân tích burnout/insight.
 * <p>
 * <b>Provider hiện tại đang sử dụng: Google Gemini API (miễn phí)</b>
 * <p>
 * Lịch sử thay đổi:
 * <ul>
 *   <li>Ban đầu dùng Ollama (chạy local, yêu cầu GPU) - ĐÃ NGỪNG SỬ DỤNG</li>
 *   <li>Từ phiên bản deploy trên Render: chuyển sang Gemini API (miễn phí, không cần GPU)</li>
 * </ul>
 * <p>
 * <b>LƯU Ý:</b> Code Ollama vẫn được giữ lại (deprecated) để hỗ trợ dev local nếu cần.
 * Trong môi trường production, biến {@code AI_PROVIDER=gemini} phải được set trên Render.
 * <p>
 * Khi AI không kết nối được hoặc prompt lỗi, client sẽ <b>throw {@link AiServiceException}</b>
 * thay vì trả về kết quả giả (mock/fallback). Điều này giúp frontend và controller
 * nhận biết chính xác trạng thái lỗi.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OllamaClient {

    private final WebClient ollamaWebClient;
    private final WebClient geminiWebClient;
    private final AiProperties props;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Gọi AI để sinh insight dựa trên prompt.
     * <p>
     * Tùy theo {@code ai.provider} trong config:
     * <ul>
     *   <li>{@code gemini} (mặc định cho production) → gọi Google Gemini API</li>
     *   <li>{@code ollama} (deprecated, chỉ dùng dev local) → gọi Ollama local</li>
     * </ul>
     *
     * @param prompt Nội dung prompt gửi cho AI
     * @return Chuỗi response từ AI (thường là JSON)
     * @throws AiServiceException nếu AI không kết nối được, API key sai, hoặc prompt lỗi
     * @throws IllegalArgumentException nếu prompt null hoặc rỗng
     */
    public String generateInsight(String prompt) {
        // Validate prompt đầu vào
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt không được để trống. Vui lòng cung cấp nội dung phân tích.");
        }

        if ("gemini".equalsIgnoreCase(props.getProvider())) {
            return generateInsightWithGemini(prompt);
        }

        // [DEPRECATED] Ollama - chỉ dùng cho dev local, không dùng trên production
        log.warn("[DEPRECATED] Đang sử dụng Ollama provider. Khuyến nghị chuyển sang 'gemini' cho production.");
        return generateInsightWithOllama(prompt);
    }

    // =========================================================================
    // GEMINI - Provider chính (Production)
    // =========================================================================

    /**
     * Gọi Google Gemini API để sinh insight.
     * Throw {@link AiServiceException} nếu:
     * - API key chưa cấu hình
     * - Gemini trả về lỗi HTTP (400, 401, 429, etc.)
     * - Timeout hoặc lỗi mạng
     * - Response rỗng
     */
    private String generateInsightWithGemini(String prompt) {
        AiProperties.Gemini geminiCfg = props.getGemini();

        log.info("Gọi Gemini API | model: {} | prompt length: {}", geminiCfg.getModel(), prompt.length());

        // Kiểm tra API key
        if (geminiCfg.getApiKey() == null || geminiCfg.getApiKey().isBlank()) {
            throw new AiServiceException(
                    "Gemini API key chưa được cấu hình. "
                    + "Vui lòng set biến môi trường GEMINI_API_KEY trên Render hoặc trong file .env"
            );
        }

        GeminiRequest request = GeminiRequest.builder()
                .contents(List.of(GeminiContent.builder()
                        .parts(List.of(GeminiPart.builder()
                                .text(prompt)
                                .build()))
                        .build()))
                .build();

        try {
            GeminiResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", geminiCfg.getApiKey())
                            .build(geminiCfg.getModel()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(geminiCfg.getTimeoutSeconds()))
                    .block();

            // Kiểm tra response hợp lệ
            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                GeminiCandidate candidate = response.getCandidates().get(0);
                if (candidate.getContent() != null
                        && candidate.getContent().getParts() != null
                        && !candidate.getContent().getParts().isEmpty()) {
                    String text = candidate.getContent().getParts().get(0).getText();
                    if (text != null && !text.isBlank()) {
                        log.info("Gemini API phản hồi thành công");
                        return text;
                    }
                }
            }

            // Response rỗng - throw lỗi rõ ràng
            throw new AiServiceException(
                    "Gemini API trả về response rỗng. Model: " + geminiCfg.getModel()
                    + ". Có thể prompt không phù hợp hoặc bị filter bởi safety settings."
            );

        } catch (AiServiceException e) {
            // Re-throw AiServiceException (đã tạo ở trên)
            throw e;
        } catch (WebClientResponseException e) {
            String errorDetail = extractGeminiErrorMessage(e);
            log.error("Gemini API lỗi HTTP {} | {}", e.getStatusCode(), errorDetail);

            if (e.getStatusCode().value() == 400) {
                throw new AiServiceException("Gemini API: Request không hợp lệ - " + errorDetail, e);
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new AiServiceException("Gemini API: API key không hợp lệ hoặc không có quyền truy cập. "
                        + "Kiểm tra lại GEMINI_API_KEY.", e);
            } else if (e.getStatusCode().value() == 429) {
                throw new AiServiceException("Gemini API: Đã vượt quá giới hạn request (rate limit). "
                        + "Vui lòng thử lại sau vài phút.", e);
            } else {
                throw new AiServiceException("Gemini API lỗi HTTP " + e.getStatusCode() + ": " + errorDetail, e);
            }
        } catch (Exception e) {
            log.error("Lỗi kết nối Gemini API: {}", e.getMessage());
            throw new AiServiceException(
                    "Không thể kết nối đến Gemini API. Kiểm tra kết nối mạng và cấu hình. Chi tiết: " + e.getMessage(), e
            );
        }
    }

    /**
     * Trích xuất thông báo lỗi từ Gemini error response body.
     */
    private String extractGeminiErrorMessage(WebClientResponseException e) {
        try {
            String body = e.getResponseBodyAsString();
            // Gemini trả lỗi dạng JSON: {"error":{"message":"..."}}
            if (body.contains("\"message\"")) {
                int start = body.indexOf("\"message\"") + 11;
                int end = body.indexOf("\"", start);
                if (end > start) {
                    return body.substring(start, end);
                }
            }
            return body.length() > 200 ? body.substring(0, 200) + "..." : body;
        } catch (Exception ex) {
            return e.getMessage();
        }
    }

    // =========================================================================
    // OLLAMA - [DEPRECATED] Chỉ dùng cho phát triển cục bộ (local development)
    // =========================================================================
    // LƯU Ý: Code Ollama được giữ lại để nhóm có thể test local nếu có cài Ollama.
    // Trong production trên Render, KHÔNG sử dụng Ollama vì yêu cầu GPU local.
    // Để chuyển sang Ollama: set AI_PROVIDER=ollama trong biến môi trường.

    /**
     * @deprecated Sử dụng {@code ai.provider=gemini} thay thế.
     * Ollama yêu cầu cài đặt local và GPU, không phù hợp cho deploy miễn phí.
     */
    @Deprecated(since = "2025-05", forRemoval = false)
    private String generateInsightWithOllama(String prompt) {
        AiProperties.Ollama ollamaCfg = props.getOllama();

        log.info("[DEPRECATED-OLLAMA] Gọi Ollama API | model: {} | prompt length: {}",
                ollamaCfg.getModel(), prompt.length());

        // Kiểm tra model có sẵn trên Ollama local
        if (!isOllamaModelAvailable()) {
            throw new AiServiceException(
                    "Không thể kết nối đến Ollama tại '" + ollamaCfg.getBaseUrl() + "' "
                    + "hoặc model '" + ollamaCfg.getModel() + "' chưa được cài đặt. "
                    + "Khuyến nghị: chuyển sang provider 'gemini' (set AI_PROVIDER=gemini)."
            );
        }

        OllamaRequest request = OllamaRequest.builder()
                .model(ollamaCfg.getModel())
                .prompt(prompt)
                .stream(false)
                .build();

        try {
            OllamaResponse response = ollamaWebClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .timeout(Duration.ofSeconds(ollamaCfg.getTimeoutSeconds()))
                    .block();

            if (response != null && response.getResponse() != null && !response.getResponse().isBlank()) {
                log.info("[DEPRECATED-OLLAMA] Ollama API phản hồi thành công");
                return response.getResponse();
            }

            throw new AiServiceException(
                    "Ollama API trả về response rỗng cho model '" + ollamaCfg.getModel() + "'. "
                    + "Kiểm tra model hoặc chuyển sang provider 'gemini'."
            );

        } catch (AiServiceException e) {
            throw e;
        } catch (WebClientResponseException.NotFound e) {
            throw new AiServiceException(
                    "Ollama endpoint không tìm thấy (404). URL: '" + ollamaCfg.getBaseUrl() + "'. "
                    + "Kiểm tra Ollama đang chạy hoặc chuyển sang AI_PROVIDER=gemini.", e
            );
        } catch (WebClientResponseException e) {
            throw new AiServiceException(
                    "Ollama API lỗi HTTP " + e.getStatusCode() + ": " + e.getMessage(), e
            );
        } catch (Exception e) {
            throw new AiServiceException(
                    "Không thể kết nối đến Ollama tại '" + ollamaCfg.getBaseUrl() + "'. "
                    + "Chi tiết: " + e.getMessage(), e
            );
        }
    }

    /**
     * @deprecated Kiểm tra model Ollama local - chỉ dùng khi provider=ollama
     */
    @Deprecated(since = "2025-05", forRemoval = false)
    private boolean isOllamaModelAvailable() {
        AiProperties.Ollama ollamaCfg = props.getOllama();
        try {
            OllamaTagsResponse response = ollamaWebClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(OllamaTagsResponse.class)
                    .timeout(Duration.ofSeconds(Math.min(ollamaCfg.getTimeoutSeconds(), 10)))
                    .block();

            boolean available = java.util.Optional.ofNullable(response)
                    .map(OllamaTagsResponse::getModels)
                    .orElse(List.of())
                    .stream()
                    .anyMatch(tag -> ollamaCfg.getModel().equals(tag.getName())
                            || ollamaCfg.getModel().equals(tag.getModel()));

            if (!available) {
                log.warn("[DEPRECATED-OLLAMA] Ollama đang chạy tại '{}', nhưng model '{}' không tìm thấy.",
                        ollamaCfg.getBaseUrl(), ollamaCfg.getModel());
            }
            return available;
        } catch (Exception e) {
            log.error("[DEPRECATED-OLLAMA] Không thể kết nối Ollama tại '{}': {}",
                    ollamaCfg.getBaseUrl(), e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // DTOs - Ollama (deprecated, giữ lại cho backward compatibility)
    // =========================================================================

    /** @deprecated Ollama DTO - không dùng trong production */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Deprecated(since = "2025-05")
    public static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream;
    }

    /** @deprecated Ollama DTO - không dùng trong production */
    @Data @NoArgsConstructor @AllArgsConstructor
    @Deprecated(since = "2025-05")
    public static class OllamaResponse {
        private String model;
        private String response;
        private boolean done;
    }

    /** @deprecated Ollama DTO - không dùng trong production */
    @Data @NoArgsConstructor @AllArgsConstructor
    @Deprecated(since = "2025-05")
    public static class OllamaTagsResponse {
        private List<OllamaModelTag> models;
    }

    /** @deprecated Ollama DTO - không dùng trong production */
    @Data @NoArgsConstructor @AllArgsConstructor
    @Deprecated(since = "2025-05")
    public static class OllamaModelTag {
        private String name;
        private String model;
    }

    // =========================================================================
    // DTOs - Gemini (provider chính cho production)
    // =========================================================================

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GeminiRequest {
        private List<GeminiContent> contents;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GeminiContent {
        private List<GeminiPart> parts;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GeminiPart {
        private String text;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GeminiResponse {
        private List<GeminiCandidate> candidates;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GeminiCandidate {
        private GeminiContent content;
    }
}
