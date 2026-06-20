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
        return buildWorkforcePrompt(
                "Phat hien qua tai va rui ro kiet suc",
                employeeName,
                analyticsData,
                ragContext,
                "Tap trung vao dau hieu qua tai, tre han, workload cao, task ngoai gio va can bang lai cong viec."
        );
    }

    public String buildPerformancePrompt(String employeeName, DashboardResponse analyticsData, String ragContext) {
        return buildWorkforcePrompt(
                "Danh gia hieu suat nhan vien",
                employeeName,
                analyticsData,
                ragContext,
                "Tap trung vao ket qua hoan thanh, chat luong dong gop, toc do xu ly task, GitHub/Jira evidence va diem can cai thien."
        );
    }

    public String buildOverloadPrompt(String employeeName, DashboardResponse analyticsData, String ragContext) {
        return buildWorkforcePrompt(
                "Phat hien qua tai",
                employeeName,
                analyticsData,
                ragContext,
                "Tap trung vao viec nhan vien dang bi qua tai hay khong, nguyen nhan truc tiep va hanh dong giam tai trong sprint hien tai."
        );
    }

    public String buildProjectWarningPrompt(String employeeName, DashboardResponse analyticsData, String ragContext) {
        return buildWorkforcePrompt(
                "Canh bao rui ro du an",
                employeeName,
                analyticsData,
                ragContext,
                "Tap trung vao rui ro cham tien do, PR/issue bi ton dong, sprint dang nguy co va viec manager can can thiep."
        );
    }

    private String buildWorkforcePrompt(String objective, String employeeName, DashboardResponse analyticsData,
                                        String ragContext, String analysisFocus) {
        String trends = "";
        if (analyticsData.getRecentTrends() != null && !analyticsData.getRecentTrends().isEmpty()) {
            trends = analyticsData.getRecentTrends().stream()
                    .map(trend -> trend.getPeriod() + ": " + trend.getWorkloadScore())
                    .collect(Collectors.joining(", "));
        }

        return String.format("""
                Ban la chuyen gia cao cap ve phan tich hieu suat nhan su va tam ly hoc to chuc.
                Muc tieu phan tich: %s.
                Doi tuong: %s.

                Du lieu analytics hien co:
                - So tac vu hoan thanh gan day: %d
                - So tac vu tre han gan day: %d
                - Workload Score hien tai: %d
                - Muc rui ro he thong phat hien: %s
                - Xu huong workload gan day: %s

                Boi canh truy xuat tu he thong noi bo (RAG):
                %s

                Trong tam phan tich:
                - %s

                Yeu cau:
                - Tra ve dung mot JSON object hop le, khong markdown, khong giai thich ngoai JSON.
                - Viet bang tieng Viet chuyen nghiep, ngan gon, dung duoc cho quan ly nhan su.
                - Khuyen nghi phai cu the va co the hanh dong.
                - Chi dua ra ket luan dua tren analytics, GitHub, Jira va boi canh RAG duoc cung cap.

                Cau truc JSON bat buoc:
                {
                  "riskLevel": "LOW | MEDIUM | HIGH",
                  "summary": "Tom tat ngan gon tinh trang va tac dong",
                  "reasons": ["Ly do 1 dua tren du lieu", "Ly do 2 dua tren du lieu"],
                  "recommendations": ["Khuyen nghi 1", "Khuyen nghi 2", "Khuyen nghi 3"]
                }
                """,
                objective,
                employeeName,
                analyticsData.getTotalTasksCompleted(),
                analyticsData.getTotalOverdueTasks(),
                analyticsData.getCurrentWorkloadScore(),
                analyticsData.getBurnoutRiskLevel(),
                trends.isBlank() ? "Chua co du lieu xu huong" : trends,
                ragContext == null || ragContext.isBlank() ? "Khong co boi canh bo sung." : ragContext,
                analysisFocus
        );
    }
}
