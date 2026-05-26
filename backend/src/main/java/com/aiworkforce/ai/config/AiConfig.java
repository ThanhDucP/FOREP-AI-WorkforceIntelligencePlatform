package com.aiworkforce.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for AI provider clients.
 * Registers two named {@link WebClient} beans:
 * <ul>
 *   <li>{@code ollamaWebClient} – points at the Ollama base URL (local/dev)</li>
 *   <li>{@code geminiWebClient} – points at the Google Generative Language API (prod)</li>
 * </ul>
 * The bean names must match the parameter names in {@link com.aiworkforce.ai.client.OllamaClient}
 * constructor so that Spring resolves them by name when there are multiple WebClient beans.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /**
     * WebClient pre-configured with the Ollama base URL from application config.
     */
    @Bean
    public WebClient ollamaWebClient(AiProperties props) {
        return WebClient.builder()
                .baseUrl(props.getOllama().getBaseUrl())
                .build();
    }

    /**
     * WebClient pre-configured with the Gemini API base URL.
     * The API key is appended per-request as a query parameter.
     */
    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }
}
