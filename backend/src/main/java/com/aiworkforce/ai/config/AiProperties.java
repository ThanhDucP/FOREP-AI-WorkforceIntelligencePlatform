package com.aiworkforce.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centralised Gemini configuration bound to the {@code ai} prefix in application.yml.
 * All values can be overridden by environment variables following Spring Boot naming conventions:
 *   GEMINI_API_KEY, GEMINI_MODEL, GEMINI_TIMEOUT_SECONDS.
 */
@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private Gemini gemini = new Gemini();
    private Rag rag = new Rag();

    @Data
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-1.5-flash";
        private int timeoutSeconds = 30;
    }

    @Data
    public static class Rag {
        private boolean enabled = true;
        private int maxContextCharacters = 4000;
        private int maxTasks = 12;
        private int maxPreviousInsights = 3;
    }
}
