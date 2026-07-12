# BÁO CÁO MÔ TẢ HỆ THỐNG CHẤM CÔNG ĐIỆN TỬ

**Dành cho người đọc không chuyên về kỹ thuật**
Ngày lập: 12/07/2026 — *cập nhật: bổ sung cơ chế QR kết nối máy chủ; đã chạy thử thành công với cơ sở dữ liệu MySQL thật.*

---

## 1. TÓM TẮT NHANH (đọc 2 phút là hiểu)

Đây là một **hệ thống chấm công cho cán bộ trực ca**, gồm 3 phần:

1. **Ứng dụng điện thoại** (Android + iPhone) — cán bộ bấm "Vào ca" / "Tan ca", xác nhận bằng **vân tay hoặc Face ID**.
2. **Máy chủ** — chạy trong mạng nội bộ của đơn vị, lưu toàn bộ dữ liệu chấm công.
3. **Trang quản trị trên web** — quản trị viên mở bằng trình duyệt để quản lý cán bộ, xem chấm công, xuất báo cáo Excel.

**Điểm khác biệt lớn nhất so với các phần mềm chấm công thông thường:** hệ thống này được thiết kế để **chống chấm công hộ**. Mỗi lần chấm công không đơn thuần là "gửi một tin nhắn lên máy chủ" — mà là một **màn xác thực bằng chữ ký số**, giống như ký một văn bản bằng con dấu điện tử mà chỉ đúng chiếc điện thoại đó, với đúng vân tay của chủ nhân, mới tạo ra được.

**Cán bộ không phải gõ bất cứ địa chỉ kỹ thuật nào.** Máy chủ **tự dò địa chỉ IP của chính nó** và in ra mã QR trên trang quản trị; cán bộ chỉ việc quét. Kể cả khi máy chủ khởi động lại và đổi sang IP khác, cán bộ chỉ cần quét lại QR — không cần cấp mã ghép cặp mới, không mất khoá bảo mật đã có.

**Tình trạng hiện tại:** phần lập trình đã hoàn thiện các chức năng chính, có **107 bài kiểm thử tự động** cho máy chủ và **15 bài** cho ứng dụng điện thoại. Máy chủ **đã được chạy thử thành công với cơ sở dữ liệu MySQL thật** (khởi tạo cấu trúc dữ liệu, tạo tài khoản quản trị, gọi API — đều chạy đúng). **Chưa triển khai vận hành thật với người dùng.** Còn 3 việc cần xử lý trước khi đưa vào sử dụng chính thức (xem Mục 9).

---

## 2. HỆ THỐNG GIẢI QUYẾT VẤN ĐỀ GÌ?

Các cách chấm công phổ biến hiện nay đều có kẽ hở:

| Cách chấm công | Kẽ hở |
|---|---|
| Ký sổ giấy | Ký hộ, ký khống, sửa giờ, khó tổng hợp cuối tháng |
| Quẹt thẻ từ | Đưa thẻ nhờ người khác quẹt hộ |
| App chấm công thông thường | Cho bạn mượn tài khoản, đăng nhập từ máy khác chấm hộ |
| Vân tay tại máy chấm công | Phải xếp hàng tại một điểm cố định; máy hỏng là tắc |

Hệ thống này ràng buộc **3 thứ vào làm một**:

> **Một cán bộ = một chiếc điện thoại đã đăng ký = một dấu vân tay.**

Muốn chấm công hộ đồng nghiệp, người ta phải có **đúng chiếc điện thoại của đồng nghiệp đó** *và* **ngón tay/khuôn mặt của chính đồng nghiệp đó**. Chỉ mượn được điện thoại thôi là **không đủ** — vì mỗi lần bấm chấm công, điện thoại đều bắt quét vân tay/Face ID.

Ngoài ra hệ thống còn phục vụ nghiệp vụ **trực ca**: khi vào ca, cán bộ có thể ghi lại **"Thông tin tiếp nhận ca trực"** (bàn giao ca) — và ghi chú này **chỉ được ghi một lần, không sửa được**, đúng tinh thần biên bản bàn giao.

---

## 3. HỆ THỐNG GỒM NHỮNG PHẦN NÀO?

```
   ĐIỆN THOẠI CÁN BỘ                MÁY CHỦ (đặt tại đơn vị)          MÁY TÍNH QUẢN TRỊ VIÊN
  ┌──────────────────┐            ┌───────────────────────┐          ┌────────────────────┐
  │  App "Chấm công" │            │  Phần mềm máy chủ     │          │  Trình duyệt web   │
  │                  │  mạng nội  │  + Kho dữ liệu (MySQL)│  mạng    │  → Trang quản trị  │
  │  • Vào ca        │◄──────────►│                       │◄────────►│  • Quản lý cán bộ  │
  │  • Tan ca        │  bộ (LAN)  │  Lưu: cán bộ, thiết bị│  nội bộ  │  • Cấp mã QR       │
  │  • Ghi chú ca    │            │  chấm công, nhật ký   │          │  • Xem chấm công   │
  │  • Xem lịch sử   │            │                       │          │  • Xuất Excel      │
  └──────────────────┘            └───────────────────────┘          └────────────────────┘
     Giữ "chìa khoá bí mật"           Chỉ giữ "ổ khoá"                 Đăng nhập bằng
     trong chip bảo mật                (không giữ chìa)                tài khoản + mật khẩu
```

