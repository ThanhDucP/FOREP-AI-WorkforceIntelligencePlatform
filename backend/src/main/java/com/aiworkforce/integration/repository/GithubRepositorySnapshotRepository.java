package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.GithubRepositorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubRepositorySnapshotRepository extends JpaRepository<GithubRepositorySnapshot, UUID> {
    Optional<GithubRepositorySnapshot> findByConfigIdAndFullNameIgnoreCase(UUID configId, String fullName);
    List<GithubRepositorySnapshot> findByTeamIdOrderByProviderUpdatedAtDesc(UUID teamId);
    List<GithubRepositorySnapshot> findByProjectIdOrderByProviderUpdatedAtDesc(UUID projectId);
    List<GithubRepositorySnapshot> findByTeamOrganizationIdOrderByProviderUpdatedAtDesc(UUID organizationId);
}