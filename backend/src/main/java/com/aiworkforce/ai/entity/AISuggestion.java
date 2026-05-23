package com.aiworkforce.ai.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.SuggestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "ai_suggestion")
@Getter
@Setter
public class AISuggestion extends AuditableEntity {

    private Integer sprintNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionType suggestionType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /** AI confidence score 0.0–1.0 */
    private Double confidenceScore;

    /** Source employee (whose workload is too high) */
    private UUID sourceEmployeeId;

    /** Target employee (who should receive the task) */
    private UUID targetEmployeeId;

    /** The specific task being suggested for reassignment */
    private UUID sourceTaskId;

    /** True if a manager adopted this recommendation */
    private Boolean isAdopted = false;
}