**Ba phần này nói chuyện với nhau trong mạng nội bộ (LAN) của đơn vị** — không cần Internet, không có dữ liệu nào gửi ra ngoài, không phụ thuộc nhà cung cấp dịch vụ đám mây nào.

---

## 4. DỰ ÁN NÀY LÀM BẰNG GÌ?

### 4.1. Ứng dụng điện thoại

| Thành phần | Tên kỹ thuật | Giải thích cho người non-tech |
|---|---|---|
| Công cụ xây app | **Flutter** (ngôn ngữ Dart) | Công nghệ của Google. Ưu điểm lớn: **viết một lần, chạy được cả Android lẫn iPhone** — tiết kiệm khoảng một nửa công sức so với làm hai app riêng. |
| Chip bảo mật | **Android Keystore / Apple Secure Enclave** | Một "két sắt" nằm ngay trong phần cứng điện thoại. Chìa khoá bí mật của cán bộ được tạo ra và nằm mãi trong két này — **không phần mềm nào, kể cả chính app này, đọc được ra ngoài**. |
| Xác thực sinh trắc | **BiometricPrompt / Face ID** | Mỗi lần chấm công, điện thoại bắt quét vân tay hoặc khuôn mặt. Không quét → không ký được → không chấm công được. |
| Quét mã ghép cặp | **mobile_scanner** | Dùng camera quét mã QR do quản trị viên cấp, thay vì gõ tay mã dài dễ sai. |

### 4.2. Máy chủ

| Thành phần | Tên kỹ thuật | Giải thích cho người non-tech |
|---|---|---|
| Ngôn ngữ | **Kotlin** (chạy trên nền Java 17) | Ngôn ngữ hiện đại, an toàn, được Google chọn làm ngôn ngữ chính cho Android. Chạy trên nền Java — nền tảng đã được kiểm chứng hàng chục năm trong ngân hàng, viễn thông, cơ quan nhà nước. |
| Bộ khung | **Spring Boot 4.1** | Bộ khung phần mềm máy chủ phổ biến nhất thế giới cho hệ thống doanh nghiệp. Chọn nó nghĩa là: dễ tìm người bảo trì về sau, tài liệu sẵn có, ít rủi ro "công nghệ lạ". |
| Kho dữ liệu | **MySQL 8** | Hệ quản trị cơ sở dữ liệu miễn phí, phổ biến bậc nhất. |
| Quản lý cấu trúc dữ liệu | **Flyway** | Ghi lại **mọi thay đổi cấu trúc dữ liệu thành các bước có đánh số** (hiện có 10 bước, V1→V10). Lợi ích: nâng cấp phần mềm không bao giờ làm hỏng dữ liệu cũ, và luôn tái lập được cấu trúc y hệt trên máy chủ mới. |
| Xuất báo cáo | **Apache POI** | Thư viện tạo file Excel (.xlsx) thật, mở được bằng Microsoft Excel. |
| Mã hoá / chữ ký số | **Chỉ dùng thư viện chuẩn của Java** | Cố ý **không dùng thư viện mã hoá của bên thứ ba** — giảm rủi ro "cửa hậu" và rủi ro thư viện ngoài ngừng được bảo trì. |

### 4.3. Trang quản trị

Là **một trang web duy nhất, tự chứa** (một file HTML), do chính máy chủ phát ra. Quản trị viên chỉ cần mở trình duyệt (Chrome/Edge) và gõ địa chỉ máy chủ — **không phải cài đặt gì thêm**.

### 4.4. Quy mô mã nguồn

- Máy chủ: **95 file mã nguồn** Kotlin, **107 bài kiểm thử tự động** trên 21 nhóm chức năng.
- Ứng dụng điện thoại: **16 file mã nguồn** chính (~2.900 dòng), **15 bài kiểm thử**, cộng phần mã "cầu nối" với chip bảo mật của Android và iOS.

---

## 5. XÂY DỰNG NHƯ THẾ NÀO? (kiến trúc & cách tổ chức)

### 5.1. Nguyên tắc tổ chức: chia theo nghiệp vụ, không chia theo kỹ thuật

Mã nguồn máy chủ được chia thành **7 khối theo đúng nghiệp vụ đời thực**, mỗi khối là một "phòng ban" độc lập:

