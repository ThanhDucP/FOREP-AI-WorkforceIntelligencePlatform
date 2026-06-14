# Backend Code Guide - FOREP AI Workforce Intelligence Platform

File nay giup doc va hieu code backend theo thu tu tu he thong den business logic. Muc tieu la sau nay mo repo len co the nhanh chong biet request di qua dau, data nam o dau, logic nghiep vu nam o service nao, va diem nao can canh giac khi sua code.

## 1. Backend dang chay bang gi?

Backend la Spring Boot app:

```text
backend/src/main/java/com/aiworkforce/AIWorkforceApplication.java
```

Class nay la entry point:

```java
@SpringBootApplication
@EnableScheduling
public class AIWorkforceApplication
```

`@EnableScheduling` bat cac job chay theo lich, vi du:

- Check task qua han luc 1h sang trong `TaskService.checkOverdueTasks()`.
- Capture workload snapshot luc 2h sang trong `WorkloadSnapshotService.captureDailyWorkloadSnapshots()`.

## 2. Request di qua backend nhu the nao?

Flow tong quat:

```text
HTTP request
  -> Security filter chain
  -> JwtAuthenticationFilter
  -> Controller
  -> Service
  -> Repository
  -> Database
  -> DTO/Entity response
```

Vi du request lay task:

```text
GET /api/v1/tasks/my-tasks
  -> JwtAuthenticationFilter doc Bearer token
  -> SecurityContextHolder co user hien tai
  -> TaskController.getMyTasks()
  -> TaskService.getMyTasks()
  -> EmployeeService.getCurrentEmployee()
  -> TaskRepository.findByAssigneeId()
  -> Tra ve ApiResponse<List<Task>>
```

## 3. Cau truc package backend

```text
com.aiworkforce
|-- ai              AI insight, AI suggestion, Gemini/Ollama client
|-- analytics       Dashboard, workload history, workload snapshot
|-- auth            Register, login, auth response
|-- core            Shared config, response, exception, enum, notification
|-- event           Workload event publisher/processor/entity
|-- identity        Account, employee, team, organization, sprint
|-- integration     GitHub/Jira webhook, external task integration config
|-- security        JWT, Spring Security config, user details
|-- task            Task CRUD, task comment
`-- timetracking    Attendance, leave request
```

Nen doc code theo thu tu:

1. `security`
2. `auth`
3. `identity`
4. `task`
5. `timetracking`
6. `analytics`
7. `ai`
8. `integration`
9. `event`
10. `core`

## 4. Lop cau hinh he thong

### 4.1. `application.yml`

File:

```text
backend/src/main/resources/application.yml
```

No cau hinh:

- App name.
- Active profile: `dev`.
- Server port: `8080`.
- Flyway migration.
- JWT config tren YAML.
- AI provider: `ollama` mac dinh, co the doi sang `gemini`.

Luu y: JWT trong YAML hien chua that su duoc `JwtService` dung, vi `JwtService` dang hardcode secret va expiration.

### 4.2. `application-dev.yml`

File:

```text
backend/src/main/resources/application-dev.yml
```

No cau hinh database local:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/forep_db
    username: postgres
    password: password
```

Hibernate chi validate schema:

```yaml
ddl-auto: validate
```

Nghia la database schema do Flyway quan ly, khong phai Hibernate tu update.

### 4.3. `docker-compose.yml`

O root repo, gom:

- PostgreSQL.
- pgAdmin.
- Ollama.

Backend dev ket noi PostgreSQL o port `5433`.

## 5. Database va entity nen doc the nao

Migration nam o:

```text
backend/src/main/resources/db/migration
```

Neu muon hieu data model, doc 3 file nay truoc:

```text
V1__Initial_Schema.sql
V2__Add_GPS_Attendance_Columns.sql
V3__Add_Sprint_Comments_Notifications.sql
```

Mapping entity nam trong cac package:

- `identity/entity`
- `task/entity`
- `timetracking/entity`
- `analytics/entity`
- `ai/entity`
- `integration/entity`
- `event/entity`
- `core/entity`

Base entity:

