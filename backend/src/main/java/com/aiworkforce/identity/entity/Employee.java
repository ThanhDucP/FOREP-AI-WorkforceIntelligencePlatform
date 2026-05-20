package com.aiworkforce.identity.entity;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.identity.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee")
@Getter
@Setter
public class Employee extends AuditableEntity {
    private String firstName;
    private String lastName;
    private String jobTitle;
    private String phoneNumber;
    
    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}