| Khối | Phụ trách việc gì |
|---|---|
| `auth` | Đăng nhập của quản trị viên |
| `employee` | Hồ sơ cán bộ (mã cán bộ, họ tên, chức vụ, cấp bậc) |
| `device` | Ghép cặp và quản lý điện thoại của cán bộ |
| `challenge` | Sinh "câu đố" dùng một lần cho mỗi lần chấm công (giải thích ở Mục 6) |
| `attendance` | Ghi nhận vào ca / tan ca, tổng hợp giờ công, xuất Excel |
| `audit` | Nhật ký kiểm toán — ghi lại mọi hành động quan trọng |
| `common` | Phần dùng chung (mã hoá, xử lý lỗi, cấu hình) |

**Vì sao cách chia này quan trọng với đơn vị?** Sau này muốn thêm nghiệp vụ mới (ví dụ: quản lý nghỉ phép, đăng ký ca trực), người lập trình chỉ cần thêm một "phòng ban" mới mà **không phải động vào các phần đang chạy ổn định** — giảm rủi ro làm hỏng cái đang tốt.

### 5.2. Quy trình sử dụng thực tế

**Bước 1 — Quản trị viên tạo hồ sơ cán bộ** (trên trang web quản trị)
Nhập: mã cán bộ, họ tên, chức vụ, cấp bậc.

**Bước 2 — Quản trị viên cấp "mã ghép cặp"**
Hệ thống sinh một mã ngắn, hiển thị kèm **mã QR** (có thể in ra hoặc cho cán bộ quét thẳng trên màn hình). Đặc điểm:
- **Chỉ dùng được một lần.**
- **Tự hết hạn sau 10 phút.**
- Mã cố tình **không chứa các ký tự dễ nhầm** (số 0 và chữ O, số 1 và chữ I) để cán bộ đọc/gõ không bị sai.
- **QR chứa sẵn cả địa chỉ máy chủ lẫn mã ghép cặp** — nên cán bộ **không phải gõ địa chỉ IP**.

**Bước 3 — Cán bộ ghép cặp điện thoại** (một lần duy nhất)
Cán bộ mở app → **quét mã QR** → quét vân tay. Xong. App tự nhận địa chỉ máy chủ và mã từ QR; điện thoại tự tạo một cặp "chìa khoá – ổ khoá" trong chip bảo mật, gửi **ổ khoá** lên máy chủ, và **giữ chìa khoá lại trong két sắt phần cứng, không bao giờ gửi đi đâu**.
Nếu điện thoại không quét được QR (hỏng camera, không cấp quyền), app vẫn có nút **nhập địa chỉ máy chủ và mã bằng tay** làm phương án dự phòng.

**Bước 4 — Hằng ngày: cán bộ chấm công**
Mở app → bấm **Vào ca** (hoặc **Tan ca**) → quét vân tay → xong. Nếu là vào ca, cán bộ có thể nhập thêm **thông tin tiếp nhận ca trực**.

**Bước 5 — Quản trị viên theo dõi & xuất báo cáo**
Xem danh sách chấm công, lọc theo cán bộ và khoảng ngày, và **xuất file Excel theo ngày / tuần / tháng**.

### 5.3. Máy chủ tự dò địa chỉ — cán bộ không phải biết IP

Đây là điểm hay bị bỏ sót ở các hệ thống nội bộ: nhân viên bị bắt gõ một dãy số như `192.168.1.10:8080` — vừa dễ sai, vừa khó hỗ trợ khi máy chủ đổi địa chỉ.

Hệ thống này xử lý như sau:

- Máy chủ **tự dò tất cả địa chỉ IP của chính nó** và hiển thị trong tab **🖥 Máy chủ** trên trang quản trị, **kèm tên card mạng** (ví dụ: `192.168.1.10 — Wi-Fi`). Máy chủ tự **xếp card mạng thật lên đầu**, đẩy các card ảo (VMware, Hyper-V — loại mà điện thoại không bao giờ kết nối được) xuống dưới.
- Quản trị viên chọn địa chỉ đúng **một lần**; địa chỉ đó được nhúng vào **mọi mã QR**.
- Có **hai loại QR**:

| Loại QR | Chứa gì | Dùng khi nào |
|---|---|---|
| **QR ghép cặp** (khi cấp mã cho cán bộ) | Địa chỉ máy chủ **+ mã ghép cặp** | Cán bộ lần đầu ghép cặp điện thoại. Một lần quét là xong cả hai việc. |
| **QR kết nối** (luôn có ở tab 🖥 Máy chủ) | **Chỉ** địa chỉ máy chủ | Máy chủ đổi IP sau khi khởi động lại. Cán bộ mở app → **Đổi máy chủ** → quét. **Không cần cấp mã mới**, thiết bị giữ nguyên khoá bảo mật. |

