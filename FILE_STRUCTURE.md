# Cấu trúc và Chức năng các file trong dự án OwlNetGuard
# File Structure and Component Functions of the OwlNetGuard Project

Tài liệu này mô tả chi tiết cấu trúc thư mục và chức năng từng thành phần trong mã nguồn cũng như tài nguyên của dự án **OwlNetGuard** (Ứng dụng Phím nổi cô lập mạng tức thì & Local VPN Sinkhole cho Android).
*This document describes in detail the folder structure and architectural role of each component in the **OwlNetGuard** project (Floating HUD instant network isolation & Local VPN Sinkhole for Android).*

---

## 📁 Thư mục Gốc / Root Directory (`OwlNetGuard/`)
- **FILE_STRUCTURE.md**: Tài liệu này, mô tả cấu trúc và chức năng các file trong toàn bộ dự án.
  *This document — describes the project's structure and file functions.*
- **README.md**: Giới thiệu sản phẩm, tính năng nổi bật, ảnh chụp màn hình và hướng dẫn cài đặt/build.
  *Product overview, key features, screenshots, and installation/build guide.*
- **LICENSE**: Văn bản đầy đủ giấy phép mã nguồn mở GNU General Public License v3.0 (GPL-3.0).
  *Full text of the GNU General Public License v3.0 (GPL-3.0).*
- **build.gradle.kts**: Tệp cấu hình Gradle cấp gốc, quản lý plugin Kotlin, Android Application, Jetpack Compose.
  *Root Gradle configuration file managing Kotlin, Android Application, and Compose plugins.*
- **settings.gradle.kts**: Khai báo các kho lưu trữ (Google Maven, Maven Central) và liên kết module `:app`.
  *Declares repositories and includes the `:app` module.*
- **gradle.properties**: Cấu hình bộ nhớ JVM, AndroidX và tối ưu hiệu năng biên dịch Gradle.
  *JVM memory configuration, AndroidX flags, and Gradle build optimizations.*
- **local.properties**: Đường dẫn cục bộ tới Android SDK trên máy phát triển.
  *Local path to the Android SDK on the developer machine.*
- **gradlew / gradlew.bat**: Tập lệnh Gradle Wrapper thực thi build đa nền tảng (Linux/macOS và Windows).
  *Gradle Wrapper execution scripts for cross-platform builds.*
