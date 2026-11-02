package dev.applesiliconboost;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cấu hình lưu tại config/applesiliconboost.json */
public final class BoostConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Bật/tắt toàn bộ mod. */
    public boolean enabled = true;

    /** true = chỉ chạy trên macOS + chip ARM (Apple Silicon). Đặt false để thử trên máy khác. */
    public boolean onlyAppleSilicon = true;

    /** FPS mục tiêu. Nếu FPS trung bình thấp hơn đáng kể, mod sẽ giảm chất lượng từng bước. */
    public int targetFps = 60;

    /** Render distance tối thiểu mà mod được phép giảm xuống. */
    public int minRenderDistance = 6;

    /** Simulation distance tối thiểu mà mod được phép giảm xuống. */
    public int minSimulationDistance = 5;

    /** Thời gian chờ giữa hai lần điều chỉnh (giây), tránh dao động liên tục. */
    public int cooldownSeconds = 8;

    /** Hệ số làm mượt FPS (0..1). Nhỏ hơn = ổn định hơn nhưng phản ứng chậm hơn. */
    public double smoothing = 0.05;

    public static BoostConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("applesiliconboost.json");
        BoostConfig cfg = new BoostConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), BoostConfig.class);
                if (cfg == null) cfg = new BoostConfig();
            }
            Files.writeString(path, GSON.toJson(cfg)); // ghi lại để có đủ các trường mới
        } catch (IOException | RuntimeException e) {
            AppleSiliconBoostClient.LOGGER.warn("Không đọc/ghi được config, dùng mặc định", e);
        }
        cfg.targetFps = Math.max(20, cfg.targetFps);
        cfg.minRenderDistance = Math.max(2, cfg.minRenderDistance);
        cfg.minSimulationDistance = Math.max(5, cfg.minSimulationDistance);
        cfg.cooldownSeconds = Math.max(2, cfg.cooldownSeconds);
        cfg.smoothing = Math.min(0.5, Math.max(0.005, cfg.smoothing));
        return cfg;
    }
}
