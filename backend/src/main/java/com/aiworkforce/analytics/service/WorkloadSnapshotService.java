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
import java.time.LocalDateTime;
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

        double workloadScore = calculateWorkloadScore(employeeTasks);

        // Determine Burnout Risk
        BurnoutRisk risk = BurnoutRisk.NONE;
        if (workloadScore >= 80.0 || overdueTasks > 3) {
            risk = BurnoutRisk.HIGH;
        } else if (workloadScore >= 55.0 || overdueTasks > 1) {
            risk = BurnoutRisk.MEDIUM;
        } else if (workloadScore >= 35.0) {
            risk = BurnoutRisk.WATCH;
        }

        double overdueRatio = openTasks > 0 ? (double) overdueTasks / openTasks : 0.0;
        double cycleTime = calculateAverageCycleTime(employeeTasks, emp);
        double outOfHours = emp.getOutOfHoursPct() != null ? emp.getOutOfHoursPct() : 0.0;
        double focusScore = Math.max(0.0, 100.0 - (overdueTasks * 15.0) - (workloadScore * 0.3) - (outOfHours * 0.2));

        // Save daily snapshot
        EmployeeWorkloadSnapshot snapshot = new EmployeeWorkloadSnapshot();
        snapshot.setEmployee(emp);
        snapshot.setSnapshotDate(date);
        snapshot.setWorkloadScore(workloadScore);
        snapshot.setBurnoutRisk(risk);
        snapshot.setTasksOpen((int) openTasks);
        snapshot.setTasksOverdue((int) overdueTasks);
        
        snapshot.setOutOfHoursPct(outOfHours);
        snapshot.setCycleTimeAvg(cycleTime);
        
        snapshotRepository.save(snapshot);

        // Update employee cached properties
        emp.setWorkloadScore(workloadScore);
        emp.setBurnoutRisk(risk);
        emp.setFocusScore(focusScore);
        emp.setOverdueRatio(overdueRatio);
        emp.setOutOfHoursPct(outOfHours);
        emp.setAvgCycleTimeDays(cycleTime);
        emp.setTasksShippedThisMonth(countCompletedThisMonth(employeeTasks, date));
        emp.setStreakDays(emp.getStreakDays() != null ? emp.getStreakDays() : 0);
        emp.setContributionScore(calculateContributionScore(employeeTasks));
        
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

    private double calculateWorkloadScore(List<Task> tasks) {
        return Math.min(100.0, tasks.stream()
                .filter(task -> task.getStatus() != com.aiworkforce.core.enums.TaskStatus.DONE)
                .mapToDouble(task -> {
                    double storyPoints = task.getStoryPoints() != null ? task.getStoryPoints() * 6.0 : 0.0;
                    double estimatedHours = task.getEstimatedHours() * 1.5;
                    double priorityWeight = switch (task.getPriority()) {
                        case CRITICAL -> 18.0;
                        case HIGH -> 12.0;
                        case MEDIUM -> 7.0;
                        case LOW -> 3.0;
                    };
                    double overdueWeight = task.getStatus() == com.aiworkforce.core.enums.TaskStatus.OVERDUE ? 20.0 : 0.0;
                    return storyPoints + estimatedHours + priorityWeight + overdueWeight;
                })
                .sum());
    }

    private double calculateAverageCycleTime(List<Task> tasks, Employee employee) {
        return tasks.stream()
                .filter(task -> task.getCycleTimeDays() != null)
                .mapToDouble(Task::getCycleTimeDays)
                .average()
                .orElse(employee.getAvgCycleTimeDays() != null ? employee.getAvgCycleTimeDays() : 0.0);
    }

    private int countCompletedThisMonth(List<Task> tasks, LocalDate snapshotDate) {
        LocalDate monthStart = snapshotDate.withDayOfMonth(1);
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        return (int) tasks.stream()
                .filter(task -> task.getStatus() == com.aiworkforce.core.enums.TaskStatus.DONE)
                .filter(task -> task.getCompletedAt() == null || !task.getCompletedAt().isBefore(monthStartDateTime))
                .count();
    }

    private double calculateContributionScore(List<Task> tasks) {
        double completedPoints = tasks.stream()
                .filter(task -> task.getStatus() == com.aiworkforce.core.enums.TaskStatus.DONE)
                .mapToDouble(task -> task.getStoryPoints() != null ? task.getStoryPoints() : 1.0)
                .sum();
        double completedTaskScores = tasks.stream()
                .filter(task -> task.getStatus() == com.aiworkforce.core.enums.TaskStatus.DONE)
                .filter(task -> task.getTaskScore() != null)
                .mapToDouble(Task::getTaskScore)
                .sum();
        return Math.min(100.0, (completedPoints * 8.0) + completedTaskScores);
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
