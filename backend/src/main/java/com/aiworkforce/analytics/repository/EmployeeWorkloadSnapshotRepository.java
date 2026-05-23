package com.aiworkforce.analytics.repository;

import com.aiworkforce.analytics.entity.EmployeeWorkloadSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeWorkloadSnapshotRepository extends JpaRepository<EmployeeWorkloadSnapshot, UUID> {
    List<EmployeeWorkloadSnapshot> findByEmployeeIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            UUID employeeId, LocalDate startDate, LocalDate endDate);

    List<EmployeeWorkloadSnapshot> findByEmployeeTeamIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            UUID teamId, LocalDate startDate, LocalDate endDate);

    List<EmployeeWorkloadSnapshot> findByEmployeeTeamIdInAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            List<UUID> teamIds, LocalDate startDate, LocalDate endDate);

    List<EmployeeWorkloadSnapshot> findByEmployeeTeamOrganizationIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            UUID organizationId, LocalDate startDate, LocalDate endDate);
    
    List<EmployeeWorkloadSnapshot> findBySnapshotDateOrderByWorkloadScoreDesc(LocalDate snapshotDate);
}
