package com.aiworkforce.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String jobTitle;
    private String phoneNumber;
    private String email;
    private String avatarUrl;
    private UUID teamId;
    private String teamName;
    private String role;
    private String department;
    private String avatarInitials;
    private Double workloadScore;
    private String burnoutRisk;
    private Double contributionScore;
    private Double overdueRatio;
    private Double outOfHoursPct;
    private Double avgCycleTimeDays;
    private Integer tasksShippedThisMonth;
    private Integer streakDays;
    private Double focusScore;
}

