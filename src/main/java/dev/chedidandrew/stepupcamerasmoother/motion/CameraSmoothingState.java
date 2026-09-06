package dev.chedidandrew.stepupcamerasmoother.motion;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds short-lived visual transitions. Game state is never changed by this
 * class. A transition first cancels vanilla's one-tick position interpolation,
 * then eases the camera offset back to zero.
 */
public final class CameraSmoothingState {
    private static final int MAXIMUM_TRANSITIONS = 32;
    private static final double TICK_MILLISECONDS = 50.0D;
    private static final double TIME_EPSILON = 1.0E-6D;

    private final Deque<Transition> transitions = new ArrayDeque<>();
    private double lastSampleTime = Double.NaN;

    public void addStep(double height, long startTick, double strength) {
        if (!Double.isFinite(height) || !Double.isFinite(strength)
                || height <= 0.0D || strength <= 0.0D) {
            return;
        }

        double compensatedHeight = height * Math.min(strength, 1.0D);
        transitions.addLast(new Transition(compensatedHeight, startTick));
        while (transitions.size() > MAXIMUM_TRANSITIONS) {
            transitions.removeFirst();
        }
    }

    /**
     * Samples the world-Y camera offset at a game time measured in ticks.
     * The returned value is always finite, non-positive, and bounded by the
     * configured maximum camera lag.
     */
    public double sample(
            double gameTimeTicks,
            int recoveryDurationMilliseconds,
            EasingCurve easing,
            double maximumCameraLag
    ) {
        if (!Double.isFinite(gameTimeTicks)
                || !Double.isFinite(maximumCameraLag)
                || maximumCameraLag <= 0.0D
                || recoveryDurationMilliseconds <= 0
                || easing == null) {
            reset();
            return 0.0D;
        }

        if (Double.isFinite(lastSampleTime)
                && gameTimeTicks + TIME_EPSILON < lastSampleTime) {
            reset();
            return 0.0D;
        }
        lastSampleTime = gameTimeTicks;

        double recoveryTicks = recoveryDurationMilliseconds / TICK_MILLISECONDS;
        double finishedAge = 1.0D + recoveryTicks;
        transitions.removeIf(transition -> gameTimeTicks - transition.startTick() >= finishedAge);

        double offset = 0.0D;
        for (Transition transition : transitions) {
            offset += transition.offsetAt(gameTimeTicks, recoveryTicks, easing);
        }

        if (!Double.isFinite(offset)) {
            reset();
            return 0.0D;
        }

        return Math.max(-maximumCameraLag, Math.min(0.0D, offset));
    }

    public void reset() {
        transitions.clear();
        lastSampleTime = Double.NaN;
    }

    public int activeTransitionCount() {
        return transitions.size();
    }

    private record Transition(double height, long startTick) {
        private double offsetAt(double gameTimeTicks, double recoveryTicks, EasingCurve easing) {
            double age = gameTimeTicks - startTick;
            if (age <= 0.0D) {
                return 0.0D;
            }

            // Vanilla interpolates the entity from the old Y to its new Y over
            // this first tick. Mirror that interpolation in the other direction.
            if (age < 1.0D) {
                return -height * age;
            }

            double recoveryProgress = (age - 1.0D) / recoveryTicks;
            if (recoveryProgress >= 1.0D) {
                return 0.0D;
            }

            return -height * (1.0D - easing.apply(recoveryProgress));
        }
    }
}
