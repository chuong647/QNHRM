# QNHRM

Website quản lý chấm công và tính tiền theo giờ bằng Spring Boot + MySQL + Thymeleaf.

## Nghiệp vụ

- 3 ca: sáng, chiều, tối; ca tối hỗ trợ qua 00:00.
- Tổng giờ tính từ các cặp vào/ra.
- Đi muộn và về sớm do người dùng nhập.
- Hồ sơ là chuỗi, hỗ trợ chữ/số và nhiều nhóm.
- Lương = tổng giờ × lương/giờ.
- Ứng lương: `CHUA_TRU` / `DA_TRU`.
- Không OT/tăng ca.
- Không Zalo.
- Không phòng ban.
- Không đăng ký tài khoản tự do.

## Quyền

`ADMIN`: quản lý nhân viên, chấm công, ứng lương, báo cáo, phiếu lương và Excel.

`STAFF`: chỉ xem dữ liệu của mình, tự nhập chấm công ngày hiện tại, xem phiếu lương và đổi mật khẩu.

## Công nghệ

- Java 17
- Spring Boot 4.1.1
- Spring Security
- Spring Data JPA
- Thymeleaf
- MySQL 8
- Apache POI

## Database

Production dùng 3 bảng chính:

- `employees`
- `attendances`
- `salary_advances`

JPA không tự alter schema: `spring.jpa.hibernate.ddl-auto=none`.

`src/main/resources/schema.sql` dùng `CREATE TABLE IF NOT EXISTS` để database có thể khởi tạo an toàn khi ứng dụng production khởi động.

## Local

- `database-local-demo.sql` tạo schema/demo local.
- `setup-mysql-demo.cmd` cài database.
- `run-local.cmd` chạy ứng dụng.

## Railway

Project đã có `Dockerfile` production. Railway sẽ tự phát hiện Dockerfile, build bằng Maven 3.9.16 + Java 17 và chạy JAR Spring Boot.

Đọc:

- `RAILWAY_DEPLOY.md` - hướng dẫn deploy từng bước.
- `PRODUCTION_RAILWAY.md` - cấu hình production.
- `RAILWAY_VARIABLES.txt` - Variables cần nhập vào Railway.

Health check:

```text
/health
```

PORT được lấy từ biến `PORT` do Railway cấp.
