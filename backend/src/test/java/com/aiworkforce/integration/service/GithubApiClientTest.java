package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.service.TokenProtectionService;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.service.TeamMembershipService;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.integration.repository.GithubCommitRepository;
import com.aiworkforce.integration.repository.GithubContributorRepository;
import com.aiworkforce.integration.repository.GithubPullRequestRepository;
import com.aiworkforce.integration.repository.GithubRepositorySnapshotRepository;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import com.aiworkforce.task.service.TaskAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private ObjectMapper objectMapper;
    private GithubApiClient githubApiClient;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(tokenProtectionService.unprotect(any())).thenAnswer(invocation -> invocation.getArgument(0));
        githubApiClient = new GithubApiClient(taskRepository, employeeRepository, objectMapper, new TaskAssessmentService(), membershipService, repositorySnapshotRepository, contributorRepository, pullRequestRepository, commitRepository, tokenProtectionService);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void syncIssues_Success_SavesTasks() throws Exception {
        String responseBody = """
                [
                  {
                    "number": 42,
                    "title": "API issue",
                    "body": "Fix API integration",
                    "html_url": "https://github.com/test/repo/issues/42",
                    "state": "open"
                  }
                ]
                """;

        startGithubServer(responseBody);

        // Configure baseUrl of GitHub API to our mock server
        String mockBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        ReflectionTestUtils.setField(githubApiClient, "githubApiUrl", mockBaseUrl);

        TaskIntegrationConfig config = new TaskIntegrationConfig();
        config.setProjectKey("test/repo");
        config.setAccessToken("test-token");
        config.setTeam(new Team());

        when(taskRepository.findByExternalTicketRefAndSourceProvider("GH-42", IntegrationProvider.GITHUB))
                .thenReturn(Optional.empty());

        githubApiClient.syncIssues(config);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());

        Task savedTask = taskCaptor.getValue();
        assertEquals("GH-42", savedTask.getExternalTicketRef());
        assertEquals("API issue", savedTask.getTitle());
        assertEquals("Fix API integration", savedTask.getDescription());
        assertEquals("https://github.com/test/repo/issues/42", savedTask.getExternalUrl());
        assertEquals(IntegrationProvider.GITHUB, savedTask.getSourceProvider());
        assertEquals(TaskStatus.TODO, savedTask.getStatus());
    }

    private void startGithubServer(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/test/repo/issues", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }
}

