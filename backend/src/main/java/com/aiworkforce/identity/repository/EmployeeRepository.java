package com.aiworkforce.identity.repository;

import com.aiworkforce.core.enums.AccountStatus;
import com.aiworkforce.identity.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByAccountId(UUID accountId);
    List<Employee> findByTeamId(UUID teamId);
    List<Employee> findByTeamOrganizationId(UUID organizationId);
    List<Employee> findByOrganizationId(UUID organizationId);
    Optional<Employee> findByAccountEmail(String email);

    @Query("""
            select count(distinct e.id)
            from Employee e
            left join e.team t
            left join t.organization teamOrganization
            where e.organization.id = :organizationId
               or teamOrganization.id = :organizationId
            """)
    long countDistinctByOrganizationScope(@Param("organizationId") UUID organizationId);

    @Query("""
            select distinct e
            from Employee e
            left join e.account a
            left join e.team t
            left join t.organization teamOrganization
            where (e.organization.id = :organizationId or teamOrganization.id = :organizationId)
              and (:accountStatus is null or a.status = :accountStatus)
            """)
    List<Employee> findDistinctByOrganizationScopeAndAccountStatus(
            @Param("organizationId") UUID organizationId,
            @Param("accountStatus") AccountStatus accountStatus
    );

    @Query("""
            select distinct e
            from Employee e
            left join e.account a
            left join e.team t
            left join t.organization teamOrganization
            where (e.organization.id = :organizationId or teamOrganization.id = :organizationId)
              and (:accountStatus is null or a.status = :accountStatus)
            """)
    Page<Employee> findPageByOrganizationScopeAndAccountStatus(
            @Param("organizationId") UUID organizationId,
            @Param("accountStatus") AccountStatus accountStatus,
            Pageable pageable
    );

    @Query("""
            select e.burnoutRisk, count(e.id)
            from Employee e
            group by e.burnoutRisk
            """)
    List<Object[]> countByBurnoutRiskGroup();
}