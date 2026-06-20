package com.aiworkforce.integration.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.identity.entity.Employee;
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

import java.time.LocalDate;

@Entity
@Table(name = "jira_issue_snapshot")
@Getter
@Setter
public class JiraIssueSnapshot extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private TaskIntegrationConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Employee assignee;

    @Column(nullable = false)
    private String jiraDomain;

    @Column(nullable = false)
    private String projectKey;

    @Column(nullable = false)
    private String issueKey;

    private String providerIssueId;
    private String summary;
    private String statusName;
    private String issueType;
    private String priorityName;
    private String externalUrl;
    private String assigneeAccountId;
    private String assigneeEmail;
    private Integer storyPoints;
    private Integer originalEstimateSeconds;
    private Integer remainingEstimateSeconds;
    private Integer sprintId;
    private String sprintName;
    private LocalDate dueDate;
}
