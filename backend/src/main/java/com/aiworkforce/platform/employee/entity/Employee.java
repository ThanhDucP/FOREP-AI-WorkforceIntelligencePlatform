package com.aiworkforce.platform.employee.entity;

import com.aiworkforce.platform.account.entity.Account;
import com.aiworkforce.platform.common.base.AuditableEntity;
import com.aiworkforce.platform.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String position;
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "is_active")
    private boolean isActive = true;
}
