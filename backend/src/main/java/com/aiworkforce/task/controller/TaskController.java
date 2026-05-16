package com.aiworkforce.task.controller;

import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.task.dto.TaskRequest;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTask(@RequestBody TaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.createTask(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks() {
        return ResponseEntity.ok(ApiResponse.success(taskService.getAllTasks()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Task>> updateTaskStatus(@PathVariable UUID id, @RequestParam TaskStatus status) {
        return ResponseEntity.ok(ApiResponse.success(taskService.updateTaskStatus(id, status)));
    }
}
