package dev.chedidandrew.stepupcamerasmoother.client.mixin;

import dev.chedidandrew.stepupcamerasmoother.client.CameraMotion;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Unique
    private double stepupCameraSmoother$startX;

    @Unique
    private double stepupCameraSmoother$startY;

    @Unique
    private double stepupCameraSmoother$startZ;

    @Unique
    private boolean stepupCameraSmoother$startedOnGround;

    @Unique
    private boolean stepupCameraSmoother$tracking;

    @Inject(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD")
    )
    private void stepupCameraSmoother$captureMovement(
            MoverType moverType,
            Vec3 requestedMovement,
            CallbackInfo callbackInfo
    ) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        stepupCameraSmoother$tracking = CameraMotion.prepareMovement(
                player,
                moverType,
                requestedMovement
        );

        if (!stepupCameraSmoother$tracking) {
            return;
        }

        stepupCameraSmoother$startX = player.getX();
        stepupCameraSmoother$startY = player.getY();
        stepupCameraSmoother$startZ = player.getZ();
        stepupCameraSmoother$startedOnGround = player.onGround();
    }

    @Inject(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("RETURN")
    )
    private void stepupCameraSmoother$resolveMovement(
            MoverType moverType,
            Vec3 requestedMovement,
            CallbackInfo callbackInfo
    ) {
        if (!stepupCameraSmoother$tracking) {
            CameraMotion.finishRejectedMovement((LocalPlayer) (Object) this);
            return;
        }

        stepupCameraSmoother$tracking = false;
        CameraMotion.resolvedMovement(
                (LocalPlayer) (Object) this,
                requestedMovement,
                stepupCameraSmoother$startX,
                stepupCameraSmoother$startY,
                stepupCameraSmoother$startZ,
                stepupCameraSmoother$startedOnGround
        );
    }
}
