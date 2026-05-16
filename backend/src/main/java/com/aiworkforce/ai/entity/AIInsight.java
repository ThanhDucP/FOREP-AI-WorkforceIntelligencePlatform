package com.aiworkforce.ai.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.identity.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = ""ai_insight"")
@Getter
@Setter
public class AIInsight extends AuditableEntity {
    
    private String summary;
    
    @Column(columnDefinition = ""TEXT"")
    private String fullAnalysis;
    
    @Enumerated(EnumType.STRING)
    private InsightSeverity severity;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ""employee_id"")
    private Employee employee;
}
