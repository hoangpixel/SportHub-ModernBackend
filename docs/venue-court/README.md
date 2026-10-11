# Báo cáo module Venue Court của Huy

Module quản lý cơ sở thể thao, sân và lịch hoạt động theo tuần, tương ứng ba bảng `venues`, `courts`, `court_schedules`. Giá MVP nằm trực tiếp trong `Court.pricePerHour`. Tài liệu này mô tả phần triển khai, cách kiểm chứng và nội dung trình bày cá nhân; kết quả chạy được ghi riêng trong `evidence.md`.

## Thiết kế và phân quyền

Luồng xử lý là Controller → Service → Repository → MySQL. Controller nhận dữ liệu, gọi Service và trả DTO. Service kiểm tra chủ sở hữu, dữ liệu nghiệp vụ, tên trùng và tài nguyên không tồn tại; transaction được đặt ở đây. Repository thực hiện truy vấn. Không trả Entity trực tiếp để tránh lộ dữ liệu và đồ thị quan hệ không cần thiết.

Owner lấy danh tính từ `Authentication.getName()`, không nhận ownerId từ body tạo cơ sở. `@PreAuthorize` dùng các permission `CREATE_VENUE`, `UPDATE_OWN_VENUE`, `MANAGE_OWN_COURTS` theo quy ước chung của dự án. Permission được cấp qua role; Service tiếp tục kiểm tra username của chủ cơ sở. Biết ID của sân người khác không đủ để sửa hoặc xóa sân đó.

Đường dẫn lồng nhau được kiểm tra: court phải thuộc venue trên URL và schedule phải thuộc court trên URL. Sai quan hệ trả 404; đúng quan hệ nhưng khác chủ trả 403. Danh sách sân trong API owner cũng kiểm tra ownership; API public vẫn cho phép tra cứu sân.

## API và dữ liệu đầu vào

| API | Ý nghĩa |
|---|---|
| GET `/api/venues` | Keyword, city, district, ownerId, page, size, sortBy, direction |
| GET `/api/venues/{id}` | Chi tiết cơ sở public |
| GET `/api/courts` | Keyword, sportId, venueId, minPrice, maxPrice, page, size, sortBy, direction |
| GET `/api/courts/{id}` | Chi tiết sân public |
| GET `/api/courts/{id}/schedules` | Lịch hoạt động public |
| GET/POST `/api/owner/venues` | Danh sách cơ sở của mình, tạo cơ sở |
| PUT/DELETE `/api/owner/venues/{id}` | Sửa/xóa cơ sở của mình |
| GET/POST `/api/owner/venues/{venueId}/courts` | Danh sách/tạo sân trong cơ sở của mình |
| PUT/DELETE `/api/owner/venues/{venueId}/courts/{courtId}` | Sửa/xóa sân |
| POST `.../courts/{courtId}/schedules` | Tạo lịch tuần |
| PUT/DELETE `.../courts/{courtId}/schedules/{scheduleId}` | Sửa/xóa lịch |

`page` bắt đầu từ 0; `size` từ 1 đến 100. Venue cho phép sort theo id, name, city, district, createdAt, updatedAt. Court cho phép id, name, pricePerHour, createdAt, updatedAt. Hướng sort là asc/desc; thêm id làm tiêu chí phụ để các bản ghi có cùng giá/tên có thứ tự ổn định. Khoảng giá không âm và minPrice không lớn hơn maxPrice.

Tên và địa chỉ cơ sở bắt buộc. Sân có sportId và giá lớn hơn 0. Giờ mở/đóng sân có thể cùng bỏ trống, nhưng nếu nhập phải có cả hai và giờ mở trước giờ đóng. Lịch yêu cầu ngày trong tuần và giờ hợp lệ; một sân chỉ có một lịch cho mỗi ngày. Mô hình hiện tại không hỗ trợ một khoảng mở cửa đi qua nửa đêm.

HTTP: 201 khi tạo; 200 khi đọc/sửa/xóa; 400 khi validation/tham số sai; 401 khi chưa đăng nhập; 403 khi thiếu permission hoặc khác chủ; 404 khi tài nguyên/quan hệ URL không tồn tại; 409 khi trùng tên/lịch hoặc vi phạm ràng buộc dữ liệu. Lỗi 500 được ghi log bởi handler chung.

Swagger ở `/swagger-ui.html`, JSON ở `/v3/api-docs`; nhóm Venue/Court public không yêu cầu Bearer, nhóm Owner sử dụng Bearer JWT.

## JPA và giải thích SQL

`VenueRepository.searchVenues` và `CourtRepository.searchCourts` dùng JPQL: tên trong query là Entity/thuộc tính Java, Hibernate chuyển thành bảng/cột SQL. Các tham số null hoặc keyword rỗng làm bỏ qua bộ lọc tương ứng. Điều kiện được nối bằng AND; tìm keyword sử dụng LOWER và LIKE để tìm chuỗi con không phân biệt hoa thường.

