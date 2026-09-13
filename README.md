# SportHub J2EE Backend

Backend cho hệ thống **SportHub** được xây dựng bằng Spring Boot, Spring Security, JWT, Spring Data JPA và MySQL.

## 1. Công nghệ sử dụng

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Security
- JWT - JJWT
- MySQL
- Bean Validation
- Lombok
- Swagger / OpenAPI - springdoc
- Maven

## 2. Module hiện tại

Nhánh `feat/auth-user-sport` phụ trách các chức năng:

- Authentication
  - Register
  - Login
  - BCrypt password hashing
  - JWT authentication
- Authorization
  - Role: `USER`, `OWNER`, `ADMIN`
  - Permission-based authorization
  - `@PreAuthorize`
- User
  - Current user/profile
  - Update profile
  - Change password
- Admin User Management
  - List users
  - Search users
  - Pagination
  - User detail
  - Update status
  - Update roles
- Sport
  - List/search/pagination
  - Detail
  - Create
  - Update
  - Delete
- Global exception handling
- Swagger/OpenAPI
- Development seed data

## 3. Cấu trúc package chính

```text
src/main/java/com/sporthub/
├── config/
├── constant/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── entity/
├── enums/
├── exception/
├── repository/
├── security/
├── seeder/
└── service/
```

## 4. Database

Tạo database MySQL:

```sql
CREATE DATABASE sporthub_j2ee;
```

Cấu hình trong `src/main/resources/application.properties`:

```properties
spring.application.name=sporthub-api

spring.datasource.url=jdbc:mysql://localhost:3306/sporthub_j2ee?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
```

> Không commit JWT secret thật hoặc thông tin nhạy cảm lên GitHub.

## 5. Tạo JWT secret

PowerShell:

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Set tạm cho terminal hiện tại:

```powershell
$env:JWT_SECRET="YOUR_BASE64_SECRET"
```

Hoặc lưu persistent cho tài khoản Windows hiện tại:

```powershell
[System.Environment]::SetEnvironmentVariable(
    "JWT_SECRET",
    "YOUR_BASE64_SECRET",
    "User"
)
```

Sau khi set persistent, đóng và mở lại terminal/VS Code.

## 6. Chạy project

```bash
mvn clean test
mvn spring-boot:run
```

Backend mặc định chạy tại:

```text
http://localhost:8080
```

## 7. Swagger / OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Để test API cần JWT:

1. Gọi `POST /api/auth/login`.
2. Copy `accessToken`.
3. Bấm **Authorize** trên Swagger.
4. Paste token.
5. Gọi các API protected.

## 8. Tài khoản seed mặc định

| Username | Password | Role |
|---|---|---|
| `user01` | `12345678` | USER |
| `owner01` | `12345678` | USER, OWNER |
| `admin01` | `12345678` | USER, ADMIN |

> Các tài khoản trên chỉ phục vụ development/demo.

## 9. Sport seed mặc định

- Football
- Badminton
- Tennis
- Basketball
- Volleyball
- Pickleball

## 10. API chính

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
```

### Current User

```text
GET /api/users/me
PUT /api/users/me
PUT /api/users/me/password
```

### Admin User

```text
GET /api/admin/users
GET /api/admin/users/{id}
PUT /api/admin/users/{id}/status
PUT /api/admin/users/{id}/roles
```

Hỗ trợ search và pagination:

```text
GET /api/admin/users?keyword=&page=0&size=10
```

### Sport

```text
GET /api/sports
GET /api/sports/{id}
POST /api/admin/sports
PUT /api/admin/sports/{id}
DELETE /api/admin/sports/{id}
```

Search + pagination:

```text
GET /api/sports?keyword=ball&page=0&size=5
```

## 11. Security conventions

Role trong database:

```text
USER
OWNER
ADMIN
```

Spring Security authority tương ứng:

```text
ROLE_USER
ROLE_OWNER
ROLE_ADMIN
```

Permission được khai báo tại:

```text
com.sporthub.constant.Permissions
```

Ví dụ bảo vệ endpoint:

```java
@PreAuthorize(
    "hasAuthority(T(com.sporthub.constant.Permissions).VIEW_USERS)"
)
```

### Khi thêm API mới

- API public: thêm matcher cần thiết vào `SecurityConfig.authorizeHttpRequests(...)`.
- API cần đăng nhập: để `.anyRequest().authenticated()` xử lý.
- API cần quyền chi tiết: dùng `@PreAuthorize` với permission phù hợp.
- Không thêm `/api/admin/**` vào `permitAll()`.

## 12. Exception handling

Các exception dùng chung nằm trong:

```text
com.sporthub.exception
```

Hiện có:

- `ConflictException` → 409
- `ResourceNotFoundException` → 404
- `GlobalExceptionHandler`
  - validation → 400
  - login sai → 401
  - account disabled → 401
  - conflict → 409
  - not found → 404
  - invalid business input → 400

Chỉ chỉnh `GlobalExceptionHandler` khi module mới có loại lỗi chung cần chuẩn hóa.

## 13. HTTP status chính

```text
200 OK            - GET/PUT thành công
201 Created       - tạo mới thành công
400 Bad Request   - dữ liệu không hợp lệ
401 Unauthorized  - chưa đăng nhập/token sai/login sai
403 Forbidden     - đã đăng nhập nhưng không đủ quyền
404 Not Found     - resource không tồn tại
409 Conflict      - dữ liệu trùng hoặc xung đột
```

## 14. Git workflow

Feature branch hiện tại:

```text
feat/auth-user-sport
```

Trước khi commit:

```bash
git status
git diff
mvn clean test
```

Commit module:

```bash
git add .
git status
git commit -m "feat: complete auth user and sport backend modules"
git push origin feat/auth-user-sport
```

Sau khi hoàn tất và review:

```text
Pull Request: feat/auth-user-sport -> develop
```

Không push trực tiếp lên `main` trong workflow hiện tại.

## 15. Lưu ý cho thành viên tích hợp module mới

1. Kiểm tra `Permissions.java` trước khi gán quyền cho Controller.
2. Dùng `@PreAuthorize` cho endpoint cần quyền cụ thể.
3. Chỉ thêm route vào `SecurityConfig` nếu route đó thực sự cần public.
4. Dùng exception dùng chung thay vì trả lỗi thủ công ở Controller.
5. Business logic đặt trong Service.
6. Query database đặt trong Repository.
7. Controller chỉ nhận request và trả response.
8. Không trả `passwordHash` ra DTO response.
9. Test API bằng Swagger trước khi merge.
10. Không commit JWT secret, database password hoặc credential thật.

---

## Trạng thái module của Hoàng

- Entity / Relationship: hoàn thành
- Repository: hoàn thành
- Seeder: hoàn thành
- Register / Login / JWT: hoàn thành
- Role / Permission Authorization: hoàn thành
- Admin User Management: hoàn thành
- Profile / Change Password: hoàn thành
- Sport CRUD + Search + Pagination: hoàn thành
- Swagger/OpenAPI: hoàn thành
- Exception Handling: hoàn thành cơ bản
