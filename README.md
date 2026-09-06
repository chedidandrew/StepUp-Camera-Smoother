# StepUp Camera Smoother

StepUp Camera Smoother is a client-only Fabric mod for Minecraft Java Edition 26.2. It softens the camera movement caused when collision handling steps the player onto a higher surface.

This alpha line is designed around [StepItUp 3.0](https://modrinth.com/mod/stepitup/version/3.0-26.2-fabric), including its 1.25-block step height, while remaining useful for vanilla stairs, slabs, snow layers, and other mods that change the effective step height.

> Status: source-complete alpha. GitHub Actions must pass and the built jar must be tested in Minecraft before a GitHub release is allowed. `release_ready=false` intentionally blocks release publication today.

## What it does

- Detects the local player's resolved upward collision step instead of guessing from block types.
- Applies a render-only world-Y camera offset. Player position, hitbox, reach, physics, and network packets are unchanged.
- Cancels the initial one-tick vertical snap, then eases the camera to the new height.
- Handles consecutive steps by adding short-lived transitions with a bounded total lag.
- Supports first-person by default, with opt-in rear and front third-person smoothing for testing.
- Resets immediately for jumps, swimming, ladders, flight, elytra, vehicles, spectator mode, death, respawn, world changes, and non-player movement sources.
- Requires no configuration library, Architectury API, or direct StepItUp dependency. Mod Menu support is optional.

This project is a distinct clean implementation. It does not contain code, assets, or binaries from StepItUp or Countered's Smooth Steps.

## Compatibility

| Component | Supported version | Relationship |
|---|---:|---|
| Minecraft Java Edition | 26.2 | Required |
| Fabric Loader | 0.19.5 or newer within the 26.2 line | Required and tested |
| Java | 25 or newer | Required |
| StepItUp | 3.0-26.2 Fabric | Optional, primary compatibility target |
| Mod Menu | 20.0.1 | Optional configuration screen |
| Fabric API | 0.159.0+26.2 | Development test fixture plus StepItUp and Mod Menu dependency |
| Text Placeholder API | 3.1.0-beta.1+26.2 | Mod Menu dependency |

Do not install another camera step-smoothing mod at the same time. Two mods applying the same visual correction can overcompensate.

See [Compatibility](docs/COMPATIBILITY.md) for the exact fixture and known boundaries.

## Installation

1. Install Fabric Loader for Minecraft 26.2.
2. Put the verified `stepup-camera-smoother-<version>.jar` in the instance `mods` folder.
3. Optionally install Mod Menu 20.0.1, Fabric API, and Text Placeholder API to configure smoothness in game. A dependency-aware launcher normally installs the latter two automatically.
4. Optionally install StepItUp, Fabric API, and Cloth Config if you want StepItUp's full-block stepping feature. Fabric API and Cloth Config are dependencies of StepItUp, not StepUp Camera Smoother.
5. Start Minecraft once to create `config/stepup-camera-smoother.json`.

Until the first release is manually approved, test jars are available only as artifacts from a successful `Build and verify` GitHub Actions run.

## Configuration

With Mod Menu installed, open **Mods**, select **StepUp Camera Smoother**, and open its configuration screen. The **Smoothness** slider ranges from 0% to 100%:

- 0% leaves the original upward camera motion unchanged.
- 50% smooths half of each upward camera snap.
- 100% applies full smoothing and is the default.

Select **Done** to save and apply the value immediately. The current camera transition is cleared so the next eligible step uses the new setting. **Cancel** or Escape discards changes made on the screen. **Reset** returns the slider to its 100% default without changing the other JSON settings.

Mod Menu is an optional integration, not a dependency required to start or use the mod. StepUp Camera Smoother does not use Cloth Config.

The generated file is `config/stepup-camera-smoother.json`:

```json
{
  "enabled": true,
  "recovery_duration_ms": 180,
  "easing": "smootherstep",
  "smoothing_strength": 1.0,
  "maximum_camera_lag": 2.5,
  "smooth_third_person": false,
  "debug_logging": false
}
```

The GUI slider stores `smoothing_strength` as a value from `0.0` to `1.0`. Changes saved through Mod Menu take effect immediately. Restart the client after editing the JSON file manually.

Valid easing values are `linear`, `smoothstep`, `smootherstep`, and `exponential`. Values are clamped to safe ranges during loading. A malformed file is not overwritten and safe defaults are used for that launch.

## How it works

StepItUp changes the local player's effective step height before vanilla movement. It does not provide a camera API. This mod therefore observes `LocalPlayer.move` before and after vanilla collision resolution.

An event qualifies only when a grounded, eligible player moves horizontally and the resolved vertical rise is greater than the requested vertical motion but no higher than `player.maxUpStep()` plus a small collision tolerance. Ordinary jumps and teleports do not meet those conditions.

The camera correction runs at the end of Minecraft 26.2's `Camera.alignWithEntity(float)` method. Using `setPosition` keeps the camera's internal block position synchronized, and running before frustum preparation keeps rendering culling aligned with the corrected position.

Direct position changes are also tracked. If the same player is corrected or teleported without passing through ordinary movement, the queued visual offset is cleared before the next camera sample.

The full design and invariants are in [Design](docs/DESIGN.md).

## Building and verification

```bash
./gradlew clean test build
python3 scripts/audit_jar.py
```

The authoritative build runs on GitHub Actions with Java 25 and Gradle 9.5.1. It performs unit tests, a fail-closed jar audit, and real client boot tests in four profiles: standalone, Mod Menu only, StepItUp only, and StepItUp plus Mod Menu.

For the full process, see [Building](docs/BUILDING.md) and [Testing](docs/TESTING.md).

## Inspiration and acknowledgements

- [StepItUp](https://github.com/mangovillage/StepItUp) provides the step-height behavior this project targets.
- [Countered's Smooth Steps](https://modrinth.com/mod/countereds-smooth-steps) demonstrated the desired camera feel and supports Minecraft 26.2 itself. This repository exists as an independent, focused implementation with no required Architectury API or MidnightLib dependency and explicit StepItUp 1.25-block coverage.

Minecraft is a trademark of Microsoft. This project is not affiliated with or endorsed by Mojang Studios or Microsoft.

## License

StepUp Camera Smoother is available under the [MIT License](LICENSE).
