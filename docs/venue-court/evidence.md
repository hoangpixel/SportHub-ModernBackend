# Minh chứng kiểm thử Venue Court

Kiểm chứng local ngày 27/09/2026 trên nền develop `a4feda0`, với phần Venue/Court đã tích hợp bảo mật, Request ID, Actuator, AOP và Azure upload của develop.

## Kết quả

- `clean verify`: build thành công. Sau khi chuẩn hóa dependency theo develop, chạy lại `verify`: **93 test, 0 failures, 0 errors, 0 skipped**.
- Test bao gồm Auth/User/Sport, AzureBlobStorageService, JWT/Request ID, repository slice, controller slice và tích hợp ownership/validation/seed của Venue/Court.
- Docker build từ source bằng Temurin 21/Maven Wrapper; stack verify dùng MySQL 8.4 .
- HTTP smoke đạt JWT, Swagger, CRUD, filter phối hợp, 400/401/403/404/409, quyền truy cập Actuator và X-Request-Id.
- Restart đạt: ID của 2 venue, 3 court, 21 schedule không đổi, không trùng dữ liệu.
- Kiểm tra Git sau rebase: develop là tổ tiên của nhánh feature; mô phỏng merge không có xung đột. Đây không phải một lần merge vào develop.

Kết quả local không thay thế CI trên GitHub hoặc review chéo. Chạy lại `./mvnw -B clean verify` trong sporthub-api để tạo báo cáo JUnit ở `target/surefire-reports`; CI lưu báo cáo thành artifact. Lệnh smoke và restart được hướng dẫn trong [README](README.md). Log máy cá nhân và các báo cáo trạng thái cũ không thuộc source cần bàn giao.

## SQL và index

[court-search.sql](court-search.sql) là SQL Hibernate đã ghi nhận cho `GET /api/courts?page=1&size=1&sortBy=pricePerHour&direction=desc`: có JOIN, WHERE, ORDER BY và LIMIT với offset/số hàng. Court search fetch venue/sport, có count query riêng; test xác nhận đúng hai prepared statements cho trang đầy và việc truy cập các quan hệ dùng trong DTO.

[explain.sql](explain.sql) dùng để chạy lại SHOW INDEX và EXPLAIN. [mysql-explain.txt](mysql-explain.txt) lưu output thực tế của lần kiểm chứng trước trên MySQL **8.0.46**, không phải benchmark MySQL 8.4. Plan minh họa chọn idx_court_sport_id khi lọc sân và idx_venue_owner_id khi lọc cơ sở; vẫn có Using filesort/Using where. Index name không bảo đảm tăng tốc LOWER(name) LIKE '%keyword%'. Cần đo lại trên dữ liệu và phiên bản triển khai thật trước khi kết luận hiệu năng.

Integration test chỉ mock các client lưu trữ ngoài phạm vi module; HTTP smoke sử dụng cấu hình Azure thật.
