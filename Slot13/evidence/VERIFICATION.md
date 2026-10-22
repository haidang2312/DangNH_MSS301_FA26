# Kết quả kiểm thử

- Java: 17 test, 0 failure (inventory 6, payment 6, Saga 5).
- Newman KB1–KB5, KB7: 96 assertions, 0 failure.
- KB6: 14 assertions, 0 failure; dừng payment-service thật, Saga trả
  CANCELLED / COMPENSATED và hoàn kho; bật lại payment sau kiểm tra.
- Ba database healthy; ba ứng dụng chạy trong Docker.
- Cổng kiểm thử: order 18081, inventory 18082, payment 18083, Mongo 27018.
- KB8 tùy chọn không chạy tích hợp; unit test đã kiểm tra 3 lần retry
  thất bại và không gọi cancelOrder khi compensation chưa hoàn thành.

Xem `report.html`, `report.png`, các `*-summary.json` và saga JSON tại đây.
Ảnh được chụp từ báo cáo Newman bằng Chrome headless.
