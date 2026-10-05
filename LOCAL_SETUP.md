# QNHRM - Chay local

## Cach don gian nhat

Double-click:

`run-local.cmd`

Script se hoi mat khau MySQL local roi tu dong dat cac bien:

- MYSQLHOST=localhost
- MYSQLPORT=3306
- MYSQLDATABASE=quan_ly_cham_cong
- MYSQLUSER=root
- MYSQLPASSWORD=(mat khau ban nhap)
- PORT=8080

Khong luu mat khau vao source code.

## Chay trong IntelliJ IDEA

Vao `Run > Edit Configurations > QuanLyChamCongApplication > Environment variables` va dat:

```text
MYSQLHOST=localhost
MYSQLPORT=3306
MYSQLDATABASE=quan_ly_cham_cong
MYSQLUSER=root
MYSQLPASSWORD=<mat-khau-MySQL-local>
PORT=8080
COOKIE_SECURE=false
```

## Kiem tra database local

```sql
USE quan_ly_cham_cong;
SHOW TABLES;
```

Can co:

- employees
- attendances
- salary_advances

## Railway

Tren Railway service `quan-ly-cham-cong` su dung cac Service Variables:

```text
MYSQLHOST       -> ${{MySQL.MYSQLHOST}}
MYSQLPORT       -> ${{MySQL.MYSQLPORT}}
MYSQLDATABASE   -> ${{MySQL.MYSQLDATABASE}}
MYSQLUSER       -> ${{MySQL.MYSQLUSER}}
MYSQLPASSWORD   -> ${{MySQL.MYSQLPASSWORD}}
```

Khong dua mat khau MySQL that vao GitHub.
