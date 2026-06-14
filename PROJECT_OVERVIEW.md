# FOREP - AI Workforce Intelligence Platform

File nay ghi lai tong quan du an de sau nay co the doc nhanh va tiep tuc phat trien ma khong phai mo tung package.

## 1. Du an nay la gi?

FOREP la mot backend cho he thong "AI Workforce Intelligence Platform". Muc tieu cua he thong la thu thap du lieu cong viec, cham cong, nghi phep, sprint, workload va tu do tao ra dashboard, canh bao burnout, insight AI va goi y tai phan bo cong viec.

Repo hien tai chu yeu la backend Spring Boot. Chua co frontend that su trong cay source, mac du tai lieu setup co de xuat tao frontend bang React + Vite.

## 2. Cong nghe chinh

- Backend: Java 21, Spring Boot 3.2.5.
- Build tool: Maven.
- Database: PostgreSQL 15.
- Migration: Flyway.
- Security: Spring Security, JWT.
- ORM: Spring Data JPA, Hibernate.
- DTO/helper: Lombok, MapStruct dependency co trong `pom.xml`.
- API docs: Springdoc OpenAPI / Swagger UI.
- AI client: Gemini API hoac Ollama local.
- Local infra: Docker Compose gom PostgreSQL, pgAdmin, Ollama.
- API testing: Postman collection trong thu muc `postman` va file `forep_api_collection.json`.

## 3. Cau truc thu muc

```text
.
|-- backend/
|   |-- pom.xml
|   |-- Dockerfile
|   |-- src/main/java/com/aiworkforce/
|   |   |-- ai/
|   |   |-- analytics/
|   |   |-- auth/
|   |   |-- core/
|   |   |-- event/
|   |   |-- identity/
|   |   |-- integration/
|   |   |-- security/
|   |   |-- task/
|   |   `-- timetracking/
|   `-- src/main/resources/
|       |-- application.yml
|       |-- application-dev.yml
|       |-- application-prod.yml
|       `-- db/migration/
|-- docker-compose.yml
|-- postman/
|-- forep_api_collection.json
|-- README.md
`-- SETUP_GUIDE.md
```

## 4. Cach chay local

Chay ha tang:

```powershell
docker compose up -d
```

Docker Compose dang cau hinh:

- PostgreSQL: container `forep_postgres`, database `forep_db`, user `postgres`, password `password`, expose ra may host o port `5433`.
- pgAdmin: `http://localhost:5050`, email `admin@forep.local`, password `admin`.
- Ollama: `http://localhost:11434`.

Chay backend:

```powershell
cd backend
mvn spring-boot:run
```

Backend chay o:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Chay test:

```powershell
cd backend
mvn test
```

Lan kiem tra gan nhat: `mvn test` pass voi 34 tests.

## 5. Cau hinh Spring

File `backend/src/main/resources/application.yml`:

- App name: `ai-workforce-api`.
- Active profile mac dinh: `dev`.
- Server port: `8080`.
- Flyway enabled, migration path: `classpath:db/migration`.
- JWT config co khai bao trong YAML, nhung `JwtService.java` hien dang hardcode secret va expiration rieng.
- AI provider mac dinh: `${AI_PROVIDER:ollama}`.
- Ollama base URL mac dinh: `http://localhost:11434`.
- Gemini model mac dinh: `gemini-1.5-flash`.

File `backend/src/main/resources/application-dev.yml`:

- Datasource: `jdbc:postgresql://localhost:5433/forep_db`.
- Username/password: `postgres` / `password`.
- Hibernate: `ddl-auto: validate`.
- SQL logging dang bat kha chi tiet.

File `backend/src/main/resources/application-prod.yml` hien dang trong.

## 6. Database va migration

Flyway migration nam o:

```text
backend/src/main/resources/db/migration
```

### V1 - Initial Schema

Tao cac bang nen tang:

- `permission`
- `role`
- `role_permission`
- `account`
- `organization`
- `employee`
- `team`
- `task`
- `workload_event`
- `ai_insight`
- `leave_request`
- `attendance`

