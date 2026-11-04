# Saga Design – Watch Management

> StudentCode: DE190025. TODO-4.1, TODO-4.2. Instructor review is pending.

## Participants

| Service | Vai trò | Database | Dữ liệu tham gia Saga |
|---|---|---|---|
| MSWatch | Orchestrator | DBWatch | `Watches`, `SagaStates`, `SagaLogs` |
| MSBrand | Participant | DBBrand | `BrandWatches` |

## Saga CREATE_WATCH

| # | Local transaction | Service / DB | Compensation | Idempotent? |
|---|---|---|---|---|
| 1 | CREATE_WATCH – INSERT Watches | MSWatch / DBWatch | DELETE_WATCH | ✔ (`deleteIfExists`) |
| 2 | ASSIGN_BRAND – upsert BrandWatches | MSBrand / DBBrand | UNASSIGN_BRAND | ✔ |

## Saga UPDATE_WATCH

| # | Local transaction | Service / DB | Compensation | Idempotent? |
|---|---|---|---|---|
| 1 | UPDATE_WATCH | MSWatch / DBWatch | RESTORE_WATCH (snapshot `before`) | ✔ |
| 2 | ASSIGN_BRAND (brand mới) | MSBrand / DBBrand | ASSIGN_BRAND_BACK (brand cũ) | ✔ |

## Saga DELETE_WATCH

| # | Local transaction | Service / DB | Compensation | Idempotent? |
|---|---|---|---|---|
| 1 | UNASSIGN_BRAND | MSBrand / DBBrand | ASSIGN_BRAND_BACK | ✔ |
| 2 | DELETE_WATCH | MSWatch / DBWatch | — **PIVOT** | — |

## State machine

```
PENDING → EXECUTING → COMPLETED
               └→ FAILED → COMPENSATING → COMPENSATED
                                 └→ (compensation lỗi) dừng ở COMPENSATING
```

## Quyết định thiết kế

1. Orchestration (không dùng Choreography) vì chỉ có 2 service, luồng ngắn, dễ quan sát.
2. Orchestrator không có `@Transactional`; mỗi bước là 1 local transaction.
3. Compensation chạy LIFO, retry tối đa 3 lần, yêu cầu idempotent.
4. Bước xoá cứng (`DELETE_WATCH`) là pivot, đặt cuối Saga xoá.
5. Validate rẻ (name, price, releaseDate, category) chạy trước khi Saga bắt đầu.

## Failure and reliability

CREATE with brand 999: commit CREATE_WATCH (id 11), ASSIGN_BRAND returns 404, mark FAILED, then COMPENSATING, delete id 11, then COMPENSATED. The next identity is 12; this is compensation after commit, not a database rollback.

RESTORE_WATCH writes the saved before snapshot to the existing WatchID. ASSIGN_BRAND_BACK upserts the saved old BrandID. UNASSIGN_BRAND deletes only the existing link; missing links succeed. DELETE_WATCH compensation deletes only if the row exists. Retrying these operations produces the same final business state.

One @Transactional cannot roll back another service's HTTP commit. 2PC would require distributed locks and XA participation, which these REST APIs do not provide. Saga accepts eventual consistency and does not provide isolation. Persist state and logs independently with REQUIRES_NEW. Retry compensation at most three times with one-second backoff; retain COMPENSATING if retries fail. A committed delete pivot completes the saga and must not trigger compensation of earlier steps.

An HTTP timeout can occur after the participant committed; production recovery must reconcile participant state or register idempotent compensation before sending the request. Gateway must never route /internal/**.
