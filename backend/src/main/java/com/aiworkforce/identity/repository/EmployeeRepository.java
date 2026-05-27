package com.aiworkforce.identity.repository;
import com.aiworkforce.identity.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByAccountId(UUID accountId);
    List<Employee> findByTeamId(UUID teamId);
    List<Employee> findByTeamOrganizationId(UUID organizationId);
    Optional<Employee> findByAccountEmail(String email);
}
