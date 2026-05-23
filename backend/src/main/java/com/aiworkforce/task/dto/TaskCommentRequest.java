package com.aiworkforce.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentRequest {
    @NotBlank(message = "Comment content is required")
    private String content;
    
    private UUID authorId;
}
