# Minecraft version matrix — 0.3.1

Separate Fabric and NeoForge JARs are built for every released 26.x version listed by Mojang on September 19, 2026, plus the two requested 1.21 versions. Snapshots and pre-releases are excluded. Each artifact declares exactly its intended Minecraft version.

| Minecraft | Java | Fabric Loader | NeoForge |
| --- | --- | --- | --- |
| 26.1 | 25 | 0.19.5 | 26.1.0.19-beta |
| 26.1.1 | 25 | 0.19.5 | 26.1.1.15-beta |
| 26.1.2 | 25 | 0.19.5 | 26.1.2.109 |
| 26.2 | 25 | 0.19.5 | 26.2.0.88 |
| 26.3 | 25 | 0.19.5 | 26.3.0.4-beta |
| 1.21.11 | 21 | 0.19.5 | 21.11.45 |
| 1.21.1 | 21 | 0.19.5 | 21.1.251 |

The mod itself is version 0.3.1, without a beta suffix. Some NeoForge dependencies are upstream beta builds; their exact names are preserved. Version pins are in `ports/versions.json` and the individual Gradle properties files.

## Shared behavior and adapters

The configuration schema/defaults, step detector, easing functions, and bounded smoothing state are canonical shared sources. Generated build sources are ignored and recreated by `scripts/generate_port.py`; do not edit them. Minecraft 26.x uses the existing camera hook after perspective selection and before third-person collision. The 1.21 ports adjust the initial camera Y-position inside `Camera.setup`, also before third-person collision. Riding a vehicle remains ineligible for smoothing. Old versions receive their corresponding camera-accessor, screen-navigation, and render-method names. Legacy Fabric artifacts are remapped to intermediary names for actual installation.

NeoForge registers the same settings screen in its native Mods menu. Fabric exposes it through optional Mod Menu. No Fabric, Mod Menu, StepItUp, or Cloth Config binaries are bundled. Optional StepItUp compatibility is not certified by this matrix; effective step height and camera behavior are handled by the shared implementation.

## Build, test, package

Use Java 25 to run Gradle, with Java 21 also installed for 1.21 targets. From the repository root:

```powershell
python scripts/build_ports.py
python scripts/build_ports.py --smoke --workers 2
python scripts/package_ports.py --require-smoke
```

Run build and smoke phases sequentially: separate output directories isolate test code, but Gradle launch configuration caches are per project and should not be rewritten during a running client.

For one build:

```powershell
.\gradlew.bat -p ports/1.21.1/neoforge --no-daemon build
.\gradlew.bat -p ports/1.21.1/neoforge --no-daemon runClient
```

On Linux, run the smoke command under `xvfb-run -a`. The CI matrix repeats these checks on all 14 targets. Test-only title-screen mixins are restricted to `build/smoke` outputs and explicitly rejected by the playable-JAR audit.

## Upload files

The collector creates `build/releases/0.3.1/` containing 14 clearly named playable JARs, `manifest.json`, and `SHA256SUMS.txt`. The sibling `smart-stepup-camera-smoother-0.3.1-all-ports.zip` is a convenience download; upload each matching JAR separately to the storefront, with its exact Minecraft version and loader. Do not upload sources JARs, test artifacts, or the combined ZIP as a mod file.

## Evidence boundaries

The maintainer confirmed Fabric 26.3 and NeoForge 26.3 worked before this matrix expansion. The additional version ports require their own hands-on camera-feel assessment; automated checks cannot establish subjective camera feel or compatibility with every other mod. Final per-artifact hashes and unit/client results are recorded by the collector, rather than reusing checksums from the earlier 0.2.0 builds.

## Local validation completed September 19, 2026

All 14 build targets passed their 25 shared unit tests (350 successful test executions in total), all 14 real-client smoke runs completed with the exact success marker and clean exit, and all 14 playable JARs passed package audits. Client checks load both camera/player mixin targets and exercise slider, third-person toggle, and Cancel through screen mouse handlers. These are automated runtime checks, not claims of manually played worlds on every version.

