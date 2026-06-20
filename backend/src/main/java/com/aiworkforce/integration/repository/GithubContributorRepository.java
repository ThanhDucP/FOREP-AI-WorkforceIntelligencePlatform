package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.GithubContributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubContributorRepository extends JpaRepository<GithubContributor, UUID> {
    Optional<GithubContributor> findByConfigIdAndRepositoryFullNameIgnoreCaseAndLoginIgnoreCase(UUID configId, String repositoryFullName, String login);
    List<GithubContributor> findByTeamIdOrderByContributionsDesc(UUID teamId);
}
