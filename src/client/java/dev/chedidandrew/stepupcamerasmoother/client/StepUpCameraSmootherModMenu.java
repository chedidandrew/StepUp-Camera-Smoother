package dev.chedidandrew.stepupcamerasmoother.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.chedidandrew.stepupcamerasmoother.client.config.SmootherConfigScreen;

/** Optional Mod Menu entrypoint. This class is never loaded when Mod Menu is absent. */
public final class StepUpCameraSmootherModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SmootherConfigScreen::new;
    }
}
