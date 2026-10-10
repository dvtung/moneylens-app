# Kế hoạch thực hiện: Group khoản đầu tư theo Tab & Sắp xếp giảm dần theo thời gian

## 1. Mục tiêu
- Sắp xếp các khoản đầu tư theo thời gian cập nhật/giao dịch gần nhất (`lastUpdated`) theo thứ tự **giảm dần** (khoản mới nhất nằm ở trên cùng).
- Phân nhóm (Group) danh mục đầu tư theo **các Tab loại tài sản riêng biệt** (Tất cả, Cổ phiếu, Bất động sản, Vàng, Crypto, Quỹ mở CCQ).

## 2. Chi tiết thay đổi

### `InvestmentScreen.kt`
- Cập nhật danh sách `filteredAssets`:
  - Thực hiện sắp xếp `.sortedByDescending { it.lastUpdated }` trên danh sách tài sản.
  - Nhóm danh mục qua thanh lựa chọn Tab loại tài sản riêng biệt (`ScrollableTabRow` hoặc các `FilterChip` định dạng Tab rõ ràng).
- Hiển thị badge số lượng tài sản trên từng Tab loại tài sản.

### `MainViewModel.kt` & `MoneyLensRepository.kt`
- Đảm bảo danh sách `assets` trong `MainUiState` được duy trì sắp xếp giảm dần theo `lastUpdated` khi thêm, sửa hoặc cập nhật giá trị tài sản.

## 3. Kiểm tra & Hoàn thiện
- Chạy `compile_applet` kiểm tra cú pháp và build giao diện.
- Kiểm tra tính đúng đắn khi thêm mới/chỉnh sửa khoản đầu tư: tài sản vừa tương tác sẽ tự động đẩy lên đầu danh sách.
