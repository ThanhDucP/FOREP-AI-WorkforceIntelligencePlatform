package com.aiworkforce.identity.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.TeamMembershipStatus;
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
@Table(name = "team_membership")
@Getter
@Setter
public class TeamMembership extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private Employee approvedBy;

    @Enumerated(EnumType.STRING)
    private TeamMembershipStatus status = TeamMembershipStatus.PENDING_LEAD_APPROVAL;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
