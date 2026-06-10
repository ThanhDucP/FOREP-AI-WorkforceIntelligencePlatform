package com.aiworkforce.task.controller;

import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.task.dto.TaskRequest;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Task>> createTask(@RequestBody TaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.createTask(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks() {
        return ResponseEntity.ok(ApiResponse.success(taskService.getAllTasks()));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<List<Task>>> getMyTasks() {
        return ResponseEntity.ok(ApiResponse.success(taskService.getMyTasks()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @employeeService.getCurrentEmployee().getId().equals(#employeeId)")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByEmployee(employeeId)));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByTeam(teamId)));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByOrganization(organizationId)));
    }

    @GetMapping("/managed-teams")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksForManagedTeams() {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksForManagedTeams()));
    }

    @GetMapping("/reported-by/{reporterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @employeeService.getCurrentEmployee().getId().equals(#reporterId)")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByReporter(@PathVariable UUID reporterId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByReporter(reporterId)));
    }

    @GetMapping("/sprint/{sprintId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksBySprint(@PathVariable UUID sprintId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksBySprint(sprintId)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByStatus(@PathVariable TaskStatus status) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByStatus(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Task>> updateTask(@PathVariable UUID id, @RequestBody TaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.updateTask(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Task>> updateTaskStatus(@PathVariable UUID id, @RequestParam TaskStatus status) {
        return ResponseEntity.ok(ApiResponse.success(taskService.updateTaskStatus(id, status)));
    }

    @PostMapping("/{id}/assess")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Task>> assessTask(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.assessTask(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
