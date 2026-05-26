package com.aiworkforce.ai.client;

import com.aiworkforce.ai.config.AiProperties;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void generateInsight_ReturnsOllamaResponse_WhenModelExists() throws Exception {
        startServer(
                "{\"models\":[{\"name\":\"gemma:2b\",\"model\":\"gemma:2b\"}]}",
                "{\"model\":\"gemma:2b\",\"response\":\"{\\\"status_evaluation\\\":\\\"OK\\\",\\\"primary_reason\\\":\\\"Stable\\\",\\\"recommendations\\\":[\\\"Keep pace\\\"]}\",\"done\":true}"
        );
        OllamaClient client = buildClient(baseUrl(), "gemma:2b", 15, "ollama", "", "gemini-1.5-flash");

        String response = client.generateInsight("risk LOW");

        JsonNode json = OBJECT_MAPPER.readTree(response);
        assertEquals("OK", json.get("status_evaluation").asText());
    }

    @Test
    void generateInsight_ReturnsStructuredFallback_WhenModelIsMissing() throws Exception {
        startServer("{\"models\":[{\"name\":\"llama3\",\"model\":\"llama3\"}]}", "{}");
        OllamaClient client = buildClient(baseUrl(), "gemma:2b", 15, "ollama", "", "gemini-1.5-flash");

        String response = client.generateInsight("risk HIGH");

        JsonNode json = OBJECT_MAPPER.readTree(response);
        assertTrue(json.has("status_evaluation"));
        assertTrue(json.has("primary_reason"));
        assertTrue(json.has("recommendations"));
        assertTrue(json.get("status_evaluation").asText().contains("quá tải"));
    }

    @Test
    void generateInsight_ReturnsStructuredFallback_WhenOllamaIsUnavailable() throws Exception {
        OllamaClient client = buildClient("http://127.0.0.1:1", "gemma:2b", 1, "ollama", "", "gemini-1.5-flash");

        String response = client.generateInsight("risk MEDIUM");

        JsonNode json = OBJECT_MAPPER.readTree(response);
        assertTrue(json.has("status_evaluation"));
        assertTrue(json.has("primary_reason"));
        assertTrue(json.has("recommendations"));
    }

    @Test
    void generateInsight_ReturnsStructuredFallback_WhenGeminiFails() throws Exception {
        OllamaClient client = buildClient(
                "http://localhost:11434", "gemma:2b", 2, "gemini", "invalid-key", "gemini-1.5-flash"
        );

        String response = client.generateInsight("risk HIGH");

        JsonNode json = OBJECT_MAPPER.readTree(response);
        assertTrue(json.has("status_evaluation"));
        assertTrue(json.has("primary_reason"));
        assertTrue(json.has("recommendations"));
        assertTrue(json.get("status_evaluation").asText().contains("quá tải"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds an OllamaClient using the new constructor (WebClient, WebClient, AiProperties).
     */
    private OllamaClient buildClient(String ollamaBaseUrl, String model, int timeoutSeconds,
                                     String provider, String geminiApiKey, String geminiModel) {
        WebClient ollamaWc = WebClient.builder().baseUrl(ollamaBaseUrl).build();
        WebClient geminiWc = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        AiProperties props = new AiProperties();
        props.setProvider(provider);

        AiProperties.Ollama ollamaCfg = new AiProperties.Ollama();
        ollamaCfg.setBaseUrl(ollamaBaseUrl);
        ollamaCfg.setModel(model);
        ollamaCfg.setTimeoutSeconds(timeoutSeconds);
        props.setOllama(ollamaCfg);

        AiProperties.Gemini geminiCfg = new AiProperties.Gemini();
        geminiCfg.setApiKey(geminiApiKey);
        geminiCfg.setModel(geminiModel);
        geminiCfg.setTimeoutSeconds(timeoutSeconds);
        props.setGemini(geminiCfg);

        return new OllamaClient(ollamaWc, geminiWc, props);
    }

    private void startServer(String tagsResponse, String generateResponse) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/tags", exchange -> writeJson(exchange, 200, tagsResponse));
        server.createContext("/api/generate", exchange -> writeJson(exchange, 200, generateResponse));
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void writeJson(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
