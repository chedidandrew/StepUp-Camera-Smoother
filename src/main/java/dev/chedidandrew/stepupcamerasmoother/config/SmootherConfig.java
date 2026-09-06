package dev.chedidandrew.stepupcamerasmoother.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import dev.chedidandrew.stepupcamerasmoother.motion.EasingCurve;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Dependency-free, restart-to-reload JSON configuration. */
public final class SmootherConfig {
    public static final String FILE_NAME = "stepup-camera-smoother.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MINIMUM_DURATION_MILLISECONDS = 50;
    private static final int MAXIMUM_DURATION_MILLISECONDS = 1_000;
    private static final double MINIMUM_CAMERA_LAG = 0.25D;
    private static final double MAXIMUM_CAMERA_LAG = 8.0D;

    private static volatile Snapshot current = Snapshot.defaults();

    private SmootherConfig() {
    }

    public static Snapshot get() {
        return current;
    }

    public static void load(Logger logger) {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        ConfigData data = new ConfigData();

        if (Files.notExists(configPath)) {
            current = data.toSnapshot();
            writeDefaults(configPath, data, logger);
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            ConfigData parsed = GSON.fromJson(reader, ConfigData.class);
            if (parsed == null) {
                throw new IllegalArgumentException("configuration root is null");
            }
            current = parsed.toSnapshot();
        } catch (IOException | RuntimeException exception) {
            current = Snapshot.defaults();
            logger.error(
                    "Could not read {}. Safe defaults will be used until the next restart.",
                    configPath,
                    exception
            );
        }
    }

    private static void writeDefaults(Path configPath, ConfigData data, Logger logger) {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(
                    configPath,
                    GSON.toJson(data) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            logger.warn("Could not create default configuration at {}.", configPath, exception);
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Snapshot(
            boolean enabled,
            int recoveryDurationMilliseconds,
            EasingCurve easing,
            double smoothingStrength,
            double maximumCameraLag,
            boolean smoothThirdPerson,
            boolean debugLogging
    ) {
        private static Snapshot defaults() {
            return new ConfigData().toSnapshot();
        }
    }

    private static final class ConfigData {
        private boolean enabled = true;

        @SerializedName("recovery_duration_ms")
        private int recoveryDurationMilliseconds = 180;

        private String easing = "smootherstep";

        @SerializedName("smoothing_strength")
        private double smoothingStrength = 1.0D;

        @SerializedName("maximum_camera_lag")
        private double maximumCameraLag = 2.5D;

        @SerializedName("smooth_third_person")
        private boolean smoothThirdPerson;

        @SerializedName("debug_logging")
        private boolean debugLogging;

        private Snapshot toSnapshot() {
            return new Snapshot(
                    enabled,
                    clamp(
                            recoveryDurationMilliseconds,
                            MINIMUM_DURATION_MILLISECONDS,
                            MAXIMUM_DURATION_MILLISECONDS
                    ),
                    EasingCurve.parse(easing),
                    clamp(smoothingStrength, 0.0D, 1.0D, 1.0D),
                    clamp(
                            maximumCameraLag,
                            MINIMUM_CAMERA_LAG,
                            MAXIMUM_CAMERA_LAG,
                            2.5D
                    ),
                    smoothThirdPerson,
                    debugLogging
            );
        }
    }
}
