package dev.chedidandrew.stepupcamerasmoother.client.mixin;

import dev.chedidandrew.stepupcamerasmoother.client.CameraMotion;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Apply correction to the initial pivot before vanilla third-person collision. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Unique private float stepupCameraSmoother$partialTick;

    @Inject(method = "setup", at = @At("HEAD"))
    private void stepupCameraSmoother$capturePartialTick(BlockGetter level, Entity entity,
            boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        stepupCameraSmoother$partialTick = partialTick;
    }

    @ModifyArg(method = "setup", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private double stepupCameraSmoother$applyVerticalOffset(double cameraY) {
        double offset = CameraMotion.sample((Camera)(Object)this, stepupCameraSmoother$partialTick);
        return cameraY + offset;
    }
}
