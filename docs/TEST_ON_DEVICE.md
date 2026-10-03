# Hướng Dẫn Kiểm Thử Trên Thiết Bị Thật (On-Device Test Guide)
Dự án: **Voice Filter: Speak Challenge (Nói Chuẩn)** — Phiên bản Android

Tài liệu này hướng dẫn chi tiết quy trình cài đặt, các kịch bản kiểm thử thực tế từ A0 đến A7 trên điện thoại Android vật lý, bộ lọc logcat chuyên dụng và bảng mã lỗi khắc phục sự cố.

---

## 1. Yêu Cầu Thiết Bị & Chuẩn Bị Môi Trường

- **Điện thoại Android:**
  - Hệ điều hành: Android 8.0 (API level 26) trở lên (Khuyến nghị Android 11+ / API 30+).
  - Phần cứng: Có Microphone vật lý, Camera trước hoạt động tốt.
  - Bộ nhớ trong khả dụng: Tối thiểu **300 MB** trống (để giải nén Vosk model và lưu video thử thách).
- **Máy tính điều khiển:**
  - Đã cài đặt Android SDK Platform-Tools (`adb`).
  - Kết nối cáp USB với điện thoại, bật **Tùy chọn cho nhà phát triển (Developer Options)** và **Gỡ lỗi qua USB (USB Debugging)**.

---

## 2. Quy Trình Cài Đặt Ứng Dụng Bằng ADB

### Bước 2.1: Kiểm tra kết nối thiết bị
Mở Terminal trên máy tính và chạy:
```bash
adb devices
```
*Kết quả hợp lệ sẽ hiển thị mã thiết bị kèm trạng thái `device` (Ví dụ: `R58M...    device`).*

### Bước 2.2: Cài đặt bản build APK
File APK Debug đã được build sẵn tại thư mục:
`app/build/outputs/apk/debug/app-debug.apk`

Thực hiện lệnh cài đặt:
```bash
adb install -r -d "app/build/outputs/apk/debug/app-debug.apk"
```
*(Tham số `-r`: cài đè giữ nguyên dữ liệu; `-d`: cho phép hạ cấp nếu cần).*

### Bước 2.3: Khởi chạy ứng dụng trực tiếp
```bash
adb shell am start -n com.antigravity.noichuan/.MainActivity
```

---

## 3. Danh Mục Các Kịch Bản Kiểm Thử Thực Tế (A0 - A7)

### Kịch bản 1: Khởi động lần đầu, Onboarding & Cấp quyền (A0, A1, A7)
1. Mở app lần đầu sau khi cài đặt.
2. Kiểm tra màn hình Splash hiển thị mượt mà không bị giật.
3. Vuốt qua 3 trang Onboarding giới thiệu tính năng.
4. Nhấn **"Bắt đầu ngay"** chuyển sang màn hình Cấp quyền.
5. Kiểm tra 2 quyền:
   - **Microphone:** Bấm cấp quyền -> Hộp thoại hệ thống hiện ra -> Chọn *"Trong khi dùng ứng dụng"*.
   - **Camera:** Thử từ chối Camera -> App phải chuyển sang chế độ nền gradient đồ họa an toàn mà không bị crash. Cấp lại Camera sau đó.

### Kịch bản 2: Trang chủ, Danh mục chủ đề & Dữ liệu mở rộng (A5)
1. Quan sát danh sách chủ đề: Colors, Animals, Vehicles, Kitchen, Countries, Weekdays, Months, Cinema, Music, Food, Travel, Slang & Brainrot.
2. Kiểm tra bộ từ vựng: Chọn chủ đề bất kỳ (ví dụ: *Slang & Brainrot* hoặc *Food & Cuisine*).
3. Xác nhận từ vựng được tải tức thì từ Room Database cache cục bộ (hoàn toàn không cần kết nối mạng).
4. Kiểm tra chuỗi streak và số sao tích lũy hiển thị chính xác ở thanh trên cùng.

### Kịch bản 3: Chế độ Voice Runner & Game State Machine (A3, A6)
1. Nhấn nút Camera tròn ở giữa thanh điều hướng để mở hộp thoại cấu hình.
2. Chọn chế độ **Voice Runner** với độ khó **Beginner (3.0s)**.
3. Nhấn **"Bắt Đầu Thử Thách"**:
   - Màn hình đếm ngược **3 - 2 - 1 - SẴN SÀNG!** xuất hiện.
   - Nhạc nền BGM vui tươi bắt đầu phát với âm lượng vừa phải.
   - Cổng 3D isometric xuất hiện cùng từ vựng tiếng Anh mục tiêu.
   - Thử phát âm đúng: Âm thanh `ding.wav` vang lên, viền neon chuyển xanh lá rực rỡ, hạt confetti rơi, điểm số tăng và combo x1.
   - Thử giữ im lặng hoặc phát âm sai: Hết 3 giây, cổng va chạm, âm thanh `buzz.wav` hoặc `crash.wav` vang lên, điện thoại rung nhẹ haptic feedback, mất 1 mạng (còn 2 mạng).
   - Tiếp tục đến khi mất hết 3 mạng -> Game over chuyển sang Màn hình Kết Quả.