Seed 3 role:

- `ADMIN`
- `MANAGER`
- `EMPLOYEE`

### V2 - GPS Attendance

Them toa do va ban kinh hop le vao organization:

- `latitude`
- `longitude`
- `allowed_radius_meters`

Muc dich: validate check-in/check-out theo vi tri GPS.

### V3 - Sprint, Comments, Notifications, Metrics

Them cac bang va cot quan trong:

- Bang `sprint`.
- Cot metric cho `employee`: workload score, burnout risk, contribution, overdue ratio, focus score...
- Cot metric cho `team`: capacity used, utilization score.
- Cot sprint/external integration cho `task`.
- Bang `task_comment`.
- Bang `notification`.
- Bang `employee_workload_snapshot`.
- Bang `ai_suggestion`.

## 7. Seed data

Class seed data:

```text
backend/src/main/java/com/aiworkforce/core/config/DataSeeder.java
```

Seeder chay luc app khoi dong neu chua co account `admin@forep.local`.

Tai khoan demo chinh:

| Email | Password | Role | Ghi chu |
|---|---|---|---|
| `admin@forep.local` | `AdminPassword123` | ADMIN | System Admin |
| `john@forep.local` | `Password123` | MANAGER | Engineering Manager |
| `grace@forep.local` | `Password123` | MANAGER | Product Director |
| `alice@forep.local` | `Password123` | EMPLOYEE | Senior Frontend Engineer |
| `bob@forep.local` | `Password123` | EMPLOYEE | Backend Developer |
| `charlie@forep.local` | `Password123` | EMPLOYEE | DevOps Engineer |
| `david@forep.local` | `Password123` | EMPLOYEE | Lead DB Architect |
| `elena@forep.local` | `Password123` | EMPLOYEE | Mobile App Specialist |
| `frank@forep.local` | `Password123` | EMPLOYEE | Lead UI/UX Designer |

Seeder tao organization `Apex AI Solutions`, cac sprint demo, team Engineering/Product, task demo, notification, comment, workload snapshot va AI suggestion.

Luu y: nhieu chuoi tieng Viet trong seed data dang bi loi encoding/mojibake trong source hien tai.

## 8. Security va Auth

Package chinh:

```text
backend/src/main/java/com/aiworkforce/security
backend/src/main/java/com/aiworkforce/auth
```

Endpoint auth:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

`SecurityConfig.java`:

- Tat CSRF.
- Tat form login va HTTP basic.
- Stateless session.
- Cho public access vao login/register va Swagger.
- Moi endpoint con lai can JWT.
- Bat method security bang `@EnableMethodSecurity`.

`JwtAuthenticationFilter` doc token tu request, validate token va set authentication vao Spring Security context.

`JwtService.java`:

- Tao token voi claim `roles`.
- Token expiry hien hardcode 1 ngay.
- Secret key hien hardcode trong class.

Diem can canh giac:

- `RegisterRequest` co field `role`, va `AuthService.register()` chap nhan role neu request gui len. Neu endpoint register public, nguoi dung co the tu dang ky role cao hon nhu `ADMIN`, tuy role co ton tai trong DB.
- `refreshToken` dang tra ve cung gia tri voi access token, chua phai refresh-token flow that.

## 9. Cac module domain

### 9.1. `identity`

Quan ly cac doi tuong nhan su va to chuc:

- `Account`
- `Role`
- `Permission`
- `Organization`
- `Employee`
- `Team`
- `Sprint`

Controller chinh:

- `/api/v1/organizations`
- `/api/v1/employees`
- `/api/v1/teams`
- `/api/v1/sprints`

Service chinh:

- `OrganizationService`
- `EmployeeService`
- `TeamService`
- `SprintService`

### 9.2. `task`

Quan ly task va comment.

Entity chinh:

- `Task`
- `TaskComment`

Controller:

- `/api/v1/tasks`
- `/api/v1/tasks/{taskId}/comments`

Task co lien ket voi:

- Assignee employee.
- Reporter employee.
- Team.
- Sprint.
- External ticket ref/provider cho integration.

