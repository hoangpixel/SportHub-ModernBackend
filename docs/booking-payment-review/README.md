# Booking · Payment · Review — SportHub Backend

Tài liệu này mô tả **mã nguồn trên nhánh `feat/booking-payment-review` tại commit `9111a7e`**, phục vụ code review, chạy demo và kiểm chứng nhận xét của giảng viên. Các giao dịch Payment/Refund hiện là **mô phỏng nội bộ**, không tích hợp cổng thanh toán thật.

## 1. Các thành phần mã nguồn

| Tầng | Thành phần |
|---|---|
| API | `BookingController`, `OwnerBookingController`, `AdminBookingController`, `CourtAvailabilityController`, `PaymentController`, `ReviewController` |
| Service | `BookingService`, `PaymentService`, `CourtAvailabilityService`, `ReviewService`, `BookingExpiryJob`, `BookingTime` |
| Repository | `BookingRepository`, `CourtRepository`, `PaymentRepository`, `ReviewRepository` |
| Dữ liệu | `Booking`, `Payment`, `Review`, các enum trạng thái |
| Migration | `V3__prepaid_booking_lifecycle.sql` |
| Test | `BookingWorkflowIntegrationTest`, `BookingRulesTest`, `BookingLifecycleRulesTest`, các test Repository/Service hiện có |

## 2. Quy tắc vòng đời Booking trả trước

1. Người dùng chọn Court còn hoạt động, ngày trong tương lai và giờ thuộc khung hoạt động của sân.
2. Khi tạo Booking, Service khóa bản ghi Court bằng `PESSIMISTIC_WRITE`, sau đó kiểm tra chồng khoảng giờ theo điều kiện **startA < endB và endA > startB**. Hai khoảng sát nhau ở cùng điểm kết thúc/bắt đầu không được xem là chồng lấn.
3. Booking mới ở trạng thái `PENDING` và được giữ chỗ tối đa **10 phút**, không kéo dài quá giờ bắt đầu.
4. Người dùng thanh toán mô phỏng trước hạn. Payment được ghi `COMPLETED`, Booking chuyển `CONFIRMED` trong một transaction. Một Booking không được tạo nhiều Payment.
5. Sau khi kết thúc khung giờ và đã có Payment hoàn tất, Owner/Admin có thể ghi nhận `COMPLETED` hoặc `NO_SHOW` theo điều kiện nghiệp vụ.
6. Booking `PENDING` chưa thanh toán có thể được chủ Booking hủy. Owner/Admin có thêm thao tác quản lý Booking theo quyền và lý do hủy. Nếu hủy Booking đã thanh toán, Payment chuyển `REFUND_PENDING`, Admin có thể ghi nhận `REFUNDED` với mã tham chiếu.
7. Job `BookingExpiryJob` chuyển các Booking `PENDING` quá hạn sang `EXPIRED`. Lịch chạy mặc định cố định độ trễ 15 giây nếu được bật bằng cấu hình.

Các Booking `CANCELLED` và `EXPIRED`, cùng Booking `PENDING` đã hết hạn giữ chỗ, không còn chặn slot mới trong truy vấn lịch. Booking `CONFIRMED`, `COMPLETED` và `NO_SHOW` được tính là chiếm khung giờ theo truy vấn hiện tại.

### Trạng thái

