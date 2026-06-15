package com.aiworkforce.ai.client;

import com.aiworkforce.core.exception.AiServiceException;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Gemini-only LangChain4j client for generating workforce insights.
 *
 * This client never returns mocked or fallback LLM output. Connection failures,
 * invalid credentials, HTTP errors, empty responses, and malformed prompts are
 * surfaced as exceptions so callers can return a real error to API consumers.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GeminiClient {

    private final ChatModel geminiChatModel;

    public String generateInsight(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank.");
        }

        log.info("Calling Gemini through LangChain4j | prompt length: {}", prompt.length());
        try {
            String response = geminiChatModel.chat(prompt);
            if (response != null && !response.isBlank()) {
                log.info("Gemini returned a non-empty response through LangChain4j");
                return response;
            }

            throw new AiServiceException("Gemini returned an empty response.");
        } catch (AiServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("LangChain4j Gemini call failed: {}", e.getMessage());
            throw new AiServiceException(
                    "Gemini call through LangChain4j failed. Check GEMINI_API_KEY, network, model, quota, and prompt. Details: "
                            + e.getMessage(),
                    e
            );
        }
    }
}
