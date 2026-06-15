package com.aiworkforce.ai.prompt;

import com.aiworkforce.analytics.dto.DashboardResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    public String buildBurnoutPrompt(String employeeName, DashboardResponse analyticsData) {
        return buildBurnoutPrompt(employeeName, analyticsData, "");
    }

    public String buildBurnoutPrompt(String employeeName, DashboardResponse analyticsData, String ragContext) {
        String trends = "";
        if (analyticsData.getRecentTrends() != null && !analyticsData.getRecentTrends().isEmpty()) {
            trends = analyticsData.getRecentTrends().stream()
                    .map(trend -> trend.getPeriod() + ": " + trend.getWorkloadScore())
                    .collect(Collectors.joining(", "));
        }

        return String.format("""
                Ban la chuyen gia cao cap ve phan tich hieu suat nhan su va tam ly hoc to chuc.
                Hay phan tich rui ro kiet suc cua nhan vien: %s.

                Du lieu analytics hien co:
                - So tac vu hoan thanh gan day: %d
                - So tac vu tre han gan day: %d
                - Workload Score hien tai: %d
                - Muc rui ro he thong phat hien: %s
                - Xu huong workload gan day: %s

                Boi canh truy xuat tu he thong noi bo (RAG):
                %s

                Yeu cau:
                - Tra ve dung mot JSON object hop le, khong markdown, khong giai thich ngoai JSON.
                - Viet bang tieng Viet chuyen nghiep, ngan gon, dung duoc cho quan ly nhan su.
                - Khuyen nghi phai cu the va co the hanh dong.
                - Chi dua ra ket luan dua tren analytics va boi canh RAG duoc cung cap.

                Cau truc JSON bat buoc:
                {
                  "status_evaluation": "Danh gia hien trang lam viec",
                  "primary_reason": "Nguyen nhan chinh dua tren du lieu",
                  "recommendations": ["Khuyen nghi 1", "Khuyen nghi 2", "Khuyen nghi 3"]
                }
                """,
                employeeName,
                analyticsData.getTotalTasksCompleted(),
                analyticsData.getTotalOverdueTasks(),
                analyticsData.getCurrentWorkloadScore(),
                analyticsData.getBurnoutRiskLevel(),
                trends.isBlank() ? "Chua co du lieu xu huong" : trends,
                ragContext == null || ragContext.isBlank() ? "Khong co boi canh bo sung." : ragContext
        );
    }
}
