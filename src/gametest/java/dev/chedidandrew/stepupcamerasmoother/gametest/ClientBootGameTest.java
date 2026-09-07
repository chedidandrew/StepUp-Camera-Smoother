package dev.chedidandrew.stepupcamerasmoother.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/** Loads both mixin targets in a real client and checks the optional fixtures. */
@SuppressWarnings("UnstableApiUsage")
public final class ClientBootGameTest implements FabricClientGameTest {
    private static final String MOD_ID = "stepup_camera_smoother";
    private static final String EXPECTED_ICON_PATH =
            "assets/stepup_camera_smoother/icon.png";
    private static final String MOD_MENU_API = "com.terraformersmc.modmenu.api.ModMenuApi";
    private static final String CONFIG_SCREEN_FACTORY_API =
            "com.terraformersmc.modmenu.api.ConfigScreenFactory";
    private static final String MOD_MENU_BRIDGE =
            "dev.chedidandrew.stepupcamerasmoother.client.StepUpCameraSmootherModMenu";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        context.runOnClient(client -> {
            require(
                    FabricLoader.getInstance().isModLoaded(MOD_ID),
                    "StepUp Camera Smoother was not loaded"
            );
            verifyIconMetadata();

            boolean expectStepItUp = Boolean.parseBoolean(
                    System.getenv("STEPUP_CAMERA_SMOOTHER_EXPECT_STEPITUP")
            );
            boolean stepItUpLoaded = FabricLoader.getInstance().isModLoaded("stepitup");
            require(
                    stepItUpLoaded == expectStepItUp,
                    "Unexpected StepItUp loaded state: expected "
                            + expectStepItUp + ", found " + stepItUpLoaded
            );

            boolean expectModMenu = Boolean.parseBoolean(
                    System.getenv("STEPUP_CAMERA_SMOOTHER_EXPECT_MODMENU")
            );
            boolean modMenuLoaded = FabricLoader.getInstance().isModLoaded("modmenu");
            require(
                    modMenuLoaded == expectModMenu,
                    "Unexpected Mod Menu loaded state: expected "
                            + expectModMenu + ", found " + modMenuLoaded
            );

            if (expectModMenu) {
                verifyModMenuIntegration(client, client.gui.screen());
            }

            loadMixinTarget("net.minecraft.client.Camera");
            loadMixinTarget("net.minecraft.client.player.LocalPlayer");
        });
    }

    private static void verifyIconMetadata() {
        String iconPath = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .orElseThrow(() -> new AssertionError(
                        "StepUp Camera Smoother mod container was not found"
                ))
                .getMetadata()
                .getIconPath(512)
                .orElseThrow(() -> new AssertionError(
                        "StepUp Camera Smoother icon metadata was not found"
                ));
        require(
                EXPECTED_ICON_PATH.equals(iconPath),
                "Unexpected StepUp Camera Smoother icon path: " + iconPath
        );
    }

    private static void verifyModMenuIntegration(
            net.minecraft.client.Minecraft client,
            Screen parent
    ) {
        require(parent != null, "The Mod Menu config screen needs a parent screen");

        try {
            ClassLoader classLoader = ClientBootGameTest.class.getClassLoader();
            Class<?> modMenuApi = Class.forName(MOD_MENU_API, true, classLoader);
            Class<?> configScreenFactoryApi = Class.forName(
                    CONFIG_SCREEN_FACTORY_API,
                    true,
                    classLoader
            );
            Class<?> bridgeClass = Class.forName(MOD_MENU_BRIDGE, true, classLoader);

            require(
                    modMenuApi.isAssignableFrom(bridgeClass),
                    "The Mod Menu bridge does not implement ModMenuApi"
            );
            Object bridge = bridgeClass.getConstructor().newInstance();
            Object factory = modMenuApi
                    .getMethod("getModConfigScreenFactory")
                    .invoke(bridge);
            require(
                    configScreenFactoryApi.isInstance(factory),
                    "The Mod Menu bridge did not return a ConfigScreenFactory"
            );

            Object configScreen = configScreenFactoryApi
                    .getMethod("create", Screen.class)
                    .invoke(factory, parent);
            require(configScreen instanceof Screen, "The config factory did not create a Screen");

            Screen screen = (Screen) configScreen;
            client.gui.setScreen(screen);
            require(client.gui.screen() == screen, "The config screen did not become active");
            client.gui.setScreen(parent);
            require(client.gui.screen() == parent, "The parent screen was not restored");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not verify the Mod Menu integration", exception);
        }
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
