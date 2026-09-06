# Changelog

All notable project changes are recorded here.

## 0.1.0-alpha.1 - Unreleased

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