- Nếu quản trị viên vô tình chọn một địa chỉ mà điện thoại không gọi được (ví dụ `localhost`), trang quản trị **cảnh báo đỏ ngay tại chỗ** thay vì để cán bộ quét phải một QR hỏng.

### 5.4. Ba cơ chế nghiệp vụ đáng chú ý

**a) Không cho phép "vào ca hai lần liên tiếp".**
Hệ thống kiểm tra tính hợp lệ: đã vào ca thì phải tan ca rồi mới vào ca tiếp được. Trên app, nút không hợp lệ **bị làm mờ** — cán bộ không bấm nhầm được.

**b) Tự động đóng ca lúc 23:59 mỗi ngày.**
Nếu cán bộ **quên bấm Tan ca**, đến 23:59 máy chủ tự động ghi một lượt tan ca cho những người còn "đang mở ca", và **ghi rõ trong nhật ký rằng đây là bản ghi do hệ thống tự sinh**, không phải do cán bộ bấm. Việc này tránh tình trạng dữ liệu treo lơ lửng làm sai lệch báo cáo cuối tháng.

**c) Nhật ký kiểm toán không thể xoá, không thể sửa.**
Mọi hành động quan trọng đều được ghi lại: đăng nhập quản trị, tạo/sửa cán bộ, cấp mã ghép cặp, ghép cặp điện thoại, thu hồi điện thoại, mỗi lần chấm công, **và cả những lần chữ ký bị phát hiện là giả**.
Điểm kỹ thuật quan trọng: nhật ký được ghi theo cơ chế **"ghi độc lập"** — nghĩa là ngay cả khi một thao tác bị hệ thống từ chối và huỷ bỏ, **dấu vết của lần thử đó vẫn còn nguyên trong nhật ký**. Kẻ gian không thể "thử rồi xoá dấu vết".

---

## 6. BẢO MẬT RA SAO?

Đây là phần được đầu tư kỹ nhất của dự án. Xin giải thích bằng một hình ảnh quen thuộc.

### 6.1. Ý tưởng cốt lõi: "con dấu điện tử không thể sao chép"

Hãy hình dung mỗi cán bộ có **một con dấu riêng**, và:

- **Con dấu (chìa khoá bí mật)** nằm trong **két sắt gắn liền trong điện thoại** — két này là một con chip phần cứng riêng biệt. Con dấu **được tạo ra bên trong két và không bao giờ ra khỏi két**. Ngay cả khi kẻ gian lấy được điện thoại và bẻ khoá phần mềm, **họ vẫn không lấy được con dấu ra để đem đi dùng chỗ khác**.
- **Máy chủ chỉ giữ "hình mẫu của con dấu" (ổ khoá công khai)** — đủ để **kiểm tra** một chữ ký là thật hay giả, nhưng **không đủ để tạo ra** chữ ký giả. Nghĩa là: **kể cả toàn bộ cơ sở dữ liệu máy chủ bị đánh cắp, kẻ trộm cũng không chấm công hộ được cho bất kỳ ai.** Đây là điểm khác biệt căn bản so với các hệ thống lưu mật khẩu.
- **Muốn đóng dấu, phải mở két bằng vân tay.** Két sắt được cấu hình **bắt buộc xác thực sinh trắc học cho từng lần ký**.

### 6.2. Cơ chế "câu đố dùng một lần" (chống phát lại)

Nếu chỉ ký một nội dung cố định, kẻ gian có thể **thu lại chữ ký hôm nay rồi phát lại vào ngày mai** để chấm công khống. Hệ thống chặn điều này như sau:

```
   Điện thoại                                    Máy chủ
       │  1. "Tôi muốn chấm công"                    │
       │ ──────────────────────────────────────────► │
       │                                              │  Sinh một CÂU ĐỐ ngẫu nhiên,
       │                                              │  chưa từng tồn tại trước đó,
       │  2. Trả về câu đố  ◄──────────────────────── │  và HẾT HẠN SAU 60 GIÂY
       │                                              │
       │  3. Quét vân tay → két sắt ký lên            │
       │     "câu đố + chữ VÀO CA"                    │
       │                                              │
       │  4. Gửi chữ ký lên  ──────────────────────►  │  • Kiểm tra chữ ký có thật không
       │                                              │  • Kiểm tra câu đố còn hạn không
       │                                              │  • ĐÁNH DẤU CÂU ĐỐ ĐÃ DÙNG
       │                                              │    (lần sau gửi lại → TỪ CHỐI)
       │  5. "Đã ghi nhận"   ◄─────────────────────── │
```

Ba đặc tính quan trọng:

