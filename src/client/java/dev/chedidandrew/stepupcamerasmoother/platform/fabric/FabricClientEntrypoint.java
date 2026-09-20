package dev.chedidandrew.stepupcamerasmoother.platform.fabric;

import dev.chedidandrew.stepupcamerasmoother.client.StepUpCameraSmootherClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StepUpCameraSmootherClient.initialize(FabricLoader.getInstance().getConfigDir());
    }
}
