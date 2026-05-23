package com.aiworkforce.task.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.identity.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "task_comment")
@Getter
@Setter
public class TaskComment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Employee author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
