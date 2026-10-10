# Cẩm Nang Toàn Diện Phát Hành MoneyLens Lên Google Play Store

Tài liệu này cung cấp toàn bộ hướng dẫn, thông số kỹ thuật, kịch bản khai báo và câu trả lời mẫu cho Google Play Console để bạn đưa ứng dụng **MoneyLens** lên Google Play thành công ngay từ lần đầu tiên.

---

## 1. Thông Tin Ứng Dụng (Store Listing Details)

Tuân thủ chính sách tiêu đề của Google Play (không chứa từ quảng bá như "Free", "Best", không viết HOA toàn bộ, không có emoji, tối đa 30 ký tự):

- **Tên ứng dụng (App Name)**: `MoneyLens - Quản Lý Chi Tiêu` *(29 ký tự - Đạt chuẩn)*
- **Mô tả ngắn (Short Description - tối đa 80 ký tự)**:
  `Quản lý chi tiêu, theo dõi biến động ngân hàng và tài sản cá nhân bảo mật on-device.`
- **Mô tả chi tiết (Full Description)**:
  ```text
  MoneyLens là ứng dụng quản lý tài chính cá nhân hiện đại, bảo mật và thông minh dành cho bạn:

  🔒 BẢO MẬT LOCAL-FIRST TUYỆT ĐỐI:
  • Dữ liệu tài chính, sổ thu chi và danh mục đầu tư được lưu trữ trực tiếp trên thiết bị của bạn.
  • Không lưu trữ mã OTP, không truyền dữ liệu giao dịch ra máy chủ bên ngoài.
  • Hỗ trợ khóa bảo vệ bằng mã PIN 6 số và cảm biến sinh trắc học (vân tay / khuôn mặt).

  ⚡ TỰ ĐỘNG HÓA GHI CHÉP GIAO DỊCH:
  • Tự động nhận diện biến động số dư từ hơn 30 ngân hàng và ví điện tử hàng đầu tại Việt Nam (Vietcombank, Techcombank, MBBank, BIDV, VPBank, MoMo, ZaloPay,...).
  • Thông báo xác nhận nhanh (Quick Confirm) giúp bạn phân loại danh mục chỉ với một chạm.
  • Quét hóa đơn và mã QR chuyển khoản nhanh chóng.

  📊 BÁO CÁO & PHÂN TÍCH TÀI CHÍNH TOÀN DIỆN:
  • Biểu đồ phân bổ chi tiêu, xu hướng tài sản ròng (Net Worth).
  • Quản lý hạn mức thẻ tín dụng, chu kỳ sao kê và ngày đến hạn.
  • Theo dõi danh mục đầu tư (Cổ phiếu, Quỹ, Vàng, Bất động sản) và các khoản vay mượn linh hoạt.
  ```

---

## 2. Hướng Dẫn Ký Số & Đóng Gói Bản Phát Hành (AAB)

Google Play yêu cầu định dạng phát hành là **Android App Bundle (.aab)** thay vì APK thông thường.

### Bước 1: Tạo tệp khóa ký (Keystore) cá nhân
Chạy lệnh sau trên máy tính của bạn (Terminal / Command Prompt):
```bash
keytool -genkey -v -keystore moneylens-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias moneylens
```
*Lưu ý: Lưu trữ cẩn thận file `moneylens-release-key.jks`, mật khẩu và alias để dùng cho mọi lần cập nhật app sau này.*

### Bước 2: Đóng gói App Bundle (.aab)
Chạy lệnh Gradle trong thư mục dự án:
```bash
gradle :app:bundleRelease
```
File phát hành sẽ được tạo tại đường dẫn:
`app/build/outputs/bundle/release/app-release.aab`
Bạn dùng file `.aab` này để tải lên Google Play Console trong mục **App Bundles**.

---

## 3. Khai Báo An Toàn Dữ Liệu (Data Safety Questionnaire)

Khi hoàn tất biểu mẫu **Data Safety** trên Play Console, hãy chọn các phương án sau:

1. **Does your app collect or share any of the required user data types?**
   - Chọn: **Yes** (vì ứng dụng thu thập dữ liệu do người dùng nhập để lưu trong Room DB).
