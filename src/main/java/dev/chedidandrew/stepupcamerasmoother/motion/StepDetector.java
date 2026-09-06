package dev.chedidandrew.stepupcamerasmoother.motion;

/** Pure collision-step detection, separated from Minecraft for direct tests. */
public final class StepDetector {
    static final double MINIMUM_RISE = 1.0D / 128.0D;
    public static final double COLLISION_MARGIN = 0.01D;
    static final double STEP_HEIGHT_TOLERANCE = 1.0D / 16.0D;
    static final double MINIMUM_HORIZONTAL_DISTANCE_SQUARED = 1.0E-8D;

    private StepDetector() {
    }

    /**
     * Returns true only when resolved collision movement raised a grounded
     * player more than the requested vertical movement.
     */
    public static boolean isUpwardCollisionStep(
            double requestedVertical,
            double requestedHorizontalDistanceSquared,
            double actualRise,
            double actualHorizontalDistanceSquared,
            double effectiveStepHeight,
            boolean startedOnGround,
            boolean endedOnGround
    ) {
        if (!allFinite(
                requestedVertical,
                requestedHorizontalDistanceSquared,
                actualRise,
                actualHorizontalDistanceSquared,
                effectiveStepHeight
        )) {
            return false;
        }

        if (!startedOnGround || !endedOnGround || requestedVertical > COLLISION_MARGIN) {
            return false;
        }

        if (requestedHorizontalDistanceSquared <= MINIMUM_HORIZONTAL_DISTANCE_SQUARED
                || actualHorizontalDistanceSquared <= MINIMUM_HORIZONTAL_DISTANCE_SQUARED) {
            return false;
        }

        if (actualRise <= MINIMUM_RISE
                || actualRise <= requestedVertical + COLLISION_MARGIN) {
            return false;
        }

        return effectiveStepHeight > 0.0D
                && actualRise <= effectiveStepHeight + STEP_HEIGHT_TOLERANCE;
    }

    private static boolean allFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}