Derived query `findByVenue_IdAndSport_Id` được Spring Data phân tích thành điều kiện theo venue_id và sport_id. `PageRequest` cung cấp sort và phân trang. MySQL dùng LIMIT/OFFSET; H2 trong test có thể in cú pháp OFFSET/FETCH. Page thường có một SELECT nội dung và một SELECT COUNT, nhưng có thể bỏ COUNT khi suy ra tổng từ trang cuối.

DTO Court cần tên venue và sport; DTO Venue cần username chủ sở hữu. Court search dùng `JOIN FETCH` cho venue/sport và count query riêng; các derived query danh sách Court và Venue search dùng `@EntityGraph` để tải các quan hệ to-one cùng truy vấn nội dung, tránh N+1 khi ánh xạ DTO. Test `pageDtoRelationsNeedOnlyContentAndCountQueries` xóa persistence context, reset thống kê, đọc một trang đầy và truy cập các quan hệ rồi xác nhận đúng hai prepared statements. Kết quả này áp dụng cho truy vấn được test, không khẳng định mọi endpoint đều không có N+1.

Index đã khai báo cho venue owner_id/name/city/district và court sport_id/venue_id/name/price_per_hour. Chúng hỗ trợ lọc bằng khóa ngoại hoặc một số thao tác sắp xếp/lọc. Index B-tree trên name không bảo đảm tăng tốc `LOWER(name) LIKE '%keyword%'`; wildcard đầu và hàm LOWER hạn chế khả năng dùng index. Cần xem EXPLAIN trên dữ liệu thật trước khi thêm composite/full-text index; full-text có ngữ nghĩa khác tìm chuỗi con. Với dữ liệu demo rất ít, optimizer có thể chọn quét toàn bảng là hợp lý.

Bật profile dev để quan sát SQL, bind parameters và thống kê Hibernate. Không dùng bind TRACE cho môi trường chứa dữ liệu người dùng thật. Lệnh và output minh chứng nằm trong `explain.sql`, `mysql-explain.txt`, `evidence.md`.

## Kiểm thử và seed

- Unit test Service: tạo thành công, tên trùng, thiếu tài nguyên, ownership, lịch trùng và giờ sai.
- `@DataJpaTest`: Venue/Court search với nhiều điều kiện, case-insensitive, null/rỗng, owner/district/sport/venue, khoảng giá, tổng số bản ghi, phân trang/sort, derived query, số truy vấn.
- `@WebMvcTest`: security config và JWT filter thật, mock Service; kiểm tra 201/400/401/403/404/409 và validation.
- Integration test: Controller–Service–Repository thật với H2, sửa/xóa tài nguyên người khác, quan hệ URL sai, owner CRUD, dữ liệu đầu vào sai, seed lặp và phục hồi dữ liệu bị thiếu.
- HTTP smoke trên Docker/MySQL: đăng nhập lấy JWT thật, CRUD, tìm kiếm phối hợp, Swagger và các HTTP status; so sánh ID seed trước/sau restart.

Seed Venue/Court chạy trong transaction. Khi venue hoặc court đã tồn tại, dùng lại đối tượng đó và tiếp tục kiểm tra các sân/lịch còn thiếu. Tránh lặp dữ liệu khi khởi động lại và hỗ trợ phục hồi lần seed chưa đầy đủ. Test recovery bao gồm xóa lịch mẫu và xóa sân mẫu rồi seed lại. Seeder dành cho demo; đây chưa phải thiết kế triển khai nhiều replica đồng thời hay môi trường production.

## Cách chạy và nghiệm thu

Yêu cầu Docker Desktop/Docker Engine với Compose v2. Từ `sporthub-api`, chạy `docker compose -p sporthub-huy-verify -f compose.verify.yml up -d --build`. Dockerfile đồng bộ với `feat/auth-user-sport`: build từ source bằng Maven Wrapper trên `eclipse-temurin:21-jdk`, chạy bằng `eclipse-temurin:21-jre`; cả hai file Compose dùng MySQL 8.4. Không yêu cầu có sẵn target/JAR. Stack verify giữ healthcheck TCP để tránh khởi động backend trước khi database thực sự sẵn sàng. Compose chính và verify cùng dùng MySQL 8.4. Sau khi cập nhật nền develop, stack verify đã được cấu hình đồng bộ để sử dụng Azure Blob Storage (thay vì Azurite) cho upload của Auth/User; yêu cầu có AZURE_STORAGE_CONNECTION_STRING. Cấu hình verify dùng database riêng, cổng loopback 18080, thông tin demo công khai; không dùng `.env` cá nhân.

Từ thư mục gốc repo, dùng PowerShell 7:

