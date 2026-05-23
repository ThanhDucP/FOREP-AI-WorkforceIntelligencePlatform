package com.aiworkforce.identity.repository;

import com.aiworkforce.identity.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByManagerId(UUID managerId);
    List<Team> findByOrganizationId(UUID organizationId);
}
