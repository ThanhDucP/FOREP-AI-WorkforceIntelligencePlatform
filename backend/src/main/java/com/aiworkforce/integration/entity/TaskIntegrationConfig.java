package com.aiworkforce.integration.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.identity.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "task_integration_config")
@Getter
@Setter
public class TaskIntegrationConfig extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationProvider provider;

    @Column(nullable = false)
    private String webhookSecret;

    private String accessToken; // Optional, for REST API polling if needed

    @Column(nullable = false)
    private Boolean isActive = true;

    // Optional config details like GitHub repository name ("owner/repo") or Jira project key
    private String projectKey; 
}
