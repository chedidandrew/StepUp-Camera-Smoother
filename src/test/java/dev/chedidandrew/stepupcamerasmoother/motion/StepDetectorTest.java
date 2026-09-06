package dev.chedidandrew.stepupcamerasmoother.motion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StepDetectorTest {
    @Test
    void detectsFullBlockStepWithStepItUpHeight() {
        assertTrue(StepDetector.isUpwardCollisionStep(
                -0.0784D,
                0.01D,
                1.0D,
                0.01D,
                1.25D,
                true,
                true
        ));
    }

    @Test
    void detectsVanillaSlabAndStairSteps() {
        assertTrue(StepDetector.isUpwardCollisionStep(
                -0.0784D,
                0.02D,
                0.5D,
                0.015D,
                0.6D,
                true,
                true
        ));
    }

    @Test
    void rejectsJumpingAndOrdinaryVerticalMotion() {
        assertFalse(StepDetector.isUpwardCollisionStep(
                0.42D,
                0.02D,
                0.42D,
                0.02D,
                1.25D,
                true,
                true
        ));
        assertFalse(StepDetector.isUpwardCollisionStep(
                -0.0784D,
                0.02D,
                -0.0784D,
                0.02D,
                1.25D,
                true,
                true
        ));
    }

    @Test
    void rejectsAirborneStationaryAndExcessiveMovement() {
        assertFalse(StepDetector.isUpwardCollisionStep(
                -0.0784D, 0.02D, 0.5D, 0.02D, 0.6D, false, true
        ));
        assertFalse(StepDetector.isUpwardCollisionStep(
                -0.0784D, 0.0D, 0.5D, 0.0D, 0.6D, true, true
        ));
        assertFalse(StepDetector.isUpwardCollisionStep(
                -0.0784D, 0.02D, 4.0D, 0.02D, 1.25D, true, true
        ));
        assertFalse(StepDetector.isUpwardCollisionStep(
                -0.0784D, 0.02D, 0.5D, 0.02D, 0.6D, true, false
        ));
    }

    @Test
    void rejectsNonFiniteInput() {
        assertFalse(StepDetector.isUpwardCollisionStep(
                Double.NaN, 0.02D, 0.5D, 0.02D, 0.6D, true, true
        ));
    }
}
