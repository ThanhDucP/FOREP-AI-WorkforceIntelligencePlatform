package com.aiworkforce.ai.config;

import com.aiworkforce.core.exception.AiServiceException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Spring configuration for the LangChain4j Gemini chat model.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /**
     * ChatModel pre-configured for Google Gemini through LangChain4j.
     * If the key is absent, keep application startup healthy and fail only when AI is called.
     */
    @Bean
    public ChatModel geminiChatModel(AiProperties props) {
        AiProperties.Gemini gemini = props.getGemini();
        if (gemini.getApiKey() == null || gemini.getApiKey().isBlank()) {
            return new ChatModel() {
                @Override
                public String chat(String prompt) {
                    throw new AiServiceException(
                            "Gemini API key is not configured. Set GEMINI_API_KEY before using AI insight generation."
                    );
                }
            };
        }

        return GoogleAiGeminiChatModel.builder()
                .apiKey(gemini.getApiKey())
                .modelName(gemini.getModel())
                .timeout(Duration.ofSeconds(gemini.getTimeoutSeconds()))
                .build();
    }
}
