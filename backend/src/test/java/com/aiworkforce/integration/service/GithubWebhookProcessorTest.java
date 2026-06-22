package com.aiworkforce.integration.service;

import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.task.repository.TaskRepository;
import com.aiworkforce.task.service.TaskAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GithubWebhookProcessorTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private GithubWebhookProcessor processor;
    private TaskIntegrationConfig config;
    private final String secret = "my-secret";

    @BeforeEach
    void setUp() {
        processor = new GithubWebhookProcessor(new ObjectMapper(), taskRepository, employeeRepository, new TaskAssessmentService());
        config = new TaskIntegrationConfig();
        config.setWebhookSecret(secret);
    }

    @Test
    void processPayload_ValidIssueWebhook_DoesNotCreateTask() throws Exception {
        String payload = """
                {
                  "action": "opened",
                  "issue": {
                    "number": 123,
                    "title": "Fix bug",
                    "body": "There is a bug",
                    "html_url": "https://github.com/org/repo/issues/123",
                    "state": "open"
                  }
                }
                """;
        String signature = "sha256=" + hmac(payload, secret);

        processor.processPayload(payload, signature, config);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void processPayload_InvalidSignature_Throws() {
        String payload = "{\"issue\":{\"number\":123}}";
        assertThrows(IllegalArgumentException.class, () -> processor.processPayload(payload, "sha256=bad", config));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void processPayload_NonIssueWebhook_Ignores() throws Exception {
        String payload = "{\"repository\":{\"full_name\":\"org/repo\"}}";
        String signature = "sha256=" + hmac(payload, secret);

        processor.processPayload(payload, signature, config);

        verify(taskRepository, never()).save(any());
    }

    private String hmac(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        sha256Hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String value = Integer.toHexString(0xff & b);
            if (value.length() == 1) hex.append('0');
            hex.append(value);
        }
        return hex.toString();
    }
}