# SportHub Modern Backend

[![SportHub CI](https://github.com/hoangpixel/SportHub-ModernBackend/actions/workflows/ci.yml/badge.svg)](https://github.com/hoangpixel/SportHub-ModernBackend/actions/workflows/ci.yml)

SportHub Modern Backend is a Spring Boot REST API for a sports-platform backend. The current backend foundation focuses on authentication, user/role/permission management, sport management, secure file upload, consistent API validation/error handling, automated testing, Docker-based reproducibility, CI, and observability.

> Course context: **Modern Programming Technologies / Spring Boot backend project**  
> Java **21** · Spring Boot **4.1.1** · MySQL · Spring Security · JWT · Spring Data JPA

---

## 1. Main Goals

The project is built to demonstrate both backend implementation and engineering workflow:

- IoC / Dependency Injection managed by Spring.
- Layered REST architecture: Controller → Service → Repository.
- Spring Data JPA / Hibernate for database access.
- Spring Security + JWT authentication and authorization.
- Bean Validation and global exception handling.
- Docker / Docker Compose for reproducible execution.
- Unit testing with JUnit 5 + Mockito.
- Git branch / Pull Request workflow and GitHub Actions CI.
- External file storage with Azure Blob Storage.
- Observability with Spring Boot Actuator, Request ID, MDC logging, and AOP timing.

---

## 2. Tech Stacks

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| REST API | Spring Web MVC |
| ORM | Spring Data JPA + Hibernate |
| Database | MySQL |
| Security | Spring Security + JWT (JJWT) |
| Validation | Jakarta Bean Validation |
| API documentation | Springdoc OpenAPI / Swagger UI |
| File storage | Azure Blob Storage |
| Observability | Spring Boot Actuator + MDC + AOP |
| Testing | JUnit 5 + Mockito |
| Build | Maven Wrapper |
| Containerization | Docker + Docker Compose |
| CI | GitHub Actions |

---

## 3. Current Backend Features

### Authentication and authorization

- Register and login.
- BCrypt password hashing.
- Stateless JWT authentication.
- Role and permission based authorization.
- Method-level authorization with `@PreAuthorize`.
- Disabled/inactive accounts are blocked from logging in.
- Existing JWTs are also rejected after a user becomes inactive because the JWT filter reloads the current user state before creating the authentication context.

### User management

- Get current user profile.
- Partial profile update.
- Change password.
- Avatar upload through Azure Blob Storage.
- Admin user listing with pagination.
- Admin user status / role management.
- Validation for request bodies, pagination parameters, and path variables.

### Sport management

- Public sport listing and sport detail APIs.
- Pagination and keyword filtering.
- Admin sport CRUD operations.
- Image upload integration with Azure Blob Storage.

### Error handling

The API uses a consistent error response format:

```json
{
  "status": 400,
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "page": "Page phải lớn hơn hoặc bằng 0"
  }
}
```

Handled cases include:

- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`
- `500 Internal Server Error`

Unexpected exceptions are logged on the server while clients receive a generic error message instead of an internal stack trace.

---

## 4. Security Notes

The backend currently applies the following security controls:

- Passwords are hashed with BCrypt.
- JWT secret is provided through environment variables.
- Azure credentials are not committed to source control.
- Spring Data JPA is used instead of manually concatenating SQL queries.
- CORS uses an allowlist instead of `*`.
- Request validation is applied before business logic.
- The application uses stateless authentication.
- CSRF is disabled because the current API uses Bearer tokens in the `Authorization` header rather than server-side login sessions.
- Public health information is intentionally minimal.
- Actuator metrics are protected.
- Client-provided Request IDs are validated before being added to logs.

Current local CORS origin:

```text
http://localhost:5173
```

---

## 5. Project Structure

```text
SportHub-ModernBackend/
└── sporthub-api/
    ├── src/
    │   ├── main/
    │   │   ├── java/com/sporthub/
    │   │   │   ├── aspect/
    │   │   │   ├── config/
    │   │   │   ├── constant/
    │   │   │   ├── controller/
    │   │   │   ├── dto/
    │   │   │   ├── entity/
    │   │   │   ├── enums/
    │   │   │   ├── exception/
    │   │   │   ├── repository/
    │   │   │   ├── security/
    │   │   │   ├── seeder/
    │   │   │   └── service/
    │   │   └── resources/
    │   │       └── application.properties
    │   └── test/
    │       └── java/com/sporthub/
    ├── .env.example
    ├── Dockerfile
    ├── docker-compose.yml
    ├── mvnw
    ├── mvnw.cmd
    └── pom.xml
```

The exact package tree may grow as additional SportHub modules are integrated.

---

## 6. Prerequisites

### Local development

Install:

- JDK 21
- Git
- MySQL 8+ or a compatible MySQL server
- Docker Desktop (recommended for the reproducible setup)

Maven does **not** need to be installed globally because the project uses Maven Wrapper.

### Optional external service

Azure Blob Storage is required for real image upload functionality.

---

## 7. Environment Variables

Never commit real secrets to Git.

The project expects the following environment variables:

```env
DB_URL=jdbc:mysql://localhost:3306/sporthub_j2ee?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=root
DB_PASSWORD=your_database_password

JWT_SECRET=your_secure_jwt_secret

AZURE_STORAGE_CONNECTION_STRING=your_azure_storage_connection_string
AZURE_STORAGE_CONTAINER=sporthub-images
```

A safe template is provided in:

```text
sporthub-api/.env.example
```

`.env` and other local secret files must remain ignored by Git.

---

## 8. Run Locally

Move into the backend directory:

```bash
cd sporthub-api
```

Make sure the required environment variables are available and MySQL is running.

### Linux / macOS / Git Bash

```bash
./mvnw spring-boot:run
```

### Windows PowerShell / CMD

```powershell
.\mvnw.cmd spring-boot:run
```

By default, the backend is available at:

```text
http://localhost:8080
```

---

## 9. Run with Docker Compose

Docker Compose is the recommended reproducible setup because it starts both the backend and MySQL.

From `sporthub-api`:

```bash
docker compose up --build
```

Expected services:

- SportHub backend: `http://localhost:8080`
- MySQL inside Docker network: port `3306`
- MySQL exposed to the host by the Compose configuration: port `3307`

Stop containers:

```bash
docker compose down
```

Remove containers **and database volume**:

```bash
docker compose down -v
```

> `down -v` deletes the persisted Docker database volume. Use it only when a clean database is intentionally required.

---

## 10. Seed Data and Test Accounts

The project seeds basic roles, permissions, sports, and test users for local development/demo.

| Account | Email | Password | Main role |
|---|---|---|---|
| `user01` | `user01@example.com` | `12345678` | USER |
| `owner01` | `owner01@example.com` | `12345678` | USER + OWNER |
| `admin01` | `admin01@example.com` | `12345678` | USER + ADMIN |

Seeded sports include examples such as:

- Football
- Badminton
- Tennis
- Basketball
- Volleyball
- Pickleball

> These credentials are development/demo accounts only. They must not be reused for a real production deployment.

---

## 11. Swagger / OpenAPI

After starting the backend, open:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Swagger can be used to:

1. Register or log in.
2. Copy the JWT returned by the login API.
3. Authorize protected endpoints.
4. Test validation, authorization, pagination, and CRUD APIs.

---

## 12. Testing

The project uses **JUnit 5 + Mockito** for unit testing.

Run all tests:

### Linux / macOS / Git Bash

```bash
./mvnw test
```

### Windows

```powershell
.\mvnw.cmd test
```

Examples of currently tested areas include:

- Authentication service.
- User service.
- Sport service.
- Azure Blob Storage service.
- JWT authentication filter.
- Request ID filter.
- Service execution-time Aspect.

Example targeted test:

```bash
./mvnw -Dtest=RequestIdFilterTest test
```

The testing style follows Arrange → Act → Assert and Mockito verification where appropriate.

---

## 13. Observability

SportHub includes a lightweight observability layer.

### Actuator health

Public endpoint:

```text
GET /actuator/health
```

Example response:

```json
{
  "groups": ["liveness", "readiness"],
  "status": "UP"
}
```

Detailed internal information is intentionally hidden from anonymous users.

### Metrics

```text
GET /actuator/metrics
```

Metrics are protected. Anonymous requests receive `401`, normal users without the required role receive `403`, and authorized administrators can inspect available metrics.

Available metrics include examples such as:

- HTTP request metrics.
- JVM memory and GC metrics.
- CPU / process metrics.
- HikariCP connection pool metrics.
- JDBC metrics.
- Spring Security metrics.
- Spring Data repository invocation metrics.

### Request ID and MDC

Each request receives an `X-Request-Id`.

If the client does not provide one, the backend generates a UUID.

```text
Client
  ↓
RequestIdFilter
  ↓
X-Request-Id
  ↓
MDC
  ↓
Controller → Service → Repository
  ↓
Response contains the same X-Request-Id
```

Example log:

```text
INFO [aop-test-001] com.sporthub.aspect.ServiceLoggingAspect - Service execution - SportService.getAllSports took 24 ms
```

The Request ID allows logs from the same request to be correlated.

### AOP execution timing

Spring AOP measures execution time for public Service methods without mixing timing code into business logic.

Example:

```text
Service execution - SportService.getAllSports took 24 ms
```

Sensitive method arguments are intentionally not logged.

---

## 14. CI with GitHub Actions

The repository uses GitHub Actions to automatically test and build the backend.

The CI workflow runs on relevant pushes and pull requests and performs:

```text
Checkout source
  ↓
Set up Java 21
  ↓
Run Maven unit tests
  ↓
Build application package
```

The CI configuration is stored in:

```text
.github/workflows/ci.yml
```

Recommended branch triggers include:

```text
main
develop
feat/**
fix/**
test/**
```

---

## 15. Git Workflow

The team does not work directly on `main` for feature development.

Recommended flow:

```text
main
  ↑
develop
  ↑
feat/*, fix/*, test/*
```

Typical workflow:

```bash
git checkout develop
git pull
git checkout -b feat/example-feature

# implement + test

git add .
git commit -m "feat: add example feature"
git push -u origin feat/example-feature
```

Then:

1. Open a Pull Request into `develop`.
2. Wait for CI.
3. Request teammate review.
4. Resolve requested changes or conflicts.
5. Merge only after review/checks pass.
6. Merge stable `develop` into `main` through another Pull Request.

---

## 16. Important API Behavior

### `401 Unauthorized`

Used when authentication cannot be established, for example:

- Missing JWT on a protected endpoint.
- Invalid/expired JWT.
- Account becomes inactive and an old JWT is reused.
- Invalid login credentials.

### `403 Forbidden`

Used when the caller is authenticated but does not have the required authority/role.

### Validation

Request bodies use `@Valid` while request parameters/path variables use method validation with `@Validated` and constraints such as:

```text
@Min
@Max
@Positive
@Past
@Pattern
@Size
```

---

## 17. External Integration: Azure Blob Storage

Azure Blob Storage is used for image/file storage instead of storing binary data directly in MySQL.

Example flow:

```text
MultipartFile
  ↓
AzureBlobStorageService
  ↓
Azure Blob Storage
  ↓
Blob URL
  ↓
Database entity/profile
```

Azure credentials are supplied only through environment variables.

---

## 18. Reproducibility Checklist

Before considering a release/demo build ready:

- [ ] Fresh clone the repository into a new directory.
- [ ] Configure only documented environment variables.
- [ ] Run `docker compose up --build` successfully.
- [ ] Confirm seeded test accounts work.
- [ ] Open Swagger UI.
- [ ] Run the full Maven test suite.
- [ ] Confirm GitHub Actions is green.
- [ ] Confirm no `.env`, password, JWT secret, or Azure connection string is committed.
- [ ] Confirm a teammate reviewed at least one Pull Request.

---

## 19. AI Assistance Disclosure

Generative AI tools were used as a development assistant for tasks such as:

- explaining Spring Boot / Spring Security concepts;
- suggesting test cases and debugging approaches;
- reviewing code structure and edge cases;
- helping draft documentation and README wording.

AI suggestions were **not accepted blindly**. The team manually reviewed and adapted the suggestions, ran the code, verified API behavior, executed unit/regression tests, and kept responsibility for the final implementation and technical decisions.

---

## 20. Development Notes

This is an academic backend project and some production concerns can still be expanded later, for example:

- dedicated configuration profiles for development/test/production;
- schema migrations instead of relying on `ddl-auto=update`;
- more integration/test-slice coverage;
- stable paginated response DTOs instead of exposing the internal `PageImpl` JSON shape;
- production HTTPS / reverse proxy configuration;
- centralized production monitoring or log aggregation.

These are intentionally treated as future improvements rather than adding unnecessary scope before the project deadline.

---

## 21. Quick Start

```bash
git clone https://github.com/hoangpixel/SportHub-ModernBackend.git
cd SportHub-ModernBackend/sporthub-api

# Configure environment variables / .env as documented

docker compose up --build
```

Then open:

```text
Swagger:  http://localhost:8080/swagger-ui/index.html
Health:   http://localhost:8080/actuator/health
```

Login with a seeded demo account and use the returned JWT for protected APIs.

---

## License

This repository is currently maintained as an academic project. Add a formal license file if the project is later distributed publicly under a specific open-source license.

## Module Venue Court của Huy

[Báo cáo và API](docs/venue-court/README.md) · [Minh chứng kiểm thử](docs/venue-court/evidence.md)