1. **Câu đố hết hạn sau 60 giây** — chữ ký cũ vô dụng.
2. **Câu đố chỉ dùng được đúng một lần** — máy chủ dùng một kỹ thuật khoá dữ liệu đảm bảo rằng dù kẻ gian gửi 1.000 yêu cầu cùng lúc, **chỉ đúng một yêu cầu được ghi nhận**.
3. **Nội dung ký bao gồm cả loại sự kiện** ("VÀO CA" hay "TAN CA") — nên **không thể lấy chữ ký của lượt vào ca rồi sửa thành tan ca**.

Một chi tiết thiết kế tinh tế: máy chủ **kiểm tra chữ ký TRƯỚC, rồi mới đánh dấu câu đố đã dùng**. Nhờ vậy, nếu ai đó cố tình gửi chữ ký sai, **họ không "đốt" được câu đố hợp lệ của cán bộ thật** — tức là không thể phá hoại làm cán bộ khác không chấm công được.

### 6.3. Bảng đối chiếu: các kịch bản gian lận và cách hệ thống chặn

| Kịch bản | Hệ thống xử lý ra sao |
|---|---|
| Nhờ đồng nghiệp chấm công hộ | ❌ Chặn — cần chính điện thoại đó **và** vân tay của chính chủ. |
| Mượn điện thoại của đồng nghiệp | ❌ Chặn — mỗi lần bấm đều bắt quét vân tay/Face ID. |
| Cài app lên máy thứ hai để chấm công từ nhà | ❌ Chặn — **một cán bộ chỉ có một điện thoại được kích hoạt**; ghép máy mới thì máy cũ **tự động bị vô hiệu hoá**. |
| Nghe lén mạng, ghi lại tin nhắn chấm công rồi gửi lại | ❌ Chặn — câu đố dùng một lần, hết hạn sau 60 giây. |
| Bẻ khoá điện thoại để trích xuất "con dấu" | ❌ Chặn — chìa khoá nằm trong chip phần cứng, **không xuất ra được**. |
| Đánh cắp toàn bộ cơ sở dữ liệu máy chủ | ❌ Không chấm công hộ được — máy chủ **không giữ chìa khoá của bất kỳ ai**. |
| Giả mạo mã ghép cặp | ❌ Chặn — máy chủ **không lưu mã gốc**, chỉ lưu "dấu vân tay" của mã; mã hết hạn sau 10 phút và chỉ dùng một lần. |
| Sửa lại giờ chấm công đã ghi | ❌ Không có chức năng sửa; mọi thao tác đều để lại vết trong nhật ký kiểm toán. |
| Chấm công rồi xoá dấu vết | ❌ Chặn — nhật ký kiểm toán chỉ ghi thêm, không sửa/xoá. |
| Cán bộ nghỉ việc nhưng máy vẫn còn | ✅ Quản trị viên **thu hồi thiết bị** ngay trên trang quản trị. |
| Cán bộ mất điện thoại | ✅ Thu hồi máy cũ + cấp mã ghép cặp mới cho máy mới. Máy cũ **lập tức vô dụng**. |

### 6.4. Các lớp bảo vệ khác

- **Mật khẩu quản trị viên không lưu dạng chữ.** Lưu dưới dạng **đã băm bằng BCrypt** — nghĩa là kể cả xem trực tiếp trong cơ sở dữ liệu cũng không đọc ra được mật khẩu.
- **Không có tài khoản mặc định.** Hệ thống **cố tình không tạo sẵn** tài khoản kiểu `admin/admin`. Người triển khai **bắt buộc phải tự đặt mật khẩu quản trị** khi khởi động lần đầu, nếu không thì **không có tài khoản nào được tạo ra cả**.
- **Phiên đăng nhập quản trị tự hết hạn sau 8 giờ.**
- **Mọi thông tin nhạy cảm (mật khẩu cơ sở dữ liệu, khoá ký phiên đăng nhập) đều nằm ngoài mã nguồn**, khai báo lúc chạy — không bị lộ khi bàn giao mã nguồn.

---

## 7. NHỮNG GÌ HỆ THỐNG CHƯA BẢO VỆ (báo cáo trung thực)

Phần này quan trọng cho việc nghiệm thu. Hệ thống **rất mạnh ở việc chống làm giả và chống chấm công hộ**, nhưng có 3 điểm cần biết rõ:

### 7.1. Đường truyền chưa được mã hoá (dùng HTTP, không phải HTTPS)

**Nghĩa là gì:** dữ liệu đi giữa điện thoại và máy chủ ở dạng "văn bản trần". Một người **đã ở bên trong mạng nội bộ của đơn vị** và có công cụ chuyên dụng thì **có thể đọc trộm nội dung** đi qua mạng (ví dụ: biết ai vừa chấm công lúc mấy giờ).

**Nhưng lưu ý:** người đó **vẫn không thể giả mạo hay sửa dữ liệu chấm công**, vì mọi lượt chấm công đều có chữ ký số bảo vệ. Đây là rủi ro về **quyền riêng tư**, không phải rủi ro về **tính toàn vẹn** của dữ liệu công.

