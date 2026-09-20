package dev.chedidandrew.stepupcamerasmoother.platform.neoforge;

import dev.chedidandrew.stepupcamerasmoother.client.StepUpCameraSmootherClient;
import dev.chedidandrew.stepupcamerasmoother.client.config.SmootherConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = StepUpCameraSmootherClient.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientEntrypoint {
    public NeoForgeClientEntrypoint(ModContainer container) {
        StepUpCameraSmootherClient.initialize(FMLPaths.CONFIGDIR.get());
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (ignored, parent) -> new SmootherConfigScreen(parent));
    }
}
