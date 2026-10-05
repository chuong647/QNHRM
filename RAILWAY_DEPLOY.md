# QNHRM - Railway deploy

## 1. Kiến trúc

```text
GitHub -> Railway QNHRM service -> Railway MySQL service
```

Project đã có `Dockerfile`, vì vậy Railway có thể tự phát hiện và build bằng Dockerfile. Ứng dụng chạy Java 17 và nhận cổng từ biến `PORT` do Railway cấp.

## 2. Tạo MySQL

Trong Railway Project, tạo một MySQL service.

Railway cung cấp các biến:

- `MYSQLHOST`
- `MYSQLPORT`
- `MYSQLUSER`
- `MYSQLPASSWORD`
- `MYSQLDATABASE`
- `MYSQL_URL`

## 3. Variables cho service QNHRM

Trong Variables của service QNHRM, dùng:

> Project dùng một file `src/main/resources/application.properties`; không cần đặt `SPRING_PROFILES_ACTIVE`.

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

`MySQL` phải đúng tên service MySQL thực tế trên Railway. Nếu tên service khác, đổi namespace trong `${{...}}` cho đúng.

Không đưa mật khẩu thật vào GitHub hoặc file source.

## 4. Database

Project đã có `src/main/resources/schema.sql` và production dùng:

```text
spring.sql.init.mode=always
spring.jpa.hibernate.ddl-auto=none
```

Khi ứng dụng khởi động, Spring Boot chạy schema với `CREATE TABLE IF NOT EXISTS` trước khi bootstrap ADMIN.

Bạn vẫn có thể chạy `database-production.sql` thủ công để dựng/kiểm tra database độc lập. Schema không có phòng ban, Zalo, OT/tăng ca hoặc chức năng tự đăng ký tài khoản.

Nếu dùng database cũ vẫn còn cột `employees.department`, chạy `migrate-remove-department.sql` một lần.

## 5. Deploy

Kết nối GitHub repository với Railway service QNHRM rồi Deploy.

Không cần custom build command hoặc custom start command nếu Railway đã nhận Dockerfile.

Dockerfile dùng:

```text
Build: Maven 3.9.16 + Java 17
Runtime: Eclipse Temurin 17 JRE
Start: java -jar /app/app.jar
```

## 6. Health check

Trong Railway Service Settings -> Deploy -> Healthcheck Path:

```text
/health
```

Health endpoint kiểm tra kết nối MySQL. Railway dùng `PORT` của deployment cho health check.

## 7. Public domain

Vào Networking -> Generate Domain.

Production dùng cookie:

```text
QNHRM_SESSION
Secure=true
HttpOnly=true
SameSite=Lax
```

## 8. ADMIN lần đầu

Khi DB chưa có ADMIN, `ProductionAdminInitializer` dùng:

```text
APP_ADMIN_USERNAME
APP_ADMIN_PASSWORD
```

Mật khẩu phải từ 12 ký tự trở lên.

Khi đã có ADMIN hoạt động, có thể xóa hai biến bootstrap này rồi redeploy. Tài khoản ADMIN trong database không bị xóa.

## 9. Sau deploy

Kiểm tra:

```text
https://<domain-railway>/health
https://<domain-railway>/login
```

Sau đó đăng nhập ADMIN, tạo STAFF thật và kiểm tra chấm công, ca qua 00:00, tổng giờ, ứng lương, báo cáo và Excel.
