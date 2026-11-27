# OwlNetGuard 🦉🛡️

<p align="center">
  <img src="docs/logo.png" alt="OwlNetGuard Logo" width="160" />
</p>

<p align="center">
  <strong>Phím nổi cô lập mạng tức thì & tường lửa Local VPN Sinkhole cho Android</strong>
</p>

<p align="center">
  <a href="https://github.com/NightOwlEyes/OwlNetGuard/releases/latest">
    <img src="https://img.shields.io/badge/Release-v1.3-8A2BE2?style=for-the-badge&logo=android&logoColor=white" alt="Release v1.3" />
  </a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-blueviolet?style=for-the-badge&logo=android" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Play_Protect-100%25_Compliant-success?style=for-the-badge&logo=googleplay" alt="Play Protect Safe" />
  <a href="https://www.virustotal.com/gui/file/1a3c07b2bfe41cff5960704d9e199a2c0a1192a7348639f5d35de2aaa4a5b229">
    <img src="https://img.shields.io/badge/VirusTotal-Clean%20(0%2F71)-brightgreen?style=for-the-badge&logo=virustotal" alt="VirusTotal Clean" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge&logo=gnu" alt="GPL 3.0" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/NightOwlEyes/OwlNetGuard/releases/latest/download/app-release.apk">
    <img src="https://img.shields.io/badge/⚡_TẢI_VỀ_BẢN_MỚI_NHẤT_(APK)-8A2BE2?style=for-the-badge&labelColor=1a1630&logo=android" alt="Download APK Button" height="42" />
  </a>
</p>

---

## 📖 Giới thiệu

**OwlNetGuard** là ứng dụng Android mã nguồn mở chuyên biệt về **Phím nổi (HUD) điều khiển cô lập mạng tức thì** cho từng ứng dụng hoặc trò chơi đang sử dụng, giúp bạn cắt/nối kết nối ngay trên màn hình mà không cần rời khỏi ứng dụng.

Ứng dụng sử dụng cơ chế **Local VPN Sinkhole** chuẩn của Android hoạt động đồng bộ 1-1 với Phím nổi (**bật Phím nổi = bật VPN nền, tắt Phím nổi = dừng VPN nền**), **không cần quyền Root**, **không chuyển hướng dữ liệu qua máy chủ trung gian** và **100% tuân thủ Google Play Protect**.

---

## ✨ Tính năng nổi bật

* 🔮 **Phím nổi thông minh (HUD Quick Ball) đồng bộ 1-1 với VPN nền**:
  * **Nút Cách ly (`🦉`)**: Ngắt hoặc mở lại kết nối mạng cho ứng dụng mục tiêu ngay trên màn hình. Tích hợp **cơ chế nhấn 2 lần để hủy cách ly** (chạm lần 1 cảnh báo, chạm lần 2 trong 2.5s mới xác nhận) giúp tránh bấm nhầm.
  * **Nút Làm gián đoạn mạng (`⚡`)**: Chủ động đưa ứng dụng ra khỏi đường hầm VPN trong chốc lát rồi đưa lại vào đường hầm, giúp kích thích ứng dụng báo mất kết nối tức thì mà không cần thao tác bật/tắt Wi-Fi hay Dữ liệu di động thủ công.
  * **Chỉ báo trạng thái trực quan**: Biểu tượng Cú tự đổi sang **Đỏ thẫm phát sáng** khi đang cách ly và **Tím** khi tạm dừng; tự ẩn nút thu gọn khi chỉ chọn 1 nút hiển thị; nhấn giữ để tắt Phím nổi & dừng VPN nhanh.
* ⚡ **Bật/tắt nhanh từ thanh Cài đặt nhanh (Quick Settings Tile)**:
  * Hỗ trợ ô bật/tắt nhanh **OwlNetGuard** trực tiếp trên thanh thông báo kéo xuống của Android (kèm thẻ đề xuất thêm nhanh ở đầu ứng dụng có thể ẩn đi).
* 🔄 **Tự động hủy cách ly thông minh**:
  * Tự động mở lại mạng cho ứng dụng ngay khi bật lại Wi-Fi / Dữ liệu di động hoặc ngay sau khi bấm **Làm gián đoạn mạng (`⚡`)**.