- **.secret/**: Thư mục bảo mật lưu trữ khóa phát hành (`owlnetguard-release.jks`) và thông tin ký (`key.properties`).
  *Secure directory storing release keystore and signing credentials.*

---

## 📁 gradle/ (Cấu hình hệ thống build / Build System Configuration)
- **libs.versions.toml**: Danh mục phiên bản tập trung (Version Catalog) quản lý thư viện Jetpack Compose, Material 3, AndroidX, Kotlin Coroutines.
  *Centralized version catalog managing Jetpack Compose, Material 3, AndroidX, and Coroutines.*
- **wrapper/gradle-wrapper.properties**: Khai báo phiên bản Gradle distribution được dự án sử dụng.
  *Specifies the Gradle distribution version used by the project.*

---

## 📁 app/ (Module ứng dụng chính / Main Application Module)
- **build.gradle.kts**: Cấu hình chi tiết module ứng dụng (SDK 36, `versionName`, Compose Compiler, Proguard/R8, cấu hình ký bản Release).
  *Detailed application module configuration (SDK 36, `versionName`, Compose, Release signing).*
- **proguard-rules.pro**: Các quy tắc tối ưu hóa và bảo vệ mã nguồn cho bản phát hành Release.
  *Code shrinking and optimization rules for the Release build.*

### 📁 src/main/ (Mã nguồn & Tài nguyên chính / Main Source & Assets)
- **AndroidManifest.xml**: Khai báo quyền hệ thống (`INTERNET`, `ACCESS_NETWORK_STATE`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`), dịch vụ nền (`LocalVpnService`, `BubbleService`, `QuickTileService` dùng icon `@drawable/ic_qs_tile_owl`), bộ thu sự kiện (`BootReceiver`, `NotificationActionReceiver`) và Activity chính.
  *Declares system permissions, services (`LocalVpnService`, `BubbleService`, `QuickTileService` with `@drawable/ic_qs_tile_owl`), receivers, and main activity.*

---

### 📁 java/com/nightowleyes/owlnetguard/ (Mã nguồn Kotlin / Kotlin Source Code)

#### 📁 ui/ (Giao diện người dùng / User Interface Layer)
- **MainActivity.kt**: Điểm khởi đầu của ứng dụng (`ComponentActivity`). Đọc phiên bản tự động từ `BuildConfig.VERSION_NAME`, quản lý xin quyền VPN (`VpnService.prepare`), quyền Overlay (`SYSTEM_ALERT_WINDOW`), quyền thông báo (`POST_NOTIFICATIONS`), xử lý Intent cấp quyền từ Ô Cài đặt nhanh (`EXTRA_ENABLE_POPUP_FROM_TILE`) và khởi chạy `MainScreen`.
  *App entry point. Reads version from `BuildConfig.VERSION_NAME`, handles VPN/Overlay/Notification permissions, routes Quick Settings Tile intents, and launches `MainScreen`.*
- **MainScreen.kt**: Bộ điều phối giao diện chính (Master Layout Orchestrator).
  - Tự động kiểm tra bản cập nhật mới từ GitHub khi mở app (`fetchAvailableUpdate`).
  - Lắng nghe thay đổi trạng thái thời gian thực qua `PreferencesManager.stateVersion`.
  - Hỗ trợ thêm trực tiếp Ô Cài đặt nhanh (`StatusBarManager.requestAddTileService` trên Android 13+).
  - Bố cục thích ứng: một bề mặt cuộn duy nhất ở màn hình dọc (Portrait) và 2 cột Split-Pane ở màn hình ngang (Landscape).
  *Master layout orchestrator with automatic GitHub update check on launch, reactive state sync, Android 13+ Quick Settings tile addition, and responsive Portrait/Landscape layouts.*

#### 📁 ui/theme/ (Hệ thống thiết kế Faceted Apex & Theme)
- **Theme.kt**: Theme Compose Dark Palette tổng thể (Slate Cyberpunk), đặt font mặc định toàn cục là **Alata Regular** (`LocalTextStyle`).
  *Overall Compose Dark Palette theme setting **Alata Regular** as the default body typography.*
- **Color.kt**: Định nghĩa các mã màu ngữ nghĩa Faceted Apex (`SlateBackground`, `SlateSurface`, `BrandPrimary`, `CyberCyan`, `DangerRed`, `CyberCyanLight`, v.v.).
  *Defines Faceted Apex semantic color tokens.*
- **Type.kt**: Kết hợp hệ thống 2 phông chữ: **Josefin Sans** cho tiêu đề / tiêu ngữ (`display`, `headline`, `title`) và **Alata Regular** cho toàn bộ nội dung, nhãn và nút bấm (`body`, `label`).
  *Dual-font typography pairing **Josefin Sans** (titles/headings) with **Alata Regular** (body/labels).*
- **FacetedShapes.kt**: Hệ thống hình khối vát góc tinh thể đặc trưng (`FacetedCardShape`, `FacetedTileShape`, `FacetedBadgeShape`).
  *Signature chamfered crystal shapes for the Faceted Apex design system.*

#### 📁 ui/components/ (Thành phần giao diện tái sử dụng / Reusable UI Components)
- **HeaderSection.kt**: Thanh tiêu đề đỉnh màn hình với logo Cú động (đổi sang đỏ thẫm khi đang cách ly), tiêu đề "OWL NETGUARD" (Josefin Sans), nút Trái tim kim cương (Ủng hộ) và menu 3 chấm (`DropdownMenu`).
  *Top header bar with dynamic Owl logo, Josefin Sans wordmark, sponsor button, and overflow menu.*
- **FloatingHudSettingsCard.kt**: Chứa các thẻ điều khiển chính ở đầu màn hình:
  - `UpdateAvailableBanner`: Banner thông báo gọn gàng khi phát hiện phiên bản mới trên GitHub (có thể đóng).
  - `QuickTileSuggestionBanner`: Thẻ đề xuất thêm **Ô Cài đặt nhanh** xuất hiện ở đầu ứng dụng (có nút `+ Thêm` và nút `✕` để xóa vĩnh viễn).
  - `FloatingHudSettingsCard`: Thẻ điều khiển trung tâm duy nhất cho **Phím nổi (HUD)** (bật/tắt đồng bộ 1-1 với VPN nền, hiển thị trạng thái sống Violet/Scarlet, chọn nút hiển thị `Cách ly` và `Làm gián đoạn mạng` kèm nút `?` giải thích tinh tế, chọn phạm vi `Tất cả app đã chọn` / `Chỉ app đang mở`, cấu hình ứng dụng ngoại lệ lớp phủ và công tắc `Tự hủy cách ly`).
  *Contains `UpdateAvailableBanner`, dismissible top `QuickTileSuggestionBanner`, and the unified `FloatingHudSettingsCard` controlling Popup/VPN, visible HUD actions (`Isolate` & `Network Disrupt` with `?` info icon), isolation scope, overlay exclusions, and auto-unisolate.*
- **SelectedAppsSection.kt**: Chứa `SelectedAppsHeaderBar` (tiêu đề danh sách + gợi ý "Vuốt ngang để bỏ" + nút `+ Chọn ứng dụng`), `LoadingStateView` và `EmptySelectedAppsView`.
  *Contains `SelectedAppsHeaderBar`, `LoadingStateView`, and `EmptySelectedAppsView`.*
- **SwipeableSelectedAppItem.kt**: Thẻ ứng dụng tinh gọn gồm Icon + Tên ứng dụng + Tên gói (không nhãn phụ/nút thừa, đổi viền đỏ nhẹ khi đang cách ly) kèm cử chỉ vuốt ngang vượt ngưỡng 50% để bỏ khỏi danh sách.
  *Minimal app list row (Icon + Name + Package) with subtle crimson highlight when isolated and >50% swipe-to-remove gesture.*
- **FacetedSwitch.kt**: Công tắc gạt On/Off vát góc hình học độc bản (`FacetedSwitchTrackShape` & `FacetedSwitchThumbShape`).
  *Custom chamfered toggle switch matching the Faceted Apex design language.*
- **FacetedAmbientBackground.kt**: Canvas vẽ nền lưới đa giác vát góc ánh sáng yếu tạo chiều sâu không gian.
  *Canvas-drawn low-poly ambient faceted background mesh.*

#### 📁 ui/dialogs/ (Hộp thoại tương tác / Interactive Dialogs)
- **AppPickerDialog.kt**: Hộp thoại chọn ứng dụng với tìm kiếm thời gian thực, lọc ứng dụng hệ thống và tự động co giãn khi xoay ngang.
  *App picker modal with live search, system app filter, and responsive landscape sizing.*
- **ExcludedAppsDialog.kt**: Hộp thoại chọn danh sách ứng dụng ngoại lệ khi mở đè (Lớp phủ: Messenger, Zalo, Auto Click...).
  *Overlay excluded apps dialog preventing floating apps from interrupting foreground isolation.*
- **InfoDialogs.kt**: Tập hợp các hộp thoại thông tin và tiện ích cập nhật:
  - `fetchAvailableUpdate()`, `compareVersions()`, `cleanReleaseNotes()`: Kiểm tra cập nhật ngầm từ GitHub API, so sánh chuẩn SemVer và lọc sạch HTML/Markdown.
  - `DisruptInfoDialog`: Giải thích ngắn gọn cơ chế và lưu ý của tính năng **Làm gián đoạn mạng**.
  - `CheckUpdateDialog`, `UsageAccessPromptDialog`, `PrivacyPolicyDialog`, `OpenSourceLicensesDialog`, `DonateDialog`, `InfoDialogType`.

#### 📁 ui/utils/ (Tiện ích giao diện / UI Utilities)
- **UiUtils.kt**: Hàm mở rộng `Drawable.toImageBitmap()` chuyển đổi icon ứng dụng Android thành Compose `ImageBitmap`.
  *Extension helper converting Android drawables to Compose `ImageBitmap`.*

---

#### 📁 data/ (Lớp dữ liệu & Lưu trữ / Data & Persistence Layer)
- **AppInfo.kt**: Lớp dữ liệu mô tả ứng dụng (`name`, `packageName`, `icon`, `isSystemApp`, `isInBubble`, `isBlocked`).
  *Data class representing an application.*
- **PreferencesManager.kt**: Trình quản lý trạng thái và lưu trữ tập trung (`SharedPreferences` + `StateFlow`).
  - Khóa đồng bộ 1-1 `isPopupEnabled` / `setPopupEnabled` (Popup bật = VPN bật, Popup tắt = VPN tắt).
  - Lưu danh sách ứng dụng, nút hiển thị (`Cách ly`, `Làm gián đoạn mạng`), trạng thái tạm thời `disruptingNetwork`, phạm vi cô lập, `foregroundTunnelPackage`, danh sách ngoại lệ lớp phủ, trạng thái ẩn đề xuất Ô Cài đặt nhanh và phiên bản cập nhật đã bỏ qua.
  *Centralized state & persistence manager enforcing 1-to-1 Popup/VPN sync, disruption state, HUD button visibility, isolation scopes, and dismissed UI prompts.*

---

#### 📁 service/ (Dịch vụ nền hệ thống / System Services Layer)
- **LocalVpnService.kt**: Lõi đường hầm Local VPN Sinkhole cục bộ không cần Root (MTU 1500, IPv4 `10.215.173.1/30`, IPv6 `fd00:1:fd00:1:fd00:1:fd00:1/128`).
  - Hỗ trợ 2 phạm vi (`Tất cả app đã chọn` và `Chỉ app đang mở`).
  - Hỗ trợ chế độ **Làm gián đoạn mạng** (`isDisruptingNetwork`): tạm thời đẩy ứng dụng mục tiêu ra khỏi đường hầm VPN (chỉ giữ chính OwlNetGuard trong đường hầm để không mất dịch vụ VPN) rồi đưa lại vào đường hầm nhằm kích thích ngắt kết nối tức thì.
  - Thông báo nền tinh gọn với 1 nút duy nhất *"Tắt phím nổi (Dừng VPN)"*.
  *Core local VPN sinkhole engine supporting Mode All, Foreground Mode, temporary tunnel ejection for Network Disruption, and a streamlined single-action foreground notification.*
- **BubbleService.kt**: Trình điều khiển Phím nổi (Floating HUD `WindowManager` Overlay).
  - Hai nút hành động chính: **Cách ly (`ISOLATE`)** (có xác nhận nhấn 2 lần trong 2.5s khi hủy cách ly) và **Làm gián đoạn mạng (`DISRUPT`)** (đẩy app ra khỏi đường hầm 650ms rồi đưa lại vào; nếu bật *Tự hủy cách ly* sẽ tự mở lại mạng cho app ngay sau đó).
  - Tự động ẩn nút thu gọn và thực thi trực tiếp khi chỉ chọn 1 nút hiển thị.
  - Kéo thả mượt từ mọi vị trí, tự hít cạnh màn hình (`snapToNearestEdge`), tự làm mờ sau 3 giây, nhấn giữ để tắt Phím nổi & dừng VPN nền.
  - Tự động hủy cách ly khi phát hiện thiết bị bật lại mạng (Wi-Fi / Dữ liệu di động) sau khi mất kết nối hoàn toàn.
  *Floating HUD service handling Isolate (with double-tap unisolate confirmation), Network Disruption (650ms tunnel reset + optional auto-unisolate), auto-unisolate on network reconnect, edge snapping, idle fade, and long-press dismiss.*
- **DraggableBubbleContainer.kt**: Container tùy chỉnh chặn bắt sự kiện cảm ứng, phân biệt chạm bấm (Click), kéo thả (Drag) và nhấn giữ (Long-press).
  *Custom touch container disambiguating taps, drags, and long-press dismiss.*
- **QuickTileService.kt**: Dịch vụ **Ô Cài đặt nhanh (Quick Settings Tile)** sử dụng icon [`ic_qs_tile_owl.png`](app/src/main/res/drawable-xxhdpi/ic_qs_tile_owl.png). Chạm 1 lần để bật/tắt ngay **Phím nổi + VPN nền** (tự mở app xin quyền nếu thiếu quyền VPN hoặc Overlay).
  *Android Quick Settings Tile service using `ic_qs_tile_owl.png` for 1-tap Popup + VPN toggling.*

---

#### 📁 receiver/ (Bộ thu phát tín hiệu / Broadcast Receivers)
- **BootReceiver.kt**: Lắng nghe `BOOT_COMPLETED` để đồng bộ khôi phục dịch vụ nếu đang bật trước khi khởi động lại.
  *Restores services on device boot if previously enabled.*
- **NotificationActionReceiver.kt**: Xử lý nút bấm trên thông báo hệ thống để tắt Phím nổi và dừng VPN nền đồng thời.
  *Handles notification action to stop both Floating HUD and Local VPN together.*

---

#### 📁 utils/ (Bộ công cụ & Tiện ích lõi / Core Utilities Layer)
- **AppListLoader.kt**: Quét danh sách ứng dụng đã cài qua `PackageManager`, loại trừ chính OwlNetGuard và sắp xếp theo bảng chữ cái.
  *Loads and sorts installed user/system apps via `PackageManager`.*
- **ForegroundAppDetector.kt**: Nhận diện ứng dụng đang mở ở tiền trình qua `UsageStatsManager`, lọc bỏ Launcher, lớp phủ hệ thống và danh sách ứng dụng ngoại lệ (`isTransientOverlayOrSystemOrExcluded`).
  *Detects foreground app via `UsageStatsManager` while filtering launchers, transient system panels, and user-excluded overlay apps.*
- **NetworkSettingsHelper.kt**: Theo dõi trạng thái kết nối mạng (Wi-Fi, Dữ liệu di động, Chế độ máy bay) qua `ConnectivityManager`, `WifiManager`, `TelephonyManager` và `ContentObserver` để phục vụ tính năng *Tự hủy cách ly khi bật lại mạng*.
  *Monitors Wi-Fi, Cellular, and Airplane Mode state to trigger auto-unisolate on network reconnect.*

---

## 📁 res/ (Tài nguyên giao diện / Resources)
- **layout/layout_floating_bubble.xml**: Bố cục XML của Phím nổi HUD (Thu gọn `cardCollapsedBubble` và Mở rộng `panelExpanded` gồm nút Cách ly, nút Làm gián đoạn mạng và nút Gom lại).
  *Floating HUD layout supporting Collapsed and Expanded states (Isolate, Network Disrupt, Collapse).*
- **drawable/ic_network_disrupt.xml**: Vector icon tia sét màu xanh Cyan cho nút **Làm gián đoạn mạng**.
  *Cyan lightning pulse vector icon for Network Disruption.*
- **drawable/ic_collapse.xml**: Vector icon chevron thu gọn Phím nổi.
  *Collapse chevron vector icon.*
- **drawable/bg_quick_ball_panel.xml, bg_floating_bubble*.xml**: Nền bo tròn kính mờ / viền phát sáng cho Phím nổi.
  *Glowing pill and circular backgrounds for the floating HUD.*
- **drawable-xxhdpi/ic_qs_tile_owl.png**: Icon đơn sắc trắng (`96×96`, xuất từ `docs/logoBackground.png`) dành riêng cho Ô Cài đặt nhanh Android.
  *Monochrome white Quick Settings Tile icon exported from `docs/logoBackground.png`.*
- **drawable-nodpi/logo_faceted_apex.png & logo_faceted_apex_isolated.png**: Logo Cú Faceted Apex trạng thái bình thường (Tím/Cyan) và đang cách ly (Đỏ thẫm/Hổ phách).
  *Faceted Apex Owl logos for normal and isolated states.*
- **font/josefin_sans_*.ttf & font/alata_regular.ttf**: Bộ phông chữ Josefin Sans (tiêu đề/tiêu ngữ) và Alata Regular (nội dung).
  *Josefin Sans and Alata Regular TTF font assets.*
- **values/strings.xml, colors.xml, themes.xml**: Chuỗi văn bản, màu sắc và style nền tảng.
  *Base string, color, and theme resources.*

---

## 📁 docs/ (Tài liệu & Hình ảnh / Documentation & Assets)
- **logo.png & logoBackground.png**: Logo chính của dự án và bản đơn sắc dùng làm nguồn cho icon hệ thống / Ô Cài đặt nhanh.
  *Main project logo and monochrome source asset for system/Quick Settings icons.*
- **screenshots/**: Ảnh chụp màn hình thực tế phiên bản mới nhất hiển thị trong `README.md`.
  *Up-to-date device screenshots showcased in `README.md`.*
