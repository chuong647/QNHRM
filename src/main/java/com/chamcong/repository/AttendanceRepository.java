package com.chamcong.repository;

import com.chamcong.entity.Attendance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @EntityGraph(attributePaths = {"employee"})
    List<Attendance> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    boolean existsByEmployeeId(Long employeeId);

    Optional<Attendance> findByEmployeeIdAndWorkDate(
            Long employeeId,
            LocalDate workDate
    );

    @EntityGraph(attributePaths = {"employee"})
    Optional<Attendance> findWithEmployeeById(Long id);
}