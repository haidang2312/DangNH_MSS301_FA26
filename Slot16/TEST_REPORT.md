# Verification report

Date: 2026-10-29 (Asia/Saigon). Microsoft OpenJDK 21.0.12.1.
UserService and PostService passed mvn clean verify. Gateway passed mvn clean test
and was packaged successfully. Tests boot real HTTP servers on random ports.

| Project | Test methods | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| DE190025UserService | 2 | 0 | 0 | 0 |
| DE190025PostService | 2 | 0 | 0 | 0 |
| DE190025MediaGateway | 2 | 0 | 0 | 0 |

Each method contains multiple HTTP scenarios:

- User lifecycle, ACTIVE default, duplicate username, invalid email/rate/daily
  limit, partial update, rollback, missing ID, filtering, invalid pagination,
  deactivation, OpenAPI and CORS preflight.
- Campaign SCHEDULED default, three-day cost 1500000, nested creator, real Feign
  HTTP decoding, upstream 404 mapping, suspended creator, invalid dates including
  February 31, invalid email, capacity, rollback, changed-rate recalculation,
  filtering, cancellation, missing GET/PUT/DELETE and OpenAPI.
- Gateway collection/detail routes preserve paths for users/posts, Swagger exposes
  both definitions, cross-origin preflight succeeds.

Regression: persistence in a UTC+7 JVM initially shifted dates one day. The
DateOnlyConverter fixes this; tests assert persisted dates and cost.

## Environment limits

SQL Server services were stopped and localhost:1433 refused connections. Starting
MSSQLSERVER from this session failed. Docker Engine was unavailable. SQL Server
DDL, credentials and full three-service execution against SQL Server have NOT
been verified. No existing databases or server settings were changed.

Tests use test-scoped H2 and local HTTP upstream stubs. They do not verify SQL
Server-specific constraints. Postman is provided for end-to-end verification
after starting SQL Server; no executed Postman result is claimed.
