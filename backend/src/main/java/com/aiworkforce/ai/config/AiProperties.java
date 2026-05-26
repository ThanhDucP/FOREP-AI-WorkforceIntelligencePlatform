package com.aiworkforce.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centralised AI provider configuration bound to the {@code ai} prefix in application.yml.
 * All values can be overridden by environment variables following Spring Boot naming conventions:
 *   AI_PROVIDER, GEMINI_API_KEY, GEMINI_MODEL, OLLAMA_BASE_URL, etc.
 */
@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** Active provider: "ollama" (local, dev) or "gemini" (cloud, prod). */
    private String provider = "ollama";

    private Ollama ollama = new Ollama();
    private Gemini gemini = new Gemini();

    @Data
    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String model = "gemma:2b";
        private int timeoutSeconds = 60;
    }

    @Data
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-1.5-flash";
        private int timeoutSeconds = 30;
    }
}
