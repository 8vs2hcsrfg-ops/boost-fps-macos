package dev.applesiliconboost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class AppleSiliconBoostClient implements ClientModInitializer {
    public static final String MOD_ID = "applesiliconboost";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        BoostConfig config = BoostConfig.load();

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean appleSilicon = os.contains("mac") && (arch.equals("aarch64") || arch.equals("arm64"));

        // Nếu chạy x86_64 qua Rosetta trên Mac ARM, thông báo cho người dùng vì đây là lỗi hiệu năng lớn nhất.
        if (os.contains("mac") && !appleSilicon) {
            LOGGER.warn("Đang chạy Java kiểu '{}' trên macOS. Hãy dùng JDK 25 bản aarch64 (native) để tránh Rosetta chậm ~2x.", arch);
        }

        boolean run = !config.onlyAppleSilicon || appleSilicon;
        AdaptiveController.init(config, run);
        LOGGER.info("Apple Silicon Boost: {} (os={}, arch={}, targetFps={})",
                run && config.enabled ? "BẬT" : "TẮT", os, arch, config.targetFps);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AdaptiveController.restore());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> AdaptiveController.restore());
    }
}