```text
core/base/BaseEntity.java
core/base/AuditableEntity.java
```

Moi entity ke thua `BaseEntity` se co:

- `id: UUID`

Moi entity ke thua `AuditableEntity` se co them:

- `createdAt`
- `updatedAt`
- `createdBy`
- `updatedBy`

## 6. Security layer

Package:

```text
backend/src/main/java/com/aiworkforce/security
```

Class can doc:

- `SecurityConfig`
- `JwtAuthenticationFilter`
- `JwtService`
- `CustomUserDetailsService`

### 6.1. `SecurityConfig`

Vai tro:

- Tat CSRF.
- Tat form login.
- Tat HTTP basic.
- Dung stateless session.
- Cho public:
  - `/api/v1/auth/login`
  - `/api/v1/auth/register`
  - Swagger/OpenAPI endpoints.
- Moi endpoint con lai can authenticated.
- Gan `JwtAuthenticationFilter` vao truoc `UsernamePasswordAuthenticationFilter`.
- Bat `@EnableMethodSecurity`, vi vay cac `@PreAuthorize` trong controller co hieu luc.

### 6.2. `JwtAuthenticationFilter`

Vai tro:

1. Lay header `Authorization`.
2. Neu header bat dau bang `Bearer ` thi cat token.
3. Goi `JwtService.extractUsername()`.
4. Load user bang `CustomUserDetailsService`.
5. Validate token.
6. Set authentication vao `SecurityContextHolder`.

Neu token sai/het han, filter clear security context va request se bi reject boi security chain neu endpoint can auth.

### 6.3. `JwtService`

Vai tro:

- Generate JWT.
- Extract username/email.
- Validate token.
- Lay expiration.

Diem can sua sau:

- Secret key dang hardcode trong class.
- Expiration dang hardcode 1 ngay.
- Co import `@Value` nhung khong dung.

### 6.4. `CustomUserDetailsService`

Vai tro:

- Load account theo email.
- Chuyen role trong DB thanh Spring Security authority dang `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`.

## 7. Auth module

Package:

```text
backend/src/main/java/com/aiworkforce/auth
```

Controller:

```text
AuthController.java
```

Service:

```text
AuthService.java
```

DTO:

- `RegisterRequest`
- `LoginRequest`
- `AuthResponse`

### 7.1. Register flow

```text
POST /api/v1/auth/register
  -> AuthController.register()
  -> AuthService.register()
```

Logic hien tai:

1. Check email da ton tai chua.
2. Lay role tu request neu co.
3. Neu role khong hop le thi fallback `EMPLOYEE`.
4. Tao `Account`.
5. Hash password bang BCrypt.
6. Tao `Employee`.
7. Generate JWT.
8. Tra `AuthResponse`.

Diem can canh giac:

- Endpoint register public.
- Request co field `role`.
- Service chap nhan role trong request.
- Vi vay user co the tu register thanh `ADMIN` neu gui role `ADMIN`.

### 7.2. Login flow

```text
POST /api/v1/auth/login
  -> AuthController.login()
  -> AuthService.login()
```

Logic:

1. Goi `AuthenticationManager.authenticate()`.
2. Tim account theo email.
3. Reset failed login attempts neu co.
4. Generate JWT.
5. Tra `AuthResponse`.

Diem can canh giac:

- `refreshToken` hien cung gia tri voi `token`, chua co refresh token dung nghia.

## 8. Identity module

Package:

```text
backend/src/main/java/com/aiworkforce/identity
```

Quan ly:

- Account
- Role
- Permission
- Organization
- Employee
- Team
- Sprint

### 8.1. Organization

Controller:

```text
OrganizationController.java
```

Service:

```text
OrganizationService.java
```

API chinh:

- `GET /api/v1/organizations`
- `GET /api/v1/organizations/{id}`
- `POST /api/v1/organizations`
- `PUT /api/v1/organizations/{id}`
- `DELETE /api/v1/organizations/{id}`

Admin moi duoc create/update/delete.

Organization co toa do GPS:

- `latitude`
- `longitude`
- `allowedRadiusMeters`

Du lieu nay duoc Attendance dung de validate check-in/check-out.

