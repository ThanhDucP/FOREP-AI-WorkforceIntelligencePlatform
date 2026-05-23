package com.aiworkforce.identity.entity;
import com.aiworkforce.core.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "team")
@Getter
@Setter
public class Team extends AuditableEntity {
    private String name;
    private String description;

    /** Percentage of team capacity currently utilized (0.0–100.0) */
    private Double capacityUsedPct;

    /** Overall team utilization/efficiency score (0.0–100.0) */
    private Double utilizationScore;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;
}
