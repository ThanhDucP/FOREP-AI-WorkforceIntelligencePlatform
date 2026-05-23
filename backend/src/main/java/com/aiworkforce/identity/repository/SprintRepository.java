package com.aiworkforce.identity.repository;
import com.aiworkforce.identity.entity.Sprint;
import com.aiworkforce.core.enums.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByOrganizationId(UUID organizationId);
    Optional<Sprint> findFirstByOrganizationIdAndStatusOrderBySprintNumberDesc(UUID orgId, SprintStatus status);
    Optional<Sprint> findFirstByStatusOrderBySprintNumberDesc(SprintStatus status);
    Optional<Sprint> findTopByOrderBySprintNumberDesc();
}
