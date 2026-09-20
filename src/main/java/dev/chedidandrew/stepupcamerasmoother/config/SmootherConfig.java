package dev.chedidandrew.stepupcamerasmoother.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import dev.chedidandrew.stepupcamerasmoother.motion.EasingCurve;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/** JSON configuration storage shared by manual and in-game configuration. */
public final class SmootherConfig {
    public static final String FILE_NAME = "stepup-camera-smoother.json";
    public static final double DEFAULT_SMOOTHING_STRENGTH = 1.5D;
    public static final double MAXIMUM_SMOOTHING_STRENGTH = 2.0D;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_CONFIG_VERSION = 1;
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

    private static Path configurationDirectory = Path.of("config");

    public static void initialize(Path directory, Logger logger) {
        configurationDirectory = directory;
        load(logger);
    }

    public static void load(Logger logger) {
        Path configPath = configurationDirectory.resolve(FILE_NAME);
        load(configPath, logger);
    }

    static void load(Path configPath, Logger logger) {
        ConfigData data = new ConfigData();

        if (Files.notExists(configPath)) {
            current = data.toSnapshot();
            writeDefaults(configPath, data, logger);
            return;
        }

        try {
            Snapshot loaded;
            boolean migrated;
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root == null || !root.isJsonObject()) {
                    throw new IllegalArgumentException("configuration root is not an object");
                }

                JsonObject object = root.getAsJsonObject();
                ConfigData parsed = GSON.fromJson(object, ConfigData.class);
                if (parsed == null) {
                    throw new IllegalArgumentException("configuration root is null");
                }
                if (parsed.configVersion > CURRENT_CONFIG_VERSION) {
                    throw new IllegalArgumentException(
                            "unsupported configuration version " + parsed.configVersion
                    );
                }

                migrated = !object.has("config_version")
                        || object.get("config_version").isJsonNull()
                        || parsed.configVersion < CURRENT_CONFIG_VERSION;
                if (migrated) {
                    parsed.configVersion = CURRENT_CONFIG_VERSION;
                }
                loaded = parsed.toSnapshot();
            }

            current = loaded;
            if (migrated) {
                persistMigration(configPath, current, logger);
            }
        } catch (IOException | RuntimeException exception) {
            current = Snapshot.defaults();
            logger.error(
                    "Could not read {}. Safe defaults will be used until the next restart.",
                    configPath,
                    exception
            );
        }
    }

    private static void persistMigration(Path configPath, Snapshot snapshot, Logger logger) {
        try {
            writeAtomically(configPath, ConfigData.fromSnapshot(snapshot));
            logger.info("Updated {} to configuration version {}.", configPath, CURRENT_CONFIG_VERSION);
        } catch (IOException | RuntimeException exception) {
            logger.warn(
                    "Could not persist the configuration upgrade for {}. The upgraded settings remain active for this launch.",
                    configPath,
                    exception
            );
        }
    }

    public static boolean save(Snapshot requested, Logger logger) {
        Path configPath = configurationDirectory.resolve(FILE_NAME);
        return save(configPath, requested, logger);
    }

    static boolean save(Path configPath, Snapshot requested, Logger logger) {
        if (requested == null) {
            logger.error("Could not save {} because the requested configuration is null.", configPath);
            return false;
        }

        Snapshot sanitized = ConfigData.fromSnapshot(requested).toSnapshot();
        ConfigData data = ConfigData.fromSnapshot(sanitized);

        try {
            writeAtomically(configPath, data);
            current = sanitized;
            return true;
        } catch (IOException | RuntimeException exception) {
            logger.error(
                    "Could not save {}. The previous configuration remains active.",
                    configPath,
                    exception
            );
            return false;
        }
    }

    private static void writeDefaults(Path configPath, ConfigData data, Logger logger) {
        try {
            writeAtomically(configPath, data);
        } catch (IOException exception) {
            logger.warn("Could not create default configuration at {}.", configPath, exception);
        }
    }

    private static void writeAtomically(Path configPath, ConfigData data) throws IOException {
        Path targetPath = configPath.toAbsolutePath();
        Path parentPath = targetPath.getParent();
        Files.createDirectories(parentPath);

        Path temporaryPath = Files.createTempFile(
                parentPath,
                targetPath.getFileName().toString() + ".",
                ".tmp"
        );

        try {
            Files.writeString(
                    temporaryPath,
                    GSON.toJson(data) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            try {
                Files.move(
                        temporaryPath,
                        targetPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temporaryPath,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporaryPath);
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
        public static Snapshot defaults() {
            return new ConfigData().toSnapshot();
        }

        public Snapshot withSmoothingStrength(double value) {
            return new Snapshot(
                    enabled,
                    recoveryDurationMilliseconds,
                    easing,
                    clamp(value, 0.0D, MAXIMUM_SMOOTHING_STRENGTH, DEFAULT_SMOOTHING_STRENGTH),
                    maximumCameraLag,
                    smoothThirdPerson,
                    debugLogging
            );
        }

        public Snapshot withSmoothThirdPerson(boolean value) {
            return new Snapshot(
                    enabled,
                    recoveryDurationMilliseconds,
                    easing,
                    smoothingStrength,
                    maximumCameraLag,
                    value,
                    debugLogging
            );
        }
    }

    private static final class ConfigData {
        @SerializedName("config_version")
        private int configVersion = CURRENT_CONFIG_VERSION;

        private boolean enabled = true;

        @SerializedName("recovery_duration_ms")
        private int recoveryDurationMilliseconds = 180;

        private String easing = "smootherstep";

        @SerializedName("smoothing_strength")
        private double smoothingStrength = DEFAULT_SMOOTHING_STRENGTH;

        @SerializedName("maximum_camera_lag")
        private double maximumCameraLag = 2.5D;

        @SerializedName("smooth_third_person")
        private boolean smoothThirdPerson = false;

        @SerializedName("debug_logging")
        private boolean debugLogging;

        private static ConfigData fromSnapshot(Snapshot snapshot) {
            ConfigData data = new ConfigData();
            data.configVersion = CURRENT_CONFIG_VERSION;
            data.enabled = snapshot.enabled();
            data.recoveryDurationMilliseconds = snapshot.recoveryDurationMilliseconds();
            data.easing = snapshot.easing() == null
                    ? null
                    : snapshot.easing().name().toLowerCase(Locale.ROOT);
            data.smoothingStrength = snapshot.smoothingStrength();
            data.maximumCameraLag = snapshot.maximumCameraLag();
            data.smoothThirdPerson = snapshot.smoothThirdPerson();
            data.debugLogging = snapshot.debugLogging();
            return data;
        }

        private Snapshot toSnapshot() {
            return new Snapshot(
                    enabled,
                    clamp(
                            recoveryDurationMilliseconds,
                            MINIMUM_DURATION_MILLISECONDS,
                            MAXIMUM_DURATION_MILLISECONDS
                    ),
                    EasingCurve.parse(easing),
                    clamp(
                            smoothingStrength,
                            0.0D,
                            MAXIMUM_SMOOTHING_STRENGTH,
                            DEFAULT_SMOOTHING_STRENGTH
                    ),
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
