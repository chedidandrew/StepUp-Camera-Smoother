# NeoForge 26.3 testing build

Added September 19, 2026, using NeoForge 26.3.0.4-beta, ModDevGradle 2.0.147, Java 25, and the root Gradle 9.6.0 wrapper. Artifact version: 0.2.0. This is a client-only mod; no server installation is required.

## Implementation

Both loaders compile the same smoothing state, step detector, easing curves, Camera/LocalPlayer mixins, configuration schema/defaults, and config screen. Loader adapters supply the configuration directory and register client startup. NeoForge registers the existing screen through IConfigScreenFactory; Fabric retains optional Mod Menu integration. The config filename remains stepup-camera-smoother.json. No Fabric or Mod Menu classes are included in the NeoForge JAR.

## Verification

- Both builds passed all 25 shared unit tests.
- Fabric real-client Mod Menu test passed, including actual slider, toggle, and Cancel mouse input.
- Both JAR audits passed. The NeoForge audit compares every shared class byte-for-byte against Fabric and verifies loader isolation, metadata, Java 25, icon, license, and required mixins.
- NeoForge development client started successfully with the mod initialized and no mixin failures in the startup log.
- NeoForge manual testing: maintainer confirmed the build worked on September 19, 2026.

## Reproduce

From the repository root:

```powershell
.\gradlew.bat --no-daemon build
.\gradlew.bat -p neoforge --no-daemon test build
python scripts/audit_jar.py
python scripts/audit_neoforge_jar.py
.\gradlew.bat -p neoforge --no-daemon runClient
```

The JAR is `neoforge/build/libs/smart-stepup-camera-smoother-neoforge-0.2.0.jar`.

## Manual testing

Create a Creative world with commands enabled. Open Mods > Smart StepUp Camera Smoother > Config. Compare 0%, 100%, and 200% smoothing on slabs and stairs while walking. Check both third-person views and Done/Cancel behavior. For full-block testing without StepItUp, disable Auto-Jump and use `/attribute @s minecraft:step_height base set 1.25`. Restore vanilla with `/attribute @s minecraft:step_height base set 0.6`. This does not establish compatibility with StepItUp or other external mods.

The maintainer completed testing of this loader build. Publication of subsequent version-matrix builds is tracked separately. The existing Fabric release is unchanged on GitHub; this development rebuild changes initialization wiring and must not reuse its stored release checksum.
