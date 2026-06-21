package com.aiworkforce.ai.service;

import com.aiworkforce.ai.client.GeminiClient;
import com.aiworkforce.ai.config.AiProperties;
import com.aiworkforce.ai.dto.AIInsightResponse;
import com.aiworkforce.ai.dto.AiRuntimeStatusResponse;
import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.prompt.PromptBuilder;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.analytics.dto.DashboardResponse;
import com.aiworkforce.analytics.service.DashboardAnalyticsService;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.InsightType;
import com.aiworkforce.core.enums.NotificationType;
import com.aiworkforce.core.exception.AiServiceException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.core.service.NotificationService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.ProjectRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIInsightService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GeminiClient geminiClient;
    private final AiProperties aiProperties;
    private final PromptBuilder promptBuilder;
    private final RagContextService ragContextService;
    private final DashboardAnalyticsService analyticsService;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeService employeeService;
    private final AIInsightRepository insightRepository;
    private final NotificationService notificationService;
    private final AccessPolicyService accessPolicyService;

    @Transactional
    public AIInsight generateInsightForEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        DashboardResponse analytics = analyticsService.getEmployeeDashboard(employeeId);
        String ragContext = ragContextService.buildEmployeeContext(employee);
        String prompt = promptBuilder.buildBurnoutPrompt(fullName(employee), analytics, ragContext);

        String aiResponse = geminiClient.generateInsight(prompt);
        ObjectNode normalizedInsight = normalizeInsightJson(aiResponse);

        AIInsight insight = new AIInsight();
        insight.setEmployee(employee);
        insight.setTeam(employee.getTeam());
        insight.setProject(resolveEmployeeProject(employee));
        insight.setInsightType(InsightType.BURNOUT_WARNING);
        insight.setSummary(normalizedInsight.get("summary").asText());
        insight.setFullAnalysis(normalizedInsight.toPrettyString());
        insight.setSeverity(resolveSeverity(normalizedInsight.get("riskLevel").asText()));
        insight.setConfidenceScore(resolveConfidenceScore(insight.getSeverity()));

        AIInsight savedInsight = insightRepository.save(insight);
        notifyHighSeverityInsight(savedInsight);
        return savedInsight;
    }

    @Transactional
    public AIInsightResponse generateInsightResponseForEmployee(UUID employeeId) {
        return mapToResponse(generateInsightForEmployee(employeeId));
    }

    public AiRuntimeStatusResponse getRuntimeStatus() {
        return AiRuntimeStatusResponse.builder()
                .provider(aiProperties.getProvider())
                .model(aiProperties.getModel())
                .apiKeyConfigured(isConfiguredProviderApiKeyPresent())
                .ragEnabled(aiProperties.getRag().isEnabled())
                .ragMaxContextCharacters(aiProperties.getRag().getMaxContextCharacters())
                .ragMaxTasks(aiProperties.getRag().getMaxTasks())
                .ragMaxPreviousInsights(aiProperties.getRag().getMaxPreviousInsights())
                .build();
    }

    private ObjectNode normalizeInsightJson(String response) {
        String jsonCandidate = extractJsonObject(cleanJsonResponse(response));
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonCandidate);
            if (!rootNode.isObject()) {
                throw new AiServiceException("AI response is not a JSON object.");
            }
            ObjectNode normalized = (ObjectNode) rootNode;
            validateRequiredInsightSchema(normalized);
            return normalized;
        } catch (Exception e) {
            if (e instanceof AiServiceException aiServiceException) {
                throw aiServiceException;
            }
            log.error("AI response is not valid JSON: {}", e.getMessage());
            throw new AiServiceException("AI response is not valid JSON. Raw response cannot be used as an AI insight.", e);
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String extractJsonObject(String response) {
        if (response == null || response.isBlank()) return "{}";
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1).trim();
        }
        return response.trim();
    }

    private void validateRequiredInsightSchema(ObjectNode node) {
        validateRequiredTextField(node, "riskLevel");
        validateAllowedRiskLevel(node.get("riskLevel").asText());
        validateRequiredTextField(node, "summary");
        validateRequiredArrayField(node, "reasons");
        validateRequiredArrayField(node, "recommendations");
    }

    private void validateAllowedRiskLevel(String riskLevel) {
        String normalized = riskLevel == null ? "" : riskLevel.trim().toUpperCase();
        if (!normalized.equals("LOW") && !normalized.equals("MEDIUM") && !normalized.equals("HIGH")) {
            throw new AiServiceException("AI response riskLevel must be one of LOW, MEDIUM, HIGH.");
        }
    }

    private void validateRequiredTextField(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new AiServiceException("AI response is missing required text field: " + fieldName + ".");
        }
    }

    private void validateRequiredArrayField(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isArray() || value.size() == 0) {
            throw new AiServiceException("AI response is missing required non-empty array field: " + fieldName + ".");
        }
    }

    private InsightSeverity resolveSeverity(String riskLevel) {
        return InsightSeverity.valueOf(riskLevel.trim().toUpperCase());
    }

    private Double resolveConfidenceScore(InsightSeverity severity) {
        return switch (severity) {
            case HIGH, CRITICAL -> 0.82;
            case MEDIUM -> 0.74;
            case LOW -> 0.68;
        };
    }

    private void notifyHighSeverityInsight(AIInsight insight) {
        if (insight == null || insight.getEmployee() == null || insight.getSeverity() == null) return;
        if (insight.getSeverity() != InsightSeverity.HIGH && insight.getSeverity() != InsightSeverity.CRITICAL) return;

        Employee employee = insight.getEmployee();
        String title = insight.getSeverity() == InsightSeverity.CRITICAL
                ? "Canh bao AI nghiem trong"
                : "Canh bao AI can chu y";
        String message = insight.getSummary() != null && !insight.getSummary().isBlank()
                ? insight.getSummary()
                : "AI phat hien tin hieu rui ro can duoc xem xet.";

        notificationService.createNotification(employee.getId(), NotificationType.AI_INSIGHT, title, message, null, employee.getId());

        Team team = employee.getTeam();
        if (team != null && team.getManager() != null && !team.getManager().getId().equals(employee.getId())) {
            notificationService.createNotification(team.getManager().getId(), NotificationType.AI_INSIGHT,
                    title + ": " + fullName(employee), message, null, employee.getId());
        }
    }

    private boolean isConfiguredProviderApiKeyPresent() {
        if ("openai".equalsIgnoreCase(aiProperties.getProvider())) {
            return aiProperties.getOpenai().getApiKey() != null && !aiProperties.getOpenai().getApiKey().isBlank();
        }
        return aiProperties.getGemini().getApiKey() != null && !aiProperties.getGemini().getApiKey().isBlank();
    }

    private Project resolveEmployeeProject(Employee employee) {
        if (employee.getTeam() == null) return null;
        return projectRepository.findAll().stream()
                .filter(project -> project.getTeam() != null && project.getTeam().getId().equals(employee.getTeam().getId()))
                .findFirst()
                .orElse(null);
    }

    private String fullName(Employee employee) {
        String first = employee.getFirstName() != null ? employee.getFirstName() : "";
        String last = employee.getLastName() != null ? employee.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? employee.getId().toString() : name;
    }

    @Transactional(readOnly = true)
    public List<AIInsightResponse> getInsightsForEmployee(UUID employeeId) {
        return insightRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AIInsightResponse> getMyInsights() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        return getInsightsForEmployee(currentEmployee.getId());
    }

    @Transactional(readOnly = true)
    public List<AIInsightResponse> getInsightsForTeam(UUID teamId) {
        return insightRepository.findByEmployeeTeamIdOrderByCreatedAtDesc(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AIInsightResponse> getInsightsForManagedTeams() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        List<UUID> teamIds = teamRepository.findByManagerId(currentEmployee.getId()).stream()
                .map(Team::getId)
                .toList();
        if (teamIds.isEmpty()) return List.of();
        return insightRepository.findByEmployeeTeamIdInOrderByCreatedAtDesc(teamIds).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AIInsightResponse> getInsightsForOrganization(UUID organizationId) {
        return insightRepository.findByEmployeeTeamOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AIInsightResponse> getInsightsForProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        accessPolicyService.ensureProjectAccess(project);
        return insightRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AIInsightResponse getInsight(UUID insightId) {
        AIInsight insight = insightRepository.findById(insightId)
                .orElseThrow(() -> new ResourceNotFoundException("AI insight not found"));
        return mapToResponse(insight);
    }
    private AIInsightResponse mapToResponse(AIInsight insight) {
        Employee employee = insight.getEmployee();
        Team team = insight.getTeam();
        Project project = insight.getProject();
        return AIInsightResponse.builder()
                .id(insight.getId())
                .summary(insight.getSummary())
                .fullAnalysis(insight.getFullAnalysis())
                .severity(insight.getSeverity())
                .insightType(insight.getInsightType())
                .confidenceScore(insight.getConfidenceScore())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeName(employee != null ? fullName(employee) : null)
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : null)
                .projectId(project != null ? project.getId() : null)
                .projectName(project != null ? project.getName() : null)
                .createdAt(insight.getCreatedAt())
                .updatedAt(insight.getUpdatedAt())
                .build();
    }
}