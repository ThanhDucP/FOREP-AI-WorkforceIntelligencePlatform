# HƯỚNG DẪN CẤU HÌNH & THIẾT LẬP DỰ ÁN FOREP
## AI Workforce Intelligence Platform

Tài liệu này cung cấp hướng dẫn chi tiết và chuẩn hóa nhất để cấu hình, chạy cơ sở hạ tầng (Database, AI Engine), khởi chạy Backend Spring Boot và khởi tạo một dự án Frontend đồng bộ cho hệ thống **FOREP (AI Workforce Intelligence Platform)**.

---

## 🗺️ 1. Tổng quan Kiến trúc Dự án

Dự án được cấu trúc theo dạng **Monorepo** phân tách rõ ràng giữa Backend, Cơ sở hạ tầng (Docker) và Frontend:

*   **Cơ sở hạ tầng (Infrastructure)**: Docker Compose quản lý PostgreSQL 15, pgAdmin 4 và Ollama (AI local).
*   **Backend ([backend](./backend))**: Xây dựng bằng **Spring Boot 3.2.5** và **Java 21**, quản lý dependency qua **Maven**. Hệ thống sử dụng Flyway để migration database, Spring Security + JWT cho bảo mật, Lombok + Mapstruct để map DTO/Entity.
*   **Frontend ([frontend](./frontend))**: Thư mục hiện tại đang trống. Hướng dẫn dưới đây sẽ giúp khởi tạo một dự án Frontend React + Vite + TypeScript hiện đại, tối ưu nhất.

---

## 🐳 2. Thiết lập & Chạy Cơ sở hạ tầng (Docker Services)

Dự án đã cấu hình sẵn [docker-compose.yml](./docker-compose.yml) ở thư mục gốc.

### Các dịch vụ được cài đặt:
1.  **PostgreSQL 15 (Alpine)**: Database chính (`forep_db`), chạy trên cổng `5432`.
2.  **pgAdmin 4**: Công cụ quản trị cơ sở dữ liệu trực quan qua web, chạy trên cổng `5050`.
3.  **Ollama**: AI Engine cục bộ để chạy các mô hình ngôn ngữ lớn (LLM), chạy trên cổng `11434`.

### Các bước thực hiện:

1.  Mở Terminal tại thư mục gốc của dự án (`FOREP---AI-Workforce-Intelligence-Platform`).
2.  Khởi chạy các container ở chế độ nền (detached mode):
    ```bash
    docker compose up -d
    ```
3.  **Cấu hình Ollama AI Model**:
    Sau khi Ollama chạy, bạn cần tải về mô hình AI (ví dụ `llama3` hoặc `qwen2:7b` tùy thuộc vào cấu hình máy của bạn):
    ```bash
    docker exec -it forep_ollama ollama pull llama3
    ```
4.  **Truy cập pgAdmin để kiểm tra**:
    *   Địa chỉ: `http://localhost:5050`
    *   Email đăng nhập: `admin@forep.local`
    *   Mật khẩu: `admin`
    *   Thêm server mới trong pgAdmin:
        *   **Host name/address**: `postgres` (nếu kết nối trong mạng Docker) hoặc `localhost` (nếu truy cập trực tiếp từ máy của bạn).
        *   **Port**: `5432`
        *   **Username**: `postgres`
        *   **Password**: `password`

---

## ☕ 3. Cấu hình & Khởi chạy Backend (Spring Boot)

Backend chạy trên nền tảng Java 21, yêu cầu các thiết lập chuẩn sau:

### 3.1. Yêu cầu hệ thống (Prerequisites)
*   **Java Development Kit (JDK)**: Phiên bản **21** (Khuyên dùng Eclipse Temurin hoặc Amazon Corretto).
*   **Apache Maven**: Phiên bản **3.9+** (hoặc tích hợp sẵn trong IDE).

### 3.2. Cấu hình IDE (Bắt buộc)
Do dự án sử dụng song song **Lombok** (sinh code tự động) và **MapStruct** (tự động ánh xạ DTO - Entity), việc cấu hình **Annotation Processing** là bắt buộc để tránh lỗi build mappers.

#### 🔹 Với IntelliJ IDEA:
1.  Mở thư mục `backend` như một dự án Maven.
2.  Đi tới: **File** ➡️ **Settings** (hoặc `Ctrl + Alt + S`).
3.  Chọn: **Build, Execution, Deployment** ➡️ **Compiler** ➡️ **Annotation Processors**.
4.  Tích chọn **Enable annotation processing**.
5.  Nhấp **Apply** và **OK**.

