# Slot13 — Order Saga (Orchestration)

Contributor duy nhất: **haidang2312** (`danghai23122005@gmail.com`).

Java 21, Spring Boot 4.1.1, Spring Framework 7, RestClient, MySQL 8.4,
MongoDB 8.0 (replica set), SQL Server 2022.

## Chạy ứng dụng

```powershell
docker compose up -d --build
docker compose ps
```

| Service | Cổng mặc định | Database |
|---|---|---|
| order-service | 8081 | MySQL / orderdb (host 3307) |
| inventory-service | 8082 | MongoDB / inventorydb (host 27017) |
| payment-service | 8083 | SQL Server / paymentdb (host 14330) |

Nếu cổng bị chiếm, copy `.env.example` thành `.env` và thay cổng host.
URL nội bộ Docker vẫn sử dụng tên service và cổng container.
Mật khẩu trong Compose chỉ dùng cho bài demo cục bộ.

```powershell
Invoke-RestMethod -Method Post http://localhost:8081/api/orders `
  -ContentType application/json `
  -Body '{"customerId":"C1","productId":"P1","quantity":2,"amount":2000}'
Invoke-RestMethod http://localhost:8081/api/orders
Invoke-RestMethod http://localhost:8081/api/orders/1/saga-logs
```

Luồng thành công: create → reserve → pay → confirm.
Khi thất bại, Saga ghi FAILED, chuyển COMPENSATING và hoàn tác theo LIFO
(refund → release → cancel, tùy bước thất bại). Mỗi action HTTP được thử
tối đa 3 lần. Nếu vẫn lỗi, order giữ PENDING / COMPENSATING để xử lý tiếp.
Orchestrator không có transaction toàn cục; từng service thực hiện local
transaction trên database riêng.

Reservation và Payment dùng orderId làm khóa để nhận diện lệnh lặp.
Release/refund khi chưa có giao dịch hoặc đã hoàn tác là no-op.
MongoDB dùng MongoTransactionManager; chỉ seed khi collection rỗng.
SQL seed dùng IF NOT EXISTS nên restart không tạo lại ví.

## Kiểm thử

```powershell
.\inventory-service\mvnw.cmd -f inventory-service/pom.xml test
.\payment-service\mvnw.cmd -f payment-service/pom.xml test
.\order-service\mvnw.cmd -f order-service/pom.xml test
npm ci
npm test
```

Có 17 unit test: kho (6), ví (6), Saga (5). Các test kiểm tra gọi lặp,
lỗi nghiệp vụ, hoàn tác, LIFO và giới hạn 3 lần compensation.

`npm test` chạy collection bằng Newman (Postman CLI runner): KB1–KB5,
KB7; sau đó chạy KB6 theo từng bước, tự dừng payment-service trước khi
đặt hàng và bật lại trong finally để kiểm tra kho, ví và saga logs.
Kết quả JSON nằm trong `evidence/`. Script không reset hoặc xóa database.
Chạy `node scripts/render-evidence.cjs` để dựng báo cáo HTML từ kết quả.
`evidence/report.png` là ảnh báo cáo Newman và saga logs, không phải ảnh
Postman Desktop.

Nếu chạy cổng khác:

```powershell
$env:ORDER_URL='http://localhost:18081'
$env:INVENTORY_URL='http://localhost:18082'
$env:PAYMENT_URL='http://localhost:18083'
npm test
```

Có thể import `Saga_Postman_Collection.json` vào Postman Desktop, sửa ba
collection variable URL, rồi chạy các folder ngoài `Manual` theo thứ tự.
KB6: Snapshot → dừng payment-service → Place order → bật lại payment,
chờ sẵn sàng → Check balances → Saga logs.

KB8 là tùy chọn: đặt PAYMENT_DELAY_MS=15000, recreate order-service,
gửi order rồi dừng payment và inventory trong khoảng chờ. Kỳ vọng
COMPENSATING, 3 log compensation FAILED, không có CANCEL_ORDER.
Bật lại service sau khi kiểm tra; khôi phục Saga là phần mở rộng EXT-4.

Snapshot kiểm tra chênh lệch kho/ví thay vì giá trị tuyệt đối. KB1 vẫn cần
P1 còn ít nhất 2 và ví C1 còn ít nhất 2000. Chạy lặp nhiều lần sẽ tiêu hao
dữ liệu seed. Chỉ reset volume khi chủ động muốn xóa dữ liệu demo.

## Các TODO

| TODO | Nội dung |
|---|---|
| 0 | Compose 3 database, volume và healthcheck |
| 1 | Document inventory, repository, seed |
| 2 | Reserve/release, Mongo transaction, REST inventory |
| 3 | Entity payment, repository, seed SQL idempotent |
| 4 | Pay/refund, local transaction, REST payment |
| 5 | Order, 6 SagaStatus, SagaLog và repository |
| 6 | Các local transaction của OrderService |
| 7 | RestClient.Builder, inventory/payment clients |
| 8 | Orchestrator, LIFO, retry và trạng thái lỗi |
| 9 | API đặt hàng, danh sách và saga logs |
| 10 | Dockerfile nhiều stage, Compose 3 service |
| 11 | Postman collection, Newman, unit tests và bằng chứng |

Phạm vi thực hiện là TODO-0 đến TODO-11; EXT-1 đến EXT-6 là bài mở rộng.
Đây là demo Saga đồng bộ theo tài liệu: stack compensation nằm trong bộ
nhớ và không tự khôi phục sau khi tiến trình chết. Timeout sau khi remote
đã commit cần chiến lược reconciliation như các bài mở rộng trong guide.
