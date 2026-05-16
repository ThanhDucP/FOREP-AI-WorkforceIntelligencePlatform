package com.aiworkforce.platform.event.repository;

import com.aiworkforce.platform.event.entity.WorkloadEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkloadEventRepository extends JpaRepository<WorkloadEvent, UUID> {
}