The final collector ran with `--require-smoke`. Recorded artifact identities are in [the 0.3.0 manifest](releases/0.3.0-manifest.json). Both original 26.3 project entrypoints were also rebuilt at version 0.3.0 and audited. The root `release_ready` latch remains false because its older single-artifact publisher is not used to publish this 14-artifact matrix. This does not mark the 0.3.0 JARs as beta; they are ready for separate user-controlled uploads.

## Maintainer testing of 0.3.0 — September 20, 2026

- Fabric 26.1: maintainer reported passed.
- NeoForge 26.1: maintainer reported passed.
- Fabric 26.1.1: maintainer reported passed.
- NeoForge 26.1.1: maintainer reported passed.
- Fabric 26.1.2: maintainer reported passed.
- NeoForge 26.1.2: maintainer reported passed.
- Fabric 26.2: maintainer reported passed.
- NeoForge 26.2: maintainer reported passed.
- Fabric 26.3: maintainer reported passed.
- Fabric 1.21.1: maintainer reported passed.
- NeoForge 1.21.1: maintainer confirmed passed; requested manual testing is complete.

## Release 0.3.1 — September 20, 2026

All 14 targets now default to 150% Smoothness and Third Person Off. Existing explicit settings are preserved, including during versionless configuration migration. Reset followed by Done adopts the new defaults. Camera motion logic is unchanged from the tested 0.3.0 builds.

Release files: `build/releases/0.3.1/`. See [CurseForge changelog](releases/0.3.1.md) and [artifact manifest](releases/0.3.1-manifest.json). Automated release validation covers all 14 builds, 25 unit tests per build, and real-client slider, toggle, Reset, and Cancel interactions. The manual results above apply to 0.3.0 before this defaults update.

## Additional ports — September 20, 2026

[33 additional targets](releases/0.3.1-additional.md) match the combinations in Accessible Step's Modrinth version API that were absent from the original 14 builds. This includes 1.20.1 Fabric/Forge and 1.20.4 Forge. The exact sparse matrix is recorded in `ports/versions.json`; a row's `loaders` list overrides the default Fabric/NeoForge pair.

The new files are collected separately in `build/releases/0.3.1-additional/`; the original 14 release files are unchanged. The additional ports use the existing 0.3.1 settings and shared motion logic. Minecraft 1.20 camera hooks capture the partial tick from `Camera.setup` and modify only the initial pivot's Y argument, before third-person collision, without depending on newer camera fields.

Forge builds include production SRG remapping and mixin refmaps. Forge 1.20.4 uses ForgeGradle 6.0.54; early NeoForge 1.20.3 and 1.20.5 use NeoGradle. These three targets use the checked-in Gradle 8.14.3 wrapper and Java 21 to run Gradle, while compiling/running Minecraft with the target's Java toolchain. Java 17, 21, and 25 toolchains are installed in CI.

To build only the additional targets, take the Minecraft/loader pairs from `docs/releases/accessible-step-targets.json` and pass them to `scripts/build_ports.py --only ...`. Run builds before the corresponding smoke runs; do not run both simultaneously for the same project. Collect using `scripts/package_ports.py --require-smoke --output-label 0.3.1-additional --only ...`.

Automated client checks validate startup, camera/player mixin application, slider input, the third-person toggle, Reset to 150%/Off, and Cancel preserving saved preferences. These checks do not replace manual gameplay testing, and matching Accessible Step's version list does not certify every integration pairing.

### Additional-port validation — September 21, 2026

All 33 new targets passed their 25 shared unit tests (825 executions), real-client smoke checks, and production-JAR audits. The final Forge 1.20.4 build uses its required unified classes/resources output and remaps the production refmap back into development names for client testing. The [additional artifact manifest](releases/0.3.1-additional-manifest.json) records all 33 upload filenames and SHA-256 hashes. The original 14 release JARs were checked against their existing manifest and are unchanged.
