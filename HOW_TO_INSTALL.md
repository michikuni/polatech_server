# Hướng dẫn triển khai OFFLINE (máy không có Internet, chạy trong LAN)

Tài liệu này hướng dẫn đóng gói toàn bộ hệ thống trên **một máy có mạng**, sau đó **copy qua USB** sang máy đích **hoàn toàn không có Internet** — chỉ copy sang là chạy được, các máy khác trong LAN truy cập bình thường.

> **Điểm mấu chốt:** KHÔNG cần copy SDK, Gradle hay thư viện gì cả. Khi build ra file JAR (`bootJar`), **toàn bộ thư viện đã được gói sẵn bên trong JAR** — kể cả Flyway migration để tự tạo bảng. Máy đích chỉ cần đúng 3 thứ:

| Thứ cần copy | Dạng | Ghi chú |
|--------------|------|---------|
| **JDK 17 bản portable (`.zip`)** | Giải nén là chạy, không cần cài | Temurin bản `.zip` (không phải `.msi`) |
| **MySQL 8 bản ZIP Archive** | Giải nén là chạy, không cần installer | Bản "Windows (x86, 64-bit), ZIP Archive" |
| **File JAR đã build** | 1 file duy nhất | `build\libs\attendance-0.0.1-SNAPSHOT.jar` |

---

## 1. Chuẩn bị trên máy CÓ mạng

Tạo một thư mục đóng gói, ví dụ `D:\attendance-offline\`, với cấu trúc:

```
attendance-offline\
├── jdk-17\                      ← giải nén sẵn JDK vào đây
├── mysql\                       ← giải nén sẵn MySQL vào đây
├── attendance.jar               ← copy từ build\libs\
├── my.ini
├── 1-init-mysql-lan-dau.bat
├── 2-start-mysql.bat
└── 3-start-server.bat
```

### 1.1. Tải JDK 17 bản portable

1. Vào: **https://adoptium.net/temurin/releases/?version=17&os=windows&arch=x64**
2. Chọn:
   - **Version:** `17 - LTS`
   - **Operating System:** `Windows`
   - **Architecture:** `x64`
   - **Package Type:** `JDK`
3. Tải file đuôi **`.zip`** (ví dụ `OpenJDK17U-jdk_x64_windows_hotspot_17.x.x.zip`) — **không** lấy bản `.msi`.
4. Giải nén vào `attendance-offline\jdk-17\`, sao cho tồn tại file:

   ```
   attendance-offline\jdk-17\bin\java.exe
   ```

   > Nếu giải nén ra thư mục con kiểu `jdk-17.0.13+11\` thì di chuyển toàn bộ nội dung bên trong lên thẳng `jdk-17\`.

### 1.2. Tải MySQL 8 bản ZIP Archive

1. Vào: **https://dev.mysql.com/downloads/mysql/**
2. Chọn bản **"Windows (x86, 64-bit), ZIP Archive"** (khoảng 200–400 MB) — **không** lấy bản MSI Installer.
3. Giải nén vào `attendance-offline\mysql\`, sao cho tồn tại file:

   ```
   attendance-offline\mysql\bin\mysqld.exe
   ```

### 1.3. Build file JAR

Mở CMD tại thư mục gốc của project (chỗ có `gradlew.bat`) và chạy:

```cmd
gradlew.bat bootJar
copy build\libs\attendance-0.0.1-SNAPSHOT.jar D:\attendance-offline\attendance.jar
```

### 1.4. Tạo file `my.ini`

Tạo file `attendance-offline\my.ini` với nội dung (MySQL chạy portable, dữ liệu lưu ngay trong thư mục):

```ini
[mysqld]
basedir=./mysql
datadir=./mysql-data
port=3306
character-set-server=utf8mb4
default-time-zone=+00:00
```

### 1.5. Tạo 3 file script `.bat`

**`1-init-mysql-lan-dau.bat`** — chỉ chạy **MỘT LẦN DUY NHẤT** trên máy đích, để khởi tạo dữ liệu MySQL và đặt mật khẩu root:

```bat
@echo off
cd /d %~dp0
mysql\bin\mysqld --defaults-file=my.ini --initialize-insecure --console
start "" mysql\bin\mysqld --defaults-file=my.ini --console
timeout /t 10
mysql\bin\mysql -u root --skip-password -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'password';"
echo Xong! MySQL da khoi tao, mat khau root la: password
pause
```

**`2-start-mysql.bat`** — chạy mỗi lần bật máy (giữ nguyên cửa sổ này, đóng là MySQL tắt):

```bat
@echo off
cd /d %~dp0
mysql\bin\mysqld --defaults-file=my.ini --console
```

**`3-start-server.bat`** — chạy sau khi MySQL đã lên (nhớ đổi 2 giá trị bên dưới):

```bat
@echo off
cd /d %~dp0
set ADMIN_PASSWORD=MatKhauAdmin123
set JWT_SECRET=doi-chuoi-nay-thanh-chuoi-ngau-nhien-dai-it-nhat-32-ky-tu
jdk-17\bin\java -jar attendance.jar
```

> Không cần đặt `JAVA_HOME` hay PATH trên máy đích — script gọi thẳng `jdk-17\bin\java` nằm ngay trong thư mục.
>
> `ADMIN_PASSWORD` chỉ có tác dụng ở **lần chạy đầu tiên** (tạo tài khoản admin). `JWT_SECRET` phải là chuỗi ngẫu nhiên **tối thiểu 32 ký tự** — bắt buộc đổi khi chạy thật.

---

## 2. Trên máy KHÔNG có mạng

1. Copy **nguyên thư mục** `attendance-offline\` qua USB sang máy đích (ví dụ vào `D:\attendance-offline\`).
2. Chạy `1-init-mysql-lan-dau.bat` — **một lần duy nhất**.
3. Từ đó về sau, mỗi lần bật máy:
   1. Chạy `2-start-mysql.bat` (giữ cửa sổ mở).
   2. Chạy `3-start-server.bat` (giữ cửa sổ mở).
4. Khi thấy log `Started AttendanceApplication in ... seconds` → server đã chạy tại `http://localhost:8080`.

