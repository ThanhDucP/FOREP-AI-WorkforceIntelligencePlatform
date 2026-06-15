package com.aiworkforce.core.config;

import com.aiworkforce.ai.entity.AISuggestion;
import com.aiworkforce.ai.repository.AISuggestionRepository;
import com.aiworkforce.analytics.service.WorkloadSnapshotService;
import com.aiworkforce.core.entity.Notification;
import com.aiworkforce.core.enums.*;
import com.aiworkforce.core.repository.NotificationRepository;
import com.aiworkforce.identity.entity.*;
import com.aiworkforce.identity.repository.*;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.entity.TaskComment;
import com.aiworkforce.task.repository.TaskCommentRepository;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final NotificationRepository notificationRepository;
    private final AISuggestionRepository aiSuggestionRepository;
    private final WorkloadSnapshotService workloadSnapshotService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (accountRepository.findByEmail("admin@forep.local").isPresent()) {
            log.info("Database already seeded. Skipping seeder...");
            return;
        }

        log.info("Starting rich database seeding based on Worklens AI schema...");

        // 1. Fetch Roles
        Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        Role managerRole = roleRepository.findByName(RoleType.MANAGER)
                .orElseThrow(() -> new RuntimeException("MANAGER role not found"));
        Role employeeRole = roleRepository.findByName(RoleType.EMPLOYEE)
                .orElseThrow(() -> new RuntimeException("EMPLOYEE role not found"));

        // 2. Create Organization
        Organization organization = new Organization();
        organization.setName("Apex AI Solutions");
        organization.setDomain("apexai.com");
        organization.setLogoUrl("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=60");
        organization = organizationRepository.save(organization);

        // 3. Create Sprints
        LocalDate today = LocalDate.now();
        
        // Sprint 23 (Completed)
        Sprint sprint23 = new Sprint();
        sprint23.setSprintNumber(23);
        sprint23.setName("Sprint #23 (Completed)");
        sprint23.setStartDate(today.minusDays(20));
        sprint23.setEndDate(today.minusDays(6));
        sprint23.setCommittedStoryPoints(48);
        sprint23.setCompletedStoryPoints(45);
        sprint23.setVelocityConfidence(0.92);
        sprint23.setStatus(SprintStatus.COMPLETED);
        sprint23.setOrganization(organization);
        sprintRepository.save(sprint23);

        // Sprint 24 (Active)
        Sprint sprint24 = new Sprint();
        sprint24.setSprintNumber(24);
        sprint24.setName("Sprint #24 (Active)");
        sprint24.setStartDate(today.minusDays(5));
        sprint24.setEndDate(today.plusDays(9));
        sprint24.setCommittedStoryPoints(52);
        sprint24.setCompletedStoryPoints(12);
        sprint24.setVelocityConfidence(0.88);
        sprint24.setStatus(SprintStatus.ACTIVE);
        sprint24.setOrganization(organization);
        sprint24 = sprintRepository.save(sprint24);

        // Sprint 25 (Planning)
        Sprint sprint25 = new Sprint();
        sprint25.setSprintNumber(25);
        sprint25.setName("Sprint #25 (Planning)");
        sprint25.setStartDate(today.plusDays(10));
        sprint25.setEndDate(today.plusDays(24));
        sprint25.setCommittedStoryPoints(0);
        sprint25.setCompletedStoryPoints(0);
        sprint25.setVelocityConfidence(0.85);
        sprint25.setStatus(SprintStatus.PLANNING);
        sprint25.setOrganization(organization);
        sprintRepository.save(sprint25);

        // 4. Seed Employee & Manager accounts
        List<Employee> seededEmployees = new ArrayList<>();

        // Admin
        Employee adminEmp = createEmployeeAccount("admin@forep.local", "AdminPassword123", "System", "Admin", "IT Director", "IT", "SA", adminRole, null);
        seededEmployees.add(adminEmp);

        // Manager 1 (Engineering Manager)
        Employee johnDoe = createEmployeeAccount("john@forep.local", "Password123", "John", "Doe", "Engineering Manager", "Engineering", "JD", managerRole, null);
        seededEmployees.add(johnDoe);

        // Manager 2 (Product Manager)
        Employee graceHopper = createEmployeeAccount("grace@forep.local", "Password123", "Grace", "Hopper", "Product Director", "Product", "GH", managerRole, null);
        seededEmployees.add(graceHopper);

        // Employees (Engineering Team)
        Employee aliceSmith = createEmployeeAccount("alice@forep.local", "Password123", "Alice", "Smith", "Senior Frontend Engineer", "Engineering", "AS", employeeRole, null);
        Employee bobJohnson = createEmployeeAccount("bob@forep.local", "Password123", "Bob", "Johnson", "Backend Developer", "Engineering", "BJ", employeeRole, null);
        Employee charlieBrown = createEmployeeAccount("charlie@forep.local", "Password123", "Charlie", "Brown", "DevOps Engineer", "Engineering", "CB", employeeRole, null);
        Employee davidMiller = createEmployeeAccount("david@forep.local", "Password123", "David", "Miller", "Lead DB Architect", "Engineering", "DM", employeeRole, null);
        Employee elenaRostova = createEmployeeAccount("elena@forep.local", "Password123", "Elena", "Rostova", "Mobile App Specialist", "Engineering", "ER", employeeRole, null);

        // Employee (Product/Design Team)
        Employee frankCastle = createEmployeeAccount("frank@forep.local", "Password123", "Frank", "Castle", "Lead UI/UX Designer", "Product", "FC", employeeRole, null);

        seededEmployees.add(aliceSmith);
        seededEmployees.add(bobJohnson);
        seededEmployees.add(charlieBrown);
        seededEmployees.add(davidMiller);
        seededEmployees.add(elenaRostova);
        seededEmployees.add(frankCastle);

        // 5. Create Teams and associate employees
        Team engineeringTeam = new Team();
        engineeringTeam.setName("Core Engineering");
        engineeringTeam.setDescription("Building the core platform API and cloud infra");
        engineeringTeam.setManager(johnDoe);
        engineeringTeam.setOrganization(organization);
        engineeringTeam.setCapacityUsedPct(92.0);
        engineeringTeam.setUtilizationScore(88.5);
        engineeringTeam = teamRepository.save(engineeringTeam);

        Team productTeam = new Team();
        productTeam.setName("Product & Design");
        productTeam.setDescription("UI/UX layout, feature specifications, user research");
        productTeam.setManager(graceHopper);
        productTeam.setOrganization(organization);
        productTeam.setCapacityUsedPct(65.0);
        productTeam.setUtilizationScore(72.0);
        productTeam = teamRepository.save(productTeam);

        // Update employee's team associations
        johnDoe.setTeam(engineeringTeam);
        aliceSmith.setTeam(engineeringTeam);
        bobJohnson.setTeam(engineeringTeam);
        charlieBrown.setTeam(engineeringTeam);
        davidMiller.setTeam(engineeringTeam);
        elenaRostova.setTeam(engineeringTeam);

        graceHopper.setTeam(productTeam);
        frankCastle.setTeam(productTeam);

        employeeRepository.saveAll(List.of(johnDoe, aliceSmith, bobJohnson, charlieBrown, davidMiller, elenaRostova, graceHopper, frankCastle));

        // 6. Create Tasks for Sprint 24 (Active)
        // High Workload tasks for David Miller
        Task davidTask1 = createTask("Tối ưu hóa các truy vấn SQL database", "Cần tối ưu hóa lại index và nén phân vùng bảng để cải thiện tốc độ API thêm 30%.", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, today.plusDays(2), 24, davidMiller, johnDoe, sprint24, engineeringTeam, 8);
        Task davidTask2 = createTask("Thiết lập hệ thống sao lưu dự phòng Multi-Region", "Tạo luồng tự động sao lưu dữ liệu sang AWS S3 Singapore để tăng khả năng chống chịu thảm họa.", TaskStatus.OVERDUE, TaskPriority.CRITICAL, today.minusDays(1), 16, davidMiller, johnDoe, sprint24, engineeringTeam, 5);
        Task davidTask3 = createTask("Nâng cấp cụm cơ sở dữ liệu Postgres", "Cập nhật Postgres từ v14 lên v16 trong khung giờ bảo trì ban đêm.", TaskStatus.TODO, TaskPriority.MEDIUM, today.plusDays(5), 12, davidMiller, johnDoe, sprint24, engineeringTeam, 5);
        Task davidTask4 = createTask("Sửa lỗi rò rỉ bộ nhớ luồng đọc ghi", "Fix issue #412 liên quan đến việc giải phóng bộ đệm ghi đè luồng logs.", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, today.plusDays(1), 8, davidMiller, johnDoe, sprint24, engineeringTeam, 3);
        Task davidTask5 = createTask("Đánh giá bảo mật an toàn Database", "Chạy công cụ quét bảo mật SonarQube và vá các lỗ hổng SQL Injection tiềm ẩn.", TaskStatus.TODO, TaskPriority.LOW, today.plusDays(7), 8, davidMiller, johnDoe, sprint24, engineeringTeam, 2);

        // Elena Rostova
        createTask("Xây dựng trang Dashboard tổng quan trên iOS", "Thiết kế và code UI màn hình Heatmap hiển thị chỉ số kiệt sức của nhân sự trên điện thoại.", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, today.plusDays(3), 20, elenaRostova, johnDoe, sprint24, engineeringTeam, 5);
        createTask("Xử lý đồng bộ dữ liệu ngoại tuyến (Offline Sync)", "Triển khai SQLite cục bộ để cho phép lưu trữ tạm thời các thao tác khi mất mạng.", TaskStatus.OVERDUE, TaskPriority.CRITICAL, today.minusDays(2), 30, elenaRostova, johnDoe, sprint24, engineeringTeam, 8);
        createTask("Tích hợp SDK Thông báo đẩy (Apple Push Notifications)", "Cấu hình chứng chỉ APNS để gửi alert khi có cảnh báo kiệt sức cho quản lý.", TaskStatus.TODO, TaskPriority.MEDIUM, today.plusDays(6), 10, elenaRostova, johnDoe, sprint24, engineeringTeam, 3);

        // Bob Johnson (Low Workload)
        createTask("Tích hợp API gửi email tự động SendGrid", "Viết class Service kết nối SendGrid để gửi thư kích hoạt tài khoản.", TaskStatus.DONE, TaskPriority.LOW, today.minusDays(3), 8, bobJohnson, johnDoe, sprint24, engineeringTeam, 2);
        createTask("Viết unit tests cho module Xác thực JWT", "Tăng độ bao phủ code module Security lên tối thiểu 85%.", TaskStatus.IN_PROGRESS, TaskPriority.LOW, today.plusDays(4), 12, bobJohnson, johnDoe, sprint24, engineeringTeam, 3);

        // Alice Smith
        createTask("Cải tiến giao diện thiết lập cài đặt Heatmap", "Xây dựng các bộ lọc thời gian, phòng ban trên giao diện quản lý.", TaskStatus.DONE, TaskPriority.MEDIUM, today.minusDays(2), 16, aliceSmith, johnDoe, sprint24, engineeringTeam, 3);
        createTask("Tích hợp thư viện biểu đồ Recharts", "Vẽ đồ thị xu hướng workload 30 ngày trên Profile nhân viên.", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, today.plusDays(2), 18, aliceSmith, johnDoe, sprint24, engineeringTeam, 5);

        // Frank Castle
        createTask("Vẽ Wireframe module Quản lý Sprint của Admin", "Thiết kế các mockup giao diện admin, quản lý tài khoản và thiết lập hệ thống.", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, today.plusDays(4), 16, frankCastle, graceHopper, sprint24, productTeam, 5);

        // 7. Capture current workload snapshots from seeded task data
        for (Employee emp : seededEmployees) {
            workloadSnapshotService.captureSnapshotForEmployee(emp, today);
        }

        // 8. Create system notifications
        createNotification(davidMiller, NotificationType.BURNOUT_ALERT, "Cảnh báo nguy cơ kiệt sức!", "Điểm lượng công việc của bạn là 95, có 2 tác vụ quá hạn. Vui lòng liên hệ John Doe để cân đối lại.", davidTask2.getId(), null);
        createNotification(johnDoe, NotificationType.AI_INSIGHT, "Phân tích AI mới khả dụng", "Phát hiện sự bất cân bằng tải trong Sprint #24. David Miller đang bị quá tải nghiêm trọng.", null, davidMiller.getId());
        createNotification(aliceSmith, NotificationType.TASK_COMPLETED, "Tác vụ hoàn thành thành công", "Tác vụ 'Cải tiến giao diện thiết lập cài đặt Heatmap' đã được duyệt bởi John Doe.", null, null);

        // 9. Create task comments
        createComment(davidTask2, johnDoe, "David ơi, tác vụ này đã trễ hạn 1 ngày rồi. Bạn có gặp khó khăn gì không?");
        createComment(davidTask2, davidMiller, "Tôi đang bị vướng ở phần đồng bộ tài nguyên database Multi-region, mất rất nhiều thời gian giải quyết lỗi kết nối.");
        createComment(davidTask2, johnDoe, "Để tôi xem xét chuyển giao bớt việc nâng cấp Postgres cho Bob Johnson xử lý hỗ trợ nhé.");

        // 10. Create AI Recommendation Suggestions
        AISuggestion suggestion = new AISuggestion();
        suggestion.setSprintNumber(24);
        suggestion.setSuggestionType(SuggestionType.REBALANCE);
        suggestion.setConfidenceScore(0.94);
        suggestion.setSourceEmployeeId(davidMiller.getId());
        suggestion.setTargetEmployeeId(bobJohnson.getId());
        suggestion.setSourceTaskId(davidTask1.getId()); // suggest reassigning the SQL optimization task
        suggestion.setDescription("Chuyển giao tác vụ 'Tối ưu hóa các truy vấn SQL database' (8 Story Points) từ David Miller sang Bob Johnson để đưa điểm tải lượng của David về mức an toàn (65) và tăng hiệu suất sprint.");
        suggestion.setIsAdopted(false);
        aiSuggestionRepository.save(suggestion);

        log.info("Database seeding successfully completed.");
    }

    private Employee createEmployeeAccount(String email, String rawPassword, String first, String last, String title, String dept, String initials, Role role, Team team) {
        Account account = new Account();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setActive(true);
        account.setLocked(false);
        account.setFailedLoginAttempts(0);
        account = accountRepository.save(account);

        Employee employee = new Employee();
        employee.setAccount(account);
        employee.setFirstName(first);
        employee.setLastName(last);
        employee.setJobTitle(title);
        employee.setDepartment(dept);
        employee.setAvatarInitials(initials);
        employee.setTeam(team);
        
        // Default cached values
        employee.setWorkloadScore(20.0);
        employee.setBurnoutRisk(BurnoutRisk.NONE);
        employee.setContributionScore(80.0);
        employee.setOverdueRatio(0.0);
        employee.setOutOfHoursPct(5.0);
        employee.setAvgCycleTimeDays(2.0);
        employee.setTasksShippedThisMonth(4);
        employee.setStreakDays(3);
        employee.setFocusScore(95.0);

        return employeeRepository.save(employee);
    }

    private Task createTask(String title, String desc, TaskStatus status, TaskPriority priority, LocalDate due, int hours, Employee assignee, Employee reporter, Sprint sprint, Team team, int points) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(desc);
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(due.atStartOfDay());
        task.setEstimatedHours(hours);
        task.setAssignee(assignee);
        task.setReporter(reporter);
        task.setSprint(sprint);
        task.setTeam(team);
        task.setStoryPoints(points);
        task.setExternalTicketRef("APX-" + (1000 + (int)(Math.random() * 9000)));
        task.setCycleTimeDays(status == TaskStatus.DONE ? 3.2 : null);
        return taskRepository.save(task);
    }

    private void createNotification(Employee recipient, NotificationType type, String title, String message, UUID taskId, UUID employeeId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedTaskId(taskId);
        notification.setRelatedEmployeeId(employeeId);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private void createComment(Task task, Employee author, String content) {
        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setContent(content);
        taskCommentRepository.save(comment);
    }
}
