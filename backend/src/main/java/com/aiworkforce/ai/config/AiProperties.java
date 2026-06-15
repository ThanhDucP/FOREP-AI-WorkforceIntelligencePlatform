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

    @Data
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-1.5-flash";
        private int timeoutSeconds = 30;
    }
}
