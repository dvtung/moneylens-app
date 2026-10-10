# Kế Hoạch Tái Cấu Trúc & Refactor Toàn Diện 5 Giai Đoạn (MoneyLens)

Kế hoạch này chi tiết hóa lộ trình tái cấu trúc toàn diện mã nguồn dự án **MoneyLens**, giải quyết triệt để các vấn đề về **Bảo mật PIN**, **Tách nhỏ God Classes** (`Modals.kt` 4,897 dòng & `SettingsScreen.kt` 2,082 dòng), **Tối ưu hiệu năng Coroutines/State**, **Dọn dẹp Warning & Icon cũ**, và **Bổ sung Unit Tests tự động**.

---

## Các Quyết Định Đã Xác Nhận Từ Người Dùng

> [!IMPORTANT]
> - **Lộ trình thực hiện**: Triển khai toàn bộ theo kế hoạch **5 Giai đoạn**.
> - **Modun hóa Modals.kt**: Tách tệp `Modals.kt` (4,897 dòng) thành thư mục `ui/screens/modals/` chứa các tệp chuyên biệt:
>   - `TransactionModals.kt` (Thêm/Sửa/Lọc giao dịch)
>   - `LoanModals.kt` (Quản lý khoản vay, nợ, trả góp)
>   - `CardModals.kt` (Thêm/Chỉnh sửa thẻ tín dụng, ví điện tử)
>   - `InvestmentModals.kt` (Thêm/Sửa/Lịch sử tài sản đầu tư)
>   - `BudgetModals.kt` (Cài đặt ngân sách)
> - **Unit Testing**: Bổ sung bộ Unit Tests tự động cho `BankParserEngine`, `CostBasisCalculator`, `CreditCardCycleCalculator` và `DataBackupManager`.

---

## 1. Tổng Quan Kế Hoạch Tái Cấu Trúc 5 Giai Đoạn

### 🔒 Giai đoạn 1: Bảo mật Mã PIN & An toàn Cấu trúc Dữ liệu
- **Bảo mật mã PIN (SHA-256 + Salt)**:
  - Tạo utility `SecurityUtils.kt` thực hiện hash mã PIN bằng `SHA-256` kết hợp Salt thiết bị.
  - Cập nhật `MainViewModel.kt` lưu/xác thực PIN qua hash SHA-256 thay vì chuỗi thô.
  - Tự động nâng cấp mã PIN thô hiện tại sang dạng hash khi người dùng mở ứng dụng lần đầu.
- **An toàn Room Database**:
  - Gỡ bỏ `.fallbackToDestructiveMigration()` trong `MoneyLensDatabase.kt` để ngăn cơ chế tự xóa sạch dữ liệu người dùng khi nâng cấp Database.

### 🧩 Giai đoạn 2: Modun hóa & Tách nhỏ God Classes (`Modals.kt` & `SettingsScreen.kt`)
- **Tách `Modals.kt` (4,897 dòng)** sang gói `com.dvt.moneylens.ui.screens.modals.*`:
  1. `TransactionModals.kt`: Modal thêm/sửa giao dịch thu/chi/chuyển khoản, bộ lọc nâng cao.
  2. `LoanModals.kt`: Modal thêm khoản vay/cho vay, ghi nhận trả nợ, đổi trả góp.
  3. `CardModals.kt`: Modal thêm/sửa thẻ tín dụng, chọn ngày sao kê, hạn thanh toán.
  4. `InvestmentModals.kt`: Sheet mua/bán tài sản, cập nhật định giá, cài đặt thuế/phí.
  5. `BudgetModals.kt`: Sheet điều chỉnh hạn mức ngân sách tháng và cảnh báo.
- **Tách `SettingsScreen.kt` (2,082 dòng)** sang gói `com.dvt.moneylens.ui.screens.settings.*`:
  1. `SecuritySettingsSection.kt`: Đổi PIN, bật/tắt sinh trắc học, chọn thời gian tự động khóa.
  2. `BackupSettingsSection.kt`: Xuất sao lưu JSON, khôi phục dữ liệu, xem lịch sử backup.
  3. `ThemeSettingsSection.kt`: Chọn chế độ Sáng/Tối/Hệ thống, chọn bộ màu Accent.
  4. `BankCaptureSettingsSection.kt`: Quản lý danh sách ngân hàng bật/tắt đọc thông báo.

