# 💎 MoneyLens - Ứng Dụng Quản Lý Tài Chính Cá Nhân & Danh Mục Đầu Tư Toàn Diện

> **MoneyLens** là ứng dụng tài chính cá nhân thế hệ mới dành cho Android, được xây dựng hoàn toàn bằng **Kotlin** và **Jetpack Compose (Material Design 3)**. Ứng dụng mang đến giải pháp toàn diện giúp người dùng kiểm soát dòng tiền, quản lý ngân sách, theo dõi danh mục đầu tư đa tài sản (BĐS, Cổ phiếu, Quỹ, Vàng, Crypto), quản lý thẻ tín dụng & các khoản nợ vay, bảo mật đa tầng và tối ưu hóa tài sản ròng (**Net Worth**).

---

## 🌟 Tính Năng Nổi Bật

### 1. 📊 Tổng Quan & Phân Tích Tài Sản Ròng (Overview & Net Worth)
- **Công thức Tài sản Ròng chuẩn xác**:
  $$\text{Tài sản Ròng} = (\text{Tiền mặt \& Ngân hàng} + \text{Đầu tư} + \text{Cho vay phải thu}) - (\text{Nợ thẻ tín dụng} + \text{Nợ vay \& Trả góp})$$
- **Biểu đồ đường cong xu hướng Tài sản ròng (Historical Net Worth Trend Chart)**:
  - Vẽ bằng đường cong mượt mà (**Smooth Cubic Bezier Curve**) kết hợp dải màu gradient sang trọng.
  - Tương tác chạm/kéo (**Interactive Scrubber**) để soi chi tiết giá trị tài sản ròng và tỷ lệ tăng trưởng tại bất kỳ thời điểm nào.
  - Bộ lọc khoảng thời gian: **3 Tháng (3M)**, **6 Tháng (6M)**, **1 Năm (1Y)**, **Tất cả (All)**.
- **Biểu đồ Thu - Chi động theo Năm**:
  - Dữ liệu được tính toán **100% động từ cơ sở dữ liệu Room** (xóa sạch hoàn toàn khi xóa dữ liệu demo).
  - Tích hợp **Bộ chọn Năm** (`2026`, `2025`...) và hiển thị đầy đủ các tháng từ đầu năm đến hiện tại.
- **Khay duyệt nhanh giao dịch (Review Tray)**: Tự động gom các biến động số dư chờ duyệt với vòng lấy nét hổ phách (**Amber Focus Ring**).

---

### 2. 📜 Sổ Giao Dịch Đa Kênh (Transactions Ledger)
- Ghi nhận thu chi, chuyển khoản nội bộ, thanh toán thẻ và trả nợ.
- Bộ lọc mạnh mẽ theo: **Loại giao dịch** (Thu/Chi/Chuyển), **Thành viên gia đình**, **Trạng thái chờ duyệt**.
- Tìm kiếm tức thì theo nội dung, tên đối tác hoặc số tiền.
- Hỗ trợ dịch vụ tự động bắt biến động số dư qua SMS và Thông báo từ 16+ ngân hàng Việt Nam (VCB, Techcombank, MB, VPBank, ACB, BIDV, MoMo, ZaloPay...).

---

### 3. 🎯 Quản Lý Ngân Sách Thông Minh (Smart Budget)
- Thiết lập hạn mức ngân sách tổng tháng và hạn mức chi tiết cho từng danh mục.
- Thanh tiến độ chi tiêu trực quan kèm cảnh báo sớm:
  - ⚠️ **Ngưỡng 80% (Warning Amber)**: Nhắc nhở người dùng điều tiết chi tiêu.
  - 🚨 **Ngưỡng 100% (Critical Red)**: Cảnh báo vượt ngân sách kèm thông báo Notification đẩy.

---

### 4. 📈 Danh Mục Đầu Tư Đa Tài Sản (Investment Portfolio)
- Hỗ trợ 5 nhóm tài sản chính:
  1. 🏢 **Bất động sản (BĐS)**: Căn hộ, nhà phố, đất nền kèm định giá thị trường.
  2. 📈 **Cổ phiếu**: VN-Index, HPG, FPT, VNM...
  3. 💼 **Chứng chỉ quỹ**: Quỹ mở DCDS, DCVFM, VFMVF1...
  4. 🥇 **Vàng miếng & Vàng nhẫn**: SJC 9999, DOJI, PNJ.
  5. 🌐 **Tiền mã hóa (Crypto)**: BTC, ETH, SOL, USDT.
- **Biểu đồ Donut phân bổ danh mục đầu tư**: Hiển thị tỷ trọng % trực quan và chi tiết giá trị từng loại tài sản.
- Cập nhật giá thị trường trực tuyến và tự động tính toán Lãi/Lỗ (**P&L**) chưa thực hiện.

---

