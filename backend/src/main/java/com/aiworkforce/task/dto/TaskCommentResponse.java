package com.aiworkforce.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentResponse {
    private UUID id;
    private UUID taskId;
    private UUID authorId;
    private String authorName;
    private String authorAvatarInitials;
    private String content;
    private LocalDateTime createdAt;
}
