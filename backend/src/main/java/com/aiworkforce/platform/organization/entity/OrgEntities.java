package com.aiworkforce.platform.organization.entity;

import com.aiworkforce.platform.common.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "organization")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization extends AuditableEntity {
    @Column(nullable = false)
    private String name;
    private String domain;
    private String industry;
}

@Entity
@Table(name = "team")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Team extends AuditableEntity {
    @Column(nullable = false)
    private String name;
    private String description;
}
