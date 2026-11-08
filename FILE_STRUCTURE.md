# Cấu trúc và Chức năng các file trong dự án OwlNetGuard
# File Structure and Component Functions of the OwlNetGuard Project

Tài liệu này mô tả chi tiết cấu trúc thư mục, chức năng của từng thành phần trong mã nguồn và tài nguyên của dự án **OwlNetGuard** (Ứng dụng cách ly mạng ứng dụng & phím nổi điều khiển nhanh).
*This document describes in detail the folder structure, architectural functions of each component in the source code and assets of the **OwlNetGuard** project (App network isolation & floating HUD controller).*

---

## 📁 Thư mục Gốc / Root Directory (`OwlNetGuard/`)
- **FILE_STRUCTURE.md**: Tài liệu này, mô tả cấu trúc và chức năng các file trong toàn bộ dự án.
  *This document — describes the project's structure and file functions.*
- **README.md**: Hướng dẫn giới thiệu, tính năng nổi bật, hình ảnh minh họa và hướng dẫn cài đặt/sử dụng ứng dụng.
  *Comprehensive user guide, feature showcase, screenshots, and installation/usage instructions.*
- **LICENSE**: Văn bản đầy đủ giấy phép mã nguồn mở GNU General Public License v3.0 (GPL-3.0).
  *Full text of the GNU General Public License v3.0 (GPL-3.0).*
- **build.gradle.kts**: Tệp cấu hình Gradle cấp gốc, quản lý plugin Kotlin, Android Application, Jetpack Compose.
  *Root Gradle configuration file, managing Kotlin, Android Application, and Compose plugins.*
- **settings.gradle.kts**: Khai báo các kho lưu trữ (Google Maven, Maven Central) và liên kết module `:app`.
  *Declares repositories (Google Maven, Maven Central) and includes the `:app` module.*
- **gradle.properties**: Cấu hình bộ nhớ JVM, AndroidX và tối ưu hiệu năng biên dịch Gradle.
  *JVM memory configuration, AndroidX flags, and Gradle build optimizations.*
- **local.properties**: Đường dẫn cục bộ tới Android SDK trên máy phát triển.
  *Local path to the Android SDK on the developer machine.*
- **gradlew / gradlew.bat**: Tập lệnh Gradle Wrapper thực thi build đa nền tảng (Linux/macOS và Windows).
  *Gradle Wrapper execution scripts for cross-platform builds (Linux/macOS and Windows).*
