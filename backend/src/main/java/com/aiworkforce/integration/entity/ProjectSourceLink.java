package com.aiworkforce.integration.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "project_source_link",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_source_link_jira_github",
                columnNames = {"jira_project_snapshot_id", "github_repository_snapshot_id"}
        )
)
@Getter
@Setter
public class ProjectSourceLink extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_project_snapshot_id", nullable = false)
    private JiraProjectSnapshot jiraProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "github_repository_snapshot_id", nullable = false)
    private GithubRepositorySnapshot githubRepository;

    @Column(columnDefinition = "TEXT")
    private String note;
}