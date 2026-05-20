package com.aiworkforce.timetracking.service;

import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.core.enums.LeaveStatus;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.event.entity.WorkloadEvent;
import com.aiworkforce.event.publisher.EventPublisher;
import com.aiworkforce.timetracking.dto.LeaveRequestRequest;
import com.aiworkforce.timetracking.dto.LeaveRequestResponse;
import com.aiworkforce.timetracking.entity.LeaveRequest;
import com.aiworkforce.timetracking.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeService employeeService;
    private final EventPublisher eventPublisher;

    @Transactional
    public LeaveRequestResponse createLeaveRequest(LeaveRequestRequest request) {
        Employee employee = employeeService.getCurrentEmployee();

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("Ngày kết thúc nghỉ phép không thể trước ngày bắt đầu.");
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setReason(request.getReason());
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(leave);
        
        publishEvent(EventType.LEAVE_REQUESTED, employee, "Đăng ký nghỉ phép: " + request.getReason());
        log.info("Leave request created for employee {} from {} to {}", employee.getId(), leave.getStartDate(), leave.getEndDate());
        
        return mapToResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse approveLeaveRequest(UUID leaveId) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu nghỉ phép không tồn tại."));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Yêu cầu nghỉ phép đã được xử lý trước đó.");
        }

        leave.setStatus(LeaveStatus.APPROVED);
        LeaveRequest saved = leaveRequestRepository.save(leave);

        // Publish event to adjust workload score
        publishEvent(EventType.LEAVE_APPROVED, leave.getEmployee(), "Nghỉ phép được phê duyệt: " + leave.getReason());
        log.info("Leave request {} approved by manager", leaveId);

        return mapToResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(UUID leaveId) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu nghỉ phép không tồn tại."));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Yêu cầu nghỉ phép đã được xử lý trước đó.");
        }

        leave.setStatus(LeaveStatus.REJECTED);
        LeaveRequest saved = leaveRequestRepository.save(leave);
        log.info("Leave request {} rejected by manager", leaveId);

        return mapToResponse(saved);
    }

    public List<LeaveRequestResponse> getMyLeaveRequests() {
        Employee employee = employeeService.getCurrentEmployee();
        return leaveRequestRepository.findByEmployeeId(employee.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestResponse> getAllLeaveRequests() {
        return leaveRequestRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void publishEvent(EventType type, Employee employee, String details) {
        WorkloadEvent event = new WorkloadEvent();
        event.setEventType(type);
        event.setEmployee(employee);
        event.setEventDetails(details);
        eventPublisher.publishEvent(event);
    }

    private LeaveRequestResponse mapToResponse(LeaveRequest leave) {
        if (leave == null) return null;
        return LeaveRequestResponse.builder()
                .id(leave.getId())
                .reason(leave.getReason())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .status(leave.getStatus().name())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName())
                .build();
    }
}
