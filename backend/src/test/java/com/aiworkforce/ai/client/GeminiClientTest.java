package com.aiworkforce.ai.client;

import com.aiworkforce.core.exception.AiServiceException;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeminiClientTest {

    @Test
    void generateInsight_ReturnsLangChain4jResponse_WhenModelReturnsText() {
        GeminiClient client = new GeminiClient(new FakeChatModel("""
                {"status_evaluation":"OK","primary_reason":"Stable","recommendations":["Keep pace"]}
                """));

        String response = client.generateInsight("risk LOW");

        assertTrue(response.contains("\"status_evaluation\":\"OK\""));
    }

    @Test
    void generateInsight_ThrowsException_WhenModelReturnsBlankResponse() {
        GeminiClient client = new GeminiClient(new FakeChatModel("   "));

        AiServiceException exception = assertThrows(AiServiceException.class, () ->
                client.generateInsight("risk HIGH")
        );

        assertEquals("Gemini returned an empty response.", exception.getMessage());
    }

    @Test
    void generateInsight_ThrowsException_WhenLangChain4jModelFails() {
        GeminiClient client = new GeminiClient(new ChatModel() {
            @Override
            public String chat(String prompt) {
                throw new RuntimeException("quota exceeded");
            }
        });

        AiServiceException exception = assertThrows(AiServiceException.class, () ->
                client.generateInsight("risk HIGH")
        );

        assertTrue(exception.getMessage().contains("Gemini call through LangChain4j failed"));
        assertTrue(exception.getMessage().contains("quota exceeded"));
    }

    @Test
    void generateInsight_ThrowsException_WhenPromptIsNull() {
        GeminiClient client = new GeminiClient(new FakeChatModel("ok"));

        assertThrows(IllegalArgumentException.class, () ->
                client.generateInsight(null)
        );
    }

    @Test
    void generateInsight_ThrowsException_WhenPromptIsBlank() {
        GeminiClient client = new GeminiClient(new FakeChatModel("ok"));

        assertThrows(IllegalArgumentException.class, () ->
                client.generateInsight("   ")
        );
    }

    private record FakeChatModel(String response) implements ChatModel {
        @Override
        public String chat(String prompt) {
            return response;
        }
    }
}
