-- QNHRM - migrate existing DB to schema without department
-- Chạy 1 lần nếu DB cũ còn employees.department.
SET @has_department := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='employees' AND column_name='department'
);
SET @sql := IF(@has_department > 0, 'ALTER TABLE employees DROP COLUMN department', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SELECT 'Department column removed.' AS result;