### Kịch bản 4: Chế độ Tongue Twister & Category Rush (A4)
1. **Tongue Twister:**
   - Chọn chế độ *Tongue Twister*.
   - Đọc câu uốn lưỡi (Ví dụ: *"She sells seashells by the seashore"*).
   - Kiểm tra thanh đo độ trôi chảy (Fluency %) hiển thị ngay sau khi đọc xong.
2. **Category Rush:**
   - Chọn chế độ *Category Rush* (30 giây đếm ngược).
   - Đọc liên tục các từ thuộc chủ đề.
   - Thử đọc lại 1 từ đã nói: Kiểm tra dòng thông báo *"Từ '...' đã được nói rồi!"* và bộ đếm không bị cộng trùng lặp.

### Kịch bản 5: Ghép Timeline Audio & Màn Hình Kết Quả (A6)
1. Sau khi thử thách kết thúc, quan sát màn hình **Kết Quả (ResultScreen)**:
   - Đánh giá sao hoàng kim (1 - 3 sao) hiển thị tương ứng với số điểm.
   - Trình phát ExoPlayer tự động phát lại đoạn video đã quay kèm toàn bộ hiệu ứng AR.
   - **Âm thanh trong video phát lại:** Nghe kỹ để xác nhận tiếng người nói và tiếng chuông `ding/buzz` đã được trộn chính xác millisecond qua `TimelineAudioMixer`.
   - Bấm nút **"Lưu Video"**: Video được lưu vào thư mục `Movies/NoiChuan` của máy và lưu bản ghi vào Room Database.
   - Bấm nút **"Chia Sẻ"**: Mở Android Share Sheet để gửi video qua tin nhắn hoặc mạng xã hội.

### Kịch bản 6: Cài Đặt, Tùy Chỉnh Âm Thanh & Chuyển Đổi Engine (A7)
1. Tại Trang Chủ, bấm biểu tượng bánh răng **Settings** ở góc trên bên phải.
2. **Tắt SFX:** Chơi thử một màn -> Kiểm tra tiếng chuông không phát.
3. **Tắt BGM:** Nhạc nền tắt hoàn toàn.
4. **Chuyển Speech Engine:**
   - Mặc định là **Vosk Offline**: Không cần bật Wifi/4G, nhận diện mượt mà và bảo mật.
   - Chuyển sang **Google SpeechRecognizer**: Ứng dụng tự động điều chỉnh cơ chế mic (không mở PcmAudioRecorder) để Google Engine nhận diện độc quyền không bị xung đột.
5. **Kiểm tra Data Safety & Credits:**
   - Bấm mở xem thông tin minh bạch về an toàn dữ liệu và giấy phép mã nguồn mở.

---

## 4. Lệnh Lọc Logcat Theo Dõi Khi Test Trên Máy Thật

Mở cửa sổ Terminal thứ hai trên máy tính trong khi đang test trên điện thoại để bắt log trực tiếp:

### Lọc quá trình nhận diện giọng nói Vosk:
```bash
adb logcat -s VoskSpeechRecognizerEngine:V
```
*Log mong đợi: `Model loaded successfully`, `Hypothesis received: ...`, `Final result: ...`.*

### Lọc bộ trộn Timeline Audio và Muxer video:
```bash
adb logcat -s TimelineAudioMixer:D PostRecordAvMuxer:D ScreenRecordManager:D
```
*Log mong đợi: `Timeline has X events`, `Successfully mixed SFX at offset ...`, `Mux finished successfully`.*

### Lọc âm thanh SoundPool & BGM:
```bash
adb logcat -s SoundEffectManager:D PcmAudioRecorder:D
```

### Lọc toàn bộ log liên quan đến ứng dụng:
```bash
adb logcat | findstr /i "com.antigravity.noichuan"
```
*(Trên macOS/Linux dùng: `adb logcat | grep -i "com.antigravity.noichuan"`).*

---

## 5. Bảng Hướng Dẫn Xử Lý Lỗi (Troubleshooting)

| Hiện tượng | Nguyên nhân có thể | Cách xử lý |
| :--- | :--- | :--- |
| **Không nghe thấy giọng nói khi chơi** | Chưa cấp quyền Microphone hoặc quyền bị thu hồi. | Vào Settings trong app hoặc `Cài đặt Android > Ứng dụng > Nói Chuẩn > Quyền > Microphone` và bật lại. |
| **Báo lỗi dung lượng thấp khi bấm quay** | Bộ nhớ trống của thiết bị dưới 100MB. | Xóa bớt ứng dụng/ảnh rác trên máy để dung lượng khả dụng trên 100MB. |
| **Video không có tiếng chuông SFX** | Sự kiện xảy ra ngoài thời lượng ghi âm hoặc luồng muxer bị gián đoạn. | Kiểm tra logcat với tag `TimelineAudioMixer`. Hệ thống đã có cơ chế fallback tự động giữ nguyên âm thanh mic gốc. |
| **Google SpeechRecognizer báo lỗi mạng** | Thiết bị không có kết nối Internet khi bật Google Engine. | Kết nối Wifi/4G hoặc chuyển sang chế độ **Vosk Offline** trong màn hình Cài đặt để hoạt động không cần mạng. |
