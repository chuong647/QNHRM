@echo off
setlocal
cd /d "%~dp0"

echo ================================================
echo QNHRM - CAI MYSQL CHO CHAY DEMO LOCAL
echo ================================================
echo.
echo Yeu cau: MySQL Server da cai va len cong 3306.
echo Tai khoan mac dinh: root
 echo.

mysql --version >nul 2>&1
if errorlevel 1 (
  echo [LOI] Khong tim thay lenh mysql trong PATH.
  echo Hay mo MySQL Workbench va chay file database-local-demo.sql thay cho script nay.
  pause
  exit /b 1
)

echo [1/2] Tao database quan_ly_cham_cong...
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS quan_ly_cham_cong CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 (
  echo [LOI] Khong tao duoc database. Kiem tra mat khau MySQL root.
  pause
  exit /b 1
)

echo.
echo [2/2] Tao bang + tai khoan mau...
mysql -u root -p quan_ly_cham_cong < "%~dp0database-local-demo.sql"
if errorlevel 1 (
  echo [LOI] Import database that bai.
  pause
  exit /b 1
)

echo.
echo ================================================
echo DA CAI XONG MYSQL CHO QNHRM
echo ================================================
echo Database: quan_ly_cham_cong
echo Bang: employees, attendances, salary_advances
 echo.
echo Chay tiep: run-local.cmd
 echo.
pause
endlocal
