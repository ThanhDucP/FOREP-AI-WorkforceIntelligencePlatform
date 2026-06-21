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

@Entity
@Table(name = "jira_project_snapshot")
@Getter
@Setter
public class JiraProjectSnapshot extends AuditableEntity {
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
    private String jiraDomain;

    @Column(nullable = false)
    private String projectKey;

    private String providerProjectId;
    private String name;
    private String projectTypeKey;
    private String leadAccountId;
    private String leadDisplayName;
    private String selfUrl;
    private Boolean sprintDataAvailable = false;
    private Boolean storyPointsAvailable = false;
    private Boolean epicDataAvailable = false;
    private Boolean versionDataAvailable = false;
    private Boolean componentDataAvailable = false;
}