- **Booking:** `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `EXPIRED`, `NO_SHOW`.
- **Payment:** `PENDING`, `COMPLETED`, `FAILED`, `REFUND_PENDING`, `REFUNDED`.
- **Payment methods** được enum hỗ trợ: `CASH`, `BANK_TRANSFER`, `MOMO`, `VNPAY`. Các giá trị này trong code hiện chỉ là cách ghi nhận phương thức, **không chứng minh kết nối hay thu tiền từ nhà cung cấp**.
- **Review:** chỉ được tạo sau khi Booking của chính người dùng đã `COMPLETED`, mỗi Booking tối đa một Review, Review `HIDDEN` không xuất hiện ở danh sách công khai hoặc điểm trung bình.

## 3. API đối chiếu từ Controller

### Người dùng

| Method | Endpoint | Vai trò |
|---|---|---|
| `POST` | `/api/bookings` | Tạo Booking, quyền `CREATE_BOOKING` |
| `GET` | `/api/bookings/my` | Danh sách Booking của bản thân, quyền `VIEW_OWN_BOOKINGS` |
| `GET` | `/api/bookings/{id}` | Chi tiết Booking có kiểm tra người sở hữu |
| `PATCH` | `/api/bookings/{id}/cancel` | Hủy Booking PENDING của bản thân khi còn hợp lệ |
| `POST` | `/api/payments` | Ghi nhận Payment mô phỏng cho Booking của bản thân |
| `GET` | `/api/payments/booking/{bookingId}` | Xem Payment của Booking do mình sở hữu |
| `POST` | `/api/reviews` | Tạo Review cho Booking hoàn tất, quyền `CREATE_REVIEW` |

`BookingController` còn khai báo `PATCH /api/bookings/{id}/complete` với quyền `VIEW_OWNER_BOOKINGS`. Nghiệp vụ bên trong vẫn kiểm tra quyền quản lý sân, nên đây **không phải endpoint hoàn tất dành cho User thông thường**. Owner/Admin có endpoint quản lý riêng bên dưới.

### Owner

Các API `/api/owner/bookings` yêu cầu `VIEW_OWNER_BOOKINGS`, đồng thời Service kiểm tra quyền sở hữu Court/Venue đối với thao tác cụ thể.

| Method | Endpoint | Vai trò |
|---|---|---|
| `GET` | `/api/owner/bookings` | Tra cứu Booking sân được quản lý, hỗ trợ `courtId`, `date`, `status`, `page`, `size` |
| `GET` | `/api/owner/bookings/{id}` | Xem chi tiết Booking thuộc sân mình quản lý |
| `PATCH` | `/api/owner/bookings/{id}/cancel` | Hủy có lý do |
| `PATCH` | `/api/owner/bookings/{id}/complete` | Đánh dấu hoàn tất theo điều kiện |
| `PATCH` | `/api/owner/bookings/{id}/no-show` | Ghi nhận vắng mặt có lý do |

### Admin

Các API `/api/admin/bookings` yêu cầu `ROLE_ADMIN` **và** `VIEW_OWNER_BOOKINGS`. Các endpoint danh sách, chi tiết, hủy, hoàn tất và no-show tương ứng với Owner, đồng thời có:

| Method | Endpoint | Vai trò |
|---|---|---|
| `PATCH` | `/api/admin/bookings/{id}/refund` | Ghi nhận hoàn tiền mô phỏng, bắt buộc mã tham chiếu |

### Public

| Method | Endpoint | Vai trò |
|---|---|---|
| `GET` | `/api/courts/{courtId}/availability?date=YYYY-MM-DD` | Tra cứu khung giờ đã chiếm và còn trống |
| `GET` | `/api/reviews/court/{courtId}` | Danh sách Review công khai |
| `GET` | `/api/reviews/court/{courtId}/average` | Điểm đánh giá trung bình |

## 4. JSON request để thử trên Swagger

**Tạo Booking** — chọn `courtId` đang tồn tại, thời gian trong tương lai, nằm trong giờ hoạt động và không trùng lịch:

```json
{
  "courtId": 1,
  "bookingDate": "2026-11-05",
  "startTime": "08:00:00",
  "endTime": "10:00:00"
}
```

**Thanh toán mô phỏng** — dùng `bookingId` thực tế trả về từ bước tạo Booking:

```json
{
  "bookingId": 1,
  "paymentMethod": "BANK_TRANSFER"
}
```

**Owner/Admin hủy có lý do**:

```json
{
  "reason": "Sân cần bảo trì đột xuất"
}
```

**Admin ghi nhận hoàn tiền**:

```json
{
  "reference": "REF-DEMO-001"
}
```

**Review** — chỉ sau khi Booking hoàn tất:

```json
{
  "bookingId": 1,
  "rating": 5,
  "comment": "Sân tốt"
}
```

Các giá trị ID và ngày giờ là **ví dụ**, cần thay bằng dữ liệu hợp lệ tại thời điểm chạy. Truy cập Swagger tại `http://localhost:8080/swagger-ui/index.html`, đăng nhập để lấy JWT và chọn Authorize cho endpoint có bảo vệ.

## 5. Flyway V3 và an toàn dữ liệu

File `sporthub-api/src/main/resources/db/migration/V3__prepaid_booking_lifecycle.sql` chạy sau V1 và V2. Nội dung chính:

- Mở rộng trạng thái Booking: `EXPIRED`, `NO_SHOW`.
- Mở rộng trạng thái Payment: `REFUND_PENDING`.
- Thêm `expires_at`, `cancellation_reason`, `cancelled_at`, `cancelled_by`, `outcome_reason`, `resolved_by`, `resolved_at` cho Booking.
- Thêm `refunded_at`, `refunded_by`, `refund_reference` cho Payment.
- Chuyển một số Booking `CONFIRMED` không có Payment thành `PENDING`.
- Tính `expires_at` cho các Booking `PENDING`, thêm index `idx_booking_expiry(status, expires_at)`.

