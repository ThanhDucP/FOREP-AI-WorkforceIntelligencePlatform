package com.aiworkforce.platform.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class OllamaClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public String generate(String prompt) {
        log.info("Sending prompt to Ollama: {}", prompt);
        // Simplified implementation - in production use a more robust HTTP client
        try {
            Map<String, Object> request = Map.of("model", "llama2", "prompt", prompt, "stream", false);
            Map<String, Object> response = restTemplate.postForObject(OLLAMA_URL, request, Map.class);
            return (String) response.get("response");
        } catch (Exception e) {
            log.error("Failed to connect to Ollama: {}", e.getMessage());
            return "AI Insight currently unavailable.";
        }
    }
}