### ⚡ Giai đoạn 3: Tối ưu Hiệu năng & Bất Đồng Bộ
- **Song song hóa Network Request (`MarketPriceService.kt`)**:
  - Cải tiến `fetchUpdatedPrices()` sử dụng `coroutineScope { assets.map { async { ... } }.awaitAll() }` lấy giá thị trường các tài sản song song thay vì tuần tự.
- **Tối ưu UI State Calculation (`MainViewModel.kt`)**:
  - Caching các getter tính toán tổng tiền ròng (`totalCashBalanceMinor`, `totalPortfolioValueMinor`) để tránh tính toán lại liên tục trên UI Thread.

### 🧹 Giai đoạn 4: Dọn Dẹp Code Rác, Cập Nhật Icon & Warnings
- Dọn dẹp các biến, hàm và class thừa không còn sử dụng.
- Cập nhật các icon Material 3 sang phiên bản `AutoMirrored` hiện đại (`Icons.AutoMirrored.Rounded.*`).

### 🧪 Giai đoạn 5: Bổ Sung Bộ Unit Tests Tự Động
- Viết Unit Tests kiểm tra tính đúng đắn trên JVM với JUnit & Robolectric:
  - `BankParserEngineTest.kt`: Kiểm tra bóc tách cú pháp SMS/Notification ngân hàng VCB, TCB, MB, VPB.
  - `CostBasisCalculatorTest.kt`: Kiểm tra công thức FIFO/Giá vốn trung bình khi Mua/Bán tài sản.
  - `DataBackupManagerTest.kt`: Kiểm tra tính toàn vẹn khi Serialization/Deserialization file sao lưu JSON v6.

---

## 2. Sơ Đồ Cấu Trúc Module Mới

```
app/src/main/java/com/dvt/moneylens/
├── data/
│   ├── backup/DataBackupManager.kt
│   ├── local/ (Entities, DAOs, MoneyLensDatabase)
│   ├── network/MarketPriceService.kt
│   └── repository/MoneyLensRepository.kt
├── domain/
│   ├── calc/ (BankParserEngine, CostBasisCalculator)
│   └── model/ (DomainModels, FinancialEnums, Money)
├── util/
│   └── SecurityUtils.kt (Hash SHA-256 + Salt)
└── ui/
    ├── MainViewModel.kt
    ├── designsystem/ (CommonComponents, MoneyText, ...)
    └── screens/
        ├── modals/
        │   ├── TransactionModals.kt
        │   ├── LoanModals.kt
        │   ├── CardModals.kt
        │   ├── InvestmentModals.kt
        │   └── BudgetModals.kt
        └── settings/
            ├── SecuritySettingsSection.kt
            ├── BackupSettingsSection.kt
            ├── ThemeSettingsSection.kt
            └── BankCaptureSettingsSection.kt
```

---

## 3. Luồng Thực Thi Chi Tiết

1. **Bước 1**: Tạo `SecurityUtils.kt` mã hóa PIN SHA-256, gỡ fallback destructive migration DB, chạy `compile_applet`.
2. **Bước 2**: Tạo các file trong `ui/screens/modals/`, chuyển các Composable Modal từ `Modals.kt` sang.
3. **Bước 3**: Tạo các file trong `ui/screens/settings/`, trích xuất các Section từ `SettingsScreen.kt`.
4. **Bước 4**: Tối ưu `MarketPriceService.kt` với `async/awaitAll` song song.
5. **Bước 5**: Tạo bộ Unit Test trong `app/src/test/java/com/dvt/moneylens/` và chạy `gradle :app:testDebugUnitTest`.
6. **Bước 6**: Biên dịch dự án bằng `compile_applet` xác nhận hoàn thành toàn bộ.
