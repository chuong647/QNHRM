-- QNHRM PRODUCTION SCHEMA (MySQL 8)
-- Không DROP database. Chạy trong database Railway đã tạo sẵn.
-- Không seed tài khoản demo.

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    hourly_rate DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    position VARCHAR(100) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employees_username (username),
    CONSTRAINT chk_employee_hourly_rate CHECK (hourly_rate >= 0),
    CONSTRAINT chk_employee_role CHECK (role IN ('ADMIN','STAFF'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    morning_in TIME NULL,
    morning_out TIME NULL,
    afternoon_in TIME NULL,
    afternoon_out TIME NULL,
    evening_in TIME NULL,
    evening_out TIME NULL,
    dossier_info VARCHAR(1000) NOT NULL DEFAULT '',
    late_minutes INT NOT NULL DEFAULT 0,
    early_leave_minutes INT NOT NULL DEFAULT 0,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_employee_date (employee_id, work_date),
    KEY idx_attendance_employee_date (employee_id, work_date),
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_attendance_late CHECK (late_minutes >= 0),
    CONSTRAINT chk_attendance_early CHECK (early_leave_minutes >= 0),
    CONSTRAINT chk_attendance_morning_pair CHECK (morning_out IS NULL OR morning_in IS NOT NULL),
    CONSTRAINT chk_attendance_afternoon_pair CHECK (afternoon_out IS NULL OR afternoon_in IS NOT NULL),
    CONSTRAINT chk_attendance_evening_pair CHECK (evening_out IS NULL OR evening_in IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS salary_advances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    advance_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    reason VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CHUA_TRU',
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_salary_advance_employee_date (employee_id, advance_date),
    CONSTRAINT fk_salary_advance_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_salary_advance_amount CHECK (amount > 0),
    CONSTRAINT chk_salary_advance_status CHECK (status IN ('CHUA_TRU','DA_TRU'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- QNHRM không có phòng ban, không có Zalo, không có OT/tăng ca, không có tự đăng ký tài khoản.
-- ADMIN đầu tiên do ProductionAdminInitializer tạo từ Railway Variables.
