package dev.chedidandrew.stepupcamerasmoother.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;

/** Loads both mixin targets in a real client and checks the optional fixture. */
@SuppressWarnings("UnstableApiUsage")
public final class ClientBootGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        context.runOnClient(client -> {
            require(
                    FabricLoader.getInstance().isModLoaded("stepup_camera_smoother"),
                    "StepUp Camera Smoother was not loaded"
            );

            boolean expectStepItUp = Boolean.parseBoolean(
                    System.getenv("STEPUP_CAMERA_SMOOTHER_EXPECT_STEPITUP")
            );
            require(
                    !expectStepItUp || FabricLoader.getInstance().isModLoaded("stepitup"),
                    "The StepItUp compatibility fixture was requested but not loaded"
            );

            loadMixinTarget("net.minecraft.client.Camera");
            loadMixinTarget("net.minecraft.client.player.LocalPlayer");
        });
    }

    private static void loadMixinTarget(String className) {
        try {
            Class.forName(className, true, ClientBootGameTest.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Could not load mixin target " + className, exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