### 5. 💳 Quản Lý Tín Dụng & Khoản Nợ (Credit & Debts)
- **Thẻ tín dụng**:
  - Quản lý hạn mức, chu kỳ sao kê, ngày miễn lãi (45 - 55 ngày), ngày đến hạn thanh toán.
  - Cảnh báo nợ thẻ sắp đến hạn hoặc quá hạn; tính năng thanh toán thẻ trích tiền tự động.
- **Đi vay & Mua hàng trả góp 0%**:
  - Hỗ trợ đầy đủ các cơ chế lãi suất: **0% Trả góp**, **%/năm (dư nợ giảm dần)**, **Lãi cố định**, **Không lãi**.
  - Bảng tính số tiền trả góp hàng tháng, tiến độ % đã hoàn thành và lịch trả nợ.
- **Cho vay & Thu nợ**:
  - Ghi chép các khoản tiền cho mượn vốn kinh doanh, bạn bè, người thân để theo dõi lịch thu hồi nợ.

---

### 6. 🔒 Bảo Mật Đa Tầng & Cá Nhân Hóa (Security & Personalization)
- **Mã PIN 6 số & Mở khóa Sinh trắc học (Vân tay / Face Unlock)**.
- **Hẹn giờ tự động khóa an toàn (Auto-Lock Timeout)**: Tùy chọn *Ngay lập tức*, *Sau 30 giây*, *Sau 1 phút*, *Sau 5 phút* khi rời ứng dụng.
- **Chào mừng cá nhân hóa trên màn hình khóa**: Hiển thị tên người dùng, avatar emoji và lời chúc tài chính.
- **Chế độ ẩn số dư (Privacy Mode)**: Che giấu số tiền nhạy cảm (`•••••• đ`) khi xem ở nơi công cộng.
- **Luồng khởi tạo người dùng mới (Onboarding)**: Lựa chọn trải nghiệm dữ liệu mẫu (Demo Data) hoặc bắt đầu hoàn toàn mới.
- **Dark Mode 3 chế độ**: Tự động theo hệ thống, Sáng (Light), Tối (Dark Mode AMOLED `#121212`).
- **Android Backstack chuẩn mực**: Hỗ trợ phím quay lại (Back key) điều hướng qua lịch sử ngăn xếp màn hình.

---

## 🏗️ Kiến Trúc Ứng Dụng (Architecture)

Ứng dụng tuân thủ nghiêm ngặt nguyên lý **Clean Architecture** kết hợp mô hình **MVVM (Model-View-ViewModel)** và luồng dữ liệu một chiều **UDF (Unidirectional Data Flow)**:

```
                  ┌────────────────────────────────────────┐
                  │       UI Layer (Jetpack Compose)       │
                  │  Screens, Design System, Theme, Charts │
                  └───────────────────▲────────────────────┘
                                      │ StateFlow (MainUiState)
                                      │ User Actions / Events
                  ┌───────────────────┴────────────────────┐
                  │       ViewModel Layer (MainViewModel)  │
                  │  State Reducer, Security, Coroutines   │
                  └───────────────────▲────────────────────┘
                                      │ Domain Models & Use Cases
                  ┌───────────────────┴────────────────────┐
                  │    Repository Layer (MoneyLensRepo)    │
                  │   Business Rules, Aggregation, Seed    │
                  └───────────────────▲────────────────────┘
                                      │ Room Entity / DAO
                  ┌───────────────────┴────────────────────┐
                  │   Data Layer (Room DB & DataStore)     │
                  │  Offline-first SQLite, SharedPreferences│
                  └────────────────────────────────────────┘
```

---

## 📁 Cấu Trúc Thư Mục (Project Structure)

