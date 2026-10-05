# QNHRM

Website quản lý chấm công và tính tiền theo giờ bằng Spring Boot + MySQL + Thymeleaf.

## Nghiệp vụ
- 3 ca: sáng, chiều, tối; ca tối hỗ trợ qua 00:00.
- Tổng giờ tự động tính từ cặp vào/ra.
- Đi muộn và về sớm do người dùng nhập.
- Hồ sơ là chuỗi, có chữ/số và nhiều nhóm.
- Lương = tổng giờ × lương/giờ.
- Ứng lương: CHUA_TRU / DA_TRU.
- Không OT/tăng ca.
- Không Zalo.
- Không phòng ban.
- Không đăng ký tài khoản tự do.

## Quyền
ADMIN: quản lý nhân viên, chấm công, ứng lương, báo cáo, phiếu lương, Excel.

STAFF: chỉ xem dữ liệu của mình, tự nhập chấm công ngày hiện tại, xem phiếu lương và đổi mật khẩu.

## Database
Production: `employees`, `attendances`, `salary_advances`.
Production giữ `spring.jpa.hibernate.ddl-auto=none`.

## Local
- `database-local-demo.sql` tạo schema local.
- `setup-mysql-demo.cmd` cài database.
- `run-local.cmd` chạy ứng dụng.

## Railway
Đọc `PRODUCTION_RAILWAY.md` và `RAILWAY_VARIABLES.txt`. Repository GitHub có thể vẫn giữ tên `chuong647/quan-ly-cham-cong`; thương hiệu ứng dụng là QNHRM.
