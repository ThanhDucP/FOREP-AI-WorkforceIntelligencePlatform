package com.aiworkforce.identity.repository;

import com.aiworkforce.identity.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByTeamId(UUID teamId);
    List<Project> findByOrganizationId(UUID organizationId);
    Optional<Project> findByGithubRepositoryIgnoreCase(String githubRepository);
    Optional<Project> findByJiraDomainIgnoreCaseAndJiraProjectKeyIgnoreCase(String jiraDomain, String jiraProjectKey);
}
