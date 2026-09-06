package dev.chedidandrew.stepupcamerasmoother.config;

import dev.chedidandrew.stepupcamerasmoother.motion.EasingCurve;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmootherConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmootherConfigTest.class);

    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsZeroThirtySevenAndOneHundredPercent() throws IOException {
        double[] strengths = {0.0D, 0.37D, 1.0D};

        for (int index = 0; index < strengths.length; index++) {
            double strength = strengths[index];
            Path configPath = temporaryDirectory.resolve("round-trip-" + index + ".json");
            SmootherConfig.Snapshot expected = new SmootherConfig.Snapshot(
                    false,
                    640,
                    EasingCurve.SMOOTHSTEP,
                    strength,
                    6.25D,
                    true,
                    true
            );

            assertTrue(SmootherConfig.save(configPath, expected, LOGGER));
            SmootherConfig.load(configPath, LOGGER);

            assertEquals(expected, SmootherConfig.get());
            assertTrue(Files.readString(configPath, StandardCharsets.UTF_8)
                    .contains("\"smoothing_strength\": " + strength));
        }
    }

    @Test
    void smoothnessCopyClampsBoundsAndNonFiniteValues() {
        SmootherConfig.Snapshot defaults = SmootherConfig.Snapshot.defaults();

        assertAll(
                () -> assertEquals(0.0D, defaults.withSmoothingStrength(-0.01D).smoothingStrength()),
                () -> assertEquals(0.0D, defaults.withSmoothingStrength(0.0D).smoothingStrength()),
                () -> assertEquals(0.37D, defaults.withSmoothingStrength(0.37D).smoothingStrength()),
                () -> assertEquals(1.0D, defaults.withSmoothingStrength(1.0D).smoothingStrength()),
                () -> assertEquals(1.0D, defaults.withSmoothingStrength(1.01D).smoothingStrength()),
                () -> assertEquals(1.0D, defaults.withSmoothingStrength(Double.NaN).smoothingStrength()),
                () -> assertEquals(
                        1.0D,
                        defaults.withSmoothingStrength(Double.POSITIVE_INFINITY).smoothingStrength()
                ),
                () -> assertEquals(
                        1.0D,
                        defaults.withSmoothingStrength(Double.NEGATIVE_INFINITY).smoothingStrength()
                )
        );
    }

    @Test
    void changingAndSavingSmoothnessPreservesEveryOtherField() {
        Path configPath = temporaryDirectory.resolve(SmootherConfig.FILE_NAME);
        SmootherConfig.Snapshot original = new SmootherConfig.Snapshot(
                false,
                731,
                EasingCurve.EXPONENTIAL,
                0.12D,
                7.75D,
                true,
                true
        );
        SmootherConfig.Snapshot changed = original.withSmoothingStrength(0.37D);

        assertAll(
                () -> assertEquals(original.enabled(), changed.enabled()),
                () -> assertEquals(
                        original.recoveryDurationMilliseconds(),
                        changed.recoveryDurationMilliseconds()
                ),
                () -> assertEquals(original.easing(), changed.easing()),
                () -> assertEquals(original.maximumCameraLag(), changed.maximumCameraLag()),
                () -> assertEquals(original.smoothThirdPerson(), changed.smoothThirdPerson()),
                () -> assertEquals(original.debugLogging(), changed.debugLogging())
        );

        assertTrue(SmootherConfig.save(configPath, changed, LOGGER));
        SmootherConfig.load(configPath, LOGGER);
        assertEquals(changed, SmootherConfig.get());
    }

    @Test
    void saveSanitizesInvalidSnapshotValuesBeforeWriting() {
        Path configPath = temporaryDirectory.resolve(SmootherConfig.FILE_NAME);
        SmootherConfig.Snapshot invalid = new SmootherConfig.Snapshot(
                true,
                Integer.MAX_VALUE,
                null,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                false,
                false
        );

        assertTrue(SmootherConfig.save(configPath, invalid, LOGGER));
        SmootherConfig.load(configPath, LOGGER);

        assertEquals(
                new SmootherConfig.Snapshot(
                        true,
                        1_000,
                        EasingCurve.SMOOTHERSTEP,
                        1.0D,
                        2.5D,
                        false,
                        false
                ),
                SmootherConfig.get()
        );
    }

    @Test
    void malformedLoadUsesDefaultsWithoutReplacingTheBadFile() throws IOException {
        Path configPath = temporaryDirectory.resolve(SmootherConfig.FILE_NAME);
        String malformed = "\"not-an-object\"";
        Files.writeString(configPath, malformed, StandardCharsets.UTF_8);

        SmootherConfig.load(configPath, LOGGER);

        assertEquals(SmootherConfig.Snapshot.defaults(), SmootherConfig.get());
        assertEquals(malformed, Files.readString(configPath, StandardCharsets.UTF_8));
    }

    @Test
    void failedSaveLeavesCurrentSnapshotAndExistingFileUntouched() throws IOException {
        Path configPath = temporaryDirectory.resolve(SmootherConfig.FILE_NAME);
        SmootherConfig.Snapshot original = new SmootherConfig.Snapshot(
                false,
                275,
                EasingCurve.LINEAR,
                0.37D,
                3.5D,
                true,
                true
        );
        assertTrue(SmootherConfig.save(configPath, original, LOGGER));
        String existingJson = Files.readString(configPath, StandardCharsets.UTF_8);

        assertFalse(SmootherConfig.save(configPath, null, LOGGER));

        assertEquals(original, SmootherConfig.get());
        assertEquals(existingJson, Files.readString(configPath, StandardCharsets.UTF_8));

        Path invalidTarget = temporaryDirectory.resolve("non-empty-directory");
        Files.createDirectory(invalidTarget);
        Path marker = invalidTarget.resolve("marker.txt");
        Files.writeString(marker, "keep", StandardCharsets.UTF_8);

        assertFalse(SmootherConfig.save(
                invalidTarget,
                original.withSmoothingStrength(0.9D),
                LOGGER
        ));

        assertEquals(original, SmootherConfig.get());
        assertEquals(existingJson, Files.readString(configPath, StandardCharsets.UTF_8));
        assertEquals("keep", Files.readString(marker, StandardCharsets.UTF_8));
    }
}
