# Watch Management ? DE190025

Assignment 02, implemented through step 5. StudentCode: **DE190025**.

| Project | Port | Responsibility |
|---|---|---|
| MSAccount_DE190025 | 8081 | BCrypt accounts, login, access/refresh JWT, logout |
| MSBrand_DE190025 | 8082 | Brands and idempotent Saga participant assignments |
| MSWatch_DE190025 | 8083 | Scaffold and DBWatch datasource only |
| APIGateway_DE190025 | 8080 | Reactive Gateway scaffold only |

MSWatch orchestration, watch CRUD, Gateway routes/JWT filtering, full Saga Postman collection and Docker are steps 6?10 and are intentionally outside this submission. Public Brand APIs currently run directly on port 8082; JWT enforcement at the Gateway belongs to step 7. Logout is stateless: the client discards tokens.

## Prerequisites

Java 21, Maven 3.9+, and SQL Server with SQL Authentication enabled and an existing `sa` login configured by the local administrator. The projects use Spring Boot 4.1.1, Spring Cloud 2025.1.3, springdoc 3.0.2 and jjwt 0.13.0 as specified in the assignment.

From this directory, create the databases using a SQL Server administrator connection:

```powershell
sqlcmd -S localhost -E -C -b -i .\scripts\create-databases.sql
```

The script creates missing DBAccount, DBBrand and DBWatch databases. It preserves existing databases and does not change the administrator password. Configure SQL Authentication and the local login separately if needed.

Copy `.env.example` to `.env` for local reference, fill in your own database password and a random JWT signing key of at least 32 bytes, and keep `.env` untracked. Spring Boot does not automatically load `.env`: export the values in each service terminal (or configure the IDE environment):

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_HOST = "localhost"
$env:DB_PORT = "1433"
$env:DB_PASSWORD = "<your-local-sql-password>"
$env:JWT_SECRET = "<your-random-signing-key-at-least-32-bytes>"
$env:JWT_EXPIRATION = "900000"
$env:REFRESH_EXPIRATION = "604800000"
```

Build each project:

```powershell
mvn -q -DskipTests package -f MSAccount_DE190025/pom.xml
mvn -q -DskipTests package -f MSBrand_DE190025/pom.xml
mvn -q -DskipTests package -f MSWatch_DE190025/pom.xml
mvn -q -DskipTests package -f APIGateway_DE190025/pom.xml
```

Start Account and Brand in separate terminals:

```powershell
java -jar MSAccount_DE190025/target/ms-account-1.0.0.jar
java -jar MSBrand_DE190025/target/ms-brand-1.0.0.jar
```

Account seeds five assignment accounts with BCrypt password `@1`. Brand seeds Rolex, Omega, Seiko, Casio and TAG Heuer, with initial watch counts 2, 2, 2, 3 and 1. Seed data is inserted only into empty tables. Use fresh paired DBWatch/DBBrand databases when initializing the assignment; do not reset a single side independently.

Use a disposable seeded database for the smoke test, which temporarily assigns WatchID 999999 and cleans it up:

```powershell
python scripts/smoke-test.py
```

The test covers the direct Account and Brand endpoints for steps 3 and 5. It is not the later end-to-end Saga collection.

## Verification ? 2026-11-04

- All four projects packaged successfully using Java 21 and Maven 3.9.16.
- Account and Brand started against temporary H2 2.4.240 databases in MSSQLServer mode, supplied externally at runtime; production dependencies and datasource defaults remain SQL Server.
- Passed 33 HTTP checks plus assertions for JWT claims/expiry, seeded brand counts, assignment uniqueness, reassignment, missing-brand errors, repeated unassignment and cleanup.
- Gateway started successfully without routes on temporary port 18080.
- SQL Server integration remains unverified: both installed SQL Server services were stopped and this session could not start MSSQLSERVER. The database creation script has not been executed successfully against SQL Server. SQL Authentication/login configuration, SQL Server-specific schema generation and BCrypt values in the actual SQL Server database still require verification once the service is available.
- The Saga design is in `docs/SAGA_DESIGN.md`. Instructor approval remains an external course requirement; no approval is claimed here.

Commits are split by TODO group on `master`, as requested, using the existing student Git identity without additional co-authors.