**Đây là lựa chọn thiết kế có chủ ý** (để tránh phải cài chứng thư số cho từng điện thoại trong mạng nội bộ), và **chấp nhận được nếu mạng nội bộ là mạng cô lập, không nối Internet**. Nếu đơn vị muốn nâng cấp lên HTTPS, đây là việc làm được, ước tính 1–2 ngày công.

### 7.2. Ba chức năng phụ chưa được bảo vệ bằng chữ ký số ⚠️

Ba chức năng sau hiện chỉ cần biết **mã số thiết bị** (là một con số đơn giản: 1, 2, 3...) là gọi được, **không cần chữ ký, không cần vân tay**:

| Chức năng | Hệ quả nếu bị lợi dụng |
|---|---|
| Xem lịch sử chấm công | Người trong mạng nội bộ **có thể xem lịch sử chấm công của cán bộ khác** bằng cách thử lần lượt các mã số. |
| Xem trạng thái hôm nay | Tương tự — lộ thông tin ai đang trong ca. |
| Ghi "thông tin tiếp nhận ca trực" | Nghiêm trọng hơn: người trong mạng nội bộ **có thể ghi chú giả mạo** vào lượt vào ca của cán bộ khác. Vì ghi chú là **ghi-một-lần-không-sửa**, việc này sẽ **chiếm mất chỗ ghi chú thật** của cán bộ đó. |

**Cần nhấn mạnh:** lỗ hổng này **không cho phép chấm công khống hay chấm công hộ** — chức năng chấm công (điều quan trọng nhất) vẫn được chữ ký số bảo vệ đầy đủ. Đây là các chức năng phụ được bổ sung sau và chưa được siết bằng cùng cơ chế.

**Khuyến nghị:** siết ba chức năng này bằng cùng cơ chế chữ ký số như chức năng chấm công. Ước tính **1–2 ngày công**. Nên làm trước khi đưa vào sử dụng thật.

### 7.3. Trang quản trị đang có sẵn công cụ "Giả lập thiết bị"

Trang quản trị hiện có một tab **"📱 Giả lập thiết bị"** — đây là **công cụ do lập trình viên tạo ra để kiểm thử**, cho phép giả lập một chiếc điện thoại ngay trên trình duyệt (vẫn phải có mã ghép cặp hợp lệ mới dùng được, nên không phải lỗ hổng trực tiếp).

**Khuyến nghị:** **gỡ bỏ tab này trước khi bàn giao vận hành thật** — vừa để tránh gây hiểu nhầm cho quản trị viên, vừa để loại bỏ hoàn toàn một con đường chấm công không qua vân tay. Ước tính **dưới 1 ngày công**.

---

## 8. NHỮNG GÌ ĐÃ LÀM XONG

**Ứng dụng điện thoại (Android + iPhone)**
- ✅ Kết nối máy chủ **bằng quét QR** — không phải gõ địa chỉ IP (vẫn có nút nhập tay dự phòng)
- ✅ **Đổi máy chủ bằng một lần quét** khi máy chủ đổi IP — không mất ghép cặp
- ✅ Ghép cặp bằng **quét mã QR** (hoặc gõ tay mã)
- ✅ Tạo và bảo vệ chìa khoá trong **chip bảo mật phần cứng**
- ✅ Vào ca / Tan ca có **xác thực vân tay – Face ID**
- ✅ Nút không hợp lệ tự động bị làm mờ (chống bấm nhầm)
- ✅ Ghi **thông tin tiếp nhận ca trực** (ghi một lần, không sửa)
- ✅ Xem **lịch sử chấm công 30 ngày** của bản thân
- ✅ Thông báo lỗi bằng **tiếng Việt**, dễ hiểu cho người dùng cuối
- ✅ Có biểu tượng app riêng, giao diện đã được thiết kế
- ✅ **15 bài kiểm thử tự động**

**Máy chủ**
- ✅ Quản lý hồ sơ cán bộ (mã, họ tên, **chức vụ, cấp bậc**), bật/tắt trạng thái hoạt động
- ✅ Cấp mã ghép cặp một lần (kèm QR), tự hết hạn
- ✅ **Tự dò địa chỉ IP của chính mình**, tự nhận biết và hạ ưu tiên card mạng ảo
- ✅ Ghép cặp / thu hồi thiết bị; **một cán bộ chỉ một máy hoạt động**
- ✅ Ghi nhận chấm công có chữ ký số, chống phát lại
- ✅ Kiểm tra logic vào ca / tan ca hợp lệ
- ✅ **Tự động đóng ca lúc 23:59** cho người quên tan ca
- ✅ Tổng hợp giờ công theo ngày
- ✅ **Xuất báo cáo Excel** theo ngày / tuần / tháng, cho một hoặc nhiều cán bộ
- ✅ Nhật ký kiểm toán không thể sửa/xoá
- ✅ **107 bài kiểm thử tự động**, riêng phần mã hoá bắt buộc đạt độ phủ ≥ 80%
- ✅ **Đã chạy thử thành công với MySQL thật** — cấu trúc dữ liệu tự khởi tạo đúng, tài khoản quản trị được tạo, API hoạt động