* 🎯 **Hai phạm vi cách ly linh hoạt**:
  * **Tất cả app đã chọn**: Điều khiển cách ly / gián đoạn đồng loạt toàn bộ danh sách ứng dụng đã chọn.
  * **Chỉ app đang mở (Foreground Mode)**: Tự động nhận diện và chỉ tác động lên ứng dụng đang mở trên màn hình; hỗ trợ **Ứng dụng ngoại lệ (Lớp phủ)** (Messenger, Zalo, Auto Click...) để không làm ngắt phiên cách ly khi có ứng dụng nổi mở đè.
* 🔔 **Tự động kiểm tra cập nhật**:
  * Tự động kiểm tra phiên bản mới trên GitHub Releases khi mở ứng dụng và thông báo gọn gàng khi có bản cập nhật mới.

---

## 📸 Ảnh chụp màn hình ứng dụng

| Bảng điều khiển chính (Chế độ chờ) | Đang cách ly & Phím nổi HUD mở rộng |
| :---: | :---: |
| ![Bảng điều khiển chính](docs/screenshots/Screenshot_20261127-233933_Owl%20NetGuard.png) | ![Phím nổi HUD mở rộng](docs/screenshots/Screenshot_20261127-233942_Owl%20NetGuard.png) |

| Bảng chọn ứng dụng (App Picker) | Chế độ Chỉ app đang mở & Menu |
| :---: | :---: |
| ![Bảng chọn ứng dụng](docs/screenshots/Screenshot_20261127-233936_Owl%20NetGuard.png) | ![Menu và Chế độ Foreground](docs/screenshots/Screenshot_20261127-234002_Owl%20NetGuard.png) |

---

## 🚀 Tải về & Cài đặt

1. Truy cập trang phát hành: [GitHub Releases - OwlNetGuard](https://github.com/NightOwlEyes/OwlNetGuard/releases/latest)
2. Tải file **`app-release.apk`**.
3. Cài đặt trực tiếp trên thiết bị Android của bạn (hỗ trợ từ Android 8.0 Oreo đến Android 15/16).
4. Mở ứng dụng, thêm các app vào danh sách và bật **Phím nổi (HUD)** (hoặc bật/tắt trực tiếp từ **Ô Cài đặt nhanh** trên thanh thông báo)!

---

## 🔒 Minh bạch quyền truy cập (Permissions)

OwlNetGuard tuyệt đối không yêu cầu các quyền nhạy cảm nguy hiểm:

* `android.permission.INTERNET`: Duy trì dịch vụ giao diện mạng cục bộ và kiểm tra phiên bản mới từ GitHub.
* `android.net.VpnService`: Thiết lập đường hầm Sinkhole cục bộ trên máy để ngăn chặn kết nối cho các app được chọn.
* `android.permission.SYSTEM_ALERT_WINDOW`: Hiển thị Phím nổi (HUD) điều khiển nhanh trên màn hình.
* `android.permission.PACKAGE_USAGE_STATS` *(tùy chọn)*: Chỉ yêu cầu khi bật phạm vi *"Chỉ app đang mở"* để phát hiện ứng dụng đang hiển thị ở tiền trình.

---

## 📁 Cấu trúc thư mục (File Structure)

> Chi tiết giải thích kiến trúc và luồng xử lý của từng tệp mã nguồn xem tại: [FILE_STRUCTURE.md](FILE_STRUCTURE.md)

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

File APK sẽ nằm tại:
`app/build/outputs/apk/release/app-release.apk`

---

## 🛡️ Kiểm định an toàn & VirusTotal

OwlNetGuard hoàn toàn sạch mã độc, đạt chuẩn an toàn 100%:
* 🟢 **Báo cáo VirusTotal**: [Clean 100%](https://www.virustotal.com/gui/file/1a3c07b2bfe41cff5960704d9e199a2c0a1192a7348639f5d35de2aaa4a5b229)
* 🟢 **Google Play Protect**: Tuân thủ 100% tiêu chuẩn bảo mật của Android, hoạt động hoàn toàn cục bộ không thu thập dữ liệu người dùng.

---

## 📄 Bản quyền & Giấy phép

Phát triển bởi **NightOwlEyes**.
Phát hành theo giấy phép mã nguồn mở [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
Logo và đồ họa biểu tượng thuộc bản quyền của dự án OwlNetGuard.
