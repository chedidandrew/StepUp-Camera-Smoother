# Minecraft 26.3 port — 0.2.0

This is the Fabric port of 0.1.0. No NeoForge implementation exists in this repository. The maintainer confirmed the port works on September 18, 2026. This records the reported overall gameplay result, without claiming every suggested test case was individually completed.

## Dependencies and behavior

Minecraft 26.3, Fabric Loader 0.19.5, Loom 1.17.21, Gradle 9.6.0, and Java 25. Production Java, camera hooks, smoothing math, configuration schema/defaults, and translations are unchanged. The client targets Minecraft >=26.3 <26.4.

Optional testing fixtures: Fabric API 0.160.7+26.3, Mod Menu 21.0.0-beta.1 (lcxkZSq6), and Text Placeholder API 3.2.0+26.3 (lXytLqWj). Modrinth lists no Fabric 26.3 StepItUp version as of September 18, 2026. StepItUp compatibility is unverified and the opt-in Gradle profile fails explicitly until a compatible fixture is pinned. The old 26.2 fixture is not loaded or dependency-overridden. CI runs the two available profiles.

## Verification

- 25 unit tests passed, covering step detection, smoothing, easing, and configuration.
- Standalone real-client startup passed; both mixin target classes transformed.
- Mod Menu real-client test passed; it exercises the actual screen mouse handler for the smoothness slider, third-person toggle, and Cancel.
- Camera hook placement remains after perspective selection and before third-person camera collision.
- Manual gameplay: maintainer confirmed it works on September 18, 2026. Actual StepItUp compatibility remains unverified.

## Build and test

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.3+9'
.\gradlew.bat --no-daemon test build
python scripts/audit_jar.py
.\gradlew.bat --no-daemon runClientGameTest
$env:STEPUP_CAMERA_SMOOTHER_EXPECT_MODMENU='true'
.\gradlew.bat --no-daemon -Pinclude_modmenu_runtime=true runClientGameTest
.\gradlew.bat --no-daemon -Pinclude_modmenu_runtime=true runClient
```

## Manual test

Create a Creative superflat world with cheats enabled. Test slabs and stairs without flying. For full-block collision steps, use `/attribute @s minecraft:step_height base set 1.25` and disable Auto-Jump. This tests the effective step height but does not certify StepItUp itself.

Open Mods > Smart StepUp Camera Smoother > Configure. Compare Smoothness at 0%, 100%, and 200%; test both third-person views, consecutive steps, and stairs near walls. Verify jumping and flying do not receive step smoothing, and that Done saves while Cancel discards. Restore vanilla step height with `/attribute @s minecraft:step_height base set 0.6`.

## Test artifact

`build/libs/smart-stepup-camera-smoother-0.2.0.jar`

SHA-256: `827bd96542ef32294dfa2435321de8eeb75341d2bc7e6f957cff051a443bc947`

The playable JAR passed the structural audit. No production Java files changed from 0.1.0.
