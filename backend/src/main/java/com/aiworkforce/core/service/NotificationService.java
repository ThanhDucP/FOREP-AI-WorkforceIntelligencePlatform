package com.aiworkforce.core.service;

import com.aiworkforce.core.dto.NotificationResponse;
import com.aiworkforce.core.entity.Notification;
import com.aiworkforce.core.enums.NotificationType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.repository.NotificationRepository;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final AccessPolicyService accessPolicyService;

    public List<NotificationResponse> getNotificationsForEmployee(UUID employeeId) {
        Employee current = employeeService.getCurrentEmployee();
        ensureSelfOrAdmin(current, employeeId);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadNotificationsForEmployee(UUID employeeId) {
        Employee current = employeeService.getCurrentEmployee();
        ensureSelfOrAdmin(current, employeeId);
        return notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(employeeId, false).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getAllNotifications() {
        Employee current = employeeService.getCurrentEmployee();
        if (!accessPolicyService.isAdmin(current)) {
            throw new BusinessException("Only admins can view all notifications");
        }
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(UUID employeeId) {
        Employee current = employeeService.getCurrentEmployee();
        ensureSelfOrAdmin(current, employeeId);
        return notificationRepository.countByRecipientIdAndIsRead(employeeId, false);
    }

    @Transactional
    public NotificationResponse createNotification(UUID recipientId, NotificationType type, String title, String message, UUID relatedTaskId, UUID relatedEmployeeId) {
        Employee recipient = employeeRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient employee not found with id: " + recipientId));

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setRelatedTaskId(relatedTaskId);
        notification.setRelatedEmployeeId(relatedEmployeeId);

        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        Notification notification = getNotificationAndEnsureOwnerOrAdmin(id);
        notification.setIsRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(UUID employeeId) {
        Employee current = employeeService.getCurrentEmployee();
        ensureSelfOrAdmin(current, employeeId);
        List<Notification> unread = notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(employeeId, false);
        for (Notification n : unread) {
            n.setIsRead(true);
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void deleteNotification(UUID id) {
        Notification notification = getNotificationAndEnsureOwnerOrAdmin(id);
        notificationRepository.delete(notification);
    }

    private Notification getNotificationAndEnsureOwnerOrAdmin(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        Employee current = employeeService.getCurrentEmployee();
        UUID recipientId = notification.getRecipient() != null ? notification.getRecipient().getId() : null;
        ensureSelfOrAdmin(current, recipientId);
        return notification;
    }

    private void ensureSelfOrAdmin(Employee current, UUID ownerId) {
        if (current == null || current.getId() == null || ownerId == null) {
            throw new BusinessException("Notification ownership cannot be verified");
        }
        if (!current.getId().equals(ownerId) && !accessPolicyService.isAdmin(current)) {
            throw new BusinessException("Current user does not own this notification");
        }
    }

    public NotificationResponse mapToResponse(Notification notification) {
        if (notification == null) return null;

        String recipientName = "";
        if (notification.getRecipient() != null) {
            recipientName = notification.getRecipient().getFirstName() + " " + notification.getRecipient().getLastName();
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient() != null ? notification.getRecipient().getId() : null)
                .recipientName(recipientName)
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .relatedTaskId(notification.getRelatedTaskId())
                .relatedEmployeeId(notification.getRelatedEmployeeId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}