package com.aiworkforce.identity.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.SprintStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "sprint")
@Getter
@Setter
public class Sprint extends AuditableEntity {

    private Integer sprintNumber;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;

    /** Story points committed at sprint start */
    private Integer committedStoryPoints;

    /** Story points actually completed */
    private Integer completedStoryPoints;

    /** AI forecast confidence (0.0–1.0) e.g. 0.89 */
    private Double velocityConfidence;

    @Enumerated(EnumType.STRING)
    private SprintStatus status = SprintStatus.PLANNING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
