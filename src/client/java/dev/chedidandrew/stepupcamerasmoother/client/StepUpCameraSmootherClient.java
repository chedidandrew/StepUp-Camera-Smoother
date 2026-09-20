package dev.chedidandrew.stepupcamerasmoother.client;

import dev.chedidandrew.stepupcamerasmoother.config.SmootherConfig;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StepUpCameraSmootherClient {
    public static final String MOD_ID = "stepup_camera_smoother";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void initialize(Path configDirectory) {
        SmootherConfig.initialize(configDirectory, LOGGER);
        LOGGER.info("Smart StepUp Camera Smoother initialized for Minecraft 26.3.");
    }
}
