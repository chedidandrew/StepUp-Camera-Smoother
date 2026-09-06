# Changelog

All notable project changes are recorded here.

## 0.1.0-alpha.3 - Unreleased

### Added

- Added first-class rear and front third-person smoothing, enabled by default.
- Added a Mod Menu third-person toggle that is saved atomically with the Smoothness value.
- Extended the Smoothness slider from 0% through 200% in 1% stored increments.
- Added configuration version 1 and a one-time migration that enables third-person smoothing for versionless alpha.2 configurations.
- Added unit coverage for 150% and 200% values, recovery scaling without vertical overshoot, the 200% safety cap, third-person defaults, toggle copying, legacy configuration migration, and future-version refusal.

### Changed

- Defined 101% through 200% as progressively longer recovery. With the default 180 ms base, the recovery phase lasts 270 ms at 150% and 360 ms at 200% while retaining full initial step correction.
- Moved the camera correction earlier in `Camera.alignWithEntity(float)`, after the perspective state is selected but before vanilla calculates detached-camera distance and collision. Third-person wall collision therefore uses the smoothed camera pivot.
- Changed Reset to restore both exposed settings: 100% Smoothness and third-person smoothing enabled.
- Changed new configurations to use `smooth_third_person: true` and `config_version: 1`.
- Changed the loader to reject unsupported future configuration versions without rewriting them, preventing an older build from silently discarding newer settings.
- Bumped the project version from `0.1.0-alpha.2` to `0.1.0-alpha.3`.

### Compatibility evidence

- Preserved alpha.2's transition amplitude and recovery semantics exactly from 0% through 100%.
- Kept all camera changes render-only and retained the existing StepItUp, Mod Menu, and dependency boundaries.
- Kept a third-person off switch for compatibility testing with camera, replay, VR, shader, and perspective mods.

### Release safety

- Kept `release_ready=false`. GitHub Actions and the expanded manual perspective and smoothness matrix must pass on the exact candidate before a release is published.

## 0.1.0-alpha.2 - 2026-09-06 (CI test build)

### Added

- Added an optional Mod Menu 20.0.1 entrypoint and a native Minecraft configuration screen.
- Added a 0% to 100% Smoothness slider over the existing `smoothing_strength` JSON setting.
- Added persistent, atomic configuration saves from the screen. Selecting Done applies the new strength immediately and clears the active transition so the next eligible step uses it.
- Added Reset, Cancel, and Done controls. Reset changes only the exposed smoothness draft, while Cancel and Escape discard unsaved screen changes.
- Added English configuration-screen translations.
- Added tests for smoothness updates, bounds, defaults, and invalid numeric input.
- Added reflective client checks that instantiate the optional Mod Menu entrypoint, create and open its configuration screen, then restore the parent screen.
- Added standalone, Mod Menu-only, StepItUp-only, and combined StepItUp plus Mod Menu client boot profiles.
- Extended the fail-closed jar audit to verify the optional entrypoint, exact translation keys, dependency metadata, and exclusion of Mod Menu, Fabric API, Cloth Config, Auto Config, and Text Placeholder API classes.

### Changed

- Bumped the project version from `0.1.0-alpha.1` to `0.1.0-alpha.2`.
- Expanded the build, compatibility, design, testing, and release documentation for the optional configuration screen.

### Compatibility evidence

- Kept Mod Menu as an optional integration suggested in metadata rather than a required dependency.
- Compiled against Mod Menu 20.0.1 without bundling its API or jar.
- Confirmed that the ordinary client initializer does not link to Mod Menu classes, allowing the same jar to start without Mod Menu installed.
- Kept Cloth Config outside this mod's dependencies. Cloth Config appears only as part of the optional StepItUp compatibility fixture.

### Release safety

- Kept `release_ready=false`. GitHub Actions and the complete manual test matrix still must pass on the exact candidate before a release is published.

### Fixed

- Added Fabric API to the compile-only classpath of opt-in client fixture profiles. Their access wideners expose Fabric lifecycle types in transformed Minecraft classes during recompilation, while the production jar and published runtime dependencies remain unchanged.
- Ordered client profiles as standalone, Mod Menu only, StepItUp only, and combined so each optional integration is independently tested before the strict combined gate.

## 0.1.0-alpha.1 - 2026-09-06 (CI test build)

### Added

- Created a new client-only Fabric 26.2 mod with mod ID `stepup_camera_smoother`.
- Added before-and-after observation of `LocalPlayer.move(MoverType, Vec3)`.
- Added generic upward collision-step detection using requested motion, resolved displacement, grounded state, horizontal movement, and the player's effective `maxUpStep()` value.
- Added a render-only world-Y camera correction at `Camera.alignWithEntity(float)` return.
- Added first-person smoothing by default and an opt-in rear/front third-person path for collision testing.
- Added additive transitions for rapid consecutive steps with a configurable maximum camera lag.
- Added four easing curves: linear, smoothstep, smootherstep, and normalized exponential.
- Added JSON configuration with validated recovery duration, strength, easing, maximum lag, third-person behavior, and debug logging.
- Added resets and detection exclusions for jumps, descents, falls, direct position corrections, teleports, non-self movement, swimming, water, lava, climbing, flight, elytra, vehicles, spectator mode, death, player replacement, and world replacement.
- Added an immediate queue reset when entering an unsmoothed third-person perspective so no hidden transition can snap into view when returning to first person.
- Added focused JUnit tests for StepItUp full-block steps, vanilla half-block steps, false-positive rejection, easing invariants, transition timing, accumulation, clamping, strength, reset, and non-monotonic time.
- Added Fabric client boot tests that force both mixin targets to load.
- Added an optional CI compatibility profile for the exact StepItUp and Cloth Config Modrinth version IDs.
- Added a fail-closed jar audit covering ZIP integrity, safe paths, metadata, entrypoints, mixin classes, Java 25 bytecode, license inclusion, dependency separation, and SHA-256 generation.
- Added pinned GitHub Actions workflows for build verification and gated releases.
- Added README, design, compatibility, build, testing, contribution, security, release-note, license, and changelog documentation.
- Added a Gradle 9.5.1 wrapper configuration with the official distribution SHA-256.
- Set Fabric Loader 0.19.5 as the minimum because it is the exact version built and boot-tested by CI.

### Compatibility evidence

- Inspected `stepitup-3.0-26.2-fabric.jar`, 56,274 bytes.
- Verified uploaded fixture SHA-256: `fe822f4398f458421ace61efefe2689e04396640a170288d4b9d3d6bbc9efe12`.
- Verified that the fixture changes the effective step height to 1.25 and leaves camera rendering unchanged.
- Verified the Minecraft 26.2 method targets `LocalPlayer.move(MoverType, Vec3)` and `Camera.alignWithEntity(float)` against current 26.2 source signatures.
- Confirmed that neither the inspected StepItUp jar nor third-party Smooth Steps code or assets are redistributed by this project.

### Release safety

- Kept `release_ready=false` throughout development. The documented release process changes it only in a final candidate commit, then builds and manually tests that exact commit before tagging.
- Required manual release inputs to equal the selected tag's full 40-character commit SHA and the rebuilt jar's SHA-256.

### Fixed

- Changed the opt-in StepItUp test fixtures from Loom's mapped `modLocalRuntime` configuration to the `localRuntime` configuration exposed by Minecraft 26.2's unobfuscated development environment.

### Known scope

- This alpha smooths upward collision steps only. Ordinary descents and falls clear any queued offset and otherwise remain visually unchanged.
- Third-person positioning is opt-in because wall, ceiling, and camera-collision edge cases remain part of the required manual test matrix before release.
