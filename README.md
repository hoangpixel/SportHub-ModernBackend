# SportHub Modern Backend

[![SportHub CI](https://github.com/hoangpixel/SportHub-ModernBackend/actions/workflows/ci.yml/badge.svg)](https://github.com/hoangpixel/SportHub-ModernBackend/actions/workflows/ci.yml)

Backend REST API minh họa cho đề tài **Tìm hiểu và ứng dụng Spring Boot trong xây dựng hệ thống quản lý thể thao SportHub**. Dự án tập trung vào kiến trúc phân tầng, Spring Data JPA, bảo mật JWT, quản lý cấu hình, Flyway Migration, kiểm thử tự động và tích hợp liên module.

**Công nghệ:** Java 21 · Spring Boot 4.1.1 · Maven Wrapper · MySQL 8 · Spring Security/JWT · Spring Data JPA/Hibernate · JUnit 5/Mockito/Spring Test · Docker Compose · GitHub Actions · Azure Blob Storage.

## 1. Kiến trúc và các module

Luồng tổng quát: `HTTP Request → Security Filter → Controller → Service → Repository → MySQL`.

| Module | Phạm vi chính |
|---|---|
| Authentication, User, Role, Permission | Đăng ký, đăng nhập, JWT, quản lý tài khoản và phân quyền |
| Sport | Tra cứu môn thể thao, quản trị môn thể thao và ảnh |
| Venue, Court, CourtSchedule | Quản lý cơ sở, sân, giờ hoạt động và tìm kiếm/phân trang |
| Booking | Đặt sân trả trước, giữ chỗ, kiểm tra trùng lịch và vòng đời Booking |
| Payment | Ghi nhận thanh toán mô phỏng, đối soát và trạng thái hoàn tiền |
| Review | Đánh giá sau khi hoàn tất Booking, danh sách và điểm trung bình |
| Team, Tournament, Match | Quản lý đội, giải đấu và trận đấu |

Thành phần dùng chung gồm Spring Security, Global Exception Handling, Bean Validation, Spring Profiles, Actuator, Request ID/MDC, Spring AOP và Azure Blob Storage.

Xem chi tiết luồng Booking, các API và test tại [tài liệu Booking/Payment/Review](docs/booking-payment-review/README.md). Sơ đồ database đặt trong [`docs/database/`](docs/database/).

## 2. Yêu cầu môi trường

- JDK 21, Git, Maven Wrapper có sẵn trong dự án.
- MySQL 8 trở lên cho môi trường phát triển hoặc thử nghiệm migration.
- Docker Desktop nếu chạy ứng dụng và MySQL qua Docker Compose.
- Thông tin JWT, mật khẩu MySQL và Azure Storage trong biến môi trường, không commit thông tin bí mật.

Mã nguồn Spring Boot nằm tại thư mục `sporthub-api/`.

## 3. Cấu hình môi trường với Spring Profiles

| Profile | Mục đích | Database / quản lý schema |
|---|---|---|
| `dev` (mặc định) | Phát triển và demo | MySQL, Hibernate `ddl-auto=update`, Flyway tắt |
| `test` | Một số bài kiểm thử tự động | H2 in-memory, `create-drop`, Flyway tắt |
| `flyway` | Thử nghiệm migration | MySQL riêng, Flyway bật, Hibernate `validate` |
| `prod` | Minh họa cấu hình triển khai | MySQL qua biến môi trường, Flyway bật, Hibernate `validate`, không chạy `DatabaseSeeder` |

Cấu hình tương ứng nằm ở `src/main/resources/application*.properties` và `src/test/resources/application-test.properties`.

**Lưu ý:** Profile `prod` mới là cấu hình thực nghiệm, **chưa phải triển khai production hoàn chỉnh**. `DatabaseSeeder` sử dụng `@Profile("!prod")`, nên dữ liệu demo không được tự tạo, nhưng dữ liệu Role/Permission thiết yếu cũng cần phương án chuẩn bị riêng trước khi triển khai thật.

### Biến môi trường

Đặt biến môi trường phù hợp với cách chạy, có thể dùng `.env` tại `sporthub-api/` nếu chạy bằng Docker Compose hoặc qua cơ chế nạp `.env` của ứng dụng.

```dotenv
DB_URL=jdbc:mysql://localhost:3306/sporthub?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=root
DB_PASSWORD=replace_with_local_password
JWT_SECRET=replace_with_secure_secret
JWT_EXPIRATION=86400000
AZURE_STORAGE_CONNECTION_STRING=replace_with_azure_connection_string
AZURE_STORAGE_CONTAINER=sporthub-images
```

Đây chỉ là **giá trị minh họa**. Không đưa mật khẩu hoặc connection string thật vào Git. File `sporthub-api/.env.example` có thể dùng làm danh sách tham khảo các biến cấu hình.

## 4. Chạy ứng dụng

### Cách A — MySQL cài tại máy

Chuẩn bị MySQL và database `sporthub`, cấu hình biến môi trường, sau đó chạy tại `sporthub-api/`:

```powershell
.\mvnw.cmd spring-boot:run
```

Mặc định sử dụng profile `dev`. Để chỉ định profile khi thực nghiệm:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=flyway"
```

Chỉ chạy profile `flyway` trên **database thử nghiệm được chọn đúng trong `application-flyway.properties`**, không chỉnh migration đã chạy trên dữ liệu quan trọng.

### Cách B — Docker Compose

Tại `sporthub-api/`, thiết lập các biến bắt buộc rồi chạy:

```bash
docker compose up --build
```

Cấu hình Compose hiện tại sử dụng MySQL 8.4, database `sporthub_j2ee`, container backend tại cổng `8080`, MySQL lộ ra máy host tại cổng `3307`. Đây là database riêng của cấu hình Docker Compose, không phải URL mặc định của `dev` khi chạy trực tiếp tại máy.

```bash
docker compose down
```

**Cảnh báo:** `docker compose down -v` xóa cả volume dữ liệu MySQL, không sử dụng trên dữ liệu cần giữ.

### Truy cập API

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

Đăng nhập qua API, lấy JWT và dùng **Authorize** trong Swagger cho các endpoint yêu cầu xác thực. `401` là chưa xác thực hợp lệ, `403` là đã xác thực nhưng thiếu quyền hoặc không sở hữu tài nguyên.

## 5. Flyway Migration V1 → V2 → V3

Các file trong `sporthub-api/src/main/resources/db/migration/`:

| Migration | Vai trò |
|---|---|
| `V1__init_sporthub_schema.sql` | Khởi tạo schema SportHub |
| `V2__add_venue_unique_constraint.sql` | UNIQUE `(owner_id, name)` trên bảng `venues` |
| `V3__prepaid_booking_lifecycle.sql` | Bổ sung trạng thái, thông tin hết hạn/hủy/hoàn tiền cho Booking và Payment, cập nhật một phần dữ liệu cũ, tạo index expiry |

V3 có thay đổi dữ liệu: chuyển một số Booking `CONFIRMED` **chưa có Payment** về `PENDING`, bổ sung `expires_at` cho Booking đang chờ thanh toán. Vì thế nên sao lưu database, rà dữ liệu hiện hữu và thử migration trên **bản sao** trước khi nâng cấp database đang sử dụng.

Không sửa các migration đã chạy, không xóa `flyway_schema_history` hoặc index để ép migration chạy lại. Nếu có thay đổi sau V3, tạo migration mới.

**Tình trạng xác minh:** source V3 đã có trên nhánh `feat/booking-payment-review` tại commit `9111a7e`. Việc Flyway V1–V3 chạy thành công trên MySQL thực tế cần được ghi nhận bằng log và bảng `flyway_schema_history`, kết quả CI H2 không thay thế minh chứng này.

## 6. Kiểm thử

Tại `sporthub-api/`:

```powershell
.\mvnw.cmd clean verify
```

Kiểm tra riêng các bài test Booking:

```powershell
.\mvnw.cmd "-Dtest=BookingWorkflowIntegrationTest,BookingRulesTest,BookingLifecycleRulesTest" test
```

Các bài kiểm thử sử dụng JUnit 5, Mockito và Spring Test, gồm service, controller, repository, security và một số luồng tích hợp. Một số bài test sử dụng H2, không mặc nhiên là kết quả MySQL.

**Mốc đã xác nhận:** GitHub Actions trên nhánh `feat/booking-payment-review`, commit [`9111a7e`](https://github.com/hoangpixel/SportHub-ModernBackend/commit/9111a7e443c247a4ddac7492ab3141b76d26be5e), run [`40685241548`](https://github.com/hoangpixel/SportHub-ModernBackend/actions/runs/40685241548) có **298 tests, 0 failures, 0 errors, 0 skipped**, build thành công. Đây là mốc trên nhánh Booking, **chưa phải mốc đã merge vào `develop`**.

Bài `BookingWorkflowIntegrationTest` cho phép chuyển sang MySQL bằng bộ biến `BOOKING_TEST_MYSQL_*` trên một database riêng. Xem cú pháp và cảnh báo dữ liệu tại [README Booking](docs/booking-payment-review/README.md).

## 7. Tài khoản demo và bảo mật

Trong profile không phải `prod`, `DatabaseSeeder` chạy quy trình seed dữ liệu phục vụ demo, bao gồm Role/Permission và các tài khoản mẫu. Tài khoản được ghi nhận trong source có tên `user01`, `owner01`, `admin01` và mật khẩu demo `12345678`. **Không sử dụng các thông tin demo trong production**.

Bảo mật dựa trên Spring Security + JWT, Role/Permission và kiểm tra quyền sở hữu tài nguyên. Một số endpoint tra cứu Sport, Venue, Court và Review mở công khai, còn các thao tác đặt sân/thanh toán/quản lý yêu cầu xác thực và authority tương ứng.

Azure Blob Storage phục vụ ảnh, file upload có giới hạn 5 MB, thông tin truy cập Azure phải được cung cấp ngoài source.

## 8. CI và Git workflow

Workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml). Pipeline chạy trên `push` và Pull Request phù hợp, thiết lập Java 21, thực thi Maven test, build và lưu artifact kết quả test.

Quy trình nhóm: làm việc trên nhánh tính năng → commit → push → Pull Request vào `develop` → kiểm tra CI và review → merge. Nhánh `develop` được bảo vệ bằng quy tắc Pull Request, tránh push trực tiếp.

Sau khi README và code Booking được merge, cập nhật mốc SHA/CI mới nhất trong báo cáo dự án, không thay các mốc lịch sử 215 hoặc 217 tests bằng phép thay thế toàn cục.

## 9. Giới hạn và định hướng tiếp theo

- Việc chạy CI xanh không chứng minh sẵn mọi luồng nghiệp vụ trên MySQL. Cần lưu log MySQL riêng cho Booking cạnh tranh, transaction rollback và Flyway V3.
- `dev` vẫn đang dùng `ddl-auto=update`, trong khi `flyway` và `prod` dùng Flyway với `validate`.
- Việc khởi tạo Role/Permission trên profile `prod` cần hoàn thiện trước triển khai thực tế.
- Các thanh toán hiện tại là **mô phỏng trạng thái**, không có bằng chứng xử lý giao dịch hoặc hoàn tiền qua cổng thanh toán thật.

## 10. Tài liệu và sơ đồ

- [Booking, Payment và Review — API, quy tắc nghiệp vụ, test](docs/booking-payment-review/README.md)
- [Sơ đồ và nguồn thiết kế database](docs/database/)
- [GitHub Actions](https://github.com/hoangpixel/SportHub-ModernBackend/actions)

Đây là sản phẩm nghiên cứu và thực hành trong học phần, không mặc nhiên đạt các yêu cầu vận hành của một dịch vụ thương mại.