- **.secret/**: Thư mục bảo mật lưu trữ khóa phát hành (`owlnetguard-release.jks`) và thông tin ký (`key.properties`).
  *Secure directory storing release keystore (`owlnetguard-release.jks`) and signing credentials (`key.properties`).*

---

## 📁 gradle/ (Cấu hình hệ thống build / Build System Configuration)
- **libs.versions.toml**: Danh mục phiên bản tập trung (Version Catalog) quản lý tất cả thư viện Jetpack Compose, Material 3, AndroidX, Kotlin Coroutines.
  *Centralized version catalog managing Jetpack Compose, Material 3, AndroidX, and Kotlin Coroutines dependencies.*
- **wrapper/gradle-wrapper.properties**: Khai báo phiên bản Gradle distribution được dự án sử dụng.
  *Specifies the Gradle distribution version used by the project.*

---

## 📁 app/ (Module ứng dụng chính / Main Application Module)
- **build.gradle.kts**: Cấu hình chi tiết module ứng dụng (SDK 36, Java 17, Compose Compiler, Proguard/R8, cấu hình ký bản phát hành Release).
  *Detailed application module configuration (SDK 36, Java 17, Compose Compiler, Proguard/R8, Release signing config).*
- **proguard-rules.pro**: Các quy tắc tối ưu hóa và chống dịch ngược mã nguồn cho bản phát hành Release.
  *Code shrinking, obfuscation, and optimization rules for the Release build.*

### 📁 src/main/ (Mã nguồn & Tài nguyên chính / Main Source & Assets)
- **AndroidManifest.xml**: Khai báo quyền hệ thống (`INTERNET`, `ACCESS_NETWORK_STATE`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`), các dịch vụ nền (`LocalVpnService`, `BubbleService`, `QuickTileService`), bộ thu sự kiện (`BootReceiver`, `NotificationActionReceiver`) và Activity chính.
  *Declares system permissions, background services, broadcast receivers, and main activity.*

---

### 📁 java/com/nightowleyes/owlnetguard/ (Mã nguồn Kotlin / Kotlin Source Code)

#### 📁 ui/ (Giao diện người dùng / User Interface Layer)
- **MainActivity.kt**: Điểm khởi đầu của ứng dụng (ComponentActivity). Quản lý launcher xin quyền VPN (`VpnService.prepare`), launcher xin quyền Overlay (`SYSTEM_ALERT_WINDOW`), thông báo (`POST_NOTIFICATIONS`), điều hướng Intent từ ngoài vào và kích hoạt `MainScreen`.
  *App entry point (ComponentActivity). Manages VPN permission launcher, overlay permission launcher, notification permission, external Intent routing, and activates `MainScreen`.*
- **MainScreen.kt**: Bộ điều phối giao diện chính (Master Layout Orchestrator). Quản lý vòng đời (`LifecycleEventObserver`), lắng nghe cập nhật trạng thái thời gian thực (`PreferencesManager.stateVersion`), bố cục thích ứng thông minh:
  - **Màn hình dọc (Portrait)**: Làm phẳng toàn bộ giao diện thành một bề mặt cuộn duy nhất (`verticalScroll`), hiển thị các thẻ điều khiển và danh sách ứng dụng đã chọn cuộn liền mạch mượt mà, tối ưu tuyệt đối cho các thiết bị có chiều dài ngắn/màn hình nhỏ.
  - **Màn hình xoay ngang (Landscape)**: Chia 2 cột Split-Pane (cột trái cuộn thẻ điều khiển, cột phải hiển thị danh sách ứng dụng với `LazyColumn`).
  - Điều phối hiển thị các hộp thoại thông tin và chọn ứng dụng.
  *Master layout orchestrator. Provides adaptive layout: Unified single-scroll surface in Portrait (optimized for compact/short screens), 2-column Split-Pane in Landscape, and coordinates dialogs.*

#### 📁 ui/theme/ (Hệ thống thiết kế Faceted Apex & Theme)
- **Theme.kt**: Theme Compose Dark Palette tổng thể của ứng dụng, kết hợp bảng màu Slate Cyberpunk với hệ thống phông chữ Josefin Sans.
  *Overall Compose Dark Palette theme, combining Slate Cyberpunk colors with Josefin Sans typography.*
- **Color.kt**: Định nghĩa các mã màu ngữ nghĩa Faceted Apex (`SlateBackground`, `SlateSurface`, `BrandPrimary`, `CyberCyan`, `DangerRed`, `CyberCyanLight`, v.v.).
  *Defines Faceted Apex semantic color tokens.*
- **Type.kt**: Định nghĩa các kiểu chữ Typography (`titleLarge`, `bodyMedium`, `labelSmall`, v.v.) liên kết chặt chẽ với font Josefin Sans.
  *Typography definitions mapped to the Josefin Sans font family.*
- **FacetedShapes.kt**: Hệ thống hình khối vát góc tinh thể đặc trưng (`FacetedCardShape`, `FacetedTileShape`, `FacetedBadgeShape`) tạo phong cách Apex đa diện.
  *Signature chamfered crystal shapes (`FacetedCardShape`, `FacetedTileShape`, `FacetedBadgeShape`) for the Faceted Apex visual language.*

#### 📁 ui/components/ (Thành phần giao diện tái sử dụng / Reusable UI Components)
- **HeaderSection.kt**: Thanh tiêu đề đỉnh màn hình với logo Cú (tự đổi sang đỏ tím khi đang cách ly), chữ "OWL NETGUARD" và menu 3 chấm (`DropdownMenu`).
  *Top header bar with dynamic Owl logo, title, and 3-dots overflow menu.*
- **IsolationMasterCard.kt**: Thẻ điều khiển trung tâm Cách ly ứng dụng (Master Switch). Hiển thị số lượng app đang cách ly, trạng thái VPN nền, hiệu ứng viền phát sáng gradient và công tắc bật/tắt chính.
  *Master isolation control card. Displays isolated count, VPN background status, gradient border glow, and main master switch.*
- **FloatingHudSettingsCard.kt**: Thẻ cài đặt phím nổi HUD. Hỗ trợ chọn nút hiển thị (Cách ly, Wi-Fi, Dữ liệu, Máy bay), chọn chế độ cô lập ("Tất cả app đã chọn" vs "Chỉ app đang mở"), switch "Tự hủy cách ly khi bật lại mạng" kèm cơ chế tự động làm mờ và khóa chức năng phụ thuộc.
  *HUD settings card. Manages visible buttons, isolation modes, and the auto-unisolate on reconnect switch with dependency auto-dimming/locking.*
- **SelectedAppsSection.kt**: Chứa `SelectedAppsHeaderBar` (thanh tiêu đề danh sách + nút "+ Chọn ứng dụng"), `LoadingStateView` (trạng thái tải app) và `EmptySelectedAppsView` (trạng thái danh sách trống).
  *Contains `SelectedAppsHeaderBar`, `LoadingStateView`, and `EmptySelectedAppsView`.*
- **SwipeableSelectedAppItem.kt**: Thành phần từng ứng dụng trong danh sách với cử chỉ vuốt ngang xóa (hành trình trên 60% chiều rộng mới xóa), nút lục giác bật/tạm ngưng cách ly và nhãn trạng thái ("ĐÃ KÍCH HOẠT", "ĐANG CÁCH LY", "TẠM NGƯNG").
  *Individual app item with swipe-to-remove gesture (>60% threshold required), hexagon active/pause toggle button, and status badges.*
- **FacetedAmbientBackground.kt**: Canvas vẽ nền lưới đa giác vát góc lập thể ánh sáng yếu tạo chiều sâu không gian cyberpunk.
  *Canvas-drawn low-poly ambient faceted background mesh creating cyberpunk spatial depth.*

#### 📁 ui/dialogs/ (Hộp thoại tương tác / Interactive Dialogs)
- **AppPickerDialog.kt**: Hộp thoại chọn ứng dụng toàn diện. Hỗ trợ tìm kiếm theo tên/package theo thời gian thực, lọc ứng dụng hệ thống, tự động co giãn kích thước tỷ lệ vàng khi xoay ngang Landscape.
  *Comprehensive app selection dialog with real-time search, system apps toggle, and responsive landscape sizing.*
- **InfoDialogs.kt**: Tập hợp các hộp thoại thông tin:
  - `UsageAccessPromptDialog`: Hộp thoại hướng dẫn cấp quyền Usage Access cho chế độ Chỉ app đang mở.
  - `PrivacyPolicyDialog`: Chính sách bảo mật (xử lý 100% offline, không thu thập dữ liệu).
  - `OpenSourceLicensesDialog`: Giấy phép mã nguồn mở (GNU General Public License v3.0 - GPL-3.0).
  - `CheckUpdateDialog`: Kiểm tra phiên bản mới từ GitHub Release API.
  - `InfoDialogType`: Enum phân loại các hộp thoại.

#### 📁 ui/utils/ (Tiện ích giao diện / UI Utilities)
- **UiUtils.kt**: Hàm mở rộng `Drawable.toImageBitmap()` tối ưu chuyển đổi icon ứng dụng Android thành Compose ImageBitmap.
  *Extension function `Drawable.toImageBitmap()` for converting Android app drawables to Compose ImageBitmaps.*

---

#### 📁 data/ (Lớp dữ liệu & Lưu trữ / Data & Persistence Layer)
- **AppInfo.kt**: Lớp dữ liệu mô tả ứng dụng (`name`, `packageName`, `icon`, `isSystemApp`, `isInBubble`, `isBlocked`).
  *Data class representing an application.*
- **PreferencesManager.kt**: Trình quản lý cài đặt toàn diện (SharedPreferences). Lưu danh sách gói tin, trạng thái kích hoạt, kiểu cô lập, cấu hình phím nổi; hàm `setPackageActive(context, packageName, active)` hỗ trợ kích hoạt/hủy kích hoạt cách ly an toàn; phát luồng phản ứng `stateVersion: StateFlow<Long>` giúp UI và Service đồng bộ tức thì; hàm `syncAllServices()` tự động điều phối kích hoạt/dừng dịch vụ.
  *Centralized configuration manager (SharedPreferences). Persists app lists, active states (`setPackageActive`), isolation modes, HUD configs; emits reactive `stateVersion` for instant UI/Service sync; provides `syncAllServices()`.*

---

#### 📁 service/ (Dịch vụ nền hệ thống / System Services Layer)
- **LocalVpnService.kt**: Trái tim của chế độ cách ly VPN (Sinkhole Architecture). Thiết lập VPN cục bộ qua `VpnService.Builder`, chặn bắt lưu lượng mạng của các ứng dụng bị cô lập, xử lý gói tin âm thầm (drop/trả RST TCP/ICMP unreachable) giúp ứng dụng bị cách ly không crash và không hay biết mạng bị ngắt.
  *Core VPN isolation engine (Sinkhole Architecture). Creates a local VPN tunnel, intercepts traffic of isolated apps, and handles packets silently so apps do not crash.*
- **BubbleService.kt**: Trình điều khiển bong bóng nổi (Floating Bubble WindowManager Overlay).
  - Hai trạng thái: **Thu gọn** (1 bong bóng tròn logo Cú Apex, tự động đổi màu Đỏ thẫm khi app đang mở bị cách ly và màu Tím khi bình thường) và **Mở rộng** (dãy N phím chức năng + phím thứ N+1 là nút Gom lại).
  - **Không tự động thu gọn**: Khi bấm vào các phím chức năng (Cách ly, Wi-Fi, Dữ liệu, Máy bay), bong bóng vẫn giữ nguyên trạng thái mở rộng.
  - **Chỉ thu gọn khi bấm đúng phím Gom lại** (Collapse button).
  - Kéo thả mượt mà từ bất kỳ vị trí nào trên bong bóng (nền hay icon), tự động hít sát viền (`snapToNearestEdge`).
  - Tự động làm mờ khi không thao tác sau 3 giây.
  - Nhấn giữ để tắt phím nổi nhanh kèm rung phản hồi.
  - Phím tắt mở bảng cài đặt mạng hệ thống (Wi-Fi, Dữ liệu di động, Chế độ máy bay).
  - **Tự động hủy cách ly khi bật lại mạng**: Nhận diện thông minh khi toàn bộ kết nối mạng (cả Wi-Fi lẫn Dữ liệu di động) bị ngắt hoàn toàn rồi được bật lại, tự động giải phóng cách ly cho ứng dụng đang dùng (hoặc app duy nhất trong danh sách).
- **DraggableBubbleContainer.kt**: Layout tùy chỉnh (`DraggableBubbleContainer`) chặn bắt sự kiện cảm ứng (`onInterceptTouchEvent`), phân biệt chuẩn xác giữa thao tác chạm bấm (Click) và thao tác kéo thả (Drag) từ bất kỳ điểm nào trên giao diện (nền hay phím bấm), hỗ trợ nhấn giữ (Long-press) để tắt HUD.
  *Custom container intercepting touch events, disambiguating taps vs drags from any surface, and handling long-press dismiss.*
- **QuickTileService.kt**: Dịch vụ ô cài đặt nhanh (Quick Settings Tile) trên thanh trạng thái kéo xuống của Android, cho phép bật/tắt nhanh chế độ cách ly mạng 1 chạm.
  *Android Quick Settings Tile service allowing 1-tap network isolation toggle from system notification shade.*

---

#### 📁 receiver/ (Bộ thu phát tín hiệu / Broadcast Receivers)
- **BootReceiver.kt**: Lắng nghe sự kiện khởi động thiết bị (`BOOT_COMPLETED`), tự động khôi phục VPN và phím nổi nếu người dùng đã bật trước đó.
  *Listens for `BOOT_COMPLETED`, restoring VPN and floating HUD if previously enabled.*
- **NotificationActionReceiver.kt**: Lắng nghe và xử lý các nút bấm trên thanh thông báo hệ thống: "Bật/Tắt cách ly" và "Tắt phím nổi".
  *Handles notification action clicks: "Toggle Isolate" and "Turn off Floating HUD".*

---

#### 📁 utils/ (Bộ công cụ & Tiện ích lõi / Core Utilities Layer)
- **AppListLoader.kt**: Quét danh sách các ứng dụng cài đặt trên thiết bị thông qua `PackageManager`, lọc các ứng dụng có Launcher, loại trừ ứng dụng hiện tại và sắp xếp chữ cái.
  *Scans installed apps via `PackageManager`, filters launcher apps, excludes self, and sorts alphabetically.*
- **ForegroundAppDetector.kt**: Nhận diện ứng dụng đang hiển thị ở tiền trình (Foreground App) thông qua `UsageStatsManager`, kiểm tra quyền `PACKAGE_USAGE_STATS`. Hỗ trợ `isSystemOrSettingsOrLauncher` và `getLastUserForegroundPackage` giúp xác định chính xác ứng dụng bị cách ly ngay cả khi bảng cài đặt hệ thống hoặc thanh thông báo đang che màn hình.
  *Detects currently active foreground application via `UsageStatsManager`. Includes `isSystemOrSettingsOrLauncher` and `getLastUserForegroundPackage` to reliably resolve target apps even when system settings overlay is present.*
- **NetworkSettingsHelper.kt**: Theo dõi trạng thái kết nối mạng (Wi-Fi, Dữ liệu di động, Chế độ máy bay) qua `ConnectivityManager`, `WifiManager`, `ContentObserver` trên `Settings.Global` ("mobile_data", "airplane_mode_on", "wifi_on") và `BroadcastReceiver`, hỗ trợ mở panel cài đặt mạng chuẩn của Android.
  *Monitors network status (Wi-Fi, Cellular, Airplane mode) via `ConnectivityManager`, `WifiManager`, `Settings.Global` `ContentObserver`, and `BroadcastReceiver`; opens system network panels.*
- **PacketUtils.kt**: Tiện ích phân tích gói tin IPv4/IPv6 (trích xuất IP nguồn, IP đích, giao thức TCP/UDP/ICMP, cổng port, checksum).
  *IPv4/IPv6 packet parsing utility (extracts source/dest IPs, TCP/UDP/ICMP protocols, ports, checksums).*
- **UdpProxyHandler.kt**: Điều hướng và phân tích luồng UDP/DNS nội bộ.
  *Inspects and routes local UDP/DNS packet streams.*

---

## 📁 res/ (Tài nguyên giao diện / Resources)
- **layout/layout_floating_bubble.xml**: Bố cục XML của bong bóng nổi (Floating Bubble Container) hỗ trợ 2 chế độ: Thu gọn (`cardCollapsedBubble`) và Mở rộng (`panelExpanded`) gồm N nút hành động và nút Gom lại.
  *XML layout of the floating bubble container supporting Collapsed and Expanded states.*
- **drawable/ic_collapse.xml**: Vector icon chevron chỉ hướng thu gọn bong bóng nổi.
  *Vector chevron icon for collapsing the floating bubble.*
- **drawable/bg_quick_ball_panel.xml**: Nền viền con nhộng bo tròn phong cách kính mờ / gradient viền cho phím nổi.
  *Pill capsule rounded background drawable with gradient border for the floating HUD.*
- **drawable-nodpi/logo_faceted_apex.png**: Logo Cú thiết kế Apex đa diện (trạng thái bình thường: xanh neon / tím).
  *Faceted Apex Owl logo (normal state: neon cyan / violet).*
- **drawable-nodpi/logo_faceted_apex_isolated.png**: Logo Cú thiết kế Apex đa diện (trạng thái cách ly: đỏ thẫm / hổ phách).
  *Faceted Apex Owl logo (isolated state: crimson / amber).*
- **font/josefin_sans_*.ttf**: Bộ phông chữ Josefin Sans đầy đủ các biến thể (Bold, Medium, Regular, Light, SemiBold).
  *Josefin Sans font family TTF files.*
- **values/strings.xml, colors.xml, themes.xml**: Các chuỗi văn bản, tài nguyên màu và theme cơ bản cho hệ thống Android.
  *String constants, color definitions, and base Android styles.*

---

## 📁 docs/ (Tài liệu & Hình ảnh kiểm thử / Documentation & Test Artifacts)
- **screenshots/**: Chứa các ảnh chụp màn hình kiểm thử thực tế của ứng dụng (Portrait, Landscape Split-Pane, Dialog, HUD).
  *Contains verified emulator screenshots demonstrating Portrait, Landscape Split-Pane, Dialogs, and HUD.*
