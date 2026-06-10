package com.aiworkforce.task.dto;
import com.aiworkforce.core.enums.TaskPriority;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TaskRequest {
    private String title;
    private String description;
    private TaskPriority priority;
    private LocalDateTime dueDate;
    private int estimatedHours;
    private UUID assigneeId;
    private UUID reporterId;
    private UUID teamId;
    private UUID sprintId;
    private String externalTicketRef;
    private Integer sprintNumber;
    private Integer storyPoints;
    private Integer difficultyScore;
    private Integer progressPercent;
    private String leadEvaluation;
    private Boolean agentAssess;
}
