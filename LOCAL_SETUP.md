# QNHRM - Chạy local

## Cách đơn giản nhất

Double-click:

`run-local.cmd`

Script sẽ hỏi mật khẩu MySQL local rồi đặt các biến:

- `MYSQLHOST=localhost`
- `MYSQLPORT=3306`
- `MYSQLDATABASE=quan_ly_cham_cong`
- `MYSQLUSER=root`
- `MYSQLPASSWORD=(mật khẩu bạn nhập)`
- `PORT=8080`
- `COOKIE_SECURE=false`

Không lưu mật khẩu vào source code.

## Chạy trong IntelliJ IDEA

Vào `Run > Edit Configurations > QNHRMApplication > Environment variables` và đặt:

```text
MYSQLHOST=localhost
MYSQLPORT=3306
MYSQLDATABASE=quan_ly_cham_cong
MYSQLUSER=root
MYSQLPASSWORD=<mat-khau-MySQL-local>
PORT=8080
COOKIE_SECURE=false
```


## Database local

```sql
USE quan_ly_cham_cong;
SHOW TABLES;
```

Cần có:

- `employees`
- `attendances`
- `salary_advances`

## Railway

Trên service QNHRM đặt:

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

`MySQL` phải đúng tên service MySQL thực tế trên Railway. Không đưa mật khẩu thật vào GitHub.
