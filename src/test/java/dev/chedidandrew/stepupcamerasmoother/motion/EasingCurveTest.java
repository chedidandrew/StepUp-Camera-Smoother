package dev.chedidandrew.stepupcamerasmoother.motion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasingCurveTest {
    @Test
    void everyCurveHasExactEndpointsAndIsMonotonic() {
        for (EasingCurve curve : EasingCurve.values()) {
            assertEquals(0.0D, curve.apply(0.0D), 1.0E-12D, curve.name());
            assertEquals(1.0D, curve.apply(1.0D), 1.0E-12D, curve.name());

            double previous = 0.0D;
            for (int index = 1; index <= 100; index++) {
                double current = curve.apply(index / 100.0D);
                assertTrue(current >= previous, curve.name());
                assertTrue(current >= 0.0D && current <= 1.0D, curve.name());
                previous = current;
            }
        }
    }

    @Test
    void parserIsCaseInsensitiveAndFallsBackSafely() {
        assertEquals(EasingCurve.LINEAR, EasingCurve.parse("linear"));
        assertEquals(EasingCurve.EXPONENTIAL, EasingCurve.parse("ExPoNeNtIaL"));
        assertEquals(EasingCurve.SMOOTHERSTEP, EasingCurve.parse("unknown"));
        assertEquals(EasingCurve.SMOOTHERSTEP, EasingCurve.parse(null));
    }
}
