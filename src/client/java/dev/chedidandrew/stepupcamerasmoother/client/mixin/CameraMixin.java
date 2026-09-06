package dev.chedidandrew.stepupcamerasmoother.client.mixin;

import dev.chedidandrew.stepupcamerasmoother.client.CameraMotion;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "alignWithEntity(F)V", at = @At("RETURN"))
    private void stepupCameraSmoother$applyVerticalOffset(
            float partialTicks,
            CallbackInfo callbackInfo
    ) {
        Camera camera = (Camera) (Object) this;
        double offset = CameraMotion.sample(camera, partialTicks);
        if (Math.abs(offset) <= 1.0E-6D) {
            return;
        }

        Vec3 position = camera.position();
        setPosition(position.x, position.y + offset, position.z);
    }
}