### 8.2. Employee

Controller:

```text
EmployeeController.java
```

Service:

```text
EmployeeService.java
```

API chinh:

- `GET /api/v1/employees/profile`
- `PUT /api/v1/employees/profile`
- `GET /api/v1/employees`
- `GET /api/v1/employees/team/{teamId}`
- `GET /api/v1/employees/organization/{organizationId}`
- `GET /api/v1/employees/{id}`
- `PUT /api/v1/employees/{id}`
- `DELETE /api/v1/employees/{id}`

`EmployeeService.getCurrentEmployee()` la method rat quan trong:

```text
SecurityContextHolder -> email -> Account -> Employee
```

No duoc nhieu module dung de biet user hien tai.

Diem can canh giac:

- `updateProfile()` hien cho user tu doi `teamId`. Team assignment nen la quyen admin/manager, khong nen nam trong self profile.

### 8.3. Team

Controller:

```text
TeamController.java
```

Service:

```text
TeamService.java
```

Team co:

- Name.
- Description.
- Organization.
- Manager.
- Members thong qua `Employee.team`.

Business logic dang co:

- Tao team.
- Cap nhat team.
- Gan manager.
- Gan employee vao team.
- Xoa team thi clear `team` cua employee lien quan truoc khi xoa.

### 8.4. Sprint

Controller:

```text
SprintController.java
```

Service:

```text
SprintService.java
```

Sprint co:

- `sprintNumber`
- `name`
- `startDate`
- `endDate`
- `committedStoryPoints`
- `completedStoryPoints`
- `velocityConfidence`
- `status`
- `organization`

Logic dang co:

- Lay active sprint.
- Tao sprint.
- Update sprint.
- Neu sprint moi/updated co status `ACTIVE`, service set active sprint cu thanh `COMPLETED`.

Diem can canh giac:

- Logic deactivate active sprint hien la global, khong ro theo tung organization. Neu co nhieu organization, co the lam completed sprint active cua org khac.

## 9. Task module

Package:

```text
backend/src/main/java/com/aiworkforce/task
```

Entity:

- `Task`
- `TaskComment`

Controller:

- `TaskController`
- `TaskCommentController`

Service:

- `TaskService`
- `TaskCommentService`

### 9.1. Task entity

Task co cac field chinh:

- `title`
- `description`
- `status`
- `priority`
- `dueDate`
- `estimatedHours`
- `sourceProvider`
- `externalUrl`
- `externalTicketRef`
- `sprintNumber`
- `sprint`
- `storyPoints`
- `cycleTimeDays`
- `cycleTimeHours`
- `completedAt`
- `isOnCriticalPath`
- `assignee`
- `reporter`
- `team`

### 9.2. Tao task

```text
POST /api/v1/tasks
  -> TaskController.createTask()
  -> TaskService.createTask()
  -> applyTaskRequest()
  -> taskRepository.save()
  -> publish TASK_CREATED event
```

`applyTaskRequest()` map DTO vao entity:

- Set title, description, priority, due date, hours.
- Tim assignee neu co `assigneeId`.
- Tim reporter neu co `reporterId`.
- Tim team neu co `teamId`, neu khong thi lay team cua assignee.
- Tim sprint neu co `sprintId`.

### 9.3. Update status

```text
PATCH /api/v1/tasks/{id}/status?status=DONE
  -> TaskService.updateTaskStatus()
```

Logic:

- Set status.
- Neu status la `DONE`, set `completedAt = now`.
- Neu status khac `DONE`, clear `completedAt`.
- Publish event:
  - `TASK_COMPLETED` neu done.
  - `TASK_UPDATED` neu status khac.

Diem can canh giac:

- Endpoint update status hien khong co `@PreAuthorize`; bat ky authenticated user nao cung co the update status task bat ky neu biet ID.

### 9.4. Check overdue scheduled job

```text
TaskService.checkOverdueTasks()
```

Chay moi ngay luc 1h sang:

- Tim task chua DONE va due date truoc hien tai.
- Neu status chua `OVERDUE`, set thanh `OVERDUE`.
- Publish `TASK_OVERDUE` event.

