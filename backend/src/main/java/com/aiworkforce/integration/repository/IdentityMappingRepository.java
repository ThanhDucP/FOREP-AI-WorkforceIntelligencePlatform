package com.aiworkforce.integration.repository;

import com.aiworkforce.core.enums.MappingStatus;
import com.aiworkforce.integration.entity.IdentityMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IdentityMappingRepository extends JpaRepository<IdentityMapping, UUID> {
    List<IdentityMapping> findByOrganizationId(UUID organizationId);
    List<IdentityMapping> findByEmployeeId(UUID employeeId);
    List<IdentityMapping> findByStatus(MappingStatus status);
}