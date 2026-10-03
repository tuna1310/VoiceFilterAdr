# 🎙️ Nói Chuẩn: Thử Thách Phát Âm

> Ứng dụng luyện phát âm tiếng Anh kết hợp AR filter - bản Android độc lập lấy cảm hứng từ Voice Filter: Speak Challenge (iOS).

---

## 📱 Tính năng chính

- 📸 **Camera AR**: Camera trước toàn màn hình + hiệu ứng đường chạy isometric 3D màu xanh chanh
- 🦊 **Nhân vật hoạt hình**: Linh vật cáo lập phương chạy và vượt cổng từ vựng
- 🎤 **Nhận diện giọng nói**: Android SpeechRecognizer nhận diện từ người chơi đọc
- 🎉 **Hiệu ứng ăn mừng (Confetti & Glow)**: Tung pháo giấy Confetti, cổng phát sáng neon lime và banner "+10đ" khi phát âm chuẩn
- 🏷️ **Watermark bản quyền**: Huy hiệu "Nói Chuẩn: Thử Thách Phát Âm" tự động đóng vào video
- 🎬 **Quay video AR chuẩn app gốc**: Ghi hình toàn bộ màn hình AR (Camera + 3D Runway + Linh vật + Cổng + Watermark) qua MediaProjection, tự động fallback CameraX
- 📺 **Trình phát video ExoPlayer**: Xem lại video vừa quay ngay trên màn hình kết quả
- 💾 **Lưu thư viện thiết bị & Chia sẻ**: Xuất video vào Movies/NoiChuan và chia sẻ file MP4 trực tiếp qua FileProvider
- 🗂️ **8 Chủ đề**: Colors, Car Brands, Weekdays, Months, Animals, Vehicles, Kitchen, Countries
- 📊 **3 Cấp độ**: Beginner / Intermediate / Expert × 15 từ/cấp/chủ đề
- 🔖 **Bookmark**: Đánh dấu chủ đề yêu thích (DataStore)
- 💎 **Premium screen**: Paywall Yearly/Weekly mô phỏng cấu trúc app thật

---

## 🏗️ Kiến trúc

MVVM + Clean Architecture
- **ui/** — Jetpack Compose screens & components
- **domain/** — Interfaces: Repository, Service
- **data/** — Implementations: Room, DataStore, Asset, SpeechRecognizer

---

## 🛠️ Công nghệ sử dụng

| Thành phần | Phiên bản |
|---|---|
| Kotlin | 2.0.21 |
| Jetpack Compose + Material 3 | Stable |
| Navigation Compose | - |
| Koin DI | 4.0.0 |
| Room | 2.6.1 |
| KSP | 2.0.21-1.0.28 |
| DataStore Preferences | - |
| Kotlinx Serialization | - |
| CameraX | 1.4.1 |
| Coroutines + StateFlow | - |
| minSdk | 26 (Android 8.0) |
| targetSdk | 35 (Android 15) |
| JDK | 17 |

---

## 🚀 Build & Chạy

`ash
# Build APK Debug
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# Cài lên thiết bị
adb install app/build/outputs/apk/debug/app-debug.apk

# Unit Tests
./gradlew testDebugUnitTest
`

---

## 🗺️ Luồng màn hình

Splash → Onboarding → PermissionScreen → HomeScreen → SubjectsScreen → SubjectDetailScreen → ChallengeScreen → ResultScreen
                                                                                              → ProfileScreen
                                                                                              → PremiumScreen

---

## ⚖️ Bản quyền

Toàn bộ đồ hoạ được vẽ bằng Jetpack Compose Canvas (procedural - không sao chép tài sản có bản quyền).
