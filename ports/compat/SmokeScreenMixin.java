package dev.chedidandrew.stepupcamerasmoother.smoke;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Diagnostics for legacy loader startup screens, excluded from playable artifacts. */
@Mixin(Minecraft.class)
public abstract class SmokeScreenMixin {
    @Unique private String stepup$lastScreen;
    @Inject(method = "tick", at = @At("RETURN"))
    private void stepup$screen(CallbackInfo ci) {
        var screen = Minecraft.getInstance().screen;
        String name = screen == null ? "null" : screen.getClass().getName();
        if (!name.equals(stepup$lastScreen)) {
            stepup$lastScreen = name;
            System.out.println("STEPUP_SMOKE_SCREEN: " + name);
        }
    }
}
