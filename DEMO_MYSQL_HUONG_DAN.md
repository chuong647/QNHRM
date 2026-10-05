# QNHRM - Kết nối MySQL để chạy demo local

## Cách 1: tự động (khuyên dùng)

1. Đảm bảo MySQL Server đang chạy ở `localhost:3306`.
2. Double-click `setup-mysql-demo.cmd`.
3. Nhập mật khẩu `root` khi MySQL hỏi.
4. Script tạo database `quan_ly_cham_cong` và import toàn bộ schema + tài khoản mẫu.
5. Double-click `run-local.cmd`.
6. Nhập lại mật khẩu MySQL local khi script hỏi.
7. Mở `http://localhost:8080`.

## Tài khoản demo

- ADMIN: `admin` / `admin123`
- STAFF: `nhanvien` / `123456`
- STAFF: `myle` / `password`
- STAFF: `hong` / `password`

## Cách 2: MySQL Workbench

Mở MySQL Workbench, kết nối tới `localhost:3306` bằng `root`, mở file `database-local-demo.sql` và Run.

Sau khi chạy xong kiểm tra:

```sql
USE quan_ly_cham_cong;
SHOW TABLES;
SELECT id, full_name, username, role, active FROM employees ORDER BY id;
```

Phải có các bảng `employees`, `attendances`, `salary_advances`.

## Cấu hình Spring Boot

Project dùng biến môi trường; `run-local.cmd` sẽ tự hỏi mật khẩu rồi đặt:

```text
MYSQLHOST=localhost
MYSQLPORT=3306
MYSQLDATABASE=quan_ly_cham_cong
MYSQLUSER=root
MYSQLPASSWORD=(mật khẩu bạn nhập)
PORT=8080
COOKIE_SECURE=false
```
