package com.aiworkforce.ai.repository;

import com.aiworkforce.ai.entity.AISuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AISuggestionRepository extends JpaRepository<AISuggestion, UUID> {
    List<AISuggestion> findBySprintNumberOrderByCreatedAtDesc(Integer sprintNumber);
    List<AISuggestion> findByIsAdopted(Boolean isAdopted);
    List<AISuggestion> findBySourceEmployeeIdOrTargetEmployeeIdOrderByCreatedAtDesc(UUID sourceEmployeeId, UUID targetEmployeeId);
    List<AISuggestion> findBySourceEmployeeIdInOrTargetEmployeeIdInOrderByCreatedAtDesc(List<UUID> sourceEmployeeIds, List<UUID> targetEmployeeIds);
}
