package com.aiworkforce.timetracking.repository;

import com.aiworkforce.timetracking.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByEmployeeIdAndCheckInDate(UUID employeeId, LocalDate date);
    List<Attendance> findByEmployeeId(UUID employeeId);
    List<Attendance> findByEmployeeTeamId(UUID teamId);
    List<Attendance> findByEmployeeTeamIdIn(List<UUID> teamIds);
    List<Attendance> findByEmployeeTeamOrganizationId(UUID organizationId);
}
