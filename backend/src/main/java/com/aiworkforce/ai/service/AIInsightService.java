package com.aiworkforce.ai.service;

import com.aiworkforce.ai.client.OllamaClient;
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

/**
 * Service xử lý logic sinh AI Insight (phân tích burnout) cho nhân viên.
 * <p>
 * Luồng chính:
 * 1. Lấy dữ liệu analytics của nhân viên
 * 2. Xây dựng prompt từ dữ liệu
 * 3. Gọi AI (Gemini) qua OllamaClient để sinh insight
 * 4. Parse + normalize JSON response
 * 5. Lưu insight vào database
 * <p>
 * <b>Xử lý lỗi:</b>
 * - Nếu AI không kết nối được → throw {@link AiServiceException} (HTTP 503)
 * - Nếu prompt lỗi → throw {@link IllegalArgumentException} (HTTP 400)
 * - Nếu AI trả về JSON không hợp lệ → dùng fallback dựa trên dữ liệu analytics thực tế
 *   (đây KHÔNG phải mock - là phân tích rule-based dựa trên data thật)
 */
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

    /**
     * Sinh AI insight cho nhân viên dựa trên dữ liệu analytics.
     * <p>
     * Gọi Gemini AI thật để phân tích - KHÔNG dùng mock/fake data.
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
            aiResponse = ollamaClient.generateInsight(prompt);
        } catch (AiServiceException e) {
            log.error("Lỗi khi gọi AI service cho nhân viên {}: {}", employeeId, e.getMessage());
            throw e; // Propagate lên controller để trả HTTP 503
        } catch (IllegalArgumentException e) {
            log.error("Lỗi prompt cho nhân viên {}: {}", employeeId, e.getMessage());
            throw e; // Propagate lên controller để trả HTTP 400
        }

        // Parse và normalize response JSON từ AI
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

    /**
     * Normalize JSON response từ AI.
     * <p>
     * Nếu AI trả về JSON hợp lệ nhưng thiếu một số field → bổ sung từ analytics data thật.
     * Nếu AI trả về text không phải JSON → tạo insight dựa trên rule-based analysis
     * (KHÔNG phải mock - sử dụng dữ liệu analytics thực tế để đánh giá).
     */
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
            log.warn("AI response không phải JSON hợp lệ: {}. Sử dụng phân tích rule-based từ dữ liệu thật.", e.getMessage());
            return buildRuleBasedInsight(analytics);
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
        ruleBasedRecommendations(analytics).forEach(fallback::add);
        node.set("recommendations", fallback);
    }

    /**
     * Tạo insight dựa trên phân tích rule-based từ dữ liệu analytics thật.
     * <p>
     * Đây KHÔNG phải mock data. Insight được sinh ra dựa trên:
     * - Workload score thực tế
     * - Số tác vụ trễ hạn thực tế
     * - Số tác vụ hoàn thành thực tế
     * - Mức burnout risk level từ hệ thống phân tích
     * <p>
     * Được sử dụng khi AI trả về text không parse được thành JSON,
     * nhưng kết nối AI vẫn thành công (không phải lỗi kết nối).
     */
    private ObjectNode buildRuleBasedInsight(DashboardResponse analytics) {
        ObjectNode insight = OBJECT_MAPPER.createObjectNode();
        insight.put("status_evaluation", fallbackStatusEvaluation(analytics));
        insight.put("primary_reason", fallbackPrimaryReason(analytics));
        insight.put("_source", "rule-based-analysis"); // Đánh dấu nguồn là rule-based, không phải AI

        ArrayNode recommendations = OBJECT_MAPPER.createArrayNode();
        ruleBasedRecommendations(analytics).forEach(recommendations::add);
        insight.set("recommendations", recommendations);
        return insight;
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

    private List<String> ruleBasedRecommendations(DashboardResponse analytics) {
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