### 9.5. Task comment

```text
/api/v1/tasks/{taskId}/comments
```

Logic:

- Get comments theo task.
- Add comment.
- Delete comment.

Diem can canh giac:

- `TaskCommentController.addComment()` chi set current user neu `authorId` null.
- Neu request gui `authorId` cua nguoi khac, service van chap nhan.
- Delete comment chi xoa theo `commentId`, khong verify comment co thuoc `taskId` path hay khong.
- Chua co ownership check.

## 10. Time Tracking module

Package:

```text
backend/src/main/java/com/aiworkforce/timetracking
```

Gom:

- Attendance.
- Leave request.

### 10.1. Attendance

Controller:

```text
AttendanceController.java
```

Service:

```text
AttendanceService.java
```

API:

- `POST /api/v1/attendance/check-in`
- `POST /api/v1/attendance/check-out`
- `GET /api/v1/attendance/my-history`
- `GET /api/v1/attendance/employee/{employeeId}`
- `GET /api/v1/attendance/team/{teamId}`
- `GET /api/v1/attendance/managed-teams`
- `GET /api/v1/attendance/organization/{organizationId}`

Check-in flow:

1. Lay current employee.
2. Kiem tra da check-in hom nay chua.
3. Validate GPS.
4. Tao attendance.
5. Set check-in date/time.
6. Neu sau 09:00 thi status `LATE`, nguoc lai `PRESENT`.

Check-out flow:

1. Lay current employee.
2. Tim attendance hom nay.
3. Neu chua check-in thi loi.
4. Neu da check-out thi loi.
5. Validate GPS.
6. Set check-out time.

GPS validation:

- Neu employee khong co team/org thi skip validation.
- Neu org chua set latitude/longitude thi skip validation.
- Neu org co toa do ma request thieu GPS thi throw business exception.
- Tinh khoang cach bang Haversine formula.
- Neu xa hon `allowedRadiusMeters` thi reject.

Diem can canh giac:

- Skip GPS khi user chua co team/org co the hop ly cho dev, nhung production nen co policy ro.
- Chua tinh `workHoursTotal` khi check-out, du schema co cot nay.

### 10.2. Leave request

Controller:

```text
LeaveRequestController.java
```

Service:

```text
LeaveRequestService.java
```

API:

- Create leave request.
- Approve leave request.
- Reject leave request.
- Get own leave history.
- Get by employee/team/managed-team/organization/status/all.

Create flow:

1. Lay current employee.
2. Check end date khong truoc start date.
3. Tao leave status `PENDING`.
4. Publish `LEAVE_REQUESTED` event.

Approve flow:

1. Tim leave.
2. Chi cho approve neu status dang `PENDING`.
3. Set status `APPROVED`.
4. Publish `LEAVE_APPROVED` event.

Diem can canh giac:

- Service approve/reject khong verify manager co quan ly employee do khong; controller chi check role admin/manager.
- Field `approvedById` trong migration/entity khong thay duoc set trong approve flow.
- `leaveType` tu request can kiem tra mapping neu business can.

## 11. Event module

Package:

```text
backend/src/main/java/com/aiworkforce/event
```

Thanh phan:

- `WorkloadEvent`
- `WorkloadEventRepository`
- `EventPublisher`
- `EventProcessor`

Flow:

```text
Service tao WorkloadEvent
  -> EventPublisher.publishEvent(event)
  -> Spring @EventListener
  -> EventProcessor.handleWorkloadEvent()
  -> tinh impactScore
  -> save WorkloadEvent
```

`EventProcessor` chay async va transaction moi:

```java
@Async
@EventListener
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

Impact score hien tai:

- `TASK_COMPLETED`: +10
- `TASK_OVERDUE`: -5
- `TASK_CREATED`: +2
- `TASK_UPDATED`: +1
- `LEAVE_APPROVED`: +15
- `LEAVE_REQUESTED`: 0

Diem can canh giac:

- Analytics event score coi workload score am la xau, nhung snapshot score coi workload score cao la xau. Hai model nay dang nguoc nhau.

## 12. Analytics module

Package:

```text
backend/src/main/java/com/aiworkforce/analytics
```

Thanh phan:

- `DashboardAnalyticsService`
- `AdminDashboardService`
- `WorkloadSnapshotService`
- `WorkloadCalculator`
- `EmployeeWorkloadSnapshot`

### 12.1. DashboardAnalyticsService

Dung event de tinh:

- Total completed tasks.
- Total overdue tasks.
- Current workload score.
- Burnout risk level.

`WorkloadCalculator.detectBurnoutRiskLevel()`:

- `HIGH` neu overdue > 5 hoac workloadScore < -20.
- `MEDIUM` neu overdue > 2 hoac workloadScore < 0.
- Con lai `LOW`.

Diem can canh giac:

- `recentTrends` dang la `List.of()` va comment "Mock empty trends".
- Model risk nay khac voi `WorkloadSnapshotService`.

### 12.2. WorkloadSnapshotService

Dung task hien tai de tao snapshot lich su workload.

Capture snapshot:

1. Lay task theo assignee.
2. Dem open tasks.
3. Dem overdue tasks.
4. Tinh workload score 0-100.
5. Gan burnout risk:
   - >= 80 hoac overdue > 3: `HIGH`
   - >= 55 hoac overdue > 1: `MEDIUM`
   - >= 35: `WATCH`
   - con lai: `NONE`
6. Save `EmployeeWorkloadSnapshot`.
7. Update cached metrics tren `Employee`.
8. Neu risk high thi tao notification burnout.

Scheduled job:

```text
0 0 2 * * ?
```

Chay moi ngay luc 2h sang.

## 13. AI module

Package:

```text
backend/src/main/java/com/aiworkforce/ai
```

Thanh phan:

- `AIInsightController`
- `AISuggestionController`
- `AIInsightService`
- `AISuggestionService`
- `OllamaClient`
- `PromptBuilder`
- `AiConfig`
- `AiProperties`

### 13.1. AIInsight flow

```text
POST /api/v1/ai/generate/{employeeId}
  -> AIInsightController
  -> AIInsightService.generateInsightForEmployee()
```

Flow service:

1. Tim employee.
2. Lay dashboard analytics.
3. Build burnout prompt.
4. Goi `OllamaClient.generateInsight(prompt)`.
5. Neu provider la `gemini`, goi Gemini API.
6. Neu provider la `ollama`, goi Ollama local.
7. Parse JSON AI response.
8. Neu response khong phai JSON hop le, build rule-based insight tu analytics.
9. Save `AIInsight`.

Diem can canh giac:

- Class ten `OllamaClient` nhung dang xu ly ca Gemini. Ten class co the gay nham lan.
- Comment code noi Gemini production, nhung `application.yml` default provider la `ollama`.
- Neu dashboard analytics score dang nguoc logic, AI insight cung bi anh huong.

### 13.2. AISuggestion flow

Service doc `AISuggestion` da co trong DB va tra response. Flow adopt suggestion:

1. Tim suggestion.
2. Neu da adopted thi loi.
3. Tim source task.
4. Tim target employee.
5. Tim source employee.
6. Reassign task sang target employee.
7. Mark suggestion adopted.
8. Publish `WORKLOAD_REBALANCED` event.
9. Notify target employee.
10. Notify source employee.

Diem can canh giac:

- `adoptSuggestion()` khong verify manager/admin co quyen voi team/org cua task/suggestion hay khong, controller chi check role.
- `mapToResponse()` goi repository nhieu lan theo tung suggestion, co the thanh N+1 query neu list lon.

## 14. Integration module

Package:

```text
backend/src/main/java/com/aiworkforce/integration
```

Thanh phan:

- `TaskIntegrationConfig`
- `TaskIntegrationService`
- `WebhookController`
- `GithubWebhookProcessor`
- `JiraWebhookProcessor`

### 14.1. Integration config

API:

- `POST /api/v1/integrations`
- `GET /api/v1/integrations/team/{teamId}`
- `PUT /api/v1/integrations/{id}`
- `DELETE /api/v1/integrations/{id}`

Config gom:

- Team.
- Provider.
- Webhook secret.
- Access token.
- Project key.
- Active flag.

Diem can canh giac:

- Response khong tra secret/access token, tot.
- Access token/webhook secret dang luu plain text trong DB entity. Production nen encrypt/secret manager.

### 14.2. GitHub webhook

Endpoint:

```text
POST /api/v1/webhooks/github/{configId}
```

Flow:

1. Lay active integration config.
2. Verify `X-Hub-Signature-256`.
3. Parse payload.
4. Neu khong phai issue event thi ignore.
5. Lay issue number/title/body/url/state.
6. Tim task theo external ref `GH-{issueNumber}` va provider `GITHUB`.
7. Neu co thi update, neu khong co thi create.
8. Map GitHub closed -> `DONE`, open/reopened -> `TODO` neu task chua co status hoac da DONE.

Diem can canh giac:

- Assignee mapping theo GitHub email co the khong hoat dong vi GitHub issue assignee thuong khong expose email.
- Webhook GitHub co signature, Jira processor can kiem tra ky hon neu production dung.

## 15. Core module

Package:

```text
backend/src/main/java/com/aiworkforce/core
```

Gom:

- Base entity.
- Exception.
- Response wrapper.
- Enum.
- CORS.
- Swagger config.
- Data seeder.
- Notification.
- Pagination response.

### 15.1. Exception handling

`GlobalExceptionHandler` xu ly:

- `BaseException`.
- `ValidationException`.
- `MethodArgumentNotValidException`.
- Generic `Exception`.

Diem can canh giac:

- Generic exception response tra `ex.getMessage()` ve client. Production nen an chi tiet noi bo.

### 15.2. Response wrapper

Controller thuong tra:

```java
ResponseEntity<ApiResponse<T>>
```

Error tra:

```java
ErrorResponse
```

### 15.3. Notification

API:

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/unread`
- `GET /api/v1/notifications/unread-count`
- `PUT /api/v1/notifications/{id}/read`
- `PUT /api/v1/notifications/read-all`
- `DELETE /api/v1/notifications/{id}`