**Trước khi nâng cấp database đã có dữ liệu:** xuất bản sao lưu, ghi nhận trạng thái Booking/Payment cũ, kiểm tra index/cột hiện có và chạy trên một **database sao chép** trước. V3 có lệnh `UPDATE`, không đơn thuần là chỉnh sửa schema. Không xóa `flyway_schema_history` hoặc sửa V1/V2/V3 đã chạy để xử lý lỗi cấu trúc.

Sau khi chạy trên database thử nghiệm, đối chiếu bằng SQL:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank
```

Kỳ vọng V1, V2, V3 đều `success = 1`, nhưng cần **kiểm tra thật** trước khi đưa thành kết quả trong báo cáo.

## 6. Test và minh chứng

### Chạy bộ test

Tại `sporthub-api/` trong PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Chạy riêng luồng Booking:

```powershell
.\mvnw.cmd "-Dtest=BookingWorkflowIntegrationTest" test
```

Test trong `BookingWorkflowIntegrationTest` dùng `@SpringBootTest`, MockMvc có Spring Security Filter Chain và có các ca tạo, thanh toán, hủy, hoàn tất, Review, xác thực/phân quyền, race Booking, race Payment và rollback. **Mặc định dùng H2**, chưa phải minh chứng chạy trên MySQL.

### Chạy BookingWorkflowIntegrationTest trên MySQL riêng

Tạo **database mới chỉ phục vụ test**, tên bắt đầu bằng `sporthub_booking_test_`. Ví dụ:

```sql
CREATE DATABASE sporthub_booking_test_20261029
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci
```

Thiết lập biến môi trường tại PowerShell (cần `DB_PASSWORD` đã được cung cấp trên máy):

```powershell
$env:BOOKING_TEST_MYSQL_URL = "jdbc:mysql://localhost:3306/sporthub_booking_test_20261029?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:BOOKING_TEST_MYSQL_USER = "root"
$env:BOOKING_TEST_MYSQL_PASSWORD = $env:DB_PASSWORD

.\mvnw.cmd "-Dtest=BookingWorkflowIntegrationTest" test
```

Khi đặt `BOOKING_TEST_MYSQL_URL`, test chuyển sang driver MySQL, Hibernate `validate` và bật Flyway. Test kiểm tra tiền tố tên database và chỉ chấp nhận host `localhost` hoặc `127.0.0.1` để hạn chế nguy cơ sử dụng nhầm database.

**Chú ý:** Chỉ chạy test trên database **trống, tách biệt và có thể bỏ đi**, vì test sẽ tạo dữ liệu nghiệp vụ. Không sử dụng database `sporthub`, `sporthub_j2ee`, `sporthub_flyway_test` hoặc bất kỳ database thật nào có dữ liệu cần giữ. Nếu chạy không thành công, đọc lỗi và chỉnh trong môi trường thử nghiệm, không ép sửa history trên database thật.

### Kết quả CI hiện có

GitHub Actions trên nhánh `feat/booking-payment-review`, commit [`9111a7e`](https://github.com/hoangpixel/SportHub-ModernBackend/commit/9111a7e443c247a4ddac7492ab3141b76d26be5e), workflow [run `40685241548`](https://github.com/hoangpixel/SportHub-ModernBackend/actions/runs/40685241548) đã hoàn thành **298 tests, 0 failures, 0 errors, 0 skipped**, bước build thành công. Đây là kết quả **trên nhánh Booking**, chưa phải kết quả MySQL riêng hoặc `develop` sau merge.

## 7. Rủi ro, giới hạn và hướng kiểm chứng

- `PESSIMISTIC_WRITE` trên Court giúp tuần tự hóa những yêu cầu đặt cùng sân trong database hỗ trợ khóa đúng cách. Cần lưu log MySQL của hai người đặt sân cùng khung giờ để chứng minh trên môi trường mục tiêu.
- Các ca H2 hoặc mock không thay thế kết quả transaction/rollback thực tế trên MySQL.
- Cần chụp `flyway_schema_history` sau V3, dữ liệu Booking trước/sau migration và kết quả hai request cạnh tranh.
- Luồng Payment/Refund là mô phỏng cập nhật trạng thái trong hệ thống, không xử lý tiền thật hay giao dịch hoàn tiền qua provider.
- Trên `prod`, `DatabaseSeeder` bị tắt toàn bộ, nên Role/Permission cần một quy trình khởi tạo an toàn riêng trước triển khai thật.

Khi có kết quả MySQL và merge PR vào `develop`, cập nhật mốc commit, workflow CI và số liệu trong báo cáo theo đúng kết quả đã kiểm chứng.