package dev.applesiliconboost.mixin;

import dev.applesiliconboost.AdaptiveController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gọi bộ điều khiển một lần mỗi khung hình (không phải mỗi tick) để đo thời gian frame thật.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void applesiliconboost$onFrame(boolean advanceGameTime, CallbackInfo ci) {
        AdaptiveController.onFrame();
    }
}
