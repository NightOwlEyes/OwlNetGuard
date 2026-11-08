# OwlNetGuard 🦉🛡️

<p align="center">
  <img src="docs/logo.png" alt="OwlNetGuard Logo" width="160" />
</p>

<p align="center">
  <strong>Công cụ cô lập mạng & tường lửa Sinkhole thông minh cho Android</strong>
</p>

<p align="center">
  <a href="https://github.com/NightOwlEyes/OwlNetGuard/releases/latest">
    <img src="https://img.shields.io/badge/Release-v1.0-8A2BE2?style=for-the-badge&logo=android&logoColor=white" alt="Release v1.0" />
  </a>
  <a href="https://github.com/NightOwlEyes/OwlNetGuard/releases/latest">
    <img src="https://img.shields.io/badge/Download-APK-00E5FF?style=for-the-badge&logo=google-play&logoColor=black" alt="Tải về APK" />
  </a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-blueviolet?style=for-the-badge&logo=android" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Play_Protect-100%25_Compliant-success?style=for-the-badge&logo=googleplay" alt="Play Protect Safe" />
  <a href="https://www.virustotal.com/gui/file/8b050f26acce898010c8334e1cb45ee06a6ca7a8520b0e93187b113e6fe6f477">
    <img src="https://img.shields.io/badge/VirusTotal-Clean%20(0%2F71)-brightgreen?style=for-the-badge&logo=virustotal" alt="VirusTotal Clean" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge&logo=gnu" alt="GPL 3.0" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/NightOwlEyes/OwlNetGuard/releases/latest">
    <img src="https://img.shields.io/badge/⚡_TẢI_VỀ_BẢN_MỚI_NHẤT_(APK)-8A2BE2?style=for-the-badge&labelColor=1a1630&logo=android" alt="Download APK Button" height="42" />
  </a>
</p>

---

## 📖 Giới thiệu

**OwlNetGuard** là ứng dụng Android mã nguồn mở được thiết kế để kiểm soát và cô lập kết nối Internet của từng ứng dụng riêng biệt một cách an toàn, nhanh chóng và mượt mà. 

Ứng dụng sử dụng cơ chế **Local VPN Sinkhole** chuẩn của Android để hấp thụ toàn bộ gói tin của các ứng dụng bị chặn ngay trên thiết bị mà **không cần quyền Root**, **không chuyển hướng dữ liệu qua bất kỳ máy chủ trung gian nào** và **Vượt kiểm duyệt của Google Play Protect**.

---

## ✨ Tính năng nổi bật

* 🛡️ **Lõi VPN Sinkhole cục bộ**: 100% lưu lượng mạng được xử lý trực tiếp trên thiết bị, bảo đảm quyền riêng tư tuyệt đối, không gửi dữ liệu ra ngoài và không hao pin.
* 🔮 **Bong bóng nổi thông minh (HUD Quick Ball)**:
  * Phím nổi thu gọn tiện dụng trên màn hình, hỗ trợ điều khiển nhanh: Cách ly, Wi-Fi, Dữ liệu di động, Chế độ máy bay.
  * Nhấn giữ vào biểu tượng con cú để tắt nhanh.
   * **Chỉ báo trạng thái trực quan**: Biểu tượng Cú tự động đổi màu **Đỏ thẫm phát sáng** khi ứng dụng đang mở bị cách ly và màu **Tím** khi bình thường. Biểu tượng Wi-Fi / Dữ liệu tự động gạch chéo mờ khi ngắt mạng.
  * Kéo thả mượt mà từ bất kỳ vị trí nào trên phím nổi, tự động hít sát viền màn hình, nhấn giữ để tắt HUD nhanh chóng.
* 🔄 **Tự động hủy cách ly khi bật lại mạng**:
  * Khi toàn bộ kết nối mạng (cả Wi-Fi lẫn Dữ liệu di động) bị ngắt rồi được kết nối mạng trở lại, hệ thống tự động giải phóng cách ly cho ứng dụng đang sử dụng (hoặc ứng dụng duy nhất được chọn).
