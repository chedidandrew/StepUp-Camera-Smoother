package dev.chedidandrew.stepupcamerasmoother.motion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraSmoothingStateTest {
    @Test
    void cancelsTheInitialStepInterpolationThenRecovers() {
        CameraSmoothingState state = new CameraSmoothingState();
        state.addStep(1.0D, 100L, 1.0D);

        assertEquals(0.0D, state.sample(100.0D, 200, EasingCurve.SMOOTHERSTEP, 2.5D), 1.0E-12D);
        assertEquals(-0.5D, state.sample(100.5D, 200, EasingCurve.SMOOTHERSTEP, 2.5D), 1.0E-12D);
        assertEquals(-1.0D, state.sample(101.0D, 200, EasingCurve.SMOOTHERSTEP, 2.5D), 1.0E-12D);
        assertEquals(-0.5D, state.sample(103.0D, 200, EasingCurve.SMOOTHERSTEP, 2.5D), 1.0E-12D);
        assertEquals(0.0D, state.sample(105.0D, 200, EasingCurve.SMOOTHERSTEP, 2.5D), 1.0E-12D);
        assertEquals(0, state.activeTransitionCount());
    }

    @Test
    void accumulatesRapidStepsButHonorsTheCameraLagLimit() {
        CameraSmoothingState state = new CameraSmoothingState();
        state.addStep(1.25D, 20L, 1.0D);
        state.addStep(1.25D, 20L, 1.0D);

        assertEquals(-1.5D, state.sample(21.0D, 180, EasingCurve.LINEAR, 1.5D), 1.0E-12D);
    }

    @Test
    void strengthScalesOnlyTheVisualCompensation() {
        CameraSmoothingState state = new CameraSmoothingState();
        state.addStep(1.0D, 10L, 0.5D);

        assertEquals(-0.5D, state.sample(11.0D, 200, EasingCurve.LINEAR, 2.5D), 1.0E-12D);
    }

    @Test
    void resetAndBackwardTimeReturnToNeutral() {
        CameraSmoothingState state = new CameraSmoothingState();
        state.addStep(1.0D, 100L, 1.0D);
        state.sample(101.0D, 200, EasingCurve.LINEAR, 2.5D);

        assertEquals(0.0D, state.sample(99.0D, 200, EasingCurve.LINEAR, 2.5D), 1.0E-12D);
        assertEquals(0, state.activeTransitionCount());

        state.addStep(1.0D, 200L, 1.0D);
        state.reset();
        assertEquals(0.0D, state.sample(201.0D, 200, EasingCurve.LINEAR, 2.5D), 1.0E-12D);
    }
}
