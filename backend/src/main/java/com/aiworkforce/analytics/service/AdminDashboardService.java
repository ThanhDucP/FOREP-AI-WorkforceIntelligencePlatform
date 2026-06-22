package com.aiworkforce.analytics.service;

import com.aiworkforce.analytics.dto.AdminDashboardResponse;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboardStats() {
        long totalUsers = employeeRepository.count();
        long activeUsers = accountRepository.countByActive(true);
        long totalTeams = teamRepository.count();
        long totalOrganizations = organizationRepository.count();
        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countByStatus(TaskStatus.DONE);

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalTeams(totalTeams)
                .totalOrganizations(totalOrganizations)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .burnoutRiskDistribution(buildBurnoutRiskDistribution())
                .build();
    }

    private Map<String, Long> buildBurnoutRiskDistribution() {
        Map<BurnoutRisk, Long> counts = new EnumMap<>(BurnoutRisk.class);
        for (BurnoutRisk risk : BurnoutRisk.values()) {
            counts.put(risk, 0L);
        }

        for (Object[] row : employeeRepository.countByBurnoutRiskGroup()) {
            BurnoutRisk risk = row[0] != null ? (BurnoutRisk) row[0] : BurnoutRisk.NONE;
            Long count = (Long) row[1];
            counts.put(risk, counts.getOrDefault(risk, 0L) + count);
        }

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (BurnoutRisk risk : BurnoutRisk.values()) {
            distribution.put(risk.name(), counts.getOrDefault(risk, 0L));
        }
        return distribution;
    }
}