package com.chamcong.service;

import com.chamcong.entity.Employee;
import com.chamcong.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword != null && passwordHash != null && passwordEncoder.matches(rawPassword, passwordHash);
    }

    @Transactional
    public boolean changePassword(Employee employee, String currentPassword, String newPassword) {
        if (employee == null || currentPassword == null || newPassword == null) return false;
        if (!passwordEncoder.matches(currentPassword, employee.getPasswordHash())) return false;
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
        return true;
    }
}