2. **Is all of the user data collected by your app encrypted in transit?**
   - Chọn: **Yes** (các kết nối tra cứu giá thị trường sử dụng HTTPS mã hóa).
3. **Do you provide a way for users to request that their data be deleted?**
   - Chọn: **Yes** (ứng dụng có tính năng *Cài đặt > Xóa sạch toàn bộ dữ liệu (Data Deletion)*).
4. **Chi tiết các loại dữ liệu**:
   - **Financial Info (Thông tin tài chính)**:
     - Chọn loại: *Other financial info* (Giao dịch thu chi, số dư).
     - Thu thập (Collected): **Yes**.
     - Chia sẻ (Shared): **No**.
     - Mục đích (Purpose): **App functionality** (Chức năng ứng dụng).
     - Có liên kết với danh tính người dùng không? **No** (Ephemeral / On-device).
   - **Messages (Tin nhắn)**:
     - Nếu bản phát hành giữ SMS: Chọn *Other in-app messages* -> Mục đích: *App functionality* -> Chỉ xử lý nội bộ, không chia sẻ.
     - Nếu chỉ dùng Notification Listener: Không cần tích chọn mục Messages.

---

## 4. Chính Sách Quyền Hạn Nhạy Cảm (Permissions Declaration)

### A. Quyền đọc thông báo (`NotificationListenerService`)
- **Mục đích**: Tự động bóc tách biến động số dư ngân hàng.
- **Tuân thủ Play Policy**:
  - MoneyLens đã tích hợp hộp thoại **Prominent Disclosure** giải thích minh bạch trước khi người dùng kích hoạt quyền.
  - Xử lý 100% On-device, không đọc tin nhắn cá nhân hay OTP.
- **Khai báo trên Play Console**: Chọn mục đích *Financial transaction recording / Personal Financial Management (PFM)*.

### B. Quyền SMS (`RECEIVE_SMS`, `READ_SMS`)
- **Khuyến nghị**: Nếu gửi xét duyệt bị Google Play yêu cầu giải trình phức tạp, bạn có thể chọn phát hành phiên bản chỉ sử dụng Notification Listener (Google Play duyệt ngay trong 24-48h).
- **Nếu giữ quyền SMS**:
  - Cần điền *Permissions Declaration Form*.
  - Quay video màn hình ngắn 30 giây chứng minh tính năng: Nhận SMS biến động ngân hàng &rarr; App hiển thị gợi ý thêm giao dịch.

---

## 5. Tài Sản Đồ Họa Cần Chuẩn Bị (Store Assets)

Chuẩn bị các file đồ họa trước khi gửi xét duyệt:

1. **Biểu tượng ứng dụng (App Icon)**:
   - Kích thước: `512 x 512 px`
   - Định dạng: `PNG (32-bit)`, dung lượng dưới 1024 KB.
2. **Ảnh bìa đồ họa (Feature Graphic)**:
   - Kích thước: `1024 x 500 px`
   - Định dạng: `PNG hoặc JPEG`.
3. **Ảnh chụp màn hình điện thoại (Phone Screenshots)**:
   - Tối thiểu: 2 ảnh, khuyến nghị từ 4 - 6 ảnh chụp các màn hình chính (Tổng quan chi tiêu, Sổ giao dịch, Quản lý thẻ ngân hàng, Báo cáo Net Worth).
   - Tỷ lệ: `16:9` hoặc `9:16`.

---

## 6. Đường Dẫn Chính Sách Bảo Mật (Privacy Policy URL)

Google Play Console bắt buộc phải có một đường dẫn URL công khai cho Chính sách bảo mật:
- File mẫu HTML đã được tạo sẵn tại: **`docs/privacy_policy.html`**.
- Bạn có thể tải file này lên:
  1. **GitHub Pages**: Đưa vào repo và bật GitHub Pages (hoàn toàn miễn phí).
  2. **Google Sites**: Tạo trang đơn giản và dán nội dung vào.
  3. **Firebase Hosting**: Triển khai file HTML tĩnh trong 1 phút.
- Sao chép đường dẫn URL công khai (ví dụ: `https://yourdomain.com/privacy-policy`) và dán vào ô **Privacy Policy** trong Google Play Console.
