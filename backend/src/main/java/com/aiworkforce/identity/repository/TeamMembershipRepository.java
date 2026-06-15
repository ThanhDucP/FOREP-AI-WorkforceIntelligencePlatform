package com.aiworkforce.identity.repository;

import com.aiworkforce.core.enums.TeamMembershipStatus;
import com.aiworkforce.identity.entity.TeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {
    Optional<TeamMembership> findByEmployeeIdAndStatus(UUID employeeId, TeamMembershipStatus status);
    Optional<TeamMembership> findByEmployeeIdAndTeamIdAndStatus(UUID employeeId, UUID teamId, TeamMembershipStatus status);
    List<TeamMembership> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
    List<TeamMembership> findByTeamIdAndStatus(UUID teamId, TeamMembershipStatus status);
    boolean existsByEmployeeIdAndTeamIdAndStatus(UUID employeeId, UUID teamId, TeamMembershipStatus status);
}
