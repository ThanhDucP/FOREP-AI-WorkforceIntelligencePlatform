package com.aiworkforce.event.repository;

import com.aiworkforce.event.entity.WorkloadEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface WorkloadEventRepository extends JpaRepository<WorkloadEvent, UUID> {
    List<WorkloadEvent> findByEmployeeId(UUID employeeId);
}