```powershell
./scripts/verify-venue-court.ps1 -SnapshotPath ./seed-snapshot.json
```

Từ `sporthub-api`, chạy `docker compose -p sporthub-huy-verify -f compose.verify.yml restart`, rồi chạy lại script từ root với cùng SnapshotPath và thêm `-AfterRestart`. Khi xong, `docker compose -p sporthub-huy-verify -f compose.verify.yml down` dừng stack và giữ dữ liệu. Chỉ thêm `-v` nếu muốn xóa database kiểm thử riêng đó.

Để chạy stack thông thường ở cổng 8080, sao chép `.env.example` thành `.env`, điền DB_PASSWORD và JWT_SECRET riêng, rồi `docker compose up -d --build` trong `sporthub-api`. Không commit `.env`. Bỏ theo dõi `.env` không xóa các phiên bản đã có trong lịch sử Git; credential thật từng commit cần được chủ tài khoản thay thế.

Chạy kiểm thử Java 21: `./mvnw -B clean verify` (Windows: `./mvnw.cmd -B clean verify`). CI thực hiện bước này và một job Docker độc lập từ checkout sạch; test reports được upload thành artifact. GitHub Actions chỉ được xác nhận xanh sau khi thay đổi được push và job hoàn tất.

## Nội dung vấn đáp

1. **Ai tạo Bean?** Spring phát hiện Controller/Service qua component scan; Spring Data tạo repository proxy. Constructor injection nhận dependency từ container. Tự new Service có thể bỏ qua transaction proxy và các cơ chế Spring.
2. **Vì sao Controller mỏng?** Controller xử lý HTTP; nghiệp vụ nằm ở Service để tái sử dụng và kiểm thử độc lập.
3. **JPQL khác SQL?** JPQL dùng Entity/thuộc tính, SQL dùng bảng/cột; Hibernate chuyển đổi. Pageable tạo ORDER BY và giới hạn trang; COUNT phục vụ metadata tổng.
4. **LAZY/N+1 là gì?** LAZY tải quan hệ khi truy cập. Một query danh sách cộng nhiều query quan hệ tạo N+1. EntityGraph fetch các quan hệ cần thiết; kiểm chứng bằng SQL/statistics, không chỉ dựa vào annotation.
5. **Ownership hoạt động thế nào?** Permission chặn chức năng; Service so chủ venue với user đăng nhập. Thiếu bước thứ hai sẽ cho chủ sân này sửa tài nguyên chủ khác chỉ bằng ID.
6. **DataJpaTest và SpringBootTest?** DataJpaTest chỉ tải phần JPA, dùng transaction rollback; SpringBootTest tải ứng dụng đầy đủ, phù hợp integration nhưng nặng hơn. WebMvcTest tập trung HTTP/security/validation, không chứng minh logic ownership của Service đã đúng nên cần integration test bổ sung.
7. **Index giúp gì?** Giảm số hàng cần đọc cho bộ lọc phù hợp, nhưng tốn dung lượng và chi phí ghi. Giải thích theo EXPLAIN thực tế; không nói index name luôn tối ưu contains search.
8. **JWT khác session?** JWT gửi trong mỗi request và được xác minh chữ ký/thời hạn; cấu hình API stateless. Session thường tham chiếu trạng thái phía server. JWT không tự giải quyết ownership.
9. **401 và 403?** 401 chưa xác thực hợp lệ; 403 đã xác thực nhưng không đủ quyền. 404 cũng dùng khi ID không thuộc quan hệ URL.
10. **Docker Compose giải quyết gì?** Mô tả image/build, mạng, cấu hình và volume nhất quán; Compose chính giữ healthcheck mysqladmin ping giống nhánh auth; stack verify dùng truy vấn TCP vào đúng database để tránh báo sẵn sàng sớm trong kiểm thử. Vẫn cần kiểm thử lần chạy đầu và restart.
11. **Transaction đảm bảo gì?** Các thay đổi trong một transaction commit/rollback cùng nhau. Mặc định RuntimeException làm rollback. Gọi method nội bộ cùng object không tạo transaction proxy mới; transaction ngoài vẫn có hiệu lực.

## Điều kiện để chốt toàn bộ nhiệm vụ

Mã nguồn và minh chứng local nằm trong repo này. Trạng thái tích hợp mới nhất và kết quả 93 test sau rebase được ghi trong `evidence.md`. Huy còn cần đưa thay đổi lên PR vào develop, nhờ một thành viên khác review, kiểm tra hai job CI trên GitHub và xác nhận bản báo cáo phù hợp mẫu nộp của nhóm. Phần chuẩn bị câu hỏi không thay thế việc Huy tự trình bày. Không tự ghi nhận review chéo hoặc kết quả CI remote khi chưa có bằng chứng.
