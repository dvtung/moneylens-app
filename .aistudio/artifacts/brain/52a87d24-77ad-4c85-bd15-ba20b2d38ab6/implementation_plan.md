# Kế Hoạch Khắc Phục Lỗi Logic & Tối Ưu Hóa Dữ Liệu Dự Án MoneyLens

Kế hoạch này chi tiết hóa phương án xử lý 9 điểm bất hợp lý trong dự án MoneyLens theo kết quả phân tích mã nguồn chi tiết, bao gồm sao lưu dữ liệu đầu tư, tính toán nợ trả góp, xử lý ID tài khoản mặc định, cảnh báo ngân sách, hiển thị biểu đồ thu chi, cập nhật giá thị trường và định dạng tiền tệ.

---

## Các Quyết Định Thay Đổi Chính

> [!IMPORTANT]
> - **Nâng cấp Schema Version sao lưu**: `DataBackupDto` sẽ nâng `schemaVersion` từ **5 lên 6** để bổ sung bảng `investmentTransactions`. Tự động tương thích ngược với file backup v5 cũ.
> - **Khôi phục ID tài khoản mặc định**: Thay thế các giá trị gán cứng `"acc_vcb"` trong receiver/service bằng gọi động `ensureDefaultCashAccount()` từ `MoneyLensRepository`.

---

## Chi Tiết Các Điểm Khắc Phục (9 Điểm)

### 1. Data & Backup Layer (`DataBackupManager.kt` & `MoneyLensRepository.kt`)
- **Vấn đề**: File backup JSON v5 bị bỏ sót lịch sử giao dịch đầu tư (`investmentTransactions`).
- **Khắc phục**:
  - Bổ sung `investmentTransactions: List<InvestmentTransactionEntity>` vào `DataBackupDto` và `BackupSummary`.
  - Tăng `CURRENT_SCHEMA_VERSION = 6`.
  - Cập nhật `DataBackupManager.toJson()`, `fromJson()` và `extractSummary()` để ghi/đọc mảng `investmentTransactions`.
  - Cập nhật `MoneyLensRepository.exportAllDataForBackup()` và `restoreAllData()` xử lý đồng bộ mảng này.

### 2. Chuỗi Tháng Ngân Sách Mặc Định (`MoneyLensRepository.kt`)
- **Vấn đề**: Tham số `periodMonth` trong `updateBudget()` bị hardcode chuỗi cũ `"2026-10"`.
- **Khắc phục**: Thay giá trị mặc định bằng chuỗi tháng hiện tại động: `SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())`.

### 3. Tính Khoản Thanh Toán Trả Góp Hàng Tháng (`DomainModels.kt`)
- **Vấn đề**: Trong `Loan.regularMonthlyPaymentMinor`, nếu `monthlyPaymentMinor` đã bao gồm phí chuyển đổi trả góp hàng tháng, việc cộng thêm `conversionFeeMinor / termMonths` sẽ bị tính trùng phí (double counting).
- **Khắc phục**: Điều chỉnh logic trả về `monthlyPaymentMinor` trực tiếp khi khoản thanh toán hàng tháng đã bao hàm phí.

### 4. Định Dạng Số Âm Tiền Tệ (`Money.kt`)
- **Vấn đề**: `formatVnd()` định dạng số âm dưới dạng `-1.500.000 ₫`. Nếu các thành phần UI thêm thủ công dấu `-` hoặc `−` phía trước sẽ tạo ra dấu âm kép (VD: `--1.500.000 ₫`).
- **Khắc phục**: Cập nhật `formatVnd()` để format giá trị tuyệt đối `abs(minor)` khi hiển thị có tiền tố định dấu hoặc xử lý không lặp lại dấu âm.

### 5. ID Tài Khoản Mặc Định Trong Service/Receiver (`BankSmsReceiver.kt` & `BankNotificationListenerService.kt`)
- **Vấn đề**: Sử dụng fallback gán cứng `"acc_vcb"` khi không tìm thấy tài khoản tương ứng từ tin nhắn/thông báo.
- **Khắc phục**: Thay thế bằng `repository.ensureDefaultCashAccount()` để đảm bảo lấy đúng tài khoản tiền mặt/ngân hàng đang hoạt động trong DB.

### 6. Cập Nhật Giá Thị Trường Đầu Tư (`MarketPriceService.kt`)
- **Vấn đề**: Khi API lấy giá cổ phiếu/vàng không trả về kết quả hoặc bị lỗi, hệ thống tự động ghi đè giá nhập tay của người dùng về 0 hoặc giá mặc định.
- **Khắc phục**: Bổ sung điều kiện kiểm tra dữ liệu API hợp lệ trước khi cập nhật; giữ nguyên giá định giá gần nhất của người dùng nếu API không có dữ liệu.

### 7. Tính Toán Cảnh Báo Ngân Sách (`MainViewModel.kt`)
- **Vấn đề**: Khi thêm giao dịch nhanh (`addQuickTransaction()`), danh mục Đầu tư (`cat_investment`) và Trả nợ thẻ tín dụng (`cat_credit_pay`) vẫn bị tính dồn vào ngân sách chi tiêu sinh hoạt, phát cảnh báo sai.
- **Khắc phục**: Bỏ qua danh mục `cat_investment` và `cat_credit_pay` khi cộng dồn tổng chi tiêu kiểm tra hạn mức ngân sách.

### 8. Biểu Đồ Thu Chi Theo Tháng (`MainViewModel.kt`)
- **Vấn đề**: Trong `monthlyBarData`, việc tính toán `maxMonth` cho các năm quá khứ hoặc tương lai so với năm hiện tại (`selectedChartYear`) bị giới hạn sai theo tháng hiện tại của năm nay.
- **Khắc phục**: Nếu `selectedChartYear < currentYear` thì `maxMonth = 12`; nếu `selectedChartYear > currentYear` thì `maxMonth = 0`; nếu bằng năm hiện tại thì `maxMonth = currentMonth`.

### 9. Giao Diện & Tìm Kiếm (`MoneyText.kt` & `TransactionsScreen.kt`)
- **Vấn đề**: Xử lý tiền tố dấu âm/dương trong `MoneyText` và thiếu nút xóa bộ lọc nhanh khi danh sách giao dịch lọc không có kết quả.
- **Khắc phục**: Tối ưu tiền tố hiển thị trong `MoneyText` và bổ sung nút "Xóa bộ lọc" trực tiếp trên Empty State của `TransactionsScreen`.

---

## Luồng Kiểm Tra & Biển Dịch

1. Chạy biên dịch `compile_applet` kiểm tra toàn bộ mã nguồn Kotlin.
2. Kiểm tra thao tác Backup/Restore file JSON.
3. Kiểm tra tính chính xác của màn hình Đầu tư, Trả góp, Ngân sách và Thẻ tín dụng.
