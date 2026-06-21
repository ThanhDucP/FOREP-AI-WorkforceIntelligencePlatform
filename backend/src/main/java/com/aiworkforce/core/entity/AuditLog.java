package com.aiworkforce.core.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.AuditActionType;
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
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_employee_id")
    private Employee actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private String targetType;
    private UUID targetId;
    private String provider;
    private LocalDateTime occurredAt;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}