package com.aiworkforce.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubWebhookRegistrar {

    private String githubApiUrl = "https://api.github.com";

    public static class RegistrationResult {
        public final boolean registered;
        public final String webhookId;
        public final String message;

        public RegistrationResult(boolean registered, String webhookId, String message) {
            this.registered = registered;
            this.webhookId = webhookId;
            this.message = message;
        }
    }

    public RegistrationResult createRepoWebhook(String ownerRepo, String token, String payloadUrl, String secret) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(githubApiUrl)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .defaultHeader("User-Agent", "FOREP-AI-Platform")
                    .defaultHeader("Accept", "application/vnd.github+json")
                    .build();

            Map<String, Object> config = new HashMap<>();
            config.put("url", payloadUrl);
            config.put("content_type", "json");
            config.put("secret", secret);

            Map<String, Object> body = new HashMap<>();
            body.put("name", "web");
            body.put("config", config);
            body.put("events", new String[]{"issues"});
            body.put("active", true);

            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/repos/" + ownerRepo + "/hooks").build())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) return new RegistrationResult(false, null, "empty response");

            // Try to extract id field simply
            String idMarker = "\"id\":";
            int idx = response.indexOf(idMarker);
            if (idx >= 0) {
                int start = idx + idMarker.length();
                int end = response.indexOf(',', start);
                if (end == -1) end = response.indexOf('}', start);
                String id = response.substring(start, end).trim();
                return new RegistrationResult(true, id, "created");
            }

            return new RegistrationResult(true, null, "created but id not found in response");

        } catch (WebClientResponseException e) {
            log.warn("GitHub webhook registration failed: {} {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            String msg = "GitHub registration failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
            return new RegistrationResult(false, null, msg);
        } catch (Exception e) {
            log.error("Error registering GitHub webhook", e);
            return new RegistrationResult(false, null, e.getMessage());
        }
    }
}
