-- QNHRM - REMOVE KNOWN DEMO ACCOUNTS FROM AN EXISTING PRODUCTION DATABASE
-- Run only when these usernames are confirmed to be demo-only accounts.

DELETE FROM employees
WHERE username IN ('myle', 'hong', 'nhanvien')
  AND NOT EXISTS (
      SELECT 1 FROM attendances a WHERE a.employee_id = employees.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM salary_advances s WHERE s.employee_id = employees.id
  );

-- Review remaining known demo accounts before deleting anything else:
SELECT id, full_name, username, role, active
FROM employees
WHERE username IN ('myle', 'hong', 'nhanvien');
