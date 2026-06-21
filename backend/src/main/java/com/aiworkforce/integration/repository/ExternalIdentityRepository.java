package com.aiworkforce.integration.repository;

import com.aiworkforce.core.enums.ExternalIdentityProvider;
import com.aiworkforce.integration.entity.ExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, UUID> {
    Optional<ExternalIdentity> findByProviderAndExternalId(ExternalIdentityProvider provider, String externalId);
    List<ExternalIdentity> findByOrganizationId(UUID organizationId);
    List<ExternalIdentity> findByTeamId(UUID teamId);
}