# Apple Silicon Boost (Fabric, Minecraft 26.3)

Mod client-side giữ FPS ổn định trên Mac chip Apple Silicon bằng cách **tự động điều chỉnh chất lượng theo FPS thực tế**.

## Nó làm gì

Mỗi khung hình mod đo thời gian frame (trung bình trượt mũ). Khi FPS thấp hơn mục tiêu, mod giảm dần theo 5 mức:

| Mức | Thay đổi |
|---|---|
| 1 | Particles → Decreased, entity distance ×0.75 |
| 2 | Render distance −2 |
| 3 | Simulation distance −2, entity distance ×0.5 |
| 4 | Render distance −4, Particles → Minimal |
| 5 | Render distance −6 |

Khi FPS dư dả, mod tăng lại từng mức. Giá trị gốc của bạn được lưu và **khôi phục khi thoát thế giới hoặc tắt game**, không ghi đè `options.txt`.

Chỉ chạy trên macOS + ARM64 (đổi `onlyAppleSilicon` trong config để thử máy khác). Mod cũng cảnh báo trong log nếu Java đang chạy x86_64 qua Rosetta.

## Cấu hình: `config/applesiliconboost.json`

`targetFps` (60), `minRenderDistance` (6), `minSimulationDistance` (5), `cooldownSeconds` (8), `smoothing` (0.05), `enabled`, `onlyAppleSilicon`.

## Build

Cần JDK 25 (bản **aarch64**) và Gradle 9.6+.

```bash
brew install --cask temurin@25    # hoặc Zulu/Corretto aarch64
brew install gradle
cd apple-silicon-boost
gradle wrapper --gradle-version 9.6.0
./gradlew build
# jar nằm ở build/libs/apple-silicon-boost-1.0.0.jar
```

Chép jar vào `mods/` cùng Fabric API 0.165.0+26.3 (hoặc mới hơn cho 26.3).

> Source chưa được biên dịch thử trong môi trường tạo ra nó. Tên một số lớp Mojang (`Options`, `OptionInstance`, `ParticleStatus`, `Minecraft.runTick`, `isWindowActive`) và plugin Loom có thể lệch nhỏ ở 26.3; nếu build lỗi, gửi log cho mình để sửa.

## Tăng FPS thật sự nhiều: kết hợp thêm

Mod này chỉ điều chỉnh cài đặt. Phần tăng FPS lớn nhất đến từ việc viết lại renderer, và việc đó đã có Sodium (có bản Fabric 26.3). Nên dùng cùng Sodium; các mod như Lithium, FerriteCore, ImmediatelyFast, Entity Culling thì hãy kiểm tra trên Modrinth xem đã có bản 26.3 chưa.

## Tinh chỉnh Mac

1. **Dùng JDK 25 native aarch64.** Bản x86_64 chạy qua Rosetta chậm hơn rõ rệt. Kiểm tra: `java -XshowSettings:properties -version 2>&1 | grep os.arch` phải ra `aarch64`.
2. **JVM args**: dán nội dung `launch/jvm-args-apple-silicon.txt` vào launcher (chỉnh `-Xmx` theo RAM; 4G cho máy 8GB, 6–8G cho 16GB trở lên, đừng cấp quá nửa RAM).
3. **Trong game**: bật VSync nếu thấy xé hình; giới hạn FPS bằng tần số quét (60/120 cho ProMotion) để máy mát và pin lâu hơn; giảm Biome Blend, Mipmap, Clouds.
4. **macOS**: cắm sạc, tắt Low Power Mode, đóng Chrome/ứng dụng nặng; chạy Minecraft ở chế độ cửa sổ thay vì fullscreen nếu bị giật khi alt-tab.
