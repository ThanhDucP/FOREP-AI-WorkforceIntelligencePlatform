package com.aiworkforce.core.service;

import com.aiworkforce.core.dto.NotificationResponse;
import com.aiworkforce.core.entity.Notification;
import com.aiworkforce.core.enums.NotificationType;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.core.repository.NotificationRepository;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private AccessPolicyService accessPolicyService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createNotification_PersistsUnreadNotificationForRecipient() {
        UUID recipientId = UUID.randomUUID();
        UUID relatedTaskId = UUID.randomUUID();
        UUID relatedEmployeeId = UUID.randomUUID();
        Employee recipient = employee(recipientId);

        when(employeeRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.createNotification(
                recipientId,
                NotificationType.AI_INSIGHT,
                "Burnout risk detected",
                "High severity AI insight was generated.",
                relatedTaskId,
                relatedEmployeeId
        );

        assertEquals(recipientId, response.getRecipientId());
        assertEquals("Linh Pham", response.getRecipientName());
        assertEquals(NotificationType.AI_INSIGHT, response.getType());
        assertEquals("Burnout risk detected", response.getTitle());
        assertFalse(response.getIsRead());
        assertEquals(relatedTaskId, response.getRelatedTaskId());
        assertEquals(relatedEmployeeId, response.getRelatedEmployeeId());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(recipient, captor.getValue().getRecipient());
        assertFalse(captor.getValue().getIsRead());
    }

    @Test
    void createNotification_ThrowsWhenRecipientDoesNotExist() {
        UUID recipientId = UUID.randomUUID();
        when(employeeRepository.findById(recipientId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.createNotification(
                recipientId,
                NotificationType.AI_INSIGHT,
                "Title",
                "Message",
                null,
                null
        ));
    }

    @Test
    void markAsRead_SetsNotificationRead() {
        UUID notificationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Notification notification = notification(notificationId, false, recipientId);
        when(employeeService.getCurrentEmployee()).thenReturn(employee(recipientId));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(notificationId);

        assertTrue(response.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAllAsRead_SavesEveryUnreadNotification() {
        UUID employeeId = UUID.randomUUID();
        Notification first = notification(UUID.randomUUID(), false, employeeId);
        Notification second = notification(UUID.randomUUID(), false, employeeId);
        when(employeeService.getCurrentEmployee()).thenReturn(employee(employeeId));

        when(notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(employeeId, false))
                .thenReturn(List.of(first, second));

        notificationService.markAllAsRead(employeeId);

        assertTrue(first.getIsRead());
        assertTrue(second.getIsRead());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void deleteNotification_DeletesExistingNotification() {
        UUID notificationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Notification notification = notification(notificationId, false, recipientId);
        when(employeeService.getCurrentEmployee()).thenReturn(employee(recipientId));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(notificationId);

        verify(notificationRepository).delete(notification);
    }

    private Employee employee(UUID employeeId) {
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("Linh");
        employee.setLastName("Pham");
        return employee;
    }

    private Notification notification(UUID notificationId, boolean isRead, UUID recipientId) {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setRecipient(employee(recipientId));
        notification.setType(NotificationType.AI_INSIGHT);
        notification.setTitle("Burnout risk detected");
        notification.setMessage("High severity AI insight was generated.");
        notification.setIsRead(isRead);
        return notification;
    }
}