Diem can canh giac:

- Mark read/delete theo ID khong check notification co thuoc current employee khong.

### 15.4. DataSeeder

Class:

```text
core/config/DataSeeder.java
```

Chay khi app start neu chua co `admin@forep.local`.

Seeder tao:

- Organization demo.
- Sprints demo.
- Admin/manager/employee accounts.
- Teams.
- Tasks.
- Workload snapshots.
- Notifications.
- Task comments.
- AI suggestion.

Diem can canh giac:

- Nhieu string tieng Viet dang bi mojibake.
- Password demo hardcoded.
- Seeder phu hop dev/demo, can tat hoac tach profile neu production.

## 16. DTO vs Entity response

Mot so controller tra DTO:

- Employee.
- Organization.
- Team.
- Sprint.
- Notification.
- Attendance.
- Leave request.
- AI suggestion.

Nhung `TaskController` dang tra truc tiep entity `Task`.

Diem can canh giac:

- Entity JPA co quan he lazy/eager co the gay serialization issue.
- Tra entity co the lo field noi bo.
- Nen tao `TaskResponse` DTO de dong bo voi cac module khac.

## 17. Cac business flow lon

### 17.1. User dang ky va lam viec

```text
Register/Login
  -> JWT
  -> Employee profile
  -> View tasks
  -> Check-in/check-out
  -> Create leave request
  -> View dashboard/insights
```

### 17.2. Manager tao task va quan ly team

```text
Manager login
  -> View managed teams
  -> Create/update tasks
  -> View team workload history
  -> Generate AI insight for employee
  -> Adopt AI suggestion
```

Can canh giac:

- Nhieu endpoint manager co the query theo team/org ID bat ky, khong verify team do co duoc manager quan ly khong.

### 17.3. Task tao workload event

```text
Create/update/complete/overdue task
  -> TaskService.publishEvent()
  -> EventPublisher
  -> EventProcessor
  -> WorkloadEvent saved with impactScore
  -> DashboardAnalyticsService reads events
```

### 17.4. Snapshot tao burnout metrics

