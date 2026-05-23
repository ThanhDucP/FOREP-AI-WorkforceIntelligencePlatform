package com.aiworkforce.ai.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.InsightType;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_insight")
@Getter
@Setter
public class AIInsight extends AuditableEntity {

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String fullAnalysis;

    @Enumerated(EnumType.STRING)
    private InsightSeverity severity;

    /** Categorized type of insight */
    @Enumerated(EnumType.STRING)
    private InsightType insightType;

    /** AI confidence score 0.0–1.0 */
    private Double confidenceScore;

    /** JSON array of affected employee IDs e.g. ["uuid1","uuid2"] */
    @Column(columnDefinition = "TEXT")
    private String affectedEmployeeIds;

    /** When a manager adopted this recommendation */
    private LocalDateTime adoptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