Trang thai task nam trong enum `TaskStatus`.
Priority nam trong enum `TaskPriority`.

### 9.3. `timetracking`

Quan ly attendance va leave request.

Controller:

- `/api/v1/attendance`
- `/api/v1/leaves`

Attendance co check-in/check-out va GPS validation. Organization co toa do van phong va `allowed_radius_meters`.

Leave request co approve/reject cho admin/manager theo logic service.

### 9.4. `analytics`

Tao dashboard va workload history.

Controller:

- `/api/v1/dashboard`
- `/api/v1/admin/dashboard`
- `/api/v1/analytics`

Thanh phan quan trong:

- `DashboardAnalyticsService`
- `AdminDashboardService`
- `WorkloadSnapshotService`
- `WorkloadCalculator`
- `EmployeeWorkloadSnapshot`

`WorkloadSnapshotService` tao snapshot lich su cho nhan vien, dung trong dashboard va insight.

### 9.5. `ai`

Tao AI insight va suggestion.

Controller:

- `/api/v1/ai`
- `/api/v1/ai/suggestions`

Thanh phan chinh:

- `AIInsightService`
- `AISuggestionService`
- `OllamaClient`
- `PromptBuilder`
- `AiConfig`
- `AiProperties`

Mac du class ten la `OllamaClient`, no co ca logic Gemini. Theo comment trong code, Gemini la provider production, Ollama giu lai cho dev local.

Luom chinh tao burnout insight:

1. Lay employee theo ID.
2. Lay dashboard analytics cua employee.
3. Build prompt bang `PromptBuilder`.
4. Goi AI qua `OllamaClient.generateInsight()`.
5. Parse JSON response.
6. Neu AI tra text khong parse duoc JSON, fallback sang rule-based insight dua tren analytics that.
7. Luu `AIInsight` vao database.

### 9.6. `integration`

Tich hop external task systems.

Controller:

- `/api/v1/integrations`
- `/api/v1/webhooks`

Provider hien co enum:

- `INTERNAL`
- `GITHUB`
- `JIRA`

GitHub webhook:

- Endpoint: `POST /api/v1/webhooks/github/{configId}`.
- Verify signature HMAC SHA-256.
- Neu payload la issue event, tao hoac update task theo external ref `GH-{issueNumber}`.

Jira webhook co processor rieng trong `JiraWebhookProcessor`.

### 9.7. `event`

Quan ly workload events:

- `WorkloadEvent`
- `WorkloadEventService`
- `EventPublisher`
- `EventProcessor`

Module nay dung de ghi nhan thay doi/anh huong workload theo event va phuc vu analytics.

### 9.8. `core`

Chua cac thanh phan dung chung:

- Base entity: `BaseEntity`, `AuditableEntity`.
- Exception: `BusinessException`, `ResourceNotFoundException`, `UnauthorizedException`, `AiServiceException`, `GlobalExceptionHandler`.
- Response wrapper: `ApiResponse`, `ErrorResponse`.
- Enum domain.
- Notification entity/service/controller.
- Config: CORS, async, Swagger, database.
- Seeder demo data.

## 10. API groups

Postman collection hien co cac group:

- Authentication
- Organizations
- Employees
- Teams
- Sprints
- Tasks
- Dashboards
- Analytics
- AI Insights
- AI Suggestions
- Attendance Tracking
- Leave Requests

Base path cua backend gan nhu tat ca deu theo:

```text
/api/v1/...
```

## 11. Quy uoc response va exception

Du an co `ApiResponse` va `ErrorResponse` trong package `core.response`.

`GlobalExceptionHandler` xu ly exception tap trung. Nen tiep tuc dung exception domain nhu:

- `BusinessException` cho loi nghiep vu.
- `ResourceNotFoundException` khi khong tim thay record.
- `UnauthorizedException` khi user khong co quyen.
- `AiServiceException` khi AI provider loi.
- `ValidationException` cho validation custom.

## 12. Test hien tai

Test nam trong:

```text
backend/src/test/java/com/aiworkforce
```

Co 8 test class:

