package com.aiworkforce.ai.config;

import com.aiworkforce.core.exception.AiServiceException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AiConfigTest {

    @Test
    void geminiChatModel_ReturnsFailingModel_WhenApiKeyMissing() {
        AiProperties props = new AiProperties();
        props.getGemini().setApiKey("");

        ChatModel model = new AiConfig().geminiChatModel(props);

        assertThrows(AiServiceException.class, () -> model.chat("hello"));
    }

    @Test
    void geminiChatModel_ReturnsLangChain4jGeminiModel_WhenApiKeyPresent() {
        AiProperties props = new AiProperties();
        props.getGemini().setApiKey("test-api-key");
        props.getGemini().setModel("gemini-1.5-flash");
        props.getGemini().setTimeoutSeconds(5);

        ChatModel model = new AiConfig().geminiChatModel(props);

        assertInstanceOf(GoogleAiGeminiChatModel.class, model);
    }
}
