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

import java.time.Duration;

@Component
@Slf4j
public class OllamaClient {
    
    private final WebClient webClient;
    private final String model;
    private final int timeoutSeconds;

    public OllamaClient(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:gemma:2b}") String model,
            @Value("${ollama.timeout-seconds:60}") int timeoutSeconds) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public String generateInsight(String prompt) {
        log.info("Calling Ollama API with model: {} and prompt: {}", model, prompt);
        
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

            if (response != null && response.getResponse() != null) {
                log.info("Ollama API responded successfully");
                return response.getResponse();
            }
        } catch (Exception e) {
            log.error("Error calling Ollama API, falling back to mock response. Error: {}", e.getMessage());
        }

        // Robust Fallback in case local Ollama isn't started
        log.warn("Using fallback mock response for AI evaluation");
        if (prompt.contains("HIGH")) {
            return "This employee is under extreme pressure and shows multiple overdue tasks. They are at high risk of burnout. Recommendation: Schedule a 1-on-1 immediately to redistribute their workload.";
        } else if (prompt.contains("MEDIUM")) {
            return "The employee is managing fine but starting to accumulate some delay. They might be approaching their capacity limit. Recommendation: Monitor their next tasks and avoid assigning critical priority items this week.";
        } else {
            return "The employee is performing optimally with a balanced workload. No signs of burnout detected. Recommendation: Acknowledge their consistent output.";
        }
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
}
