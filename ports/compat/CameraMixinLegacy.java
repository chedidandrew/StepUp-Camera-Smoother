package dev.chedidandrew.stepupcamerasmoother.client.mixin;

import dev.chedidandrew.stepupcamerasmoother.client.CameraMotion;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Apply correction to the initial pivot before vanilla third-person collision. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private float partialTickTime;

    @ModifyArgs(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void stepupCameraSmoother$applyVerticalOffset(Args args) {
        double offset = CameraMotion.sample((Camera)(Object)this, partialTickTime);
        args.set(1, (double) args.get(1) + offset);
    }
}