- `OllamaClientTest`
- `AIInsightServiceTest`
- `WorkloadCalculatorTest`
- `AdminDashboardServiceTest`
- `AuthServiceTest`
- `OrganizationServiceTest`
- `GithubWebhookProcessorTest`
- `AttendanceServiceTest`

Lan chay gan nhat:

```text
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 13. Nhung diem can sua/cai thien sau

### Bao mat

- Dua JWT secret va expiration ra config/env, khong hardcode trong `JwtService.java`.
- Khong cho public register tu chon role `ADMIN`/`MANAGER`. Nen mac dinh register la `EMPLOYEE`, viec cap role cao hon chi admin lam.
- Tach refresh token that su, khong dung chung access token.
- Gioi han CORS theo domain frontend thuc te, khong dung wildcard credentials trong production.
- Kiem tra lai permission bang `@PreAuthorize` o cac endpoint admin/manager. Security chain hien moi yeu cau authenticated, con role-level authorization phu thuoc controller/service.

### Cau hinh production

- `application-prod.yml` dang trong, can bo sung datasource production, AI provider, logging, CORS, JWT env.
- `SETUP_GUIDE.md` co cho bi lech port DB va bi loi encoding. Nen sua lai thanh UTF-8 dung.
- Neu deploy Render/Cloud, nen set `AI_PROVIDER=gemini` va `GEMINI_API_KEY`.

### Database

- Migration V1 dung `gen_random_uuid()` trong seed role. PostgreSQL can extension phu hop. Neu moi truong nao chua co extension, co the can `CREATE EXTENSION IF NOT EXISTS pgcrypto;`.
- Can xem lai cac constraint `NOT NULL`, index va unique key cho cac bang quan trong nhu task external ref, sprint number, employee/account relation.
- Cac file migration da chay thi khong sua truc tiep; tao migration moi.

### Encoding

- Nhieu text tieng Viet dang bi mojibake trong:
  - `SETUP_GUIDE.md`
  - `DataSeeder.java`
  - comment trong mot so Java class
- Nen chuan hoa tat ca file ve UTF-8 va sua lai noi dung tieng Viet neu can hien thi cho user.

### Frontend

- Tai lieu co noi frontend nhung repo chua co thu muc/frontend app.
- Neu can build frontend, nen tao `frontend/` bang Vite + React + TypeScript, dung API `/api/v1`.
- Nen sinh API client tu OpenAPI de dong bo DTO voi backend.

## 14. Huong tiep can uu tien

Neu tiep tuc phat trien du an, nen lam theo thu tu:

1. Sua security register role va hardcoded JWT secret.
2. Sua encoding/tai lieu de doc duoc tieng Viet.
3. Bo sung `application-prod.yml` va env variables.
4. Review role authorization cho tung controller.
5. Tao frontend hoac cap nhat README neu frontend chua nam trong scope.
6. Bo sung integration tests cho auth/security va cac endpoint quan trong.
7. Them index/constraint database cho cac query dashboard va webhook.

## 15. Lenh hay dung

Chay infra:

```powershell
docker compose up -d
```

Dung infra:

```powershell
docker compose down
```

Chay backend:

```powershell
cd backend
mvn spring-boot:run
```

Chay test:

```powershell
cd backend
mvn test
```

Build jar:

```powershell
cd backend
mvn clean package
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

pgAdmin:

```text
http://localhost:5050
```

## 16. Ghi chu nhanh cho nguoi moi vao du an

- Bat dau doc tu `AIWorkforceApplication.java`, `SecurityConfig.java`, `AuthService.java`, `DataSeeder.java`.
- Sau do doc entity trong cac package `identity`, `task`, `analytics`, `ai`.
- Neu muon hieu database, doc 3 file migration truoc.
- Neu muon test API nhanh, import Postman collection.
- Neu app fail luc start, kiem tra PostgreSQL port `5433`, Flyway migration va profile `dev`.
- Neu AI insight fail, kiem tra `AI_PROVIDER`, `GEMINI_API_KEY` hoac Ollama local co model hay chua.
