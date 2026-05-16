package com.aiworkforce.platform.security.entity;

import com.aiworkforce.platform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "permission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String name;
    private String module;
    private String action;
    private String description;
}
