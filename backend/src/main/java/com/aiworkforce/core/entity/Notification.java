package com.aiworkforce.core.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.NotificationType;
import com.aiworkforce.identity.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Employee recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Boolean isRead = false;

    /** Optional reference to a related task */
    private UUID relatedTaskId;

    /** Optional reference to a related employee */
    private UUID relatedEmployeeId;
}
