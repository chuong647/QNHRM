# QNHRM - PRODUCTION / RAILWAY

QNHRM là ứng dụng Spring Boot + Thymeleaf + Spring Security + MySQL.

Project được đóng gói cho Railway bằng `Dockerfile`:

```text
GitHub -> Railway QNHRM -> Railway MySQL
```

## Variables

```text
MYSQLHOST=${{MySQL.MYSQLHOST}}
MYSQLPORT=${{MySQL.MYSQLPORT}}
MYSQLDATABASE=${{MySQL.MYSQLDATABASE}}
MYSQLUSER=${{MySQL.MYSQLUSER}}
MYSQLPASSWORD=${{MySQL.MYSQLPASSWORD}}
COOKIE_SECURE=true
DB_POOL_MAX=8
DB_POOL_MIN=2
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=<mat-khau-manh-tu-12-ky-tu>
```

`MySQL` phải đúng tên service database trên Railway.

## Database

Production dùng `src/main/resources/schema.sql` với `spring.sql.init.mode=always` và `spring.jpa.hibernate.ddl-auto=none`.

Schema không có phòng ban, Zalo, OT/tăng ca hoặc tự đăng ký tài khoản.

Các bảng chính:

- `employees`
- `attendances`
- `salary_advances`

Nếu chuyển từ database cũ có `employees.department`, chạy `migrate-remove-department.sql` một lần.

## Deploy

Kết nối GitHub repo với Railway. Railway sẽ phát hiện `Dockerfile` và build/deploy ứng dụng.

Health check:

```text
/health
```

Application tự nhận biến `PORT` của Railway.

## HTTPS / session

Production profile dùng:

```text
QNHRM_SESSION
Secure=true
HttpOnly=true
SameSite=Lax
```

## Bootstrap ADMIN

ADMIN đầu tiên lấy từ `APP_ADMIN_USERNAME` và `APP_ADMIN_PASSWORD`. Mật khẩu production phải có ít nhất 12 ký tự.

Sau khi đã có ADMIN hoạt động, có thể xóa 2 biến bootstrap khỏi Railway Variables rồi redeploy.

## Kiểm tra sau deploy

```text
/health
/login
/
```

Kiểm tra thêm STAFF chỉ thấy dữ liệu của mình, 3 ca trong ngày, ca tối qua 00:00, tổng giờ, ứng lương, báo cáo và xuất Excel.
