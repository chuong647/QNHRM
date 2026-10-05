# QNHRM - PRODUCTION / RAILWAY

## Kiến trúc

```text
GitHub main -> Railway service QNHRM -> Railway MySQL
```

Railway hỗ trợ deploy trực tiếp từ GitHub và tự deploy khi branch đã liên kết có commit mới. Cấu hình healthcheck và restart được đặt trực tiếp trong Railway Settings để tránh phụ thuộc file Config-as-Code đang trong giai đoạn chuyển đổi.

## Variables

```text
SPRING_PROFILES_ACTIVE=prod
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

Sau khi đã có ADMIN hoạt động, có thể xóa hai biến `APP_ADMIN_USERNAME` và `APP_ADMIN_PASSWORD`.

## Database production

Chạy `database-production.sql` trong MySQL Railway. Nếu database cũ còn `employees.department`, chạy `migrate-remove-department.sql` một lần.

Kiểm tra:

```sql
SHOW TABLES;
DESCRIBE employees;
DESCRIBE attendances;
DESCRIBE salary_advances;
```

Không còn phòng ban, Zalo schema hay bảng `notification_logs`.

## Deploy

```powershell
git add .
git commit -m "QNHRM Production"
git push origin main
```

Railway sẽ build bằng Railpack và deploy. Healthcheck: `/health`. Endpoint này kiểm tra cả kết nối ứng dụng và database.

## HTTPS

Tạo Railway public domain trong Networking. Production profile đã bật cookie tên `QNHRM_SESSION` với `Secure`, `HttpOnly`, `SameSite=Lax`.

## Trước khi mở cho nhân viên

- Deployment SUCCESS.
- `/health` trả UP.
- ADMIN đăng nhập.
- Tạo STAFF thật.
- STAFF chỉ thấy dữ liệu của mình.
- Test đủ 3 ca, ca tối qua 00:00.
- Test tổng giờ, ứng lương, báo cáo, Excel.
- Bật backup database trên Railway.
