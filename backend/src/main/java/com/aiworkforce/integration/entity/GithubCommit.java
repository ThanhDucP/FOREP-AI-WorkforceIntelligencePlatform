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
@Table(name = "github_commit")
@Getter
@Setter
public class GithubCommit extends AuditableEntity {
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
    private String sha;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String authorName;
    private String authorEmail;
    private String htmlUrl;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    private LocalDateTime committedAt;
}
