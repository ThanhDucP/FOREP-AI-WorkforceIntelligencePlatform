package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import com.aiworkforce.task.service.TaskAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JiraWebhookProcessorTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private JiraWebhookProcessor processor;
    private TaskIntegrationConfig config;
    private Project project;
    private Team team;

    @BeforeEach
    void setUp() {
        processor = new JiraWebhookProcessor(
                new ObjectMapper(), taskRepository, employeeRepository, new TaskAssessmentService());

        team = new Team();
        team.setId(UUID.randomUUID());
        project = new Project();
        project.setId(UUID.randomUUID());
        project.setTeam(team);

        config = new TaskIntegrationConfig();
        config.setTeam(team);
        config.setProject(project);
    }

    @Test
    void processPayload_CreatesProjectTask() {
        String payload = """
                {
                  "webhookEvent": "jira:issue_created",
                  "issue": {
                    "key": "PROJ-321",
                    "self": "https://test.atlassian.net/rest/api/3/issue/321",
                    "fields": {
                      "summary": "Fix Jira import",
                      "description": "Webhook-created issue",
                      "status": {
                        "name": "To Do"
                      }
                    }
                  }
                }
                """;

        when(taskRepository.findByExternalTicketRefAndSourceProviderAndProjectId(
                "PROJ-321", IntegrationProvider.JIRA, project.getId()))
                .thenReturn(Optional.empty());

        processor.processPayload(payload, null, config);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());

        Task savedTask = taskCaptor.getValue();
        assertEquals("PROJ-321", savedTask.getExternalTicketRef());
        assertEquals("Fix Jira import", savedTask.getTitle());
        assertEquals(IntegrationProvider.JIRA, savedTask.getSourceProvider());
        assertEquals(team, savedTask.getTeam());
        assertEquals(project, savedTask.getProject());
    }
}
