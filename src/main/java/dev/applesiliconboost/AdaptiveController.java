package dev.applesiliconboost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;

/**
 * Bộ điều khiển chất lượng thích ứng.
 *
 * Đo thời gian mỗi khung hình, làm mượt bằng trung bình trượt mũ, rồi tăng/giảm "mức giảm chất lượng"
 * (0 = nguyên bản của người chơi, tối đa = MAX_LEVEL). Giá trị gốc của người chơi được lưu lại và
 * khôi phục khi thoát thế giới hoặc tắt game, nên không ghi đè vĩnh viễn options.txt.
 */
public final class AdaptiveController {
    private static final int MAX_LEVEL = 5;

    private static BoostConfig config;
    private static boolean active;

    private static long lastFrameNs;
    private static double avgFrameMs = 16.7;
    private static long lastAdjustNs;
    private static int level;

    private static Baseline baseline;

    private record Baseline(int renderDistance, int simulationDistance, double entityScale, ParticleStatus particles) {}

    private AdaptiveController() {}

    public static void init(BoostConfig cfg, boolean shouldRun) {
        config = cfg;
        active = shouldRun && cfg.enabled;
    }

    public static boolean isActive() {
        return active;
    }

    /** Gọi mỗi khung hình từ MinecraftMixin. */
    public static void onFrame() {
        if (!active) return;

        long now = System.nanoTime();
        if (lastFrameNs != 0) {
            double dtMs = (now - lastFrameNs) / 1_000_000.0;
            // Bỏ qua các khung cực dài (nạp thế giới, alt-tab...) để không làm lệch trung bình.
            if (dtMs < 250.0) {
                avgFrameMs += (dtMs - avgFrameMs) * config.smoothing;
            }
        }
        lastFrameNs = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return; // chỉ điều chỉnh khi đang trong thế giới
        }
        if (!mc.isWindowActive()) {
            return; // cửa sổ không focus: FPS bị hạ nên không phản ứng
        }
        if (now - lastAdjustNs < config.cooldownSeconds * 1_000_000_000L) {
            return;
        }

        double targetMs = 1000.0 / config.targetFps;
        if (avgFrameMs > targetMs * 1.15 && level < MAX_LEVEL) {
            setLevel(mc, level + 1);
            lastAdjustNs = now;
        } else if (avgFrameMs < targetMs * 0.60 && level > 0) {
            setLevel(mc, level - 1);
            lastAdjustNs = now;
        }
    }

    private static void setLevel(Minecraft mc, int newLevel) {
        Options o = mc.options;
        if (baseline == null) {
            baseline = new Baseline(
                    o.renderDistance().get(),
                    o.simulationDistance().get(),
                    o.entityDistanceScaling().get(),
                    o.particles().get());
        }
        level = newLevel;
        apply(o, level);
        AppleSiliconBoostClient.LOGGER.info("Mức tối ưu {} (frame trung bình {} ms)", level, String.format("%.1f", avgFrameMs));
    }

    private static void apply(Options o, int lvl) {
        Baseline b = baseline;
        if (b == null) return;

        ParticleStatus particles = b.particles();
        double entityScale = b.entityScale();
        int rd = b.renderDistance();
        int sd = b.simulationDistance();

        if (lvl >= 1) {
            if (particles == ParticleStatus.ALL) particles = ParticleStatus.DECREASED;
            entityScale = Math.min(entityScale, Math.max(0.5, b.entityScale() * 0.75));
        }
        if (lvl >= 2) rd = b.renderDistance() - 2;
        if (lvl >= 3) {
            sd = b.simulationDistance() - 2;
            entityScale = Math.min(entityScale, Math.max(0.5, b.entityScale() * 0.5));
        }
        if (lvl >= 4) {
            rd = b.renderDistance() - 4;
            particles = ParticleStatus.MINIMAL;
        }
        if (lvl >= 5) rd = b.renderDistance() - 6;

        // Không bao giờ vượt quá giá trị gốc và không thấp hơn giới hạn trong config.
        rd = clamp(rd, Math.min(config.minRenderDistance, b.renderDistance()), b.renderDistance());
        sd = clamp(sd, Math.min(config.minSimulationDistance, b.simulationDistance()), b.simulationDistance());

        setIfChanged(o.renderDistance(), rd);
        setIfChanged(o.simulationDistance(), sd);
        setIfChanged(o.entityDistanceScaling(), entityScale);
        setIfChanged(o.particles(), particles);
    }

    private static <T> void setIfChanged(net.minecraft.client.OptionInstance<T> opt, T value) {
        if (!opt.get().equals(value)) {
            opt.set(value);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Khôi phục cài đặt gốc (thoát thế giới / tắt game). */
    public static void restore() {
        if (baseline != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null) {
                level = 0;
                apply(mc.options, 0);
            }
        }
        baseline = null;
        level = 0;
        lastAdjustNs = 0;
        lastFrameNs = 0;
        avgFrameMs = 1000.0 / (config != null ? config.targetFps : 60);
    }
}
