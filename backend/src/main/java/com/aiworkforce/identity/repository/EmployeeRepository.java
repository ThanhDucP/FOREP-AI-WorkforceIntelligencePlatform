package com.aiworkforce.identity.repository;
import com.aiworkforce.identity.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByAccountId(UUID accountId);
}
