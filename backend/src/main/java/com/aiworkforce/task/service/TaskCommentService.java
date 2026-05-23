package com.aiworkforce.task.service;

import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.event.entity.WorkloadEvent;
import com.aiworkforce.event.publisher.EventPublisher;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.task.dto.TaskCommentRequest;
import com.aiworkforce.task.dto.TaskCommentResponse;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.entity.TaskComment;
import com.aiworkforce.task.repository.TaskCommentRepository;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final EventPublisher eventPublisher;

    public List<TaskCommentResponse> getCommentsByTaskId(UUID taskId) {
        // Verify task exists
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskCommentResponse addComment(UUID taskId, TaskCommentRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        
        Employee author = employeeRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getAuthorId()));

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setContent(request.getContent());

        TaskComment savedComment = taskCommentRepository.save(comment);

        // Publish TASK_COMMENT_ADDED workload event
        WorkloadEvent event = new WorkloadEvent();
        event.setEventType(EventType.TASK_COMMENT_ADDED);
        event.setTask(task);
        event.setEmployee(task.getAssignee()); // employee whose task is commented on
        event.setActorId(author.getId()); // employee who wrote the comment
        event.setEventDetails("Comment added by " + author.getFirstName() + " " + author.getLastName() + ": " + request.getContent());
        eventPublisher.publishEvent(event);

        return mapToResponse(savedComment);
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        taskCommentRepository.delete(comment);
    }

    private TaskCommentResponse mapToResponse(TaskComment comment) {
        if (comment == null) return null;
        
        String authorName = "";
        String authorAvatar = "";
        if (comment.getAuthor() != null) {
            authorName = comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName();
            authorAvatar = comment.getAuthor().getAvatarInitials();
        }

        return TaskCommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTask() != null ? comment.getTask().getId() : null)
                .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                .authorName(authorName)
                .authorAvatarInitials(authorAvatar)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
