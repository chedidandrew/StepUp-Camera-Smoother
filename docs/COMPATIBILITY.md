# Compatibility

## Primary matrix

| Item | Version | Status |
|---|---:|---|
| Minecraft Java Edition | 26.2 | Target |
| Fabric Loader | 0.19.5 | Build and client test |
| Java | 25 | Build and runtime target |
| StepItUp | 3.0-26.2 Fabric | Optional compatibility client test |
| Fabric API | 0.159.0+26.2 | Test fixture and StepItUp dependency |
| Cloth Config | 26.2.155 Fabric | StepItUp dependency fixture |

The public dependency range is deliberately limited to `>=26.2 <26.3`. A later Minecraft line must be compiled and tested before the range is widened because the camera pipeline changed in 26.2.

## Exact StepItUp fixture

- File name: `stepitup-3.0-26.2-fabric.jar`
- Mod ID: `stepitup`
- Modrinth project ID: `9ggA4qiy`
- Modrinth version ID: `wkrRH2Bd`
- Size: 56,274 bytes
- SHA-256: `fe822f4398f458421ace61efefe2689e04396640a170288d4b9d3d6bbc9efe12`

Static inspection found no camera changes or formal integration API. The mod sets the local player's effective step height to 1.25 at `LocalPlayer.move` head and disables auto-jump under its configured conditions. StepUp Camera Smoother reads only the vanilla resolved movement and effective step height.

The fixture is downloaded from Modrinth only for the opt-in development or CI runtime. It is not stored in this repository and is not bundled in published jars.

## Expected interoperability

The detector should also work with another mod that raises the vanilla effective step height before `LocalPlayer.move`, provided that mod lets vanilla collision resolution perform the actual move.

It may not detect a mod that teleports the player upward, directly rewrites camera position, replaces collision handling, or performs movement outside `LocalPlayer.move`.

## Conflicts

Do not run this alongside Countered's Smooth Steps or another mod that already adds a step camera offset. Multiple camera corrections can stack.

Third-person smoothing is disabled by default in the alpha. When enabled, it happens after vanilla calculates camera collision distance but before frustum preparation. It must be manually checked near walls and low ceilings before becoming a default.
