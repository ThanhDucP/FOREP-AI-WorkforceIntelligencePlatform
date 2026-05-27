package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GithubWebhookProcessorTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private ObjectMapper objectMapper;

    @InjectMocks
    private GithubWebhookProcessor processor;

    private TaskIntegrationConfig config;
    private final String secret = "my-secret";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new GithubWebhookProcessor(objectMapper, taskRepository, employeeRepository);
        
        config = new TaskIntegrationConfig();
        config.setWebhookSecret(secret);
    }

    @Test
    void testProcessPayload_ValidSignature_CreatesNewTask() throws Exception {
        String payload = """
                {
                  "action": "opened",
                  "issue": {
                    "number": 123,
                    "title": "Fix bug",
                    "body": "There is a bug",
                    "html_url": "https://github.com/org/repo/issues/123",
                    "state": "open",
                    "assignee": {
                      "login": "dev1",
                      "email": "dev1@example.com"
                    }
                  }
                }
                """;
        
        String signature = "sha256=" + calculateHmac(payload, secret);

        Employee mockEmployee = new Employee();
        // Since test doesn't need to know the inner mapping, we just mock the repository method.
        when(employeeRepository.findByAccountEmail("dev1@example.com")).thenReturn(Optional.of(mockEmployee));
        
        when(taskRepository.findByExternalTicketRefAndSourceProvider("GH-123", IntegrationProvider.GITHUB))
                .thenReturn(Optional.empty());

        processor.processPayload(payload, signature, config);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());

        Task savedTask = taskCaptor.getValue();
        assertEquals("GH-123", savedTask.getExternalTicketRef());
        assertEquals("Fix bug", savedTask.getTitle());
        assertEquals("There is a bug", savedTask.getDescription());
        assertEquals("https://github.com/org/repo/issues/123", savedTask.getExternalUrl());
        assertEquals(IntegrationProvider.GITHUB, savedTask.getSourceProvider());
        assertEquals(mockEmployee, savedTask.getAssignee());
    }

    @Test
    void testProcessPayload_InvalidSignature_ThrowsException() {
        String payload = "{}";
        String invalidSignature = "sha256=invalidhash";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> 
                processor.processPayload(payload, invalidSignature, config)
        );
        
        assertEquals("Invalid webhook signature", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }

    private String calculateHmac(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
