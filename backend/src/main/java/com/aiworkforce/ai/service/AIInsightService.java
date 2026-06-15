package com.aiworkforce.ai.service;

import com.aiworkforce.ai.client.GeminiClient;
import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.prompt.PromptBuilder;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.analytics.dto.DashboardResponse;
import com.aiworkforce.analytics.service.DashboardAnalyticsService;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.InsightType;
import com.aiworkforce.core.exception.AiServiceException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
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

/**
 * Service xử lý logic sinh AI Insight (phân tích burnout) cho nhân viên.
 * <p>
 * Luồng chính:
 * 1. Lấy dữ liệu analytics của nhân viên
 * 2. Xây dựng prompt từ dữ liệu
 * 3. Gọi Gemini để sinh insight
 * 4. Parse + normalize JSON response
 * 5. Lưu insight vào database
 * <p>
 * <b>Xử lý lỗi:</b>
 * - Nếu AI không kết nối được → throw {@link AiServiceException} (HTTP 503)
 * - Nếu prompt lỗi → throw {@link IllegalArgumentException} (HTTP 400)
 * - Nếu AI trả về JSON không hợp lệ → throw {@link AiServiceException}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIInsightService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GeminiClient geminiClient;
    private final PromptBuilder promptBuilder;
    private final DashboardAnalyticsService analyticsService;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final EmployeeService employeeService;
    private final AIInsightRepository insightRepository;

    /**
     * Sinh AI insight cho nhân viên dựa trên dữ liệu analytics.
     * <p>
     * Gọi Gemini AI thật để phân tích - không dùng dữ liệu giả hoặc fallback che lỗi.
     * Nếu AI không khả dụng, exception sẽ được propagate lên controller.
     *
     * @param employeeId UUID của nhân viên cần phân tích
     * @return AIInsight đã được lưu vào database
     * @throws ResourceNotFoundException nếu không tìm thấy nhân viên
     * @throws AiServiceException nếu không kết nối được AI hoặc AI trả về lỗi
     */
    @Transactional
    public AIInsight generateInsightForEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        DashboardResponse analytics = analyticsService.getEmployeeDashboard(employeeId);

        // Xây dựng prompt từ dữ liệu thật
        String prompt = promptBuilder.buildBurnoutPrompt(
                employee.getFirstName() + " " + employee.getLastName(), analytics);

        // Gọi AI - sẽ throw AiServiceException nếu lỗi kết nối/prompt
        String aiResponse;
        try {
            aiResponse = geminiClient.generateInsight(prompt);
        } catch (AiServiceException e) {
            log.error("Lỗi khi gọi AI service cho nhân viên {}: {}", employeeId, e.getMessage());
            throw e; // Propagate lên controller để trả HTTP 503
        } catch (IllegalArgumentException e) {
            log.error("Lỗi prompt cho nhân viên {}: {}", employeeId, e.getMessage());
            throw e; // Propagate lên controller để trả HTTP 400
        }

        // Parse và normalize response JSON từ AI
        ObjectNode normalizedInsight = normalizeInsightJson(aiResponse);
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

    /**
     * Normalize JSON response từ AI.
     * <p>
     * Nếu AI trả về JSON hợp lệ nhưng thiếu field bắt buộc → throw {@link AiServiceException}.
     * Nếu AI trả về text không phải JSON → throw {@link AiServiceException}; service không tự sinh insight thay Gemini.
     */
    private ObjectNode normalizeInsightJson(String response) {
        String jsonCandidate = extractJsonObject(cleanJsonResponse(response));

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonCandidate);
            if (!rootNode.isObject()) {
                throw new AiServiceException("Gemini response is not a JSON object.");
            }

            ObjectNode normalized = (ObjectNode) rootNode;
            validateRequiredInsightSchema(normalized);
            return normalized;
        } catch (Exception e) {
            if (e instanceof AiServiceException aiServiceException) {
                throw aiServiceException;
            }
            log.error("Gemini response is not valid JSON: {}", e.getMessage());
            throw new AiServiceException("Gemini response is not valid JSON. Raw response cannot be used as an AI insight.", e);
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

    private void validateRequiredInsightSchema(ObjectNode node) {
        validateRequiredTextField(node, "status_evaluation");
        validateRequiredTextField(node, "primary_reason");

        JsonNode recommendations = node.get("recommendations");
        if (recommendations != null && recommendations.isArray() && recommendations.size() > 0) {
            return;
        }

        throw new AiServiceException("Gemini response is missing required array field: recommendations.");
    }

    private void validateRequiredTextField(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new AiServiceException("Gemini response is missing required text field: " + fieldName + ".");
        }
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
