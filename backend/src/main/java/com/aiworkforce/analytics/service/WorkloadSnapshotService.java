package com.aiworkforce.analytics.service;

import com.aiworkforce.analytics.entity.EmployeeWorkloadSnapshot;
import com.aiworkforce.analytics.repository.EmployeeWorkloadSnapshotRepository;
import com.aiworkforce.analytics.dto.WorkloadHistoryResponse;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.core.enums.NotificationType;
import com.aiworkforce.core.service.NotificationService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkloadSnapshotService {

    private final EmployeeWorkloadSnapshotRepository snapshotRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final EmployeeService employeeService;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public List<WorkloadHistoryResponse> getWorkloadHistory(UUID employeeId, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();

        return snapshotRepository.findByEmployeeIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(employeeId, start, end)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<WorkloadHistoryResponse> getTeamWorkloadHistory(UUID teamId, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();

        return snapshotRepository.findByEmployeeTeamIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(teamId, start, end)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<WorkloadHistoryResponse> getManagedTeamWorkloadHistory(LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();

        Employee currentEmployee = employeeService.getCurrentEmployee();
        List<UUID> teamIds = teamRepository.findByManagerId(currentEmployee.getId()).stream()
                .map(Team::getId)
                .collect(Collectors.toList());

        if (teamIds.isEmpty()) {
            return List.of();
        }

        return snapshotRepository.findByEmployeeTeamIdInAndSnapshotDateBetweenOrderBySnapshotDateAsc(teamIds, start, end)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<WorkloadHistoryResponse> getOrganizationWorkloadHistory(UUID organizationId, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();

        return snapshotRepository.findByEmployeeTeamOrganizationIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(organizationId, start, end)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Runs daily at 2:00 AM
    public void captureDailyWorkloadSnapshots() {
        log.info("Starting daily workload snapshot capture...");
        List<Employee> employees = employeeRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Employee emp : employees) {
            try {
                captureSnapshotForEmployee(emp, today);
            } catch (Exception e) {
                log.error("Failed to capture workload snapshot for employee: " + emp.getId(), e);
            }
        }
        log.info("Daily workload snapshot capture completed.");
    }

    @Transactional
    public EmployeeWorkloadSnapshot captureSnapshotForEmployee(Employee emp, LocalDate date) {
        List<Task> employeeTasks = taskRepository.findByAssigneeId(emp.getId());

        long openTasks = employeeTasks.stream().filter(t -> !"DONE".equalsIgnoreCase(t.getStatus().name())).count();
        long overdueTasks = employeeTasks.stream().filter(t -> "OVERDUE".equalsIgnoreCase(t.getStatus().name())).count();

        // Calculate workload score (mocking based on open tasks and story points)
        // Normal range is 0 to 100. Over 80 is high/burnout risk.
        double workloadScore = Math.min(100.0, Math.max(0.0, (openTasks * 15.0) + (overdueTasks * 20.0)));
        if (workloadScore == 0 && !employeeTasks.isEmpty()) {
            workloadScore = 30.0; // baseline if tasks exist but none overdue/high count
        } else if (employeeTasks.isEmpty()) {
            workloadScore = 15.0; // baseline default
        }

        // Determine Burnout Risk
        BurnoutRisk risk = BurnoutRisk.NONE;
        if (workloadScore >= 80.0 || overdueTasks > 3) {
            risk = BurnoutRisk.HIGH;
        } else if (workloadScore >= 55.0 || overdueTasks > 1) {
            risk = BurnoutRisk.MEDIUM;
        } else if (workloadScore >= 35.0) {
            risk = BurnoutRisk.WATCH;
        }

        // Focus score composite: cycle time, overdue ratio (0.0-1.0), etc.
        double focusScore = Math.max(0.0, 100.0 - (overdueTasks * 15.0) - (workloadScore * 0.3));

        // Save daily snapshot
        EmployeeWorkloadSnapshot snapshot = new EmployeeWorkloadSnapshot();
        snapshot.setEmployee(emp);
        snapshot.setSnapshotDate(date);
        snapshot.setWorkloadScore(workloadScore);
        snapshot.setBurnoutRisk(risk);
        snapshot.setTasksOpen((int) openTasks);
        snapshot.setTasksOverdue((int) overdueTasks);
        
        // Populate realistic out of hours and cycle time based on employee initials / mock logic
        double outOfHours = emp.getOutOfHoursPct() != null ? emp.getOutOfHoursPct() : Math.min(45.0, (openTasks * 4.2));
        double cycleTime = emp.getAvgCycleTimeDays() != null ? emp.getAvgCycleTimeDays() : 2.5 + (overdueTasks * 1.5);
        
        snapshot.setOutOfHoursPct(outOfHours);
        snapshot.setCycleTimeAvg(cycleTime);
        
        snapshotRepository.save(snapshot);

        // Update employee cached properties
        emp.setWorkloadScore(workloadScore);
        emp.setBurnoutRisk(risk);
        emp.setFocusScore(focusScore);
        emp.setOverdueRatio(openTasks > 0 ? (double) overdueTasks / openTasks : 0.0);
        emp.setOutOfHoursPct(outOfHours);
        emp.setAvgCycleTimeDays(cycleTime);
        
        if (emp.getTasksShippedThisMonth() == null) emp.setTasksShippedThisMonth(5);
        if (emp.getStreakDays() == null) emp.setStreakDays(4);
        if (emp.getContributionScore() == null) emp.setContributionScore(75.0);
        
        employeeRepository.save(emp);

        // Check if we should notify managers/employee about high burnout risk
        if (risk == BurnoutRisk.HIGH) {
            notificationService.createNotification(
                    emp.getId(),
                    NotificationType.BURNOUT_ALERT,
                    "Cảnh báo nguy cơ kiệt sức!",
                    "Tải lượng công việc hiện tại của bạn đã vượt quá giới hạn an toàn. Vui lòng thảo luận với quản lý để phân bổ lại các tác vụ.",
                    null,
                    emp.getId()
            );
        }

        return snapshot;
    }

    @Transactional
    public void generateHistoricalSnapshots(Employee emp, int days) {
        LocalDate start = LocalDate.now().minusDays(days);
        for (int i = 0; i <= days; i++) {
            LocalDate date = start.plusDays(i);
            
            // Randomize workload score a bit for beautiful charts
            double baseScore = 30.0 + (i % 7) * 8.0;
            if (emp.getFirstName().equalsIgnoreCase("David") || emp.getFirstName().equalsIgnoreCase("Elena")) {
                baseScore += 25.0; // Higher workload for David/Elena
            }
            double finalScore = Math.min(98.0, baseScore);
            
            BurnoutRisk risk = BurnoutRisk.NONE;
            if (finalScore >= 80.0) risk = BurnoutRisk.HIGH;
            else if (finalScore >= 55.0) risk = BurnoutRisk.MEDIUM;
            else if (finalScore >= 35.0) risk = BurnoutRisk.WATCH;

            EmployeeWorkloadSnapshot snapshot = new EmployeeWorkloadSnapshot();
            snapshot.setEmployee(emp);
            snapshot.setSnapshotDate(date);
            snapshot.setWorkloadScore(finalScore);
            snapshot.setBurnoutRisk(risk);
            snapshot.setTasksOpen(3 + (i % 4));
            snapshot.setTasksOverdue(i % 5 == 0 ? 1 : 0);
            snapshot.setOutOfHoursPct(10.0 + (i % 3) * 8.0);
            snapshot.setCycleTimeAvg(2.0 + (i % 4) * 0.8);
            
            snapshotRepository.save(snapshot);
        }
    }

    private WorkloadHistoryResponse mapToResponse(EmployeeWorkloadSnapshot snapshot) {
        if (snapshot == null) return null;

        String empName = "";
        if (snapshot.getEmployee() != null) {
            empName = snapshot.getEmployee().getFirstName() + " " + snapshot.getEmployee().getLastName();
        }

        return WorkloadHistoryResponse.builder()
                .employeeId(snapshot.getEmployee() != null ? snapshot.getEmployee().getId() : null)
                .employeeName(empName)
                .date(snapshot.getSnapshotDate())
                .workloadScore(snapshot.getWorkloadScore())
                .burnoutRisk(snapshot.getBurnoutRisk())
                .tasksOpen(snapshot.getTasksOpen())
                .tasksOverdue(snapshot.getTasksOverdue())
                .outOfHoursPct(snapshot.getOutOfHoursPct())
                .cycleTimeAvg(snapshot.getCycleTimeAvg())
                .build();
    }
}
