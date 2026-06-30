# Attendance — Hệ thống chấm công nội bộ LAN

Backend chấm công (attendance) chạy trong mạng **LAN nội bộ**, viết bằng **Kotlin + Spring Boot 4.1**. Mỗi nhân viên chấm công bằng **thiết bị đã ghép cặp (paired device)**, và mỗi lần chấm công được xác thực bằng **chữ ký số ECDSA (challenge–response)** thay vì chỉ gọi REST API thông thường — nên hệ thống chống được giả mạo, phát lại (replay) và chấm công hộ.

> Đây là **backend-only** (REST API + cơ sở dữ liệu). Thiết bị của nhân viên (mobile/desktop client) là phía nắm giữ **private key** và tự sinh chữ ký; backend chỉ giữ **public key**.

---

## Mục lục

- [Tổng quan kiến trúc](#tổng-quan-kiến-trúc)
- [Các module (feature)](#các-module-feature)
- [Thuật toán & cơ chế bảo mật](#thuật-toán--cơ-chế-bảo-mật)
- [Ngoài CRUD còn gì nữa?](#ngoài-crud-còn-gì-nữa)
- [Danh sách REST API](#danh-sách-rest-api)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc dữ liệu (migrations)](#cấu-trúc-dữ-liệu-migrations)
- [Cấu hình & cách chạy](#cấu-hình--cách-chạy)
- [Kiểm thử](#kiểm-thử)

---

## Tổng quan kiến trúc

- **Kiến trúc feature-first**: mỗi nghiệp vụ là một package độc lập dưới `com.mpcorp.attendance.<feature>`, gồm các lớp `entity` / `repository` / `dto` / `mapper` / `service` / `controller`. Phần dùng chung nằm trong `common`.
- **Phân lớp rõ ràng**: Controller (HTTP/validation) → Service (business logic + transaction) → Repository (Spring Data JPA) → MySQL.
- **Schema do Flyway sở hữu**: Hibernate chạy ở chế độ `ddl-auto=validate` (chỉ kiểm tra, không tự tạo bảng). Mọi thay đổi schema đi qua migration `V1`…`V7`.
- **Mapper viết tay** (extension function/lớp mapper Kotlin) — không dùng MapStruct/Lombok.
- **Thời gian lưu UTC**, biên giới ngày (day boundary) được tính theo múi giờ nghiệp vụ có thể cấu hình (`app.time-zone`, mặc định `Asia/Ho_Chi_Minh`) thông qua một bean `ZoneId` được inject.

```
com.mpcorp.attendance
├── common        // crypto, response, exception, config, persistence
├── auth          // đăng nhập admin + JWT
├── employee      // quản lý nhân viên
├── device        // ghép cặp & quản lý thiết bị
├── challenge     // sinh & quản lý challenge (nonce)
├── attendance    // ghi nhận & truy vấn chấm công
└── audit         // nhật ký kiểm toán append-only
```

---

## Các module (feature)

### 1. `common` — Hạ tầng dùng chung
- **`common.crypto`** — lõi mật mã, không phụ thuộc thư viện ngoài (chỉ Java Security API):
  - `CryptoConstants` — tham số chuẩn: `EC` / `SHA256withECDSA` / challenge 32 byte (256-bit) / TTL 60s / đường cong P-256.
  - `PublicKeyParser` — giải mã public key X.509 (Base64) và **bắt buộc đúng đường cong P-256** (kiểm tra `fieldSize == 256`).
  - `SignatureVerifier` — xác minh chữ ký ECDSA `SHA256withECDSA`.
  - `ChallengeGenerator` — sinh nonce ngẫu nhiên 32 byte bằng `SecureRandom`.
  - `Base64Utils`, `CryptoException`.
- **`common.response`** — `ApiResponse` / `ApiError` (định dạng phản hồi thống nhất) và `PageResponse` (bao bọc phân trang).
- **`common.exception`** — `ErrorCode`, `ApiException` và các con (`ResourceNotFound`, `BusinessRule`, `InvalidSignature`…), `GlobalExceptionHandler` (`@RestControllerAdvice` ánh xạ lỗi → HTTP status).
- **`common.config`** — `JpaConfig` (`@EnableJpaAuditing`), `AppConfig` (bean `Clock`, bean `ZoneId`).
- **`common.persistence`** — `BaseEntity` (id, createdAt/updatedAt được audit tự động).

### 2. `auth` — Xác thực Admin
- Đăng nhập admin → cấp **JWT (HS256)** tự ký bằng JDK (không dùng thư viện JWT ngoài).
- `JwtAuthenticationFilter` + `SecurityConfig` (stateless), `RestAuthenticationEntryPoint` trả lỗi 401 dạng JSON.
- **`AdminBootstrap`**: tạo admin đầu tiên từ biến môi trường (`ADMIN_USERNAME`/`ADMIN_PASSWORD`) — không có admin nào được tạo nếu `ADMIN_PASSWORD` rỗng. Mật khẩu băm bằng `PasswordEncoder` của Spring Security.

### 3. `employee` — Quản lý nhân viên (admin-only)
- CRUD nhân viên: tạo / sửa / xem / tìm kiếm (theo trạng thái active + từ khóa `q`, có phân trang) + bật/tắt `active`.
- Chống trùng `code` nhân viên. Sửa/đổi trạng thái dựa vào **JPA dirty-checking** (không gọi `save` thủ công).

### 4. `device` — Ghép cặp & quản lý thiết bị
- **Phát mã ghép cặp (pairing code) một lần** cho nhân viên (admin): mã ngắn dễ gõ, sinh bằng `SecureRandom` với **bảng chữ cái loại bỏ ký tự dễ nhầm** (0/O, 1/I). Mã có TTL (mặc định 600s) và **chỉ lưu SHA-256 hash** của mã, không lưu mã thô.
- **Ghép cặp thiết bị (public, không cần đăng nhập)**: thiết bị tự sinh cặp khóa EC, gửi pairing code + public key + **chữ ký proof-of-possession** ký lên chính pairing code. Backend xác minh → **revoke thiết bị ACTIVE cũ** → tạo thiết bị ACTIVE mới → đánh dấu mã đã dùng. Mỗi nhân viên chỉ có **một thiết bị ACTIVE** tại một thời điểm.
- Backend **chỉ lưu public key** (kèm fingerprint SHA-256); private key không bao giờ rời thiết bị.
- Admin: liệt kê / thu hồi (revoke) thiết bị.

### 5. `challenge` — Sinh thử thách (nonce)
- `POST /api/challenge` (public) cấp một **challenge dùng-một-lần** cho thiết bị ACTIVE: nonce 32 byte (Base64) + `expiresAt`.
- **Tính hợp lệ được suy ra** (derived), không có cột trạng thái: challenge hợp lệ nếu chưa hết hạn và chưa bị tiêu thụ.
- `consume(id, now)` là câu lệnh **`@Modifying` nguyên tử** — trả về 1 nếu lần gọi này "giành" được challenge → đảm bảo **đúng-một-lần (exactly-once)**, chống race/replay.

### 6. `attendance` — Ghi nhận & truy vấn chấm công
- **Ghi nhận (public)**: xác minh chữ ký rồi mới ghi sự kiện CHECK_IN/CHECK_OUT (chi tiết ở [thuật toán](#thuật-toán--cơ-chế-bảo-mật) bên dưới).
- **Truy vấn (admin)**: tìm theo nhân viên + khoảng ngày (phân trang), và **`daily-summary`** — tổng hợp công trong ngày.

### 7. `audit` — Nhật ký kiểm toán
- Bảng **append-only** (`AuditLog`, không kế thừa `BaseEntity` — chỉ có `at`, không có `updatedAt`).
- `AuditService.record(...)` chạy trong transaction **`REQUIRES_NEW`** → một bản ghi audit (ví dụ "chữ ký sai") **vẫn được lưu dù transaction nghiệp vụ bị rollback**.
- Được gắn vào các nghiệp vụ chính: đăng nhập admin, tạo/sửa nhân viên, phát mã ghép cặp, ghép cặp thiết bị, thu hồi thiết bị, ghi nhận chấm công, và **chữ ký xác minh thất bại**. (Cố tình **không** audit việc cấp challenge vì tần suất quá cao.)

---

## Thuật toán & cơ chế bảo mật

### Mật mã đường cong elliptic (ECDSA / P-256)
- Cặp khóa: **EC trên đường cong P-256 (secp256r1)**.
- Chữ ký: **`SHA256withECDSA`** (băm SHA-256 rồi ký bằng private key).
- Public key trao đổi dưới dạng **X.509 SubjectPublicKeyInfo, mã hóa Base64**.
- Toàn bộ chỉ dùng **`java.security`** (KeyFactory/Signature/SecureRandom) — không phụ thuộc thư viện crypto bên thứ ba.

### Luồng Challenge–Response (cốt lõi của hệ thống)

Đây là điểm khiến dự án **không phải** chỉ là CRUD: mỗi lần chấm công là một giao thức xác thực bằng chữ ký số.

```
Thiết bị                                  Backend
   │  POST /api/challenge {deviceId}          │
   │ ───────────────────────────────────────► │  sinh nonce 32 byte (SecureRandom),
   │                                           │  lưu kèm expiresAt (60s), trả về Base64
   │ ◄─────────────────────────────────────── │
   │                                           │
   │  ký = ECDSA( Base64decode(nonce) ‖ UTF8(type) )   ← ký bằng private key
   │                                           │
   │  POST /api/attendance                     │
   │     {challengeId, type, signature}        │
   │ ───────────────────────────────────────► │  1. challenge còn hợp lệ?
   │                                           │  2. device còn ACTIVE?
   │                                           │  3. verify chữ ký TRƯỚC
   │                                           │  4. consume nguyên tử (exactly-once)
   │                                           │  5. lưu AttendanceEvent + lastUsedAt
   │ ◄─────────────────────────────────────── │
```

Các điểm thiết kế quan trọng (đều có trong [AttendanceService.kt](src/main/kotlin/com/mpcorp/attendance/attendance/service/AttendanceService.kt)):

1. **Ký cả loại sự kiện**: thông điệp được ký là `nonce ‖ UTF8(type.name)` (nối nonce với "CHECK_IN"/"CHECK_OUT"). Nhờ vậy không thể tráo CHECK_IN thành CHECK_OUT mà giữ nguyên chữ ký.
2. **Verify trước khi consume**: nếu chữ ký sai thì **không tiêu thụ** challenge (chữ ký sai không "đốt" một challenge hợp lệ) — đồng thời ghi audit `SIGNATURE_VERIFY_FAILED` và trả `401`.
3. **Consume nguyên tử = chống replay/race**: `markConsumed` dùng UPDATE có điều kiện; chỉ một request "thắng". Gửi lại cùng challenge → bị từ chối.
4. **Challenge có thời hạn ngắn** (60s) và **dùng một lần**, ràng buộc với đúng một `deviceId`.

### Proof-of-possession khi ghép cặp
Khi enroll, thiết bị phải **ký lên chính pairing code** bằng private key tương ứng với public key gửi lên. Backend verify chữ ký này → đảm bảo thiết bị **thực sự sở hữu private key**, không chỉ "copy" một public key bất kỳ.

### Thuật toán tổng hợp công trong ngày (`dailySummary`)
Xem [AttendanceQueryService.kt](src/main/kotlin/com/mpcorp/attendance/attendance/service/AttendanceQueryService.kt). Duyệt các sự kiện trong ngày theo **thứ tự thời gian** và **ghép cặp CHECK_IN với CHECK_OUT kế tiếp**:
- Mở một phiên khi gặp CHECK_IN; cộng dồn `workedSeconds = Σ Duration(in → out)` khi gặp CHECK_OUT đóng phiên.
- Theo dõi `firstCheckIn`, `lastCheckOut`, đếm số lần in/out, và cờ **`openSession`** (còn một CHECK_IN chưa có CHECK_OUT tương ứng).
- Biên ngày `[start, end)` tính theo **múi giờ nghiệp vụ** (`businessZone`), dữ liệu vẫn lưu UTC.

---

## Ngoài CRUD còn gì nữa?

Đây là câu hỏi trọng tâm — và câu trả lời là **rất nhiều**. CRUD chỉ là phần `employee` và một phần `device`. Phần còn lại là logic bảo mật & nghiệp vụ:

| Hạng mục | Mô tả |
|---|---|
| **Giao thức challenge–response** | Xác thực mỗi lần chấm công bằng chữ ký số, không chỉ là POST dữ liệu. |
| **Mật mã ECDSA/P-256 tự cài** | Parse/validate public key, verify chữ ký, sinh nonce — chỉ bằng JDK. |
| **Proof-of-possession** | Chứng minh quyền sở hữu private key khi ghép cặp thiết bị. |
| **Chống replay & race** | Challenge dùng-một-lần + UPDATE nguyên tử đảm bảo exactly-once. |
| **Chống chấm công hộ** | Một nhân viên = một thiết bị ACTIVE; thiết bị cũ bị revoke tự động. |
| **Bảo mật bí mật** | Pairing code & public key chỉ lưu dưới dạng **SHA-256 hash/fingerprint**. |
| **JWT tự ký (HS256)** | Cấp/verify token admin bằng JDK, filter bảo mật stateless. |
| **Bootstrap admin** | Tạo admin đầu tiên an toàn từ biến môi trường. |
| **Audit log append-only** | Ghi nhật ký bất biến trong transaction `REQUIRES_NEW` (sống sót qua rollback). |
| **Tổng hợp công** | Thuật toán ghép cặp in/out + tính giờ làm theo múi giờ. |
| **Xử lý lỗi tập trung** | `GlobalExceptionHandler` ánh xạ lỗi nghiệp vụ → HTTP status nhất quán. |
| **Migration & validate schema** | Flyway sở hữu schema, Hibernate chỉ `validate`. |

---

## Danh sách REST API

### Công khai (thiết bị, không cần đăng nhập)
| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/devices/enroll` | Ghép cặp thiết bị (pairing code + public key + proof signature) |
| `POST` | `/api/challenge` | Cấp challenge cho thiết bị ACTIVE |
| `POST` | `/api/attendance` | Ghi nhận chấm công (kèm chữ ký) |

### Admin (cần JWT)
| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/auth/login` | Đăng nhập admin → JWT |
| `GET` / `POST` | `/api/admin/employees` | Liệt kê / tạo nhân viên |
| `GET` / `PUT` | `/api/admin/employees/{id}` | Xem / cập nhật nhân viên |
| `PATCH` | `/api/admin/employees/{id}/active` | Bật/tắt nhân viên |
| `POST` | `/api/admin/employees/{employeeId}/enrollment-codes` | Phát mã ghép cặp |
| `GET` | `/api/admin/devices` | Liệt kê thiết bị |
| `POST` | `/api/admin/devices/{id}/revoke` | Thu hồi thiết bị |
| `GET` | `/api/admin/attendance` | Tra cứu sự kiện chấm công (lọc + phân trang) |
| `GET` | `/api/admin/attendance/daily-summary` | Tổng hợp công theo ngày |
| `GET` | `/api/admin/audit` | Tra cứu nhật ký kiểm toán |

---

## Công nghệ sử dụng

- **Ngôn ngữ**: Kotlin 2.3 (JVM toolchain 17), `-Xjsr305=strict` (null-safety nghiêm ngặt với API Java/Spring).
- **Framework**: Spring Boot **4.1.0** — `spring-boot-starter-webmvc` (REST), Spring Data JPA, Spring Security, Bean Validation.
- **CSDL**: MySQL 8 + Flyway (migration `V1`–`V7`).
- **JSON**: Jackson 3 (`tools.jackson`) + `jackson-module-kotlin`.
- **Mật mã**: chỉ Java Security API (không thư viện ngoài).
- **Build**: Gradle (Kotlin DSL) + JaCoCo (gate coverage ≥ 80% cho `common.crypto`).
- **Test**: JUnit 5 + Kotlin test + Mockito.

> Lưu ý: dự án **không** dùng Lombok, MapStruct, devtools hay springdoc-openapi (xem `memory`/lịch sử quyết định).

---

## Cấu trúc dữ liệu (migrations)

| File | Nội dung |
|---|---|
| `V1__baseline.sql` | Baseline (chưa có bảng nghiệp vụ) |
| `V2__create_admin.sql` | `admin` |
| `V3__create_employee.sql` | `employee` |
| `V4__create_device_and_enrollment_code.sql` | `device`, `enrollment_code` |
| `V5__create_challenge.sql` | `challenge` |
| `V6__create_attendance_event.sql` | `attendance_event` (FK → employee/device/challenge) |
| `V7__create_audit_log.sql` | `audit_log` |

---

## Cấu hình & cách chạy

Mọi thông tin nhạy cảm được externalize qua biến môi trường (giá trị mặc định **chỉ dùng cho local dev**, phải override khi production):

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | localhost / attendance / attendance | Kết nối MySQL |
| `JWT_SECRET` | (dev secret) | Khóa ký JWT — **≥ 32 byte**, bắt buộc đổi ở production |
| `JWT_EXPIRATION_SECONDS` | 28800 (8h) | Thời hạn token admin |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | admin / *(rỗng)* | Bootstrap admin đầu tiên (không tạo nếu password rỗng) |
| `APP_TIME_ZONE` | `Asia/Ho_Chi_Minh` | Múi giờ tính biên ngày |
| `ENROLLMENT_CODE_TTL_SECONDS` | 600 | TTL mã ghép cặp |
| `ENROLLMENT_CODE_LENGTH` | 10 | Độ dài mã ghép cặp |
| `SERVER_PORT` | 8080 | Cổng HTTP |

```bash
# Chạy ứng dụng (cần MySQL 8 sẵn sàng; Flyway sẽ tự migrate)
ADMIN_PASSWORD='...' ./gradlew bootRun

# Build
./gradlew build
```

---

## Kiểm thử

```bash
./gradlew test                              # 76 unit test trên 17 lớp
./gradlew jacocoTestCoverageVerification    # gate ≥ 80% cho common.crypto
```

- **76 unit test** bao phủ crypto, auth/JWT, employee, enrollment/device, challenge, attendance và audit.
- JaCoCo bắt buộc **độ phủ ≥ 80%** cho package `com.mpcorp.attendance.common.crypto` (hiện ~85.8% instruction).

> ⚠️ **Rủi ro còn lại cần lưu ý**: hệ thống chưa được chạy thực tế với MySQL 8 — cặp `ddl-auto=validate` + Flyway migrations cần được kiểm chứng khi triển khai lần đầu (đảm bảo schema do Flyway tạo khớp với mapping của Hibernate).
