# SportHub Modern Backend

[![SportHub CI](https://github.com/hoangpixel/SportHub-ModernBackend/actions/workflows/ci.yml/badge.svg)](https://github.com/hoangpixel/SportHub-ModernBackend/actions/workflows/ci.yml)

Backend REST API phục vụ đề tài **“Tìm hiểu và ứng dụng Spring Boot trong xây dựng backend hệ thống quản lý thể thao SportHub”**. Dự án tập trung vào kiến trúc phân tầng, xác thực và phân quyền, quản lý dữ liệu với JPA/Flyway, kiểm thử tự động, triển khai bằng Docker Compose và tích hợp Azure Blob Storage.

**Công nghệ chính:** Java 21 · Spring Boot 4.1.1 · Maven Wrapper · MySQL 8.4 · Spring Data JPA/Hibernate · Spring Security/JWT · Flyway · JUnit 5/Mockito/Spring Test · Docker Compose · GitHub Actions · Azure Blob Storage.

## 1. Kiến trúc và chức năng

Luồng xử lý tổng quát:

```text
HTTP Request -> Spring Security Filter -> Controller -> Service -> Repository -> MySQL
```

| Nhóm chức năng | Nội dung |
|---|---|
| Authentication, User, Role, Permission | Đăng ký, đăng nhập, JWT, thông tin cá nhân, quản trị tài khoản và phân quyền |
| Sport | Tra cứu, quản trị môn thể thao và hình ảnh |
| Venue, Court, CourtSchedule | Quản lý cơ sở/sân, lịch hoạt động, tìm kiếm và phân trang |
| Booking | Đặt sân, kiểm tra trùng lịch, giữ chỗ và quản lý vòng đời đặt sân |
| Payment | Ghi nhận thanh toán mô phỏng, trạng thái thanh toán và hoàn tiền |
| Review | Đánh giá sau khi hoàn thành lượt đặt sân, xem danh sách và điểm trung bình |
| Team, Tournament, Match | Quản lý đội thể thao, giải đấu và trận đấu |

Các thành phần dùng chung: Spring Profiles, Bean Validation, Global Exception Handling, Actuator, Request ID/MDC, Spring AOP và Azure Blob Storage.

- Mã nguồn ứng dụng: [`sporthub-api/`](sporthub-api/)
- Sơ đồ và nguồn thiết kế cơ sở dữ liệu: [`docs/database/`](docs/database/)
- Pipeline CI: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

## 2. Chuẩn bị môi trường

**Chạy bằng Docker Compose (khuyến nghị cho demo):** Docker Desktop và Docker Compose, ứng dụng cùng MySQL được chạy trong container, không bắt buộc cài MySQL/JDK trên máy chỉ để chạy stack Docker.

**Chạy trực tiếp trên Windows:** JDK 21, MySQL 8+, PowerShell và Maven Wrapper (`mvnw.cmd`) có sẵn trong dự án.

Tất cả lệnh chạy ứng dụng bên dưới, trừ khi có ghi chú khác, được thực hiện **trong thư mục `sporthub-api/`**:

```powershell
cd sporthub-api
```

## 3. Tạo `.env` và JWT Secret

Tạo tệp `.env` từ tệp mẫu tại `sporthub-api/`:

```powershell
Copy-Item .env.example .env
```

Điền các biến cấu hình vào `.env` (chỉ là **mẫu**, không dùng nguyên giá trị minh họa):

```dotenv
# Khi chạy trực tiếp bằng Maven và MySQL trên máy (cổng 3306)
DB_URL=jdbc:mysql://localhost:3306/sporthub?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# JWT
JWT_SECRET=your_generated_base64_secret
JWT_EXPIRATION=86400000

# Azure Blob Storage
AZURE_STORAGE_CONNECTION_STRING=your_real_azure_storage_connection_string
AZURE_STORAGE_CONTAINER=sporthub-images
```

### Tạo khóa `JWT_SECRET` bằng PowerShell

`JwtService` giải mã khóa từ **Base64** để ký và kiểm tra JWT. Tạo khóa ngẫu nhiên 32 byte (256 bit):

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
} finally {
    $rng.Dispose()
}
[Convert]::ToBase64String($bytes)
```

Sao chép chuỗi Base64 in ra và gán vào `JWT_SECRET` trong `.env`. Không dùng chuỗi ví dụ trong tài liệu làm khóa thật, không chia sẻ hoặc commit secret. Khi đổi khóa, các JWT đã ký bằng khóa cũ sẽ không còn xác thực được.

**Cách nạp `.env`:** `SporthubApiApplication.java` đã tích hợp thư viện `dotenv-java`, nạp `.env` trước khi khởi tạo Spring Boot. Khi chạy từ IDE, cần đặt **working directory là `sporthub-api/`** hoặc cấu hình biến môi trường tương đương. Biến môi trường hệ điều hành được ưu tiên hơn giá trị cùng tên trong `.env`.

**Lưu ý Azure:** Cấu hình hiện tại tạo Blob client và gọi `createIfNotExists()` khi ứng dụng khởi động. Vì vậy, cần Azure connection string hợp lệ, container hợp lệ và quyền truy cập phù hợp, ngay cả khi chưa thao tác upload ảnh.

> `.env` chứa thông tin bí mật và đã được đưa vào `.gitignore`. Chỉ commit `.env.example` với giá trị minh họa, tuyệt đối không commit `.env` chứa mật khẩu hoặc khóa thực tế.

## 4. Chạy SportHub bằng Docker Compose

Dự án dùng **`sporthub-api/docker-compose.yml`** làm cấu hình Docker Compose chính, gồm hai service:

| Service | Container | Cổng trên máy | Chức năng |
|---|---|---|---|
| `mysql` | `sporthub-mysql` | `3307` -> `3306` | MySQL 8.4, database `sporthub` |
| `backend` | `sporthub-api` | `8080` -> `8080` | Spring Boot REST API |

Bên trong Docker, backend kết nối MySQL qua địa chỉ `mysql:3306`, không phải `localhost:3306`. Cấu hình Compose truyền `DB_URL` trỏ đến database `sporthub` và sử dụng volume `mysql_data` để lưu dữ liệu MySQL.

**Bước 1.** Tạo `.env` theo Mục 3 và khai báo `DB_PASSWORD`, `JWT_SECRET`, `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER`.

**Bước 2.** Kiểm tra cấu hình và khởi động:

```powershell
docker compose config -q
docker compose up --build -d
```

**Bước 3.** Kiểm tra container và log:

```powershell
docker compose ps
docker compose logs -f backend
```

**Bước 4.** Khi không cần chạy nữa:

```powershell
docker compose down
```

`docker compose down` không tự xóa named volume. **Không sử dụng `docker compose down -v` nếu cần giữ dữ liệu**, vì tùy chọn `-v` xóa volume của Compose.

**Quan trọng khi đổi tên database:** Nếu đã khởi tạo volume MySQL bằng cấu hình cũ `sporthub_j2ee`, việc thay `MYSQL_DATABASE` trong YAML **không tự đổi tên hoặc tạo lại database trong volume có sẵn**. Hãy sao lưu dữ liệu, sau đó tạo database `sporthub` trên MySQL hiện có và cho Flyway chạy trên database phù hợp, hoặc khởi tạo volume mới **chỉ khi chắc chắn dữ liệu cũ không cần giữ**. Không xóa dữ liệu thật chỉ để làm sạch môi trường.

**Cổng và biến môi trường:** MySQL mở ra máy host ở cổng `3307`, còn bên trong mạng Compose vẫn là cổng `3306`. Biến `DB_URL` trong Compose ghi đè giá trị `DB_URL` dành cho Maven trong `.env`. Giá trị `JWT_EXPIRATION` của `.env` không được Compose hiện tại truyền riêng vào backend, container dùng mặc định `86400000` ms từ `application.properties` trừ khi bổ sung biến đó vào Compose.

## 5. Chạy trực tiếp bằng Maven trên Windows

Chuẩn bị MySQL cục bộ và tạo database `sporthub` (hoặc cấu hình một database MySQL đã được tạo sẵn đúng mục đích). File `application-dev.properties` sử dụng `DB_URL` từ môi trường, nên cần khai báo biến này trong `.env` khi chạy trực tiếp.

Tại `sporthub-api/`:

```powershell
.\mvnw.cmd spring-boot:run
```

Profile mặc định là `dev`. Nếu dùng MySQL của Docker từ máy host thay vì MySQL cài trực tiếp, URL trong `.env` phải dùng `localhost:3307` (không phải `localhost:3306`).

Để thực nghiệm Flyway trên **database riêng**, chỉnh URL thử nghiệm trong `application-flyway.properties` đúng với database được chọn rồi chạy:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=flyway"
```

Không chạy migration thử nghiệm trên cơ sở dữ liệu quan trọng khi chưa sao lưu.

### Truy cập API

- **Swagger UI:** <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI JSON:** <http://localhost:8080/v3/api-docs>
- **Actuator Health:** <http://localhost:8080/actuator/health>

Đăng nhập qua API, lấy JWT và chọn **Authorize** trong Swagger đối với endpoint cần xác thực. Thông thường `401` là chưa xác thực hợp lệ, `403` là đã xác thực nhưng thiếu quyền hoặc không được phép truy cập tài nguyên.

## 6. Spring Profiles và Flyway Migration

| Profile | Mục đích | Database và quản lý schema |
|---|---|---|
| `dev` (mặc định) | Phát triển/demo | MySQL, Flyway bật, Hibernate `ddl-auto=validate` |
| `test` | Kiểm thử tự động | H2 in-memory, Hibernate `create-drop`, Flyway tắt |
| `flyway` | Thực nghiệm migration riêng | MySQL thử nghiệm, Flyway bật, Hibernate `validate` |
| `prod` | Cấu hình triển khai tham khảo | MySQL qua biến môi trường, Flyway bật, Hibernate `validate`, tắt `DatabaseSeeder` |

Cấu hình nằm tại `sporthub-api/src/main/resources/application*.properties` và `sporthub-api/src/test/resources/application-test.properties`.

Các migration được lưu ở [`sporthub-api/src/main/resources/db/migration/`](sporthub-api/src/main/resources/db/migration/):

| Phiên bản | Vai trò |
|---|---|
| `V1__init_sporthub_schema.sql` | Khởi tạo schema nghiệp vụ |
| `V2__add_venue_unique_constraint.sql` | Tạo ràng buộc UNIQUE `(owner_id, name)` cho `venues` |
| `V3__prepaid_booking_lifecycle.sql` | Bổ sung dữ liệu vòng đời Booking/Payment, các trường hủy/hết hạn/hoàn tiền và index hỗ trợ |

V3 bao gồm thao tác cập nhật dữ liệu cũ, chẳng hạn điều chỉnh một số Booking `CONFIRMED` chưa có Payment sang `PENDING` và bổ sung `expires_at`. Trước khi nâng cấp database có dữ liệu, cần **backup và thử migration trên bản sao**. Không chỉnh sửa nội dung migration đã chạy, không xóa bảng `flyway_schema_history` để ép chạy lại. Thay đổi schema mới phải được quản lý bằng migration mới.

Đối chiếu lịch sử migration trên MySQL:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank,
```

## 7. Kiểm thử và minh chứng CI

Chạy toàn bộ kiểm thử và đóng gói ứng dụng bằng Maven:

```powershell
.\mvnw.cmd clean verify
```

Chạy các bài kiểm thử trọng tâm Booking:

```powershell
.\mvnw.cmd "-Dtest=BookingWorkflowIntegrationTest,BookingRulesTest,BookingLifecycleRulesTest" test
```

Các bài kiểm thử sử dụng JUnit 5, Mockito, Spring Test, MockMvc và các test JPA/Service/Security. **Mặc định, các bài kiểm thử H2 không thay thế việc kiểm chứng trên MySQL thực tế.**

### Kiểm chứng `BookingWorkflowIntegrationTest` trên MySQL riêng

Lớp `BookingWorkflowIntegrationTest` hỗ trợ chế độ MySQL thông qua `BOOKING_TEST_MYSQL_URL`, `BOOKING_TEST_MYSQL_USER`, `BOOKING_TEST_MYSQL_PASSWORD`. **Chỉ sử dụng database trống dành riêng cho kiểm thử**, tên bắt đầu bằng `sporthub_booking_test_` và host `localhost` hoặc `127.0.0.1`, không dùng database `sporthub` chạy demo.

Ví dụ với MySQL riêng đã có database `sporthub_booking_test_local` ở cổng `3306`:

```powershell
$env:BOOKING_TEST_MYSQL_URL = "jdbc:mysql://localhost:3306/sporthub_booking_test_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:BOOKING_TEST_MYSQL_USER = "root"
$env:BOOKING_TEST_MYSQL_PASSWORD = $env:DB_PASSWORD
.\mvnw.cmd "-Dtest=BookingWorkflowIntegrationTest" test
```

Nếu dùng MySQL container được mở ra máy ở cổng `3307`, thay `3306` bằng `3307` trong URL thử nghiệm. Lưu ý `$env:DB_PASSWORD` phải có trong phiên PowerShell hiện tại, việc ứng dụng Java tự nạp `.env` không làm biến này tự xuất hiện trong PowerShell.

### Mốc kiểm chứng đã lưu

Tại **ngày 29/10/2026**, nhánh `develop` đạt merge commit [`16d54cf`](https://github.com/hoangpixel/SportHub-ModernBackend/commit/16d54cf0095dabe92ce3cfd1b2a509fdc8abc9eb) sau [Pull Request #14](https://github.com/hoangpixel/SportHub-ModernBackend/pull/14). [GitHub Actions run `40841435961`](https://github.com/hoangpixel/SportHub-ModernBackend/actions/runs/40841435961) kết thúc thành công, báo cáo đồ án ghi nhận **298 tests, 0 failures, 0 errors, 0 skipped** và `BUILD SUCCESS`. Báo cáo cũng ghi nhận **23 bài kiểm thử Booking trên MySQL** được thực hiện riêng, bao gồm cạnh tranh đặt sân và Transaction Rollback.

Đây là **mốc lịch sử**, không tự động đại diện cho phiên bản mới sau khi thay đổi Docker/README. Sau mỗi PR mới, cần kiểm tra CI và cập nhật kết quả theo run tương ứng, không sửa mốc lịch sử 215/217/298 tests thành số liệu chưa chạy lại.

## 8. Tài khoản demo và bảo mật

Ở profile không phải `prod`, `DatabaseSeeder` chạy các bước khởi tạo Role/Permission, dữ liệu môn thể thao và tài khoản demo:

| Tài khoản | Mật khẩu demo | Vai trò |
|---|---|---|
| `user01` | `12345678` | User |
| `owner01` | `12345678` | User, Owner |
| `admin01` | `12345678` | User, Admin |

**Chỉ sử dụng các tài khoản và mật khẩu này trong môi trường phát triển/demo.** Không sử dụng mặc định này cho production.

- Xác thực dựa trên Spring Security và JWT, API phân quyền qua Role/Permission và kiểm tra sở hữu tài nguyên.
- `JWT_SECRET` phải là chuỗi Base64 hợp lệ với khóa đủ độ dài.
- Kích thước tệp upload được giới hạn 5 MB trong cấu hình ứng dụng.
- Dịch vụ Azure Blob Storage yêu cầu thông tin kết nối hợp lệ và quyền truy cập phù hợp.
- Profile `prod` tắt `DatabaseSeeder`, vì vậy cần quy trình khởi tạo Role/Permission riêng trước khi vận hành thực tế.

## 9. CI/CD và quy trình Git

Workflow [`SportHub CI`](.github/workflows/ci.yml) chạy khi push lên các nhánh phù hợp (`main`, `develop`, `feat/**`, `fix/**`, `test/**`) và khi tạo Pull Request đến `develop` hoặc `main`.

Pipeline hiện tại thực hiện: checkout source -> thiết lập Java 21 -> `./mvnw -B clean test` -> `./mvnw -B package -DskipTests` -> lưu artifact báo cáo test.

Quy trình đóng góp của nhóm:

```text
Tạo/đồng bộ feature branch -> sửa code -> chạy test -> commit -> push
-> tạo Pull Request vào develop -> kiểm tra CI/review -> merge
```

Các thay đổi trên `develop` được tích hợp qua Pull Request theo quy tắc bảo vệ nhánh. Kết quả CI xanh cho thấy các bước pipeline được cấu hình đã thành công, **không đồng nghĩa** hệ thống đã được triển khai production hoặc kiểm chứng đầy đủ trên MySQL.

## 10. Giới hạn và tài liệu liên quan

- **Payment/Refund hiện là mô phỏng nghiệp vụ**, chưa tích hợp cổng thanh toán hoặc hoàn tiền thực tế.
- Kiểm thử tự động trên H2, kiểm chứng MySQL và kết quả chạy Docker là các phạm vi khác nhau, cần ghi nhận minh chứng riêng.
- Cần hoàn thiện quy trình sao lưu/khôi phục, quản lý secrets và khởi tạo dữ liệu nền tảng an toàn trước khi triển khai production.
- Profile `prod` trong repository là cấu hình tham khảo, **chưa phải cấu hình production hoàn chỉnh**.

**Tài liệu:** [Sơ đồ database](docs/database/) · [Lịch sử Pull Request](https://github.com/hoangpixel/SportHub-ModernBackend/pulls) · [GitHub Actions](https://github.com/hoangpixel/SportHub-ModernBackend/actions).