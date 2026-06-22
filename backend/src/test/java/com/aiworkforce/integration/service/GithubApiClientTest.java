package com.aiworkforce.integration.service;

import com.aiworkforce.core.service.TokenProtectionService;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.service.TeamMembershipService;
import com.aiworkforce.integration.dto.SyncResult;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.integration.repository.ExternalIdentityRepository;
import com.aiworkforce.integration.repository.GithubCommitRepository;
import com.aiworkforce.integration.repository.GithubContributorRepository;
import com.aiworkforce.integration.repository.GithubPullRequestRepository;
import com.aiworkforce.integration.repository.GithubRepositorySnapshotRepository;
import com.aiworkforce.task.repository.TaskRepository;
import com.aiworkforce.task.service.TaskAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GithubApiClientTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TeamMembershipService membershipService;

    @Mock
    private GithubRepositorySnapshotRepository repositorySnapshotRepository;

    @Mock
    private GithubContributorRepository contributorRepository;

    @Mock
    private GithubPullRequestRepository pullRequestRepository;

    @Mock
    private GithubCommitRepository commitRepository;

    @Mock
    private TokenProtectionService tokenProtectionService;

    @Mock
    private ExternalIdentityRepository externalIdentityRepository;

    private GithubApiClient githubApiClient;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        lenient().when(tokenProtectionService.unprotect(any())).thenAnswer(invocation -> invocation.getArgument(0));
        githubApiClient = new GithubApiClient(taskRepository, employeeRepository, objectMapper, new TaskAssessmentService(), membershipService, repositorySnapshotRepository, contributorRepository, pullRequestRepository, commitRepository, tokenProtectionService, externalIdentityRepository);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void syncIssues_DoesNotCreateLocalTasksFromGithubIssues() throws Exception {
        startGithubServer();

        String mockBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        ReflectionTestUtils.setField(githubApiClient, "githubApiUrl", mockBaseUrl);

        TaskIntegrationConfig config = new TaskIntegrationConfig();
        config.setProjectKey("test/repo");
        config.setAccessToken("test-token");
        config.setTeam(new Team());

        SyncResult result = githubApiClient.syncIssues(config);

        assertEquals(0, result.getTotalCreated());
        assertEquals(0, result.getTotalUpdated());
        verify(taskRepository, never()).save(any());
    }

    private void startGithubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/test/repo/issues", exchange -> writeJson(exchange, "[]"));
        server.createContext("/repos/test/repo/pulls", exchange -> writeJson(exchange, "[]"));
        server.createContext("/repos/test/repo/commits", exchange -> writeJson(exchange, "[]"));
        server.createContext("/repos/test/repo/contributors", exchange -> writeJson(exchange, "[]"));
        server.createContext("/repos/test/repo", exchange -> writeJson(exchange, "{\"full_name\":\"test/repo\",\"name\":\"repo\",\"owner\":{\"login\":\"test\"}}"));
        server.start();
    }

    private void writeJson(com.sun.net.httpserver.HttpExchange exchange, String responseBody) throws IOException {
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}