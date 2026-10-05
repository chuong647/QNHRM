package com.chamcong.repository;

import com.chamcong.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Tìm nhân viên theo username.
     */
    Optional<Employee> findByUsername(String username);

    /**
     * Kiểm tra username đã tồn tại hay chưa.
     */
    boolean existsByUsername(String username);

    /**
     * Lấy danh sách nhân viên đang hoạt động,
     * sắp xếp theo họ tên tăng dần.
     */
    List<Employee> findByActiveTrueOrderByFullNameAsc();

    boolean existsByRoleAndActiveTrue(String role);
}