**Trang quản trị web**
- ✅ Đăng nhập, quản lý cán bộ, quản lý thiết bị
- ✅ Cấp mã ghép cặp và **hiển thị mã QR** cho cán bộ quét
- ✅ Tab **🖥 Máy chủ**: chọn địa chỉ máy chủ (tự dò, kèm tên card mạng) và **QR kết nối** cho cán bộ quét khi đổi IP
- ✅ Tra cứu chấm công, tra cứu nhật ký, xuất Excel
- ✅ Giao diện chạy được trên **màn hình nhỏ** (hộp thoại tự co, không che mất nút)

---

## 9. CẦN CHUẨN BỊ GÌ ĐỂ TRIỂN KHAI?

### 9.1. Hạ tầng (chi phí phần mềm: **0 đồng** — toàn bộ công nghệ đều miễn phí)

| Hạng mục | Yêu cầu |
|---|---|
| **Máy chủ** | Một máy tính chạy Windows hoặc Linux, luôn bật, nối cùng mạng nội bộ với điện thoại của cán bộ. Cấu hình phổ thông là đủ. |
| **Phần mềm nền** | Java 17 + MySQL 8 (đều **miễn phí**). |
| **Mạng** | Mạng nội bộ (LAN/Wi-Fi nội bộ) phủ đến nơi cán bộ chấm công. **Không cần Internet.** |
| **Điện thoại cán bộ** | Android (từ Android 6 trở lên) hoặc iPhone, **bắt buộc đã cài vân tay hoặc Face ID**. Máy không có sinh trắc học sẽ **không ghép cặp được** — đây là chủ đích thiết kế. |
| **Phân phối app** | Cần quyết định: cài trực tiếp qua file (nội bộ), hay đưa lên Google Play / App Store. Nếu lên App Store của Apple thì cần **tài khoản nhà phát triển Apple (99 USD/năm)**. |

### 9.2. Việc cần làm trước khi chạy thật (theo thứ tự ưu tiên)

| # | Việc | Vì sao | Ước tính |
|---|---|---|---|
| 1 | **Siết bảo mật 3 chức năng phụ** (Mục 7.2) | Tránh lộ lịch sử chấm công và ghi chú giả mạo | 1–2 ngày |
| 2 | **Gỡ tab "Giả lập thiết bị"** khỏi trang quản trị (Mục 7.3) | Loại bỏ đường chấm công không qua vân tay | < 1 ngày |
| 3 | **Chạy thử nghiệm với 3–5 cán bộ trong 1 tuần** | Kiểm chứng thực tế (điện thoại thật, vân tay thật) trước khi mở rộng toàn đơn vị | 1 tuần |
| 4 | *(Tuỳ chọn)* Nâng cấp lên HTTPS | Chống nghe lén trong mạng nội bộ | 1–2 ngày |
| ~~✔~~ | ~~Chạy thử với cơ sở dữ liệu MySQL~~ | **ĐÃ XONG** — máy chủ đã chạy thật với MySQL 8: cấu trúc dữ liệu tự khởi tạo đúng, tài khoản quản trị được tạo, API trả kết quả đúng | — |

---

## 10. NHỮNG CÂU HỎI THƯỜNG GẶP

**Hỏi: Cán bộ mất điện thoại thì sao?**
Quản trị viên vào trang web **thu hồi thiết bị** — chiếc máy đó lập tức không chấm công được nữa, kể cả người nhặt được có mở khoá được máy. Sau đó cấp mã ghép cặp mới cho máy mới.

**Hỏi: Cán bộ đổi điện thoại mới thì sao?**
Quản trị viên cấp mã ghép cặp mới, cán bộ quét QR trên máy mới. Máy cũ **tự động bị vô hiệu hoá** — không cần thao tác thêm.

**Hỏi: Nếu cán bộ đổi vân tay đã đăng ký trên điện thoại?**
Chip bảo mật sẽ **tự huỷ chìa khoá** (đây là cơ chế an toàn của Android/iOS, không phải lỗi). Cán bộ cần **ghép cặp lại** bằng mã mới.

**Hỏi: Máy chủ khởi động lại và đổi sang IP khác thì sao?**
Quản trị viên vào tab **🖥 Máy chủ** trên trang quản trị — máy chủ **tự dò các địa chỉ IP của chính nó** và hiện sẵn một **QR kết nối**. Cán bộ chỉ cần mở app → **Đổi máy chủ** → quét QR đó. **Không cần cấp lại mã ghép cặp**, thiết bị giữ nguyên khoá bảo mật đã có.

