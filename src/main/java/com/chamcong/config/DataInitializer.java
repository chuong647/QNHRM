package com.chamcong.config;

import com.chamcong.entity.Employee;
import com.chamcong.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * Demo/local seed only.
 *
 * Production uses ProductionAdminInitializer so no default credentials
 * are embedded in the production startup path.
 */
@Configuration
@Profile("!prod")
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            EmployeeRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (repository.findByUsername("admin").isEmpty()) {
                Employee admin = new Employee();
                admin.setFullName("Quản trị viên");
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                admin.setHourlyRate(BigDecimal.ZERO);
                admin.setActive(true);
                repository.save(admin);
            }

            if (repository.findByUsername("nhanvien").isEmpty()) {
                Employee employee = new Employee();
                employee.setFullName("Nhân viên mẫu");
                employee.setUsername("nhanvien");
                employee.setPasswordHash(passwordEncoder.encode("123456"));
                employee.setRole("STAFF");
                employee.setHourlyRate(new BigDecimal("50000"));
                employee.setActive(true);
                repository.save(employee);
            }
        };
    }
}
