package com.aiworkforce.task.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.task.dto.TaskCommentRequest;
import com.aiworkforce.task.dto.TaskCommentResponse;
import com.aiworkforce.task.service.TaskCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;
    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> getComments(@PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(taskCommentService.getCommentsByTaskId(taskId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskCommentResponse>> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskCommentRequest request) {
        
        // If authorId is not provided, populate with current logged-in employee
        if (request.getAuthorId() == null) {
            Employee currentEmployee = employeeService.getCurrentEmployee();
            request.setAuthorId(currentEmployee.getId());
        }
        
        return ResponseEntity.ok(ApiResponse.success(taskCommentService.addComment(taskId, request)));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId) {
        taskCommentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
