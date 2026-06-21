package com.aiworkforce.integration.entity;

import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.ExternalIdentityProvider;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "external_identity")
@Getter
@Setter
public class ExternalIdentity extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExternalIdentityProvider provider;

    @Column(nullable = false)
    private String externalId;

    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}