* 🎯 **Hai chế độ cô lập mạng linh hoạt**:
  * **Tất cả ứng dụng đã chọn**: Tạm ngưng hoặc kích hoạt chặn mạng hàng loạt cho toàn bộ danh sách chỉ với 1 chạm.
  * **Chỉ ứng dụng đang mở (Foreground Mode)**: Tự động phát hiện ứng dụng bạn đang sử dụng trên màn hình. Khi bấm nút *Cô lập* trên phím nổi, hệ thống sẽ **bật/tắt mạng riêng cho đúng ứng dụng đó**, trong khi các ứng dụng nền khác vẫn tiếp tục bị cách ly liên tục mà không hề bị gián đoạn.
---

## 📸 Ảnh chụp màn hình ứng dụng

| Bảng điều khiển chính (VPN & HUD) | Bảng chọn ứng dụng (App Picker) | Phiên bản & Cài đặt |
| :---: | :---: | :---: |
| ![Bảng điều khiển](docs/screenshots/active_dashboard.png) | ![Bảng chọn ứng dụng](docs/screenshots/app_picker.png) | ![Menu Cài đặt](docs/screenshots/menu_version.png) |

---

## 🚀 Tải về & Cài đặt

1. Truy cập trang phát hành: [GitHub Releases - OwlNetGuard](https://github.com/NightOwlEyes/OwlNetGuard/releases/latest)
2. Tải file **`app-release.apk`**.
3. Cài đặt trực tiếp trên thiết bị Android của bạn (hỗ trợ từ Android 8.0 Oreo đến Android 15/16).
4. Mở ứng dụng, chọn các app bạn muốn cách ly mạng và bật **Cách ly ứng dụng**!

---

## 🔒 Minh bạch quyền truy cập (Permissions)

OwlNetGuard tuyệt đối không yêu cầu các quyền nhạy cảm nguy hiểm:

* `android.permission.INTERNET`: Duy trì dịch vụ giao diện mạng.
* `android.net.VpnService`: Thiết lập đường hầm Sinkhole cục bộ trên máy để ngăn chặn kết nối cho các app được chọn.
* `android.permission.SYSTEM_ALERT_WINDOW`: Hiển thị phím nổi thông minh trên màn hình.
* `android.permission.PACKAGE_USAGE_STATS` *(tùy chọn)*: Chỉ yêu cầu khi bạn bật chế độ *"Chỉ ứng dụng đang mở"* để phát hiện gói ứng dụng Foreground hiện thời.

---

## 🛠️ Hướng dẫn tự build từ mã nguồn

### Yêu cầu:
* JDK 17 hoặc JDK 21
* Android SDK (API Level 36)

### Lệnh build:

```powershell
# Clone mã nguồn
git clone https://github.com/NightOwlEyes/OwlNetGuard.git
cd OwlNetGuard

# Build bản Debug để trải nghiệm
.\gradlew.bat assembleDebug

# Build bản Release (yêu cầu cấu hình .secret/key.properties)
.\gradlew.bat assembleRelease
```

File APK xuất xưởng sẽ nằm tại:
`app/build/outputs/apk/release/app-release.apk`

---

## 🛡️ Kiểm định an toàn & VirusTotal

OwlNetGuard hoàn toàn sạch mã độc, đạt chuẩn an toàn 100%:
* 🟢 **Báo cáo VirusTotal**: [0/71 Nhà cung cấp bảo mật phát hiện - Clean 100%](https://www.virustotal.com/gui/file/8b050f26acce898010c8334e1cb45ee06a6ca7a8520b0e93187b113e6fe6f477)
* 🟢 **Google Play Protect**: Tuân thủ 100% tiêu chuẩn bảo mật của Android, hoạt động hoàn toàn offline không thu thập dữ liệu người dùng.

---

## 📄 Bản quyền & Giấy phép

Phát triển bởi **NightOwlEyes**.
Phát hành theo giấy phép mã nguồn mở [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
Logo và đồ họa biểu tượng thuộc bản quyền của dự án OwlNetGuard.
