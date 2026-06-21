package com.aiworkforce.integration.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.MappingStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "identity_mapping")
@Getter
@Setter
public class IdentityMapping extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_identity_id")
    private ExternalIdentity jiraIdentity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "github_identity_id")
    private ExternalIdentity githubIdentity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MappingStatus status = MappingStatus.UNMATCHED;

    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String evidenceSummary;

    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_employee_id")
    private Employee confirmedBy;
}