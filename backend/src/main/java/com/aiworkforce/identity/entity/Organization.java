package com.aiworkforce.identity.entity;
import com.aiworkforce.core.base.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_employee_id")
    private Employee director;
}