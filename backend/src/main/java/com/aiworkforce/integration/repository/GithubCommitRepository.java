package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.GithubCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubCommitRepository extends JpaRepository<GithubCommit, UUID> {
    Optional<GithubCommit> findByConfigIdAndRepositoryFullNameIgnoreCaseAndSha(UUID configId, String repositoryFullName, String sha);
    List<GithubCommit> findByTeamIdOrderByCommittedAtDesc(UUID teamId);
    List<GithubCommit> findByProjectIdOrderByCommittedAtDesc(UUID projectId);
    List<GithubCommit> findByTeamOrganizationIdOrderByCommittedAtDesc(UUID organizationId);
}