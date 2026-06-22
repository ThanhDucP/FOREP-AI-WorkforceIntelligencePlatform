package com.aiworkforce.integration.service;

import com.aiworkforce.integration.dto.GithubRepositoryDiscoveryRequest;
import com.aiworkforce.integration.dto.GithubRepositoryOptionResponse;
import com.aiworkforce.integration.dto.JiraProjectDiscoveryRequest;
import com.aiworkforce.integration.dto.JiraProjectOptionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegrationDiscoveryService {

    private final ObjectMapper objectMapper;

    public List<JiraProjectOptionResponse> discoverJiraProjects(JiraProjectDiscoveryRequest request) throws Exception {
        String domain = normalizeJiraDomain(request.getJiraDomain());
        WebClient webClient = WebClient.builder()
                .baseUrl("https://" + domain)
                .defaultHeader("Authorization", buildJiraAuthorizationHeader(request.getConnectionKey()))
                .defaultHeader("Accept", "application/json")
                .build();

        String responseJson = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/rest/api/3/project/search").queryParam("maxResults", 100).build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode values = objectMapper.readTree(responseJson).path("values");
        List<JiraProjectOptionResponse> projects = new ArrayList<>();
        if (!values.isArray()) return projects;
        for (JsonNode project : values) {
            projects.add(JiraProjectOptionResponse.builder()
                    .providerProjectId(project.path("id").asText(null))
                    .projectKey(project.path("key").asText(null))
                    .name(project.path("name").asText(null))
                    .projectTypeKey(project.path("projectTypeKey").asText(null))
                    .leadDisplayName(project.path("lead").path("displayName").asText(null))
                    .selfUrl(project.path("self").asText(null))
                    .build());
        }
        return projects;
    }

    public List<GithubRepositoryOptionResponse> discoverGithubRepositories(GithubRepositoryDiscoveryRequest request) throws Exception {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + request.getConnectionKey())
                .defaultHeader("User-Agent", "FOREP-AI-Platform")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();

        String responseJson = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/user/repos")
                        .queryParam("per_page", 100)
                        .queryParam("sort", "updated")
                        .queryParam("direction", "desc")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode repositories = objectMapper.readTree(responseJson);
        List<GithubRepositoryOptionResponse> options = new ArrayList<>();
        if (!repositories.isArray()) return options;
        for (JsonNode repository : repositories) {
            boolean isPrivate = repository.path("private").asBoolean(false);
            options.add(GithubRepositoryOptionResponse.builder()
                    .providerRepositoryId(repository.path("id").isNumber() ? repository.path("id").asLong() : null)
                    .fullName(repository.path("full_name").asText(null))
                    .name(repository.path("name").asText(null))
                    .ownerLogin(repository.path("owner").path("login").asText(null))
                    .visibility(isPrivate ? "private" : "public")
                    .mainLanguage(repository.path("language").asText(null))
                    .htmlUrl(repository.path("html_url").asText(null))
                    .privateRepository(isPrivate)
                    .pushedAt(parseGithubDate(repository.path("pushed_at").asText(null)))
                    .providerUpdatedAt(parseGithubDate(repository.path("updated_at").asText(null)))
                    .build());
        }
        return options;
    }

    private String normalizeJiraDomain(String domain) {
        return domain.replace("https://", "").replace("http://", "").replaceAll("/+$", "");
    }

    private String buildJiraAuthorizationHeader(String accessToken) {
        if (accessToken.contains(":")) {
            String encoded = Base64.getEncoder().encodeToString(accessToken.getBytes(StandardCharsets.UTF_8));
            return "Basic " + encoded;
        }
        return "Bearer " + accessToken;
    }

    private LocalDateTime parseGithubDate(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) return null;
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}