```
com.dvt.moneylens
├── MainActivity.kt                       # Entry Point, Edge-to-Edge & Navigation Backstack
├── MoneyLensApplication.kt               # Application Class & Notifications Setup
├── domain
│   ├── model
│   │   ├── DomainModels.kt               # Account, CreditCard, Transaction, Asset, Loan, Member...
│   │   ├── FinancialEnums.kt             # TransactionType, AssetType, LoanType, InterestType...
│   │   └── Money.kt                      # Value object biểu diễn tiền tệ & định dạng VNĐ
│   └── calc
│       └── CreditCardCycleCalculator.kt  # Thuật toán tính chu kỳ sao kê & ngày miễn lãi
├── data
│   ├── local
│   │   ├── MoneyLensDatabase.kt          # Room Database v2 (fallbackToDestructiveMigration)
│   │   ├── Entities.kt                   # Room Entities & Data Mappers
│   │   └── Daos.kt                       # Room Data Access Objects (Flow-based)
│   ├── repository
│   │   ├── MoneyLensRepository.kt        # Single Source of Truth cho toàn bộ ứng dụng
│   │   └── SeedDataProvider.kt           # Dữ liệu mẫu tài chính phong phú cho người mới
│   └── network
│       └── MarketPriceService.kt         # Dịch vụ cập nhật giá thị trường trực tuyến
├── service
│   ├── BankNotificationListenerService.kt# Lắng nghe thông báo ngân hàng
│   ├── BankSmsReceiver.kt                # Bắt SMS biến động số dư
│   └── BudgetAlertNotifier.kt            # Gửi thông báo đẩy cảnh báo ngân sách
└── ui
    ├── MainViewModel.kt                  # State Manager & Business Logic
    ├── theme
    │   ├── Theme.kt                      # MoneyLensTheme (Light / Dark AMOLED / System)
    │   ├── Color.kt                      # Bảng màu Emerald & Sapphire sang trọng
    │   ├── Type.kt                       # Typography
    │   └── Spacing.kt                    # Hệ thống khoảng cách chuẩn M3
    ├── designsystem
    │   ├── NetWorthBreakdownCard.kt      # Thẻ phân tích tài sản ròng đa nguồn
    │   ├── NetWorthTrendChart.kt         # Biểu đồ đường cong xu hướng tài sản ròng
    │   ├── MonthlyBarChart.kt            # Biểu đồ thu chi động kèm bộ chọn năm
    │   ├── InvestmentDonutChart.kt       # Biểu đồ Donut phân bổ danh mục đầu tư
    │   ├── LoanCardTile.kt               # Thẻ khoản vay & trả góp
    │   ├── CreditCardTile.kt             # Thẻ tín dụng mô phỏng vật lý
    │   └── ReviewTray.kt                 # Khay duyệt giao dịch nhanh
    └── screens
        ├── OverviewScreen.kt             # Màn hình Tổng quan
        ├── TransactionsScreen.kt         # Màn hình Sổ giao dịch
        ├── BudgetScreen.kt               # Màn hình Quản lý ngân sách
        ├── InvestmentScreen.kt           # Màn hình Đầu tư & Tích sản
        ├── CreditAndDebtsScreen.kt       # Màn hình Tín dụng & Khoản nợ (3 Sub-tabs)
        ├── SettingsScreen.kt             # Màn hình Cài đặt, Giao diện & Bảo mật
        ├── LockScreen.kt                 # Màn hình Khóa bảo mật PIN / Sinh trắc học
        └── Modals.kt                     # Các BottomSheet thêm giao dịch, thẻ, khoản vay
```

---

## 🛠️ Công Nghệ & Thư Viện Sử Dụng

- **Ngôn ngữ**: Kotlin 2.0+ (100% Coroutines & Flow).
- **Giao diện**: Jetpack Compose, Material Design 3 (M3).
- **Cơ sở dữ liệu**: Room Database (KSP).
- **Kiến trúc**: Android Architecture Components (ViewModel, StateFlow, Lifecycle).
- **Đồ họa & Trực quan hóa**: Modern Compose Canvas (Bezier Splines, Radial DrawScope, Gradients).
- **Bảo mật**: BiometricPrompt, SHA-256 Hashing, Android Encrypted SharedPreferences.

---

## 🚀 Hướng Dẫn Biên Dịch & Chạy Dự Án

### Yêu cầu môi trường:
- **Android Studio**: Ladybug / Meerkat hoặc mới hơn.
- **JDK**: OpenJDK 17 hoặc 21.
- **Android SDK**: `minSdk = 26` (Android 8.0+), `targetSdk = 34` (Android 14).

### Lệnh biên dịch Gradle:
```bash
# Biên dịch và kiểm tra tính toàn vẹn
gradle assembleDebug

# Chạy Unit Tests
gradle :app:testDebugUnitTest
```

---

## 📱 Hướng Dẫn Cài Đặt APK & Cấp Quyền Hệ Thống

### 1. Vượt qua cảnh báo Google Play Protect khi cài APK:
Do file APK được đóng gói trực tiếp trong môi trường thử nghiệm và có tính năng tự động đọc SMS biến động số dư từ ngân hàng:
1. Khi xuất hiện bảng thông báo màu đỏ/cam **"Bị Play Protect chặn" (Blocked by Play Protect)**:
2. Nhấn vào dòng chữ **"Chi tiết khác" (More details)** hoặc **"Xem thêm"**.
3. Chọn **"Vẫn cài đặt" (Install anyway)** để hoàn tất.

### 2. Bật quyền đọc SMS & Thông báo trên Android 13+ (Sideloaded APKs):
Đối với file APK cài ngoài Google Play Store, Android 13+ tự động khóa quyền SMS trong mục *Cài đặt bị hạn chế*:
1. Vào **Cài đặt điện thoại > Ứng dụng > MoneyLens**.
2. Nhấn vào biểu tượng **dấu 3 chấm (⋮)** ở góc trên bên phải màn hình.
3. Chọn **"Cho phép cài đặt bị hạn chế" (Allow restricted settings)**.
4. Quay lại mục **Quyền (Permissions)** và bật quyền **Tin nhắn SMS** & **Thông báo**.

---

## 📜 Giấy Phép & Bản Quyền

Dự án được xây dựng với mục tiêu mang lại giải pháp quản trị tài chính cá nhân an toàn, riêng tư (**Offline-First**) và đẳng cấp cho người dùng Việt Nam.
