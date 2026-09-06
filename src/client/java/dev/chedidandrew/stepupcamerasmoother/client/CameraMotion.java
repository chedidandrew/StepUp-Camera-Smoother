package dev.chedidandrew.stepupcamerasmoother.client;

import dev.chedidandrew.stepupcamerasmoother.config.SmootherConfig;
import dev.chedidandrew.stepupcamerasmoother.motion.CameraSmoothingState;
import dev.chedidandrew.stepupcamerasmoother.motion.StepDetector;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Bridges resolved local-player movement to the render-only smoothing state. */
public final class CameraMotion {
    private static final CameraSmoothingState STATE = new CameraSmoothingState();
    private static final double RESET_VERTICAL_DISTANCE = 1.0D / 128.0D;
    private static final double MAXIMUM_HORIZONTAL_DISTANCE_SQUARED = 16.0D;

    private static LocalPlayer trackedPlayer;
    private static Object trackedLevel;
    private static double lastObservedX;
    private static double lastObservedY;
    private static double lastObservedZ;
    private static boolean hasObservedPosition;

    private CameraMotion() {
    }

    public static boolean prepareMovement(
            LocalPlayer player,
            MoverType moverType,
            Vec3 requestedMovement
    ) {
        ensureContext(player);
        resetForExternalPositionChange(player);

        SmootherConfig.Snapshot config = SmootherConfig.get();
        boolean shouldTrack = config.enabled()
                && moverType == MoverType.SELF
                && requestedMovement.y <= StepDetector.COLLISION_MARGIN
                && player.onGround()
                && isEligiblePlayerState(player);
        if (!shouldTrack) {
            STATE.reset();
        }
        return shouldTrack;
    }

    public static void resolvedMovement(
            LocalPlayer player,
            Vec3 requestedMovement,
            double startX,
            double startY,
            double startZ,
            boolean startedOnGround
    ) {
        try {
            ensureContext(player);

            if (!isEligiblePlayerState(player) || !player.onGround()) {
                STATE.reset();
                return;
            }

            double actualX = player.getX() - startX;
            double actualRise = player.getY() - startY;
            double actualZ = player.getZ() - startZ;
            double actualHorizontalDistanceSquared = actualX * actualX + actualZ * actualZ;

            if (!StepDetector.isUpwardCollisionStep(
                    requestedMovement.y,
                    requestedMovement.horizontalDistanceSqr(),
                    actualRise,
                    actualHorizontalDistanceSquared,
                    player.maxUpStep(),
                    startedOnGround,
                    player.onGround()
            )) {
                if (Math.abs(actualRise) > RESET_VERTICAL_DISTANCE
                        || actualHorizontalDistanceSquared > MAXIMUM_HORIZONTAL_DISTANCE_SQUARED) {
                    STATE.reset();
                }
                return;
            }

            SmootherConfig.Snapshot config = SmootherConfig.get();
            STATE.addStep(actualRise, player.tickCount, config.smoothingStrength());

            if (config.debugLogging()) {
                StepUpCameraSmootherClient.LOGGER.info(
                        "Detected upward collision step: rise={}, stepHeight={}, tick={}",
                        actualRise,
                        player.maxUpStep(),
                        player.tickCount
                );
            }
        } finally {
            observePosition(player);
        }
    }

    public static void finishRejectedMovement(LocalPlayer player) {
        ensureContext(player);
        observePosition(player);
    }

    public static double sample(Camera camera, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        SmootherConfig.Snapshot config = SmootherConfig.get();

        if (player == null || camera.entity() != player) {
            reset();
            return 0.0D;
        }

        ensureContext(player);
        resetForExternalPositionChange(player);
        if (!config.enabled() || !player.onGround() || !isEligiblePlayerState(player)) {
            STATE.reset();
            observePosition(player);
            return 0.0D;
        }

        if (camera.isDetached() && !config.smoothThirdPerson()) {
            STATE.reset();
            return 0.0D;
        }

        double clampedPartialTicks = Math.max(0.0D, Math.min(1.0D, partialTicks));
        return STATE.sample(
                player.tickCount + clampedPartialTicks,
                config.recoveryDurationMilliseconds(),
                config.easing(),
                config.maximumCameraLag()
        );
    }

    public static void reset() {
        trackedPlayer = null;
        trackedLevel = null;
        hasObservedPosition = false;
        STATE.reset();
    }

    private static void ensureContext(LocalPlayer player) {
        Object level = player.level();
        if (trackedPlayer != player || trackedLevel != level) {
            STATE.reset();
            trackedPlayer = player;
            trackedLevel = level;
            observePosition(player);
        }
    }

    private static void resetForExternalPositionChange(LocalPlayer player) {
        if (hasObservedPosition
                && (Math.abs(player.getX() - lastObservedX) > 1.0E-7D
                || Math.abs(player.getY() - lastObservedY) > 1.0E-7D
                || Math.abs(player.getZ() - lastObservedZ) > 1.0E-7D)) {
            STATE.reset();
            observePosition(player);
        }
    }

    private static void observePosition(LocalPlayer player) {
        lastObservedX = player.getX();
        lastObservedY = player.getY();
        lastObservedZ = player.getZ();
        hasObservedPosition = true;
    }

    private static boolean isEligiblePlayerState(LocalPlayer player) {
        return player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping()
                && !player.isSwimming()
                && !player.isInWater()
                && !player.isInLava()
                && !player.isFallFlying()
                && !player.onClimbable()
                && !player.getAbilities().flying;
    }
}
