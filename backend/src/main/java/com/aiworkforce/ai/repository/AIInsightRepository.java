package com.aiworkforce.ai.repository;

import com.aiworkforce.ai.entity.AIInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AIInsightRepository extends JpaRepository<AIInsight, UUID> {
    List<AIInsight> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
}
