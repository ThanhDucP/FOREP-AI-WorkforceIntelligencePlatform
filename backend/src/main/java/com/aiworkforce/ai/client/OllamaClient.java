package com.aiworkforce.ai.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Slf4j
public class OllamaClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;

    public OllamaClient(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:gemma:2b}") String model,
            @Value("${ollama.timeout-seconds:60}") int timeoutSeconds) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String generateInsight(String prompt) {
        log.info("Calling Ollama API with model: {} and prompt length: {}", model, prompt != null ? prompt.length() : 0);

        if (!isModelAvailable()) {
            log.warn("Ollama model '{}' is not available or Ollama is not reachable. Using structured fallback.", model);
            return buildFallbackResponse(prompt);
        }

        OllamaRequest request = OllamaRequest.builder()
                .model(model)
                .prompt(prompt)
                .stream(false)
                .build();

        try {
            OllamaResponse response = webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response != null && response.getResponse() != null && !response.getResponse().isBlank()) {
                log.info("Ollama API responded successfully");
                return response.getResponse();
            }

            log.warn("Ollama API returned an empty response for model '{}'. Using structured fallback.", model);
        } catch (WebClientResponseException.NotFound e) {
            log.error("Ollama generate endpoint returned 404. Check ollama.base-url '{}' and model '{}'.", baseUrl, model);
        } catch (WebClientResponseException e) {
            log.error("Ollama API returned HTTP {}. Using structured fallback. Error: {}", e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Error calling Ollama API. Using structured fallback. Error: {}", e.getMessage());
        }

        return buildFallbackResponse(prompt);
    }

    private boolean isModelAvailable() {
        try {
            OllamaTagsResponse response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(OllamaTagsResponse.class)
                    .timeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                    .block();

            boolean available = Optional.ofNullable(response)
                    .map(OllamaTagsResponse::getModels)
                    .orElse(List.of())
                    .stream()
                    .anyMatch(tag -> model.equals(tag.getName()) || model.equals(tag.getModel()));

            if (!available) {
                log.warn("Ollama is reachable at '{}', but model '{}' was not found in /api/tags.", baseUrl, model);
            }
            return available;
        } catch (WebClientResponseException.NotFound e) {
            log.error("Ollama /api/tags returned 404. The configured base URL '{}' may not point to an Ollama server.", baseUrl);
            return false;
        } catch (Exception e) {
            log.error("Unable to validate Ollama model '{}' at '{}'. Error: {}", model, baseUrl, e.getMessage());
            return false;
        }
    }

    private String buildFallbackResponse(String prompt) {
        log.warn("Using structured rule-based fallback for AI evaluation");
        String normalizedPrompt = prompt == null ? "" : prompt.toUpperCase(Locale.ROOT);

        if (normalizedPrompt.contains("CRITICAL") || normalizedPrompt.contains("HIGH")) {
            return """
                    {
                      "status_evaluation": "Nhân viên đang có dấu hiệu quá tải đáng kể và cần được can thiệp sớm để giảm rủi ro kiệt sức.",
                      "primary_reason": "Điểm tải công việc hoặc số tác vụ trễ hạn đang ở mức cao, cho thấy áp lực thực thi và khả năng tồn đọng đang tăng.",
                      "recommendations": [
                        "Tổ chức cuộc trao đổi 1-1 trong tuần này để xác định điểm nghẽn và mức hỗ trợ cần thiết.",
                        "Tái phân bổ các tác vụ ưu tiên cao sang thành viên còn năng lực tiếp nhận.",
                        "Tạm dừng giao thêm việc khẩn cấp cho đến khi các tác vụ tồn đọng được xử lý."
                      ]
                    }
                    """;
        }

        if (normalizedPrompt.contains("MEDIUM")) {
            return """
                    {
                      "status_evaluation": "Nhân viên đang ở vùng cần theo dõi, tải công việc chưa vượt ngưỡng nghiêm trọng nhưng đã có tín hiệu mất cân bằng.",
                      "primary_reason": "Khối lượng công việc hoặc tác vụ trễ hạn bắt đầu tăng, có thể gây giảm tập trung nếu tiếp tục kéo dài.",
                      "recommendations": [
                        "Rà soát lại thứ tự ưu tiên backlog và giảm các tác vụ chuyển ngữ cảnh nhiều.",
                        "Theo dõi tiến độ trong vài ngày tới để phát hiện sớm xu hướng quá tải.",
                        "Trao đổi với nhóm trưởng trước khi giao thêm nhiệm vụ có độ ưu tiên cao."
                      ]
                    }
                    """;
        }

        return """
                {
                  "status_evaluation": "Nhân viên đang duy trì nhịp làm việc ổn định, chưa có dấu hiệu rủi ro kiệt sức đáng kể.",
                  "primary_reason": "Tải công việc và số tác vụ trễ hạn đang ở mức kiểm soát được so với năng lực hiện tại.",
                  "recommendations": [
                    "Duy trì nhịp làm việc hiện tại và tiếp tục theo dõi workload định kỳ.",
                    "Ghi nhận đóng góp tích cực để củng cố động lực làm việc.",
                    "Có thể giao thêm nhiệm vụ vừa phải nếu vẫn giữ được tiến độ ổn định."
                  ]
                }
                """;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaResponse {
        private String model;
        private String response;
        private boolean done;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaTagsResponse {
        private List<OllamaModelTag> models;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaModelTag {
        private String name;
        private String model;
    }
}
