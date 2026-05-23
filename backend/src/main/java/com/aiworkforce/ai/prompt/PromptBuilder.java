package com.aiworkforce.ai.prompt;

import com.aiworkforce.analytics.dto.DashboardResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    public String buildBurnoutPrompt(String employeeName, DashboardResponse analyticsData) {
        String trends = "";
        if (analyticsData.getRecentTrends() != null && !analyticsData.getRecentTrends().isEmpty()) {
            trends = analyticsData.getRecentTrends().stream()
                    .map(trend -> trend.getPeriod() + ": " + trend.getWorkloadScore())
                    .collect(Collectors.joining(", "));
        }

        return String.format("""
                Bạn là chuyên gia cao cấp về phân tích hiệu suất nhân sự và tâm lý học tổ chức.
                Hãy phân tích rủi ro kiệt sức của nhân viên: %s.

                Dữ liệu hiện có:
                - Số tác vụ hoàn thành gần đây: %d
                - Số tác vụ trễ hạn gần đây: %d
                - Workload Score hiện tại: %d
                - Mức rủi ro hệ thống phát hiện: %s
                - Xu hướng workload gần đây: %s

                Yêu cầu:
                - Trả về đúng một JSON object hợp lệ, không markdown, không giải thích ngoài JSON.
                - Viết bằng tiếng Việt chuyên nghiệp, ngắn gọn, dùng được cho quản lý nhân sự.
                - Khuyến nghị phải cụ thể và có thể hành động.

                Cấu trúc JSON bắt buộc:
                {
                  "status_evaluation": "Đánh giá hiện trạng làm việc",
                  "primary_reason": "Nguyên nhân chính dựa trên dữ liệu",
                  "recommendations": ["Khuyến nghị 1", "Khuyến nghị 2", "Khuyến nghị 3"]
                }
                """,
                employeeName,
                analyticsData.getTotalTasksCompleted(),
                analyticsData.getTotalOverdueTasks(),
                analyticsData.getCurrentWorkloadScore(),
                analyticsData.getBurnoutRiskLevel(),
                trends.isBlank() ? "Chưa có dữ liệu xu hướng" : trends
        );
    }
}
