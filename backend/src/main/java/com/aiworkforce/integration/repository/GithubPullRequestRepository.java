package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.GithubPullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubPullRequestRepository extends JpaRepository<GithubPullRequest, UUID> {
    Optional<GithubPullRequest> findByConfigIdAndRepositoryFullNameIgnoreCaseAndNumber(UUID configId, String repositoryFullName, Integer number);
    List<GithubPullRequest> findByTeamIdOrderByProviderUpdatedAtDesc(UUID teamId);
}
