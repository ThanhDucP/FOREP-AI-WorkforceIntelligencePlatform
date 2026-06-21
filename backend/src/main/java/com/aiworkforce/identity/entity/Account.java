package com.aiworkforce.identity.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
public class Account extends AuditableEntity {
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "github_id", unique = true)
    private String githubId;

    @Column(name = "jira_id", unique = true)
    private String jiraId;
    
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    private boolean active = true;
    private boolean locked = false;
    private int failedLoginAttempts = 0;
    private String avatarUrl;
    private String timezone;
    private Double focusScore;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;
}
