package com.chamcong.repository;

import com.chamcong.entity.SalaryAdvance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {

    @EntityGraph(attributePaths = {"employee"})
    List<SalaryAdvance> findAllByOrderByAdvanceDateDesc();

    @Override
    @EntityGraph(attributePaths = {"employee"})
    Optional<SalaryAdvance> findById(Long id);

    boolean existsByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {"employee"})
    List<SalaryAdvance> findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );
}