#### 🔹 Với Visual Studio Code (VS Code):
1.  Mở thư mục `backend` trong VS Code.
2.  Cài đặt các Extension sau:
    *   `Extension Pack for Java` (Microsoft)
    *   `Spring Boot Extension Pack` (VMware)
    *   `Lombok Annotations Support for VS Code` (Gabrielbb)
3.  Đảm bảo file cấu hình `.vscode/settings.json` có dòng sau (đã được tạo sẵn):
    ```json
    {
        "java.compile.nullAnalysis.mode": "automatic"
    }
    ```

### 3.3. Cấu hình File ứng dụng (`application.yml`)
*   [application.yml](./backend/src/main/resources/application.yml): Định nghĩa các cấu hình dùng chung như cổng mặc định (`8080`), cấu hình kích hoạt profile `dev` và thời gian sống của JWT token.
*   [application-dev.yml](./backend/src/main/resources/application-dev.yml): Chứa thông số kết nối Database local và mức log chi tiết cho môi trường phát triển:
    *   `spring.datasource.url`: `jdbc:postgresql://localhost:5432/forep_db`
    *   `spring.jpa.hibernate.ddl-auto`: `validate` (Spring Boot chỉ kiểm tra cấu trúc schema, không tự động can thiệp sửa đổi bảng để tránh xung đột với Flyway).

> [!IMPORTANT]
> **Khóa JWT Secret Key**:
> Khóa bí mật hiện đang hardcode trong [application.yml](./backend/src/main/resources/application.yml) và [JwtService.java](./backend/src/main/java/com/aiworkforce/security/jwt/JwtService.java) để phục vụ chạy demo nhanh.
> *Khi deploy lên môi trường Production*, bạn **PHẢI** chuyển khóa này thành biến môi trường để đảm bảo an toàn bảo mật:
> ```yaml
> app:
>   security:
>     jwt:
>       secret-key: ${JWT_SECRET_KEY}
> ```

### 3.4. Chạy Backend & Migration Database
Khi bạn chạy Backend lần đầu tiên, **Flyway** sẽ tự động quét thư mục `db/migration` và thực thi file SQL [V1__Initial_Schema.sql](./backend/src/main/resources/db/migration/V1__Initial_Schema.sql) để tạo các bảng cần thiết (`account`, `employee`, `role`, `permission`, `task`, `workload_event`...) và seed sẵn các Role (`ADMIN`, `MANAGER`, `EMPLOYEE`).

#### Cách chạy ứng dụng:
*   **Cách 1 (Từ IDE)**: Mở file [AIWorkforceApplication.java](./backend/src/main/java/com/aiworkforce/AIWorkforceApplication.java) và nhấn nút **Run/Debug**.
*   **Cách 2 (Từ Dòng lệnh)**:
    ```bash
    cd backend
    mvn clean spring-boot:run
    ```

### 3.5. Kiểm tra APIs qua Swagger UI
Khi ứng dụng khởi động thành công trên cổng `8080`, bạn có thể kiểm tra danh sách và thử nghiệm trực tiếp các API bằng tài liệu tương tác Swagger UI:
*   **Đường dẫn**: `http://localhost:8080/swagger-ui/index.html`
*   **Tài liệu OpenAPI định dạng JSON**: `http://localhost:8080/v3/api-docs`

---

## 🎨 4. Khởi tạo & Cấu hình Frontend (React + Vite)

Thư mục `frontend` của dự án hiện đang trống. Để xây dựng một ứng dụng Single Page Application (SPA) hiện đại, nhẹ, mượt mà và giao diện cao cấp (Premium), bạn nên sử dụng hệ sinh thái sau:

### 🌟 Bộ Công nghệ Khuyên dùng cho Frontend:
1.  **Vite + React**: Đảm bảo tốc độ compile cực nhanh và tối ưu kích thước bundle.
2.  **TypeScript**: Bắt buộc để kiểm soát kiểu chặt chẽ, đồng bộ cấu trúc DTO với Backend.
3.  **Tailwind CSS + Shadcn/ui + Radix UI**: Bộ thư viện UI component tốt nhất hiện nay, mang lại giao diện Glassmorphic, Modern Dark Mode cực đẹp và mượt mà.
4.  **Zustand**: Quản lý State toàn cục siêu nhẹ (thay thế cho Redux cồng kềnh).
5.  **TanStack Query (React Query)**: Quản lý trạng thái đồng bộ server, caching dữ liệu và tự động refetch APIs cực tốt.
6.  **Lucide React**: Bộ icon phong cách tối giản cao cấp.

### 🚀 Các bước khởi tạo dự án Frontend:

1.  Mở terminal tại thư mục gốc dự án.
2.  Khởi tạo dự án Vite mới trong thư mục `frontend`:
    ```bash
    # Khởi tạo React + TypeScript bằng Vite
    npm create vite@latest frontend -- --template react-ts
    ```
3.  Di chuyển vào thư mục frontend và cài đặt dependencies cơ bản:
    ```bash
    cd frontend
    npm install
    ```
4.  Cài đặt các thư viện cần thiết cho Routing, State Management và Icons:
    ```bash
    npm install react-router-dom zustand lucide-react axios @tanstack/react-query
    ```
5.  **Cài đặt Tailwind CSS**:
    ```bash
    npm install -D tailwindcss postcss autoprefixer
    npx tailwindcss init -p
    ```
    *Cập nhật file `tailwind.config.js` để quét các file React:*
    ```javascript
    /** @type {import('tailwindcss').Config} */
    export default {
      content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
      ],
      theme: {
        extend: {},
      },
      plugins: [],
    }
    ```
    *Thêm các chỉ dẫn Tailwind vào file `src/index.css`:*
    ```css
    @tailwind base;
    @tailwind components;
    @tailwind utilities;
    ```

6.  **Cấu hình Dev Server Proxy (Quan trọng)**:
    Để tránh lỗi CORS khi gọi API từ Frontend (`localhost:5173`) lên Backend (`localhost:8080`), hãy cấu hình Proxy trong file `vite.config.ts`:
    ```typescript
    import { defineConfig } from 'vite'
    import react from '@vitejs/plugin-react'

    export default defineConfig({
      plugins: [react()],
      server: {
        port: 3000, // Chạy frontend ở cổng 3000
        proxy: {
          '/api': {
            target: 'http://localhost:8080',
            changeOrigin: true,
            secure: false
          }
        }
      }
    })
    ```
7.  Khởi chạy Frontend ở môi trường local:
    ```bash
    npm run dev
    ```
    Ứng dụng sẽ khả dụng tại địa chỉ: `http://localhost:3000`

---

## 🛠️ 5. Quy trình Phát triển & Quản lý Thay đổi Database chuẩn chỉnh

Để dự án luôn giữ vững tính ổn định và chuẩn hóa cấp doanh nghiệp, hãy tuân thủ quy trình sau:

### 🟢 5.1. Quy trình thay đổi Schema Database (Flyway)
*   **Quy tắc**: Không tự động thay đổi cấu trúc bảng trực tiếp trên database hoặc dùng `ddl-auto: update`.
*   **Thực hiện**: Mọi thay đổi về cấu trúc bảng (thêm cột, tạo bảng mới, đổi kiểu dữ liệu) phải được định nghĩa bằng các file SQL migration mới trong thư mục [db/migration](./backend/src/main/resources/db/migration):
    *   Tên file tuân thủ quy tắc: `V<Version>__<Description>.sql` (Ví dụ: `V2__Add_Workload_Metrics.sql`).
    *   File migration đã chạy rồi thì **tuyệt đối không được chỉnh sửa**. Nếu có lỗi, cần viết file migration mới để rollback hoặc điều chỉnh.

### 🟢 5.2. Đồng bộ DTO và REST API Contract
*   Khi viết Controller trong Backend, luôn sử dụng DTO thay vì trả trực tiếp Entity để bảo vệ cấu trúc dữ liệu nội bộ.
*   Cập nhật đầy đủ các Annotation của `springdoc-openapi` (như `@Schema`, `@Operation`, `@ApiResponse`) trong Controller và DTO của Backend để Swagger UI luôn hiển thị tài liệu chính xác nhất, giúp phát triển Frontend cực nhanh và không cần trao đổi thủ công.

---

Chúc bạn có một trải nghiệm lập trình tuyệt vời với **FOREP - AI Workforce Intelligence Platform**! Nếu cần bất kỳ hỗ trợ nào về việc dựng layout Frontend hoặc viết API Backend mới, hãy yêu cầu tôi ngay lập tức.
