# QNHRM - DEPLOY CHECKLIST

## Before push
- Project root contains `pom.xml`.
- Production source has no `department` field/accessor.
- Production source has no Zalo integration.
- `database-production.sql` has only `employees`, `attendances`, `salary_advances`.
- No real secrets are stored in the repository.

## Railway service
Use GitHub repository source and branch `main`.

Variables:

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
APP_ADMIN_PASSWORD=<strong-password-12-plus>
```

Healthcheck:

```text
/health
```

Restart policy:

```text
ON_FAILURE
Max retries: 10
```

## Database
1. Run `database-production.sql` in the Railway MySQL service.
2. For an old database that still has `employees.department`, run `migrate-remove-department.sql` in the selected production database.
3. If the old database contains known demo accounts, review `cleanup-demo-accounts-production.sql` before running it.

## First production login
The first active ADMIN is bootstrapped from Railway Variables. After login, change the password. Once an ADMIN exists, bootstrap variables may be removed.

## Go-live tests
- `/health` returns `UP`.
- ADMIN login works.
- Create one real STAFF account.
- STAFF can only see its own attendance and payroll data.
- Staff self-entry only accepts the current date.
- Morning / afternoon / evening work periods calculate correctly.
- Overnight evening shift calculates correctly.
- Late/early minutes remain manual input.
- No OT calculation exists.
- Salary advance deduction is correct.
- Excel exports open successfully.
- Database backup is enabled in Railway.
