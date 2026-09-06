package dev.chedidandrew.stepupcamerasmoother.client.config;

import dev.chedidandrew.stepupcamerasmoother.client.CameraMotion;
import dev.chedidandrew.stepupcamerasmoother.client.StepUpCameraSmootherClient;
import dev.chedidandrew.stepupcamerasmoother.config.SmootherConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Vanilla configuration screen exposed through the optional Mod Menu integration. */
public final class SmootherConfigScreen extends Screen {
    private static final int MAXIMUM_CONTENT_WIDTH = 310;
    private static final int MINIMUM_CONTENT_WIDTH = 180;
    private static final int SLIDER_WIDTH = 108;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 5;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DESCRIPTION_COLOR = 0xFFA0A0A0;
    private static final int ERROR_COLOR = 0xFFFF5555;

    private final Screen parent;
    private double draftSmoothingStrength;
    private SmoothnessSlider smoothnessSlider;
    private int descriptionY;
    private int errorY;
    private boolean saveFailed;

    public SmootherConfigScreen(Screen parent) {
        super(Component.translatable("stepup_camera_smoother.config.title"));
        this.parent = parent;
        this.draftSmoothingStrength = SmootherConfig.get().smoothingStrength();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(
                MAXIMUM_CONTENT_WIDTH,
                Math.max(MINIMUM_CONTENT_WIDTH, this.width - 32)
        );
        int left = (this.width - contentWidth) / 2;
        int sliderWidth = Math.min(SLIDER_WIDTH, contentWidth);
        int sliderLeft = (this.width - sliderWidth) / 2;
        int sliderY = Math.max(78, this.height / 2 - 20);
        int buttonsY = sliderY + 34;
        int buttonWidth = (contentWidth - BUTTON_GAP * 2) / 3;

        this.descriptionY = sliderY - 32;
        this.errorY = buttonsY + BUTTON_HEIGHT + 12;

        this.smoothnessSlider = this.addRenderableWidget(new SmoothnessSlider(
                sliderLeft,
                sliderY,
                sliderWidth,
                BUTTON_HEIGHT,
                this.draftSmoothingStrength
        ));

        this.addRenderableWidget(Button.builder(
                Component.translatable("stepup_camera_smoother.config.reset"),
                button -> resetSmoothness()
        ).bounds(left, buttonsY, buttonWidth, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("stepup_camera_smoother.config.cancel"),
                button -> returnToParent()
        ).bounds(
                left + buttonWidth + BUTTON_GAP,
                buttonsY,
                buttonWidth,
                BUTTON_HEIGHT
        ).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("stepup_camera_smoother.config.done"),
                button -> saveAndClose()
        ).bounds(
                left + (buttonWidth + BUTTON_GAP) * 2,
                buttonsY,
                contentWidth - (buttonWidth + BUTTON_GAP) * 2,
                BUTTON_HEIGHT
        ).build());
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 28, TEXT_COLOR);
        graphics.centeredText(
                this.font,
                Component.translatable("stepup_camera_smoother.config.description"),
                this.width / 2,
                this.descriptionY,
                DESCRIPTION_COLOR
        );
        graphics.centeredText(
                this.font,
                Component.translatable("stepup_camera_smoother.config.description_range"),
                this.width / 2,
                this.descriptionY + 12,
                DESCRIPTION_COLOR
        );

        if (this.saveFailed) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("stepup_camera_smoother.config.save_failed"),
                    this.width / 2,
                    this.errorY,
                    ERROR_COLOR
            );
        }
    }

    private void resetSmoothness() {
        this.saveFailed = false;
        this.smoothnessSlider.setSmoothingStrength(
                SmootherConfig.Snapshot.defaults().smoothingStrength()
        );
    }

    private void saveAndClose() {
        SmootherConfig.Snapshot updated = SmootherConfig.get()
                .withSmoothingStrength(this.draftSmoothingStrength);
        if (!SmootherConfig.save(updated, StepUpCameraSmootherClient.LOGGER)) {
            this.saveFailed = true;
            return;
        }

        CameraMotion.reset();
        returnToParent();
    }

    private void returnToParent() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private final class SmoothnessSlider extends AbstractSliderButton {
        private SmoothnessSlider(int x, int y, int width, int height, double initialValue) {
            super(x, y, width, height, Component.empty(), quantize(initialValue));
            SmootherConfigScreen.this.draftSmoothingStrength = this.value;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int percentage = (int) Math.round(this.value * 100.0D);
            setMessage(Component.translatable(
                    "stepup_camera_smoother.config.smoothness",
                    percentage
            ));
        }

        @Override
        protected void applyValue() {
            int percentage = (int) Math.round(this.value * 100.0D);
            SmootherConfigScreen.this.draftSmoothingStrength = percentage / 100.0D;
            SmootherConfigScreen.this.saveFailed = false;
        }

        private void setSmoothingStrength(double strength) {
            this.value = quantize(strength);
            SmootherConfigScreen.this.draftSmoothingStrength = this.value;
            updateMessage();
        }

        private static double clamp(double value) {
            if (!Double.isFinite(value)) {
                return 1.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, value));
        }

        private static double quantize(double value) {
            return Math.round(clamp(value) * 100.0D) / 100.0D;
        }
    }
}
