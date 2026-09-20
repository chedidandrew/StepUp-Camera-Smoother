package dev.chedidandrew.stepupcamerasmoother.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import dev.chedidandrew.stepupcamerasmoother.client.config.SmootherConfigScreen;
import dev.chedidandrew.stepupcamerasmoother.config.SmootherConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only compiled into isolated smoke-test output; never shipped in playable JARs. */
@Mixin(TitleScreen.class)
public abstract class SmokeTitleMixin {
    @Unique private static boolean stepup$tested;
    @Inject(method = "init", at = @At("RETURN"))
    private void stepup$verify(CallbackInfo ci) throws Exception {
        if (stepup$tested) return;
        stepup$tested = true;
        Class.forName(Camera.class.getName(), true, Camera.class.getClassLoader());
        Class.forName(LocalPlayer.class.getName(), true, LocalPlayer.class.getClassLoader());
        Minecraft client = Minecraft.getInstance();
        Screen parent = (Screen)(Object)this;
        Screen screen = new SmootherConfigScreen(parent);
        SmootherConfig.Snapshot saved = SmootherConfig.get();
        client.gui.setScreen(screen);
        AbstractSliderButton slider = screen.children().stream().filter(AbstractSliderButton.class::isInstance)
                .map(AbstractSliderButton.class::cast).findFirst().orElseThrow();
        stepup$click(screen, slider.getX()+slider.getWidth()-1, slider.getY()+10);
        if (!(slider.getMessage().getContents() instanceof TranslatableContents label) || !label.getArgs()[0].toString().equals("200")) throw new AssertionError("Slider mouse input failed: " + slider.getMessage().getString() + " width=" + slider.getWidth());
        Button toggle = stepup$button(screen, "stepup_camera_smoother.config.third_person", false);
        var old = toggle.getMessage().copy();
        stepup$click(screen,toggle.getX()+5,toggle.getY()+5);
        if (old.equals(toggle.getMessage())) throw new AssertionError("Toggle mouse input failed");
        Button cancel = stepup$button(screen, "stepup_camera_smoother.config.cancel", true);
        stepup$click(screen,cancel.getX()+5,cancel.getY()+5);
        if (client.gui.screen()!=parent || !saved.equals(SmootherConfig.get())) throw new AssertionError("Cancel did not preserve saved config");
        System.out.println("STEPUP_PORT_SMOKE_PASS: camera/player mixins loaded; slider, toggle, cancel passed");
        client.stop();
    }
    @Unique private static Button stepup$button(Screen screen,String text,boolean exact) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> b.getMessage().getContents() instanceof TranslatableContents label && label.getKey().equals(text))
                .findFirst().orElseThrow();
    }
    @Unique private static void stepup$click(Screen screen,double x,double y) {
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        if (!screen.mouseClicked(event, false)) throw new AssertionError("Screen rejected mouse input");
        screen.mouseReleased(event);
    }
}
