package com.aiworkforce.core.controller;

import com.aiworkforce.core.dto.NotificationResponse;
import com.aiworkforce.core.enums.NotificationType;
import com.aiworkforce.core.service.NotificationService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmployeeService employeeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new NotificationController(notificationService, employeeService)).build();
    }

    @Test
    void getNotifications_ReturnsCurrentEmployeeNotifications() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = currentEmployee(employeeId);
        NotificationResponse notification = notification(employeeId, false);

        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(notificationService.getNotificationsForEmployee(employeeId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data[0].id").value(notification.getId().toString()))
                .andExpect(jsonPath("$.data[0].recipientId").value(employeeId.toString()))
                .andExpect(jsonPath("$.data[0].type").value("AI_INSIGHT"))
                .andExpect(jsonPath("$.data[0].title").value("Burnout risk detected"))
                .andExpect(jsonPath("$.data[0].isRead").value(false));

        verify(employeeService).getCurrentEmployee();
        verify(notificationService).getNotificationsForEmployee(employeeId);
    }

    @Test
    void getUnreadNotifications_ReturnsUnreadItemsForCurrentEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(employeeService.getCurrentEmployee()).thenReturn(currentEmployee(employeeId));
        when(notificationService.getUnreadNotificationsForEmployee(employeeId)).thenReturn(List.of(notification(employeeId, false)));

        mockMvc.perform(get("/api/v1/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].isRead").value(false));

        verify(notificationService).getUnreadNotificationsForEmployee(employeeId);
    }

    @Test
    void getUnreadCount_ReturnsUnreadCountForCurrentEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(employeeService.getCurrentEmployee()).thenReturn(currentEmployee(employeeId));
        when(notificationService.getUnreadCount(employeeId)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));

        verify(notificationService).getUnreadCount(employeeId);
    }

    @Test
    void markAsRead_ReturnsUpdatedNotification() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationResponse response = notification(employeeId, true);
        response.setId(notificationId);

        when(notificationService.markAsRead(notificationId)).thenReturn(response);

        mockMvc.perform(put("/api/v1/notifications/{id}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.data.isRead").value(true));

        verify(notificationService).markAsRead(notificationId);
    }

    @Test
    void markAllAsRead_MarksCurrentEmployeeNotifications() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(employeeService.getCurrentEmployee()).thenReturn(currentEmployee(employeeId));

        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAllAsRead(employeeId);
    }

    @Test
    void deleteNotification_DeletesById() throws Exception {
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).deleteNotification(notificationId);
    }

    private Employee currentEmployee(UUID employeeId) {
        Employee employee = new Employee();
        employee.setId(employeeId);
        return employee;
    }

    private NotificationResponse notification(UUID employeeId, boolean isRead) {
        return NotificationResponse.builder()
                .id(UUID.randomUUID())
                .recipientId(employeeId)
                .recipientName("Linh Pham")
                .type(NotificationType.AI_INSIGHT)
                .title("Burnout risk detected")
                .message("High severity AI insight was generated.")
                .isRead(isRead)
                .relatedEmployeeId(employeeId)
                .build();
    }
}