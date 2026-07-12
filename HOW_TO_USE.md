# Hướng dẫn chạy Backend Attendance trên Windows (không cần IDE)

Tài liệu này hướng dẫn chạy server **từ đầu** trên Windows Home, chỉ dùng **CMD** (Command Prompt), không cần cài IntelliJ / Eclipse hay bất kỳ IDE nào.

Dự án dùng: **Kotlin 2.3.21 + Spring Boot 4.1.0**, build bằng **Gradle 9.5.1** (đã kèm sẵn trong project qua `gradlew.bat`, **không cần cài Gradle riêng**), database **MySQL**.

---

## 1. Cần cài gì trước?

| Phần mềm | Phiên bản | Bắt buộc? | Ghi chú |
|----------|-----------|-----------|---------|
| **JDK (Java)** | **17** (LTS) | ✅ Bắt buộc | Project cấu hình chạy trên Java 17. |
| **MySQL Server** | 8.x | ✅ Bắt buộc | Server sẽ không khởi động được nếu không có MySQL. |
| Gradle | — | ❌ Không cần | Đã có sẵn `gradlew.bat` trong project. |

---

## 2. Tải và cài JDK 17

### 2.1. Tải JDK

Nên dùng **Eclipse Temurin (Adoptium)** — bản JDK miễn phí, ổn định:

1. Vào trang: **https://adoptium.net/temurin/releases/?version=17&os=windows&arch=x64**
2. Chọn:
   - **Version:** `17 - LTS`
   - **Operating System:** `Windows`
   - **Architecture:** `x64`
   - **Package Type:** `JDK`
3. Tải file cài đặt đuôi **`.msi`** (ví dụ: `OpenJDK17U-jdk_x64_windows_hotspot_17.x.x_x.msi`).

### 2.2. Cài đặt JDK

1. Chạy file `.msi` vừa tải → bấm **Next** liên tục.
2. Ở bước **Custom Setup**, nên bật (chọn "Will be installed on local hard drive") cho:
   - ✅ **Add to PATH**
   - ✅ **Set JAVA_HOME variable**

   > Bật 2 tùy chọn này thì Windows tự nhận Java, đỡ phải cấu hình tay ở bước sau.
3. Bấm **Install** → chờ xong → **Finish**.

### 2.3. Kiểm tra JDK đã cài đúng

Mở **CMD** (bấm phím `Windows`, gõ `cmd`, Enter) rồi chạy:

```cmd
java -version
```

Kết quả phải hiện dòng bắt đầu bằng `openjdk version "17...`, ví dụ:

```
openjdk version "17.0.13" 2024-10-15
OpenJDK Runtime Environment Temurin-17...
```

> **Nếu báo `'java' is not recognized`**: nghĩa là chưa thêm vào PATH. Xem [Phụ lục A](#phụ-lục-a-tự-đặt-java_home-nếu-lúc-cài-quên-tích-chọn) để đặt tay `JAVA_HOME`.

---

## 3. Cài và chuẩn bị MySQL

Server lưu dữ liệu vào MySQL nên **phải có MySQL đang chạy** trước khi khởi động server.

### 3.1. Tải MySQL

1. Vào: **https://dev.mysql.com/downloads/installer/**
2. Tải **MySQL Installer for Windows** (chọn bản `mysql-installer-community`, file lớn "Windows (x86, 32-bit), MSI Installer").
3. Cài đặt: chọn kiểu **Server only** (hoặc **Developer Default** nếu muốn kèm công cụ Workbench).
4. Trong quá trình cài, khi được hỏi mật khẩu cho tài khoản `root`, **hãy nhớ mật khẩu này** — mặc định project dùng mật khẩu là `password`.

### 3.2. Thông tin kết nối mặc định của project

Nếu bạn không đổi gì, project sẽ kết nối tới:

| Thông số | Giá trị mặc định |
|----------|------------------|
| Host / Port | `localhost:3306` |
| Database | `attendance` (server **tự tạo** nếu chưa có) |
| Username | `root` |
| Password | `password` |

> Bạn **không cần tự tạo** database `attendance` — project có sẵn `createDatabaseIfNotExist=true` nên nó tự tạo khi chạy lần đầu. Bạn chỉ cần MySQL đang chạy và đúng user/mật khẩu.

Nếu MySQL của bạn dùng mật khẩu `root` **khác** `password`, xem [mục 5](#5-tùy-chỉnh-cấu-hình-qua-biến-môi-trường-tùy-chọn) để khai báo lại.

---

## 4. Chạy server bằng CMD

### 4.1. Mở CMD đúng thư mục

Cần mở CMD **ngay tại thư mục gốc của project** (thư mục có chứa file `gradlew.bat`):

```
d:\Work\polatech_server\attendance
```

**Cách 1 — nhanh nhất (mở CMD sẵn tại thư mục):**
1. Mở **File Explorer**, đi tới thư mục `d:\Work\polatech_server\attendance`.
2. Bấm vào **thanh địa chỉ** ở trên cùng (chỗ hiển thị đường dẫn), xóa hết, gõ `cmd` rồi nhấn **Enter**.
3. Một cửa sổ CMD mở ra, con trỏ đã nằm sẵn ở đúng thư mục.

**Cách 2 — mở CMD rồi tự chuyển thư mục:**
```cmd
d:
cd \Work\polatech_server\attendance
```

> Kiểm tra đúng thư mục: gõ `dir` và phải thấy file `gradlew.bat` trong danh sách.

### 4.2. Chạy server (cách đơn giản nhất)

Tại thư mục project, chạy:

```cmd
gradlew.bat bootRun
```

- **Lần chạy đầu tiên** sẽ hơi lâu (vài phút): Gradle tự tải về công cụ build và các thư viện cần thiết. Cần có **kết nối Internet** cho lần đầu.
- Khi thấy dòng log kiểu:

  ```
  Started AttendanceApplication in 5.2 seconds
  ```

  → nghĩa là server đã chạy thành công tại **http://localhost:8080**.

- Để **dừng server**: bấm `Ctrl + C` trong cửa sổ CMD.

### 4.3. Kiểm tra server đã chạy

Mở một cửa sổ CMD **khác** và thử gọi API đăng nhập:

```cmd
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"your-password\"}"
```

Hoặc mở trình duyệt vào `http://localhost:8080` (sẽ trả về lỗi 401/403 vì cần đăng nhập — điều đó vẫn chứng tỏ server **đang chạy**).

---

## 5. Tạo tài khoản Admin đầu tiên

Vì lý do bảo mật, project **không tạo sẵn** tài khoản admin với mật khẩu cố định. Lần đầu chạy, bạn phải khai báo mật khẩu admin qua biến môi trường `ADMIN_PASSWORD`.

Trong CMD, chạy **2 lệnh này** (thay `MatKhauAdmin123` bằng mật khẩu bạn muốn):

```cmd
set ADMIN_PASSWORD=MatKhauAdmin123
gradlew.bat bootRun
```

- Server khởi động sẽ tự tạo tài khoản admin:
  - **Username:** `admin`
  - **Password:** giá trị bạn đặt ở `ADMIN_PASSWORD`
- Nếu **không** đặt `ADMIN_PASSWORD`, log sẽ cảnh báo `No admin exists...` và **không có tài khoản nào được tạo**.
- Tài khoản chỉ được tạo **một lần**. Nếu đã có admin trong database thì lần sau khởi động sẽ bỏ qua bước này.

> **Lưu ý:** lệnh `set` chỉ có hiệu lực trong **cửa sổ CMD hiện tại**. Đóng CMD là mất. Muốn đặt vĩnh viễn, xem [Phụ lục B](#phụ-lục-b-đặt-biến-môi-trường-vĩnh-viễn).

---

## 6. Tùy chỉnh cấu hình qua biến môi trường (tùy chọn)

Nếu MySQL của bạn dùng thông số khác mặc định, khai báo trước khi chạy `gradlew.bat bootRun`. Ví dụ MySQL `root` có mật khẩu là `abc123` và chạy ở cổng 3307:

```cmd
set DB_URL=jdbc:mysql://localhost:3307/attendance?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
set DB_USERNAME=root
set DB_PASSWORD=abc123
set ADMIN_PASSWORD=MatKhauAdmin123
gradlew.bat bootRun
```

Các biến môi trường thường dùng:

| Biến | Mặc định | Ý nghĩa |
|------|----------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/attendance?...` | Chuỗi kết nối MySQL |
| `DB_USERNAME` | `root` | Tài khoản MySQL |
| `DB_PASSWORD` | `password` | Mật khẩu MySQL |
| `SERVER_PORT` | `8080` | Cổng server lắng nghe |
| `ADMIN_USERNAME` | `admin` | Tên tài khoản admin đầu tiên |
| `ADMIN_PASSWORD` | *(trống)* | Mật khẩu admin đầu tiên (bắt buộc đặt lần đầu) |
| `JWT_SECRET` | *(giá trị dev)* | Khóa ký JWT (nên đổi khi chạy thật) |

---

## 7. Chạy dạng file JAR (tùy chọn — cho khi triển khai)

Cách ở [mục 4](#4-chạy-server-bằng-cmd) phù hợp để chạy nhanh/thử. Nếu muốn đóng gói thành 1 file `.jar` chạy độc lập:

**Bước 1 — Đóng gói (build):**
```cmd
gradlew.bat bootJar
```
File JAR sẽ nằm ở: `build\libs\attendance-0.0.1-SNAPSHOT.jar`

**Bước 2 — Chạy JAR bằng Java:**
```cmd
set ADMIN_PASSWORD=MatKhauAdmin123
java -jar build\libs\attendance-0.0.1-SNAPSHOT.jar
```

> Cách này chỉ cần **JDK 17** đã cài, không cần Gradle nữa (JAR đã gói sẵn mọi thứ). Rất tiện để copy file JAR sang máy khác chạy.

---

## 8. Xử lý lỗi thường gặp

| Triệu chứng | Nguyên nhân & cách xử lý |
|-------------|---------------------------|
| `'java' is not recognized` | JDK chưa vào PATH → cài lại JDK và tích **Add to PATH**, hoặc xem [Phụ lục A](#phụ-lục-a-tự-đặt-java_home-nếu-lúc-cài-quên-tích-chọn). |
| `'gradlew.bat' is not recognized` | Bạn đang **sai thư mục**. Phải `cd` vào `d:\Work\polatech_server\attendance` (chỗ có file `gradlew.bat`). |
| Log có `Communications link failure` / `Connection refused` | MySQL **chưa chạy** hoặc sai port. Kiểm tra dịch vụ MySQL đang bật (mở `services.msc` → tìm dịch vụ `MySQL`). |
| Log có `Access denied for user 'root'` | Sai mật khẩu MySQL. Đặt lại `set DB_PASSWORD=...` cho đúng mật khẩu root của bạn. |
| `Port 8080 was already in use` | Cổng 8080 đang bị chiếm. Đổi cổng: `set SERVER_PORT=8081` rồi chạy lại. |
| Đăng nhập admin báo sai mật khẩu | Có thể admin đã được tạo từ lần chạy trước với mật khẩu khác. Tài khoản chỉ tạo 1 lần — dùng đúng mật khẩu lần đầu, hoặc xóa bản ghi admin trong DB rồi chạy lại với `ADMIN_PASSWORD` mới. |
| Lần đầu build đứng rất lâu | Bình thường — Gradle đang tải thư viện. Đảm bảo có **Internet** và chờ. |

---

## Phụ lục A: Tự đặt JAVA_HOME (nếu lúc cài quên tích chọn)

1. Bấm `Windows`, gõ **"environment variables"**, chọn **"Edit the system environment variables"**.
2. Bấm nút **Environment Variables...**
3. Ở phần **System variables**, bấm **New...**:
   - **Variable name:** `JAVA_HOME`
   - **Variable value:** đường dẫn thư mục JDK, ví dụ `C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot`
4. Vẫn ở **System variables**, chọn dòng **`Path`** → **Edit...** → **New** → thêm: `%JAVA_HOME%\bin`
5. Bấm **OK** hết các cửa sổ.
6. **Đóng và mở lại CMD**, kiểm tra lại: `java -version`.

## Phụ lục B: Đặt biến môi trường vĩnh viễn

Nếu không muốn gõ `set ...` mỗi lần, có thể đặt cố định:

```cmd
setx ADMIN_PASSWORD "MatKhauAdmin123"
setx DB_PASSWORD "abc123"
```

> Lưu ý: `setx` chỉ có hiệu lực ở **CMD mở MỚI sau đó** (không áp dụng cho cửa sổ hiện tại). Không dùng `setx` cho thông tin nhạy cảm trên máy dùng chung.

---

## Tóm tắt nhanh (TL;DR)

```cmd
:: 1. Cài JDK 17 (Temurin) + MySQL 8 (nhớ mật khẩu root)
:: 2. Mở CMD tại thư mục project:
d:
cd \Work\polatech_server\attendance

:: 3. Đặt mật khẩu admin (lần đầu) và chạy:
set ADMIN_PASSWORD=MatKhauAdmin123
gradlew.bat bootRun

:: 4. Server chạy tại http://localhost:8080  — dừng bằng Ctrl + C
```