```text
Scheduled daily at 2 AM
  -> WorkloadSnapshotService.captureDailyWorkloadSnapshots()
  -> For each employee
  -> Read assigned tasks
  -> Compute workload score/risk
  -> Save EmployeeWorkloadSnapshot
  -> Update Employee cached metrics
  -> Notify employee if HIGH risk
```

### 17.5. AI insight

```text
Generate insight
  -> Get employee dashboard
  -> Build prompt
  -> Call Gemini/Ollama
  -> Parse JSON or fallback rule-based
  -> Save AIInsight
```

## 18. Test hien co

Test class:

- `OllamaClientTest`
- `AIInsightServiceTest`
- `WorkloadCalculatorTest`
- `AdminDashboardServiceTest`
- `AuthServiceTest`
- `OrganizationServiceTest`
- `GithubWebhookProcessorTest`
- `AttendanceServiceTest`

Lenh:

```powershell
cd backend
mvn test
```

Trang thai gan nhat:

```text
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Khoang trong test nen them:

- Register khong duoc tu set role admin.
- Task get/update status phai check owner/role.
- Notification mark/delete phai check owner.
- Comment author spoofing khong duoc phep.
- Manager chi xem team minh quan ly.
- Sprint active theo organization.
- JWT config doc tu env/config.

## 19. Nhung diem nen sua dau tien

Thu tu uu tien:

1. Chan public register tu set role.
2. Them authorization cho `GET /tasks/{id}` va `PATCH /tasks/{id}/status`.
3. Sua comment author: luon lay current employee, khong tin `authorId` tu request.
4. Check ownership notification khi mark/delete.
5. Doi `JwtService` sang doc config/env.
6. Gioi han CORS theo config.
7. Tach `TaskResponse` DTO, khong tra entity truc tiep.
8. Dong bo model workload score giua event analytics va snapshot analytics.
9. Sua encoding tieng Viet.
10. Them test security/business boundary.

## 20. Cach doc code khi sua feature moi

Neu sua API:

1. Tim controller endpoint.
2. Tim service method duoc goi.
3. Tim repository query.
4. Kiem tra entity va migration co field do chua.
5. Kiem tra `@PreAuthorize`.
6. Kiem tra service co verify ownership/business rule khong.
7. Kiem tra DTO request/response.
8. Them/sua test.
9. Chay `mvn test`.

Neu sua database:

1. Khong sua migration da chay.
2. Tao migration moi `Vx__Description.sql`.
3. Sua entity mapping.
4. Sua DTO/service/controller neu can.
5. Chay app/test voi DB moi.

Neu sua business workload/AI:

1. Doc `EventProcessor`.
2. Doc `WorkloadCalculator`.
3. Doc `DashboardAnalyticsService`.
4. Doc `WorkloadSnapshotService`.
5. Doc `PromptBuilder`.
6. Doc `AIInsightService`.
7. Dam bao score/risk model thong nhat.

## 21. Ban do file quan trong

```text
System:
- AIWorkforceApplication.java
- application.yml
- application-dev.yml
- docker-compose.yml

Security:
- SecurityConfig.java
- JwtAuthenticationFilter.java
- JwtService.java
- CustomUserDetailsService.java

Auth:
- AuthController.java
- AuthService.java
- RegisterRequest.java
- LoginRequest.java
- AuthResponse.java

Identity:
- EmployeeService.java
- TeamService.java
- OrganizationService.java
- SprintService.java

Task:
- TaskController.java
- TaskService.java
- TaskCommentController.java
- TaskCommentService.java

Time tracking:
- AttendanceService.java
- LeaveRequestService.java

Analytics:
- DashboardAnalyticsService.java
- WorkloadSnapshotService.java
- WorkloadCalculator.java

AI:
- AIInsightService.java
- AISuggestionService.java
- OllamaClient.java
- PromptBuilder.java

Integration:
- TaskIntegrationService.java
- WebhookController.java
- GithubWebhookProcessor.java
- JiraWebhookProcessor.java

Core:
- GlobalExceptionHandler.java
- ApiResponse.java
- ErrorResponse.java
- NotificationService.java
- DataSeeder.java
```

