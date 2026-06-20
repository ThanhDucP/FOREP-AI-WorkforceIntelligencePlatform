package com.aiworkforce.integration.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_pull_request")
@Getter
@Setter
public class GithubPullRequest extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private TaskIntegrationConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String repositoryFullName;

    @Column(nullable = false)
    private Integer number;

    private String title;
    private String state;
    private String htmlUrl;
    private String authorLogin;
    private Boolean draft;
    private Boolean merged;
    private LocalDateTime providerCreatedAt;
    private LocalDateTime providerUpdatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime mergedAt;
}
