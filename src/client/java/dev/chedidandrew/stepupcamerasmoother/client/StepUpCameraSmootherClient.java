package dev.chedidandrew.stepupcamerasmoother.client;

import dev.chedidandrew.stepupcamerasmoother.config.SmootherConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StepUpCameraSmootherClient implements ClientModInitializer {
    public static final String MOD_ID = "stepup_camera_smoother";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        SmootherConfig.load(LOGGER);
        LOGGER.info("Smart StepUp Camera Smoother initialized for Minecraft 26.2.");
    }
}
