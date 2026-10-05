package com.chamcong.config;

import com.chamcong.entity.Employee;
import com.chamcong.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Creates the first production administrator from environment-backed
 * configuration. No real production password is stored in source control.
 */
@Configuration
@Profile("prod")
public class ProductionAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(ProductionAdminInitializer.class);

    @Value("${app.bootstrap.admin.username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initProductionAdmin(
            EmployeeRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            String username = adminUsername == null ? "" : adminUsername.trim().toLowerCase();
            String password = adminPassword == null ? "" : adminPassword;

            boolean activeAdminExists = repository.existsByRoleAndActiveTrue("ADMIN");

            if (username.isBlank() || password.isBlank()) {
                if (activeAdminExists) {
                    log.info("QNHRM production đã có ADMIN hoạt động; bỏ qua bootstrap admin.");
                    return;
                }
                throw new IllegalStateException(
                        "Chưa có ADMIN hoạt động. Hãy cấu hình APP_ADMIN_USERNAME và APP_ADMIN_PASSWORD."
                );
            }

            if (username.length() > 80) {
                throw new IllegalStateException("APP_ADMIN_USERNAME không được dài quá 80 ký tự.");
            }

            if (password.length() < 12) {
                throw new IllegalStateException(
                        "APP_ADMIN_PASSWORD phải có ít nhất 12 ký tự khi khởi tạo production."
                );
            }

            if (repository.findByUsername(username).isEmpty()) {
                Employee admin = new Employee();
                admin.setFullName("Quản trị viên");
                admin.setUsername(username);
                admin.setPasswordHash(passwordEncoder.encode(password));
                admin.setRole("ADMIN");
                admin.setHourlyRate(BigDecimal.ZERO);
                admin.setActive(true);
                repository.save(admin);
            }
        };
    }
}
