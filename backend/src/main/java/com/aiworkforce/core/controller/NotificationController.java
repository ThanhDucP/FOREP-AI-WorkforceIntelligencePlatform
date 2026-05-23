package com.aiworkforce.core.controller;

import com.aiworkforce.core.dto.NotificationResponse;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.core.service.NotificationService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications() {
        Employee current = employeeService.getCurrentEmployee();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsForEmployee(current.getId())));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications() {
        Employee current = employeeService.getCurrentEmployee();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotificationsForEmployee(current.getId())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        Employee current = employeeService.getCurrentEmployee();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(current.getId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAsRead(id)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Employee current = employeeService.getCurrentEmployee();
        notificationService.markAllAsRead(current.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
