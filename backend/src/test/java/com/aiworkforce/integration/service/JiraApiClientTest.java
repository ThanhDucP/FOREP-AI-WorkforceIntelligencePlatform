package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.service.TeamMembershipService;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
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
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JiraApiClientTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TeamMembershipService membershipService;

    private ObjectMapper objectMapper;
    private JiraApiClient jiraApiClient;
    private HttpServer server;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jiraApiClient = new JiraApiClient(taskRepository, employeeRepository, objectMapper, new TaskAssessmentService(), membershipService);
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
                {
                  "issues": [
                    {
                      "key": "PROJ-123",
                      "self": "https://test.atlassian.net/rest/api/2/issue/123",
                      "fields": {
                        "summary": "Fix JIRA",
                        "description": "API issues in Jira",
                        "status": {
                          "name": "In Progress"
                        }
                      }
                    }
                  ]
                }
                """;

        startJiraServer(responseBody);

        // Configure baseUrl of Jira API to our mock server
        String mockBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        ReflectionTestUtils.setField(jiraApiClient, "jiraApiUrlOverride", mockBaseUrl);

        TaskIntegrationConfig config = new TaskIntegrationConfig();
        config.setProjectKey("test.atlassian.net/PROJ");
        config.setAccessToken("user@example.com:test-token");
        config.setTeam(new Team());

        when(taskRepository.findByExternalTicketRefAndSourceProvider("PROJ-123", IntegrationProvider.JIRA))
                .thenReturn(Optional.empty());

        jiraApiClient.syncIssues(config);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());

        Task savedTask = taskCaptor.getValue();
        assertTrue(authorizationHeader.startsWith("Basic "));
        assertEquals(
                "user@example.com:test-token",
                new String(Base64.getDecoder().decode(authorizationHeader.substring("Basic ".length())), StandardCharsets.UTF_8)
        );
        assertEquals("PROJ-123", savedTask.getExternalTicketRef());
        assertEquals("Fix JIRA", savedTask.getTitle());
        assertEquals("API issues in Jira", savedTask.getDescription());
        assertEquals("https://test.atlassian.net/browse/PROJ-123", savedTask.getExternalUrl());
        assertEquals(IntegrationProvider.JIRA, savedTask.getSourceProvider());
        assertEquals(TaskStatus.IN_PROGRESS, savedTask.getStatus());
    }

    private void startJiraServer(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rest/api/3/search/jql", exchange -> {
            authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
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
