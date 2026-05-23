package com.aiworkforce.ai.service;

import com.aiworkforce.ai.client.OllamaClient;
import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.prompt.PromptBuilder;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.analytics.dto.DashboardResponse;
import com.aiworkforce.analytics.service.DashboardAnalyticsService;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.InsightType;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

    private final OllamaClient ollamaClient;
    private final PromptBuilder promptBuilder;
    private final DashboardAnalyticsService analyticsService;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final EmployeeService employeeService;
    private final AIInsightRepository insightRepository;

    @Transactional
    public AIInsight generateInsightForEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        DashboardResponse analytics = analyticsService.getEmployeeDashboard(employeeId);

        String prompt = promptBuilder.buildBurnoutPrompt(employee.getFirstName() + " " + employee.getLastName(), analytics);
        String aiResponse = ollamaClient.generateInsight(prompt);
        ObjectNode normalizedInsight = normalizeInsightJson(aiResponse, analytics);
        String summaryText = normalizedInsight.get("status_evaluation").asText();
        String fullAnalysis = normalizedInsight.toPrettyString();

        AIInsight insight = new AIInsight();
        insight.setEmployee(employee);
        insight.setFullAnalysis(fullAnalysis);
        insight.setSummary(summaryText);
        insight.setSeverity(resolveSeverity(analytics.getBurnoutRiskLevel()));
        insight.setTeam(employee.getTeam());
        insight.setInsightType(InsightType.BURNOUT_WARNING);
        insight.setConfidenceScore(resolveConfidenceScore(insight.getSeverity(), normalizedInsight));

        return insightRepository.save(insight);
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

    private ObjectNode normalizeInsightJson(String response, DashboardResponse analytics) {
        String jsonCandidate = extractJsonObject(cleanJsonResponse(response));

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonCandidate);
            if (!rootNode.isObject()) {
                throw JsonMappingException.from(OBJECT_MAPPER.createParser(jsonCandidate), "AI response is not a JSON object");
            }

            ObjectNode normalized = (ObjectNode) rootNode;
            ensureTextField(normalized, "status_evaluation", fallbackStatusEvaluation(analytics));
            ensureTextField(normalized, "primary_reason", fallbackPrimaryReason(analytics));
            ensureRecommendations(normalized, analytics);
            return normalized;
        } catch (Exception e) {
            log.error("Failed to parse AI response JSON: {}. Using structured fallback insight.", e.getMessage());
            return buildFallbackInsight(analytics);
        }
    }

    private String extractJsonObject(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1).trim();
        }

        return response.trim();
    }

    private void ensureTextField(ObjectNode node, String fieldName, String fallbackValue) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            node.put(fieldName, fallbackValue);
        }
    }

    private void ensureRecommendations(ObjectNode node, DashboardResponse analytics) {
        JsonNode recommendations = node.get("recommendations");
        if (recommendations != null && recommendations.isArray() && recommendations.size() > 0) {
            return;
        }

        ArrayNode fallback = OBJECT_MAPPER.createArrayNode();
        fallbackRecommendations(analytics).forEach(fallback::add);
        node.set("recommendations", fallback);
    }

    private ObjectNode buildFallbackInsight(DashboardResponse analytics) {
        ObjectNode fallback = OBJECT_MAPPER.createObjectNode();
        fallback.put("status_evaluation", fallbackStatusEvaluation(analytics));
        fallback.put("primary_reason", fallbackPrimaryReason(analytics));

        ArrayNode recommendations = OBJECT_MAPPER.createArrayNode();
        fallbackRecommendations(analytics).forEach(recommendations::add);
        fallback.set("recommendations", recommendations);
        return fallback;
    }

    private String fallbackStatusEvaluation(DashboardResponse analytics) {
        InsightSeverity severity = resolveSeverity(analytics.getBurnoutRiskLevel());
        return switch (severity) {
            case CRITICAL, HIGH -> "Nhân viên đang có dấu hiệu quá tải đáng kể và cần được can thiệp sớm để giảm rủi ro kiệt sức.";
            case MEDIUM -> "Nhân viên đang ở vùng cần theo dõi, tải công việc chưa vượt ngưỡng nghiêm trọng nhưng đã có tín hiệu mất cân bằng.";
            case LOW -> "Nhân viên đang duy trì nhịp làm việc ổn định, chưa có dấu hiệu rủi ro kiệt sức đáng kể.";
        };
    }

    private String fallbackPrimaryReason(DashboardResponse analytics) {
        return String.format(
                "Dữ liệu hiện tại ghi nhận workload score %d, %d tác vụ trễ hạn và %d tác vụ hoàn thành gần đây.",
                analytics.getCurrentWorkloadScore(),
                analytics.getTotalOverdueTasks(),
                analytics.getTotalTasksCompleted()
        );
    }

    private List<String> fallbackRecommendations(DashboardResponse analytics) {
        InsightSeverity severity = resolveSeverity(analytics.getBurnoutRiskLevel());
        return switch (severity) {
            case CRITICAL, HIGH -> List.of(
                    "Tổ chức cuộc trao đổi 1-1 trong tuần này để xác định điểm nghẽn và mức hỗ trợ cần thiết.",
                    "Tái phân bổ các tác vụ ưu tiên cao sang thành viên còn năng lực tiếp nhận.",
                    "Tạm dừng giao thêm việc khẩn cấp cho đến khi các tác vụ tồn đọng được xử lý."
            );
            case MEDIUM -> List.of(
                    "Rà soát lại thứ tự ưu tiên backlog và giảm các tác vụ chuyển ngữ cảnh nhiều.",
                    "Theo dõi tiến độ trong vài ngày tới để phát hiện sớm xu hướng quá tải.",
                    "Trao đổi với nhóm trưởng trước khi giao thêm nhiệm vụ có độ ưu tiên cao."
            );
            case LOW -> List.of(
                    "Duy trì nhịp làm việc hiện tại và tiếp tục theo dõi workload định kỳ.",
                    "Ghi nhận đóng góp tích cực để củng cố động lực làm việc.",
                    "Có thể giao thêm nhiệm vụ vừa phải nếu vẫn giữ được tiến độ ổn định."
            );
        };
    }

    private InsightSeverity resolveSeverity(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return InsightSeverity.LOW;
        }

        try {
            return InsightSeverity.valueOf(riskLevel.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown burnout risk level '{}'. Falling back to LOW severity.", riskLevel);
            return InsightSeverity.LOW;
        }
    }

    private Double resolveConfidenceScore(InsightSeverity severity, ObjectNode normalizedInsight) {
        JsonNode confidence = normalizedInsight.get("confidence_score");
        if (confidence != null && confidence.isNumber()) {
            return Math.max(0.0, Math.min(1.0, confidence.asDouble()));
        }

        return switch (severity) {
            case CRITICAL, HIGH -> 0.82;
            case MEDIUM -> 0.74;
            case LOW -> 0.68;
        };
    }

    public List<AIInsight> getInsightsForEmployee(UUID employeeId) {
        return insightRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    public List<AIInsight> getMyInsights() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        return getInsightsForEmployee(currentEmployee.getId());
    }

    public List<AIInsight> getInsightsForTeam(UUID teamId) {
        return insightRepository.findByEmployeeTeamIdOrderByCreatedAtDesc(teamId);
    }

    public List<AIInsight> getInsightsForManagedTeams() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        List<UUID> teamIds = teamRepository.findByManagerId(currentEmployee.getId()).stream()
                .map(Team::getId)
                .toList();

        if (teamIds.isEmpty()) {
            return List.of();
        }

        return insightRepository.findByEmployeeTeamIdInOrderByCreatedAtDesc(teamIds);
    }

    public List<AIInsight> getInsightsForOrganization(UUID organizationId) {
        return insightRepository.findByEmployeeTeamOrganizationIdOrderByCreatedAtDesc(organizationId);
    }
}
