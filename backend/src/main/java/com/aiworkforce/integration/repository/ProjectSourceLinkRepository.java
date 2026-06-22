package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.ProjectSourceLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectSourceLinkRepository extends JpaRepository<ProjectSourceLink, UUID> {
    Optional<ProjectSourceLink> findByJiraProjectIdAndGithubRepositoryId(UUID jiraProjectId, UUID githubRepositoryId);
    List<ProjectSourceLink> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);
    List<ProjectSourceLink> findByTeamIdOrderByUpdatedAtDesc(UUID teamId);
    List<ProjectSourceLink> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}