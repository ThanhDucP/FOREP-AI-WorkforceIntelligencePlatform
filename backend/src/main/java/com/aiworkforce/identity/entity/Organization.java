package com.aiworkforce.identity.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.ContractStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "organization")
@Getter
@Setter
public class Organization extends AuditableEntity {
    private String name;
    private String domain;
    private String logoUrl;
    private String address;
    private String githubOrganization;
    private Integer currentSprintNumber;
    @Enumerated(EnumType.STRING)
    private ContractStatus contractStatus = ContractStatus.ACTIVE;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private Integer maxUsers;
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_employee_id")
    private Employee director;
}