Server sẽ tự làm mọi thứ mà **không cần Internet**:
- Tự tạo database `attendance` (nhờ `createDatabaseIfNotExist=true`).
- Tự chạy migration tạo bảng (Flyway nằm sẵn trong JAR).
- Tự tạo tài khoản admin từ `ADMIN_PASSWORD` (chỉ lần đầu).

---

## 3. Cho các máy khác trong LAN truy cập

### 3.1. Mở firewall cổng 8080 trên máy chạy server

Mở CMD **Run as Administrator** và chạy:

```cmd
netsh advfirewall firewall add rule name="Attendance Server" dir=in action=allow protocol=TCP localport=8080
```

> **KHÔNG** mở cổng 3306 ra LAN — MySQL chỉ để server dùng nội bộ trong máy.

### 3.2. Tìm địa chỉ IP của máy server

```cmd
ipconfig
```

Tìm dòng **IPv4 Address** của card mạng LAN, ví dụ `192.168.1.10`.

> Nên đặt **IP tĩnh** cho máy server (hoặc gán IP cố định theo MAC trên router) để địa chỉ không đổi sau mỗi lần khởi động.

### 3.3. Truy cập từ máy/điện thoại khác trong LAN

Các thiết bị cùng mạng LAN gọi API qua:

```
http://192.168.1.10:8080
```

(thay `192.168.1.10` bằng IP thật của máy server).

---

## 4. Xử lý lỗi thường gặp

| Triệu chứng | Nguyên nhân & cách xử lý |
|-------------|---------------------------|
| Chạy `1-init...bat` báo lỗi `datadir ... already exists` | Đã khởi tạo rồi. Chỉ chạy file này 1 lần. Muốn làm lại từ đầu: xóa thư mục `mysql-data\` rồi chạy lại (⚠️ mất toàn bộ dữ liệu). |
| `mysqld` tắt ngay khi vừa chạy | Xem thông báo lỗi trong cửa sổ console. Thường do thiếu **Visual C++ Redistributable 2019** — tải file `vc_redist.x64.exe` từ máy có mạng, copy sang cài offline. |
| Log server có `Communications link failure` | MySQL chưa chạy. Chạy `2-start-mysql.bat` trước, đợi vài giây rồi mới chạy `3-start-server.bat`. |
| Log server có `Access denied for user 'root'` | Mật khẩu root không phải `password`. Thêm dòng `set DB_PASSWORD=matkhauthat` vào `3-start-server.bat`. |
| `Port 8080 was already in use` | Cổng bị chiếm. Thêm `set SERVER_PORT=8081` vào `3-start-server.bat` (nhớ mở firewall cổng mới). |
| Máy khác trong LAN không truy cập được | Kiểm tra: (1) đã mở firewall cổng 8080 chưa, (2) hai máy có cùng dải mạng không (`ping 192.168.1.10`), (3) mạng Wi-Fi có bật chế độ cách ly thiết bị (AP/Client Isolation) không. |
| Đăng nhập admin báo sai mật khẩu | Admin đã được tạo từ lần chạy đầu với mật khẩu lúc đó. Tài khoản chỉ tạo 1 lần — `ADMIN_PASSWORD` các lần sau không có tác dụng đổi mật khẩu. |

---

## 5. (Nâng cao) Muốn build/sửa code trên máy offline?

Cách ở trên chỉ để **chạy** hệ thống — đây là cách khuyến nghị. Nếu bắt buộc phải **build lại từ source** trên máy không có mạng:

1. Trên máy có mạng, build thành công ít nhất 1 lần (`gradlew.bat bootJar`) để Gradle tải đủ mọi thứ.
2. Copy sang máy đích, **đúng vị trí tương ứng**:
   - Toàn bộ thư mục source code của project.
   - `C:\Users\<user>\.gradle\wrapper\` → `C:\Users\<user-máy-đích>\.gradle\wrapper\` (bản Gradle mà wrapper đã tải).
   - `C:\Users\<user>\.gradle\caches\` → `C:\Users\<user-máy-đích>\.gradle\caches\` (toàn bộ thư viện đã tải, có thể vài GB).
3. Trên máy đích, build ở chế độ offline:

   ```cmd
   gradlew.bat --offline bootJar
   ```

> Cách này dễ trục trặc (cache khác phiên bản, khác đường dẫn user...). Khuyến nghị: luôn build ở máy có mạng, chỉ copy file `attendance.jar` mới sang máy đích khi cần cập nhật.

---

## Tóm tắt nhanh (TL;DR)

```
:: MÁY CÓ MẠNG:
::   1. Tải JDK 17 (.zip) + MySQL 8 (ZIP Archive), giải nén vào jdk-17\ và mysql\
::   2. gradlew.bat bootJar  →  copy attendance.jar vào thư mục đóng gói
::   3. Tạo my.ini + 3 file .bat (theo mục 1.4, 1.5)
::   4. Copy nguyên thư mục qua USB

:: MÁY OFFLINE:
::   5. Chạy 1-init-mysql-lan-dau.bat   (một lần duy nhất)
::   6. Mỗi lần bật máy: 2-start-mysql.bat  →  3-start-server.bat
::   7. Mở firewall cổng 8080  →  LAN truy cập qua http://<IP-máy-server>:8080
```
