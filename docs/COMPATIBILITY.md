# Compatibility

> For the current Minecraft 26.3 port, use [the 26.3 validation and testing record](MINECRAFT_26_3.md). The 26.2 fixtures and four-profile evidence below are historical; StepItUp has no verified 26.3 fixture yet.

## Primary matrix

| Item | Version | Status |
|---|---:|---|
| Minecraft Java Edition | 26.2 | Target |
| Fabric Loader | 0.19.5 | Build and client test |
| Java | 25 | Build and runtime target |
| StepItUp | 3.0-26.2 Fabric | Optional compatibility client test |
| Mod Menu | 20.0.1 | Optional configuration screen and client test |
| Fabric API | 0.159.0+26.2 | Test fixture and StepItUp dependency |
| Cloth Config | 26.2.155 Fabric | StepItUp dependency fixture |
| Text Placeholder API | 3.1.0-beta.1+26.2 | Mod Menu dependency fixture |

The public dependency range is deliberately limited to `>=26.2 <26.3`. A later Minecraft line must be compiled and tested before the range is widened because the camera pipeline changed in 26.2.

## Stable rename compatibility

Version 0.1.0 changes the public project name to Smart StepUp Camera Smoother and the release artifact basename to `smart-stepup-camera-smoother`. This does not create a second mod identity.

The internal mod ID remains `stepup_camera_smoother`. The resource namespace remains `stepup_camera_smoother`, the mixin configuration remains `stepup_camera_smoother.client.mixins.json`, Java packages remain under `dev.chedidandrew.stepupcamerasmoother`, translation keys retain their `stepup_camera_smoother` prefix, and the configuration file remains `config/stepup-camera-smoother.json`. Fabric, Mod Menu, existing worlds, other mods, and existing alpha configurations therefore continue to recognize the same installation.

## Exact StepItUp fixture

- File name: `stepitup-3.0-26.2-fabric.jar`
- Mod ID: `stepitup`
- Modrinth project ID: `9ggA4qiy`
- Modrinth version ID: `wkrRH2Bd`
- Size: 56,274 bytes
- SHA-256: `fe822f4398f458421ace61efefe2689e04396640a170288d4b9d3d6bbc9efe12`

Static inspection found no camera changes or formal integration API. The mod sets the local player's effective step height to 1.25 at `LocalPlayer.move` head and disables auto-jump under its configured conditions. Smart StepUp Camera Smoother reads only the vanilla resolved movement and effective step height.

The fixture is downloaded from Modrinth only for the opt-in development or CI runtime. It is not stored in this repository and is not bundled in published jars.

## Optional Mod Menu integration

Mod Menu 20.0.1 can expose Smart StepUp Camera Smoother's configuration screen. The screen uses native Minecraft widgets and does not use Cloth Config. It provides a 0% to 200% Smoothness slider backed by `smoothing_strength` plus a rear/front third-person toggle backed by `smooth_third_person`.

The CI fixture resolves Mod Menu project `mOgUt4GM`, version `njXb639R`, plus Text Placeholder API project `eXts2L7r`, version `NDqH16LT`. These immutable Modrinth identifiers match the Minecraft 26.2 releases and are used only by the opt-in test runtime.

The Mod Menu API is compile-only, Mod Menu is suggested rather than required in Fabric metadata, and no Mod Menu classes or nested jar are included in the production jar. The optional entrypoint is isolated from the ordinary client initializer, so the mod starts and smooths steps normally when Mod Menu is absent.

Automated client boots cover all four relevant combinations: neither optional mod, Mod Menu only, StepItUp only, and StepItUp plus Mod Menu. The combined profile checks that the configuration integration and the primary movement compatibility target can coexist.

## Expected interoperability

The detector should also work with another mod that raises the vanilla effective step height before `LocalPlayer.move`, provided that mod lets vanilla collision resolution perform the actual move.

It may not detect a mod that teleports the player upward, directly rewrites camera position, replaces collision handling, or performs movement outside `LocalPlayer.move`.

Other configuration-screen providers do not affect the JSON loader. When Mod Menu is present, selecting Done persists both exposed controls and applies them immediately. Manual JSON changes still require a client restart. Versionless alpha.2 configurations migrate once to configuration version 1 with third-person smoothing enabled.

## Conflicts

Do not run this alongside Countered's Smooth Steps or another mod that already adds a step camera offset. Multiple camera corrections can stack.

Third-person smoothing is enabled by default and can be disabled through Mod Menu or JSON. The offset is applied before vanilla calculates detached-camera distance and wall collision, so collision uses the smoothed pivot. Camera, replay, VR, shader, and perspective mods still require manual interoperability checks.

Smoothness from 101% through 200% extends recovery time without increasing the initial inverse offset beyond the detected step height. This prevents downward overshoot and limits added third-person collision risk.
