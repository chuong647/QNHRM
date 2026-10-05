# QNHRM - DEPLOY CHECKLIST

## Before push

- Project root contains `pom.xml` and `Dockerfile`.
- `src/main/resources/application.properties` is the single application configuration file; `application-prod.properties` is not required.
- Production source has no `department` field/accessor or dependency on Zalo/OT.
- `src/main/resources/schema.sql` contains only `employees`, `attendances`, `salary_advances`.
- No real secrets are stored in the repository.
- `target/` and `.idea/` are excluded from the deploy source.

## Railway service

Use the GitHub repository and the desired branch (normally `main`). Railway will detect the `Dockerfile`.

Variables:

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
APP_ADMIN_PASSWORD=<strong-password-12-plus>
```

`MySQL` must be replaced by the exact Railway MySQL service name if it is different.

Healthcheck:

```text
/health
```

Runtime:

```text
Java 17 JRE
```

## Database

On a new database, `src/main/resources/schema.sql` initializes the three application tables on startup via `CREATE TABLE IF NOT EXISTS`.

For a manual/independent database setup, run `database-production.sql` once.

For an old database that still has `employees.department`, run `migrate-remove-department.sql` once.

If the old database contains known demo accounts, review `cleanup-demo-accounts-production.sql` before running it.

## First production login

The first active ADMIN is bootstrapped from Railway Variables. `APP_ADMIN_PASSWORD` must contain at least 12 characters.

After an ADMIN exists, the two bootstrap variables can be removed and the service redeployed.

## Go-live tests

- `/health` returns HTTP 200 with `status=UP`.
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
