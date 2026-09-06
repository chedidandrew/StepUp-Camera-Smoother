package dev.chedidandrew.stepupcamerasmoother.motion;

import java.util.Locale;

/** Curves used while the camera catches up to the player's stepped position. */
public enum EasingCurve {
    LINEAR,
    SMOOTHSTEP,
    SMOOTHERSTEP,
    EXPONENTIAL;

    private static final double EXPONENTIAL_STEEPNESS = 5.0D;
    private static final double EXPONENTIAL_NORMALIZER = 1.0D - Math.exp(-EXPONENTIAL_STEEPNESS);

    /**
     * Maps progress in the inclusive range 0 to 1 to eased progress in the
     * same range. Input outside that range is clamped.
     */
    public double apply(double progress) {
        double value = clamp01(progress);
        return switch (this) {
            case LINEAR -> value;
            case SMOOTHSTEP -> value * value * (3.0D - 2.0D * value);
            case SMOOTHERSTEP -> value * value * value
                    * (value * (value * 6.0D - 15.0D) + 10.0D);
            case EXPONENTIAL -> (1.0D - Math.exp(-EXPONENTIAL_STEEPNESS * value))
                    / EXPONENTIAL_NORMALIZER;
        };
    }

    public static EasingCurve parse(String value) {
        if (value == null || value.isBlank()) {
            return SMOOTHERSTEP;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return SMOOTHERSTEP;
        }
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0.0D;
        }
        return Math.min(value, 1.0D);
    }
}
