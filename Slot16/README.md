# Slot16 - Sponsored post booking

Student: **DE190025**. Author: **haidang2312**.
Java 21, Spring Boot 4.1.0, Spring Cloud 2025.1.2, springdoc 3.0.3.

| Project | Package | Port |
| --- | --- | --- |
| DE190025UserService | fu.de190025.user | 8081 |
| DE190025PostService | fu.de190025.post | 8082 |
| DE190025MediaGateway | fu.de190025.gateway | 8080 |

## Database

Use SQL Server 2016+, TCP port 1433 and the SQL login specified in the exam.
Production uses MSS301_2026_PE_MEDIA and ddl-auto=none. Run these scripts once
on a fresh database, in order, using SSMS or sqlcmd:

1. database/00_create_database.sql
2. database/create_users_sql.txt
3. database/create_posts_sql.txt
4. database/03_seed.sql (optional)

The supplied table scripts are unchanged. Seed includes two ACTIVE creators and
one SUSPENDED creator.

## Build and run

Set JAVA_HOME to JDK 21. Open each pom.xml in IntelliJ. Maven Wrapper is included.
From Slot16, run:

```powershell
.\DE190025UserService\mvnw.cmd -f .\DE190025UserService\pom.xml clean verify
.\DE190025PostService\mvnw.cmd -f .\DE190025PostService\pom.xml clean verify
.\DE190025MediaGateway\mvnw.cmd -f .\DE190025MediaGateway\pom.xml clean verify
```

Start in three terminals, User first, then Post, then Gateway:

```powershell
java -jar .\DE190025UserService\target\userservice-1.0.0.jar
java -jar .\DE190025PostService\target\postservice-1.0.0.jar
java -jar .\DE190025MediaGateway\target\gatewayservice-1.0.0.jar
```

Swagger: http://localhost:8080/swagger-ui.html. Select user-service or post-service.
Both backends expose /swagger-ui.html and /v3/api-docs on their respective ports.
Keep both running; the browser fetches their docs directly using CORS.

Use http://localhost:8080/api/users and /api/posts as API entry points.
Import postman/Slot16.postman_collection.json into Postman and run the 22 requests
in order. It creates its own creator and campaign, then cancels/deactivates them.
Assertions check HTTP codes, application status and ISO timestamps.

## Behavior

| JSON status | HTTP | Meaning |
| --- | --- | --- |
| 1 | 200 / 201 | Success / created |
| 2 | 406 | Validation failed |
| 3 | 226 | Duplicate username |
| 4 | 404 | User or campaign not found |
| 5 | 400 | Creator not ACTIVE when booking |
| 0 | 500 | Unexpected server or downstream error |

Responses contain status, message, data and an ISO 8601 UTC timestamp.
Pagination is zero-based, default size 10, max 100. Filters: role/status for users;
partial advertiserName/status for campaigns. DELETE changes status. PUT accepts
partial updates; null fields retain existing values. Invalid changes roll back.
Creation defaults to ACTIVE for users and SCHEDULED for campaigns.

Dates use strict dd/MM/yyyy. Cost equals current creator rate multiplied by
calendar days between dates, WITHOUT multiplying by numberOfPosts, per the exam.
Updates recalculate cost and validate dates and capacity. A date-only JPA
converter preserves dates across timezones with java.util.Date DTOs.

## Verification and submission

See TEST_REPORT.md for executed checks and limits. H2 is test-scoped only;
production remains SQL Server. Stop services and run mvnw.cmd clean on each
project before submission. ZIP source with a filename starting DE190025, excluding
target, IDE files and Git metadata. A source ZIP is supplied next to Slot16.

Work is committed incrementally on feat/slot16-media-platform using Conventional
Commits and the existing repository author identity, without co-author trailers.