**Hỏi: Mất mạng thì có chấm công được không?**
**Không.** App bắt buộc kết nối được máy chủ tại thời điểm chấm công (chính cơ chế "câu đố dùng một lần" đòi hỏi điều này). Đây là **đánh đổi có chủ ý**: ưu tiên chống gian lận hơn là chấm công ngoại tuyến. Nếu đơn vị cần chấm công khi mất mạng, đây là một hạng mục phát triển thêm và cần thảo luận riêng về mặt bảo mật.

**Hỏi: Dữ liệu có bị gửi ra ngoài Internet không?**
**Không.** Toàn bộ hệ thống chạy trong mạng nội bộ. Không có dịch vụ đám mây, không có bên thứ ba.

**Hỏi: Sau này muốn thêm chức năng (nghỉ phép, phân ca, lương) có được không?**
Được. Kiến trúc được chia theo nghiệp vụ (Mục 5.1) chính là để phục vụ việc mở rộng — thêm nghiệp vụ mới không phải đụng vào phần đang chạy.

---

## 11. ĐÁNH GIÁ TỔNG QUAN

| Tiêu chí | Đánh giá |
|---|---|
| **Chống gian lận chấm công** | ⭐⭐⭐⭐⭐ Rất mạnh — cao hơn hẳn mặt bằng chung của phần mềm chấm công phổ thông. Dùng đúng các kỹ thuật mà ngân hàng dùng để bảo vệ giao dịch. |
| **Lựa chọn công nghệ** | ⭐⭐⭐⭐⭐ Phổ biến, miễn phí, dễ tìm người bảo trì. Không dùng công nghệ lạ, không phụ thuộc nhà cung cấp nào. |
| **Chất lượng mã nguồn** | ⭐⭐⭐⭐ 107 bài kiểm thử tự động (máy chủ) + 15 (app), tổ chức mã rõ ràng, có ràng buộc chất lượng cho phần mã hoá. |
| **Tính năng nghiệp vụ** | ⭐⭐⭐⭐ Đầy đủ cho bài toán chấm công trực ca: chấm công, bàn giao ca, báo cáo Excel, nhật ký. |
| **Dễ dùng cho người không rành kỹ thuật** | ⭐⭐⭐⭐⭐ Cán bộ **không phải gõ địa chỉ IP hay mã dài** — chỉ quét QR. Quản trị viên **không cần biết IP máy chủ** — hệ thống tự dò. |
| **Sẵn sàng vận hành thật** | ⭐⭐⭐⭐ Máy chủ **đã chạy thật với MySQL thành công**. Còn 2 việc kỹ thuật ở Mục 9.2 (ước tính **2–3 ngày công**) rồi có thể chạy thử nghiệm với cán bộ thật. |

**Kết luận:** Nền tảng kỹ thuật của hệ thống **vững và đúng hướng**; điểm mạnh nhất — chống chấm công hộ — đã được xây dựng nghiêm túc, đúng chuẩn. Hệ thống đã **chứng minh chạy được thật** (máy chủ + cơ sở dữ liệu). Các tồn tại còn lại đều **rõ ràng, khoanh vùng được, và không nằm ở phần lõi**; xử lý dứt điểm trong **2–3 ngày công** là có thể đưa vào chạy thử nghiệm với cán bộ thật.

---

## PHỤ LỤC: GIẢI THÍCH THUẬT NGỮ

| Thuật ngữ | Nghĩa đời thường |
|---|---|
| **Chữ ký số (ECDSA)** | "Con dấu điện tử". Ai cũng kiểm tra được là dấu thật, nhưng chỉ chủ nhân mới đóng được. |
| **Khoá bí mật / Khoá công khai** | Cặp "chìa khoá – ổ khoá". Chìa nằm trong điện thoại (bí mật), ổ khoá gửi lên máy chủ (công khai). |
| **Chip bảo mật (Keystore / Secure Enclave)** | "Két sắt" gắn liền trong điện thoại — thứ nằm trong đó không lấy ra ngoài được. |
| **Challenge (câu đố)** | Một dãy số ngẫu nhiên máy chủ đưa ra, dùng một lần, hết hạn sau 60 giây. |
| **Băm / Hash (BCrypt, SHA-256)** | "Nghiền" dữ liệu thành dấu vết không thể lần ngược — dùng để lưu mật khẩu và mã ghép cặp an toàn. |
| **Nhật ký kiểm toán (Audit log)** | Sổ ghi chép mọi hành động, chỉ ghi thêm, không sửa/xoá được. |
| **LAN / Mạng nội bộ** | Mạng riêng trong cơ quan, không nối ra Internet. |
| **API** | "Cửa giao dịch" mà điện thoại dùng để nói chuyện với máy chủ. |
