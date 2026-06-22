package com.aiworkforce.task.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.task.dto.TaskCommentResponse;
import com.aiworkforce.task.service.TaskCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> getComments(@PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(taskCommentService.getCommentsByTaskId(taskId)));
    }
}
