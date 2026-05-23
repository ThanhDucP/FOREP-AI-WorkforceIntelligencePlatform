package com.aiworkforce.analytics.service;

import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.analytics.dto.AdminDashboardResponse;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final TaskRepository taskRepository;
    private final AIInsightRepository insightRepository;

    public AdminDashboardResponse getAdminDashboardStats() {
        long totalUsers = employeeRepository.count();
        long activeUsers = accountRepository.countByActive(true);
        long totalTeams = teamRepository.count();
        long totalOrganizations = organizationRepository.count();
        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countByStatus(TaskStatus.DONE);

        Map<String, Long> distribution = new HashMap<>();
        distribution.put("NONE", 0L);
        distribution.put("WATCH", 0L);
        distribution.put("MEDIUM", 0L);
        distribution.put("HIGH", 0L);

        List<Employee> employees = employeeRepository.findAll();
        for (Employee emp : employees) {
            String risk = emp.getBurnoutRisk() != null ? emp.getBurnoutRisk().name() : "NONE";
            distribution.put(risk, distribution.getOrDefault(risk, 0L) + 1);
        }

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalTeams(totalTeams)
                .totalOrganizations(totalOrganizations)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .burnoutRiskDistribution(distribution)
                .build();
    }
}
