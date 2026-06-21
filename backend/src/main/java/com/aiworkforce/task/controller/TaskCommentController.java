package com.aiworkforce.task.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.core.security.ReadOnlyScopeGuard;
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
    private final ReadOnlyScopeGuard readOnlyScopeGuard;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> getComments(@PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(taskCommentService.getCommentsByTaskId(taskId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskCommentResponse>> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskCommentRequest request) {
        
        readOnlyScopeGuard.block("CREATE_TASK_COMMENT", "Task", taskId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId) {
        readOnlyScopeGuard.block("DELETE_TASK_COMMENT", "TaskComment", commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}


