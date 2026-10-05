$ErrorActionPreference = 'Stop'

Write-Host '=========================================' -ForegroundColor Cyan
Write-Host ' QNHRM - CHAY LOCAL' -ForegroundColor Cyan
Write-Host '=========================================' -ForegroundColor Cyan

$env:MYSQLHOST = 'localhost'
$env:MYSQLPORT = '3306'
$env:MYSQLDATABASE = 'quan_ly_cham_cong'
$env:MYSQLUSER = 'root'
$env:PORT = '8080'
$env:COOKIE_SECURE = 'false'

$secure = Read-Host 'Nhap mat khau MySQL local' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:MYSQLPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}

Write-Host 'Dang khoi dong QNHRM...' -ForegroundColor Green
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
