package com.aiworkforce.ai.config;

import com.aiworkforce.core.exception.AiServiceException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    public ChatModel geminiChatModel(AiProperties props) {
        if (!"gemini".equalsIgnoreCase(props.getProvider())) {
            return new ChatModel() {
                @Override
                public String chat(String prompt) {
                    throw new AiServiceException("AI provider '" + props.getProvider() + "' is configured but only Gemini adapter is installed in this build.");
                }
            };
        }

        AiProperties.Gemini gemini = props.getGemini();
        if (gemini.getApiKey() == null || gemini.getApiKey().isBlank()) {
            return new ChatModel() {
                @Override
                public String chat(String prompt) {
                    throw new AiServiceException("Gemini API key is not configured. Set ai.gemini.api-key or GEMINI_API_KEY before using AI insight generation.");
                }
            };
        }

        return GoogleAiGeminiChatModel.builder()
                .apiKey(gemini.getApiKey())
                .modelName(resolveModel(props))
                .timeout(Duration.ofSeconds(gemini.getTimeoutSeconds()))
                .build();
    }
    private String resolveModel(AiProperties props) {
        if (props.getModel() != null && !props.getModel().isBlank()) {
            return props.getModel();
        }
        return props.getGemini().getModel();
    }
}