package com.aiworkforce.ai.client;

import com.aiworkforce.ai.config.AiProperties;
import com.aiworkforce.core.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class OllamaClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateInsight_ReturnsGeminiResponse_WhenApiKeyValid() throws Exception {
        // Simulate a Gemini API server that returns a valid response
        startGeminiServer(200, """
                {"candidates":[{"content":{"parts":[{"text":"{\\"status_evaluation\\":\\"OK\\",\\"primary_reason\\":\\"Stable\\",\\"recommendations\\":[\\"Keep pace\\"]}"}]}}]}
                """);
        OllamaClient client = buildGeminiClient(geminiBaseUrl(), "test-api-key", "gemini-1.5-flash", 15);

        String response = client.generateInsight("risk LOW");

        JsonNode json = OBJECT_MAPPER.readTree(response);
        assertEquals("OK", json.get("status_evaluation").asText());
    }

    @Test
    void generateInsight_ThrowsException_WhenGeminiApiKeyMissing() {
        OllamaClient client = buildGeminiClient(
                "https://generativelanguage.googleapis.com", "", "gemini-1.5-flash", 5
        );

        AiServiceException exception = assertThrows(AiServiceException.class, () ->
                client.generateInsight("risk HIGH")
        );

        assertTrue(exception.getMessage().contains("API key chưa được cấu hình"));
    }

    @Test
    void generateInsight_ThrowsException_WhenGeminiReturnsError() throws Exception {
        // Simulate Gemini returning 401
        startGeminiServer(401, """
                {"error":{"code":401,"message":"API key not valid","status":"UNAUTHENTICATED"}}
                """);
        OllamaClient client = buildGeminiClient(geminiBaseUrl(), "invalid-key", "gemini-1.5-flash", 15);

        AiServiceException exception = assertThrows(AiServiceException.class, () ->
                client.generateInsight("risk HIGH")
        );

        assertTrue(exception.getMessage().contains("API key không hợp lệ")
                || exception.getMessage().contains("Gemini API"));
    }

    @Test
    void generateInsight_ThrowsException_WhenGeminiConnectionFails() {
        // Point to a non-existent server
        OllamaClient client = buildGeminiClient("http://127.0.0.1:1", "some-key", "gemini-1.5-flash", 2);

        AiServiceException exception = assertThrows(AiServiceException.class, () ->
                client.generateInsight("risk MEDIUM")
        );

        assertTrue(exception.getMessage().contains("Không thể kết nối"));
    }

    @Test
    void generateInsight_ThrowsException_WhenPromptIsNull() {
        OllamaClient client = buildGeminiClient(
                "https://generativelanguage.googleapis.com", "key", "gemini-1.5-flash", 5
        );

        assertThrows(IllegalArgumentException.class, () ->
                client.generateInsight(null)
        );
    }

    @Test
    void generateInsight_ThrowsException_WhenPromptIsBlank() {
        OllamaClient client = buildGeminiClient(
                "https://generativelanguage.googleapis.com", "key", "gemini-1.5-flash", 5
        );

        assertThrows(IllegalArgumentException.class, () ->
                client.generateInsight("   ")
        );
    }

    @Test
    void generateInsight_ThrowsException_WhenOllamaUnavailable() {
        // Test Ollama provider (deprecated) - should throw instead of returning fallback
        OllamaClient client = buildOllamaClient("http://127.0.0.1:1", "gemma:2b", 1);

        AiServiceException exception = assertThrows(AiServiceException.class, () ->
                client.generateInsight("risk HIGH")
        );

        assertTrue(exception.getMessage().contains("Không thể kết nối") || exception.getMessage().contains("Ollama"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private OllamaClient buildGeminiClient(String geminiBaseUrl, String apiKey, String model, int timeout) {
        WebClient ollamaWc = WebClient.builder().baseUrl("http://localhost:11434").build();
        WebClient geminiWc = WebClient.builder().baseUrl(geminiBaseUrl).build();

        AiProperties props = new AiProperties();
        props.setProvider("gemini");

        AiProperties.Gemini geminiCfg = new AiProperties.Gemini();
        geminiCfg.setApiKey(apiKey);
        geminiCfg.setModel(model);
        geminiCfg.setTimeoutSeconds(timeout);
        props.setGemini(geminiCfg);

        return new OllamaClient(ollamaWc, geminiWc, props);
    }

    private OllamaClient buildOllamaClient(String ollamaBaseUrl, String model, int timeout) {
        WebClient ollamaWc = WebClient.builder().baseUrl(ollamaBaseUrl).build();
        WebClient geminiWc = WebClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();

        AiProperties props = new AiProperties();
        props.setProvider("ollama");

        AiProperties.Ollama ollamaCfg = new AiProperties.Ollama();
        ollamaCfg.setBaseUrl(ollamaBaseUrl);
        ollamaCfg.setModel(model);
        ollamaCfg.setTimeoutSeconds(timeout);
        props.setOllama(ollamaCfg);

        return new OllamaClient(ollamaWc, geminiWc, props);
    }

    private void startGeminiServer(int statusCode, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    private String geminiBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
