# Building

## Requirements

- Java Development Kit 25
- Internet access for Gradle, Fabric, Minecraft, and test dependencies
- Git when creating the source snapshot

The included Gradle wrapper downloads Gradle 9.5.1 and verifies the distribution against SHA-256 `bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f`.

## Standard build

```bash
./gradlew --no-daemon clean test build
python3 scripts/audit_jar.py
```

The playable jar is created under `build/libs`. The audit writes its SHA-256 file under `build/checksums`.

## Client tests

The client tests cover four runtime profiles. Optional fixtures are enabled only for the command that needs them.

Standalone, without Mod Menu or StepItUp:

```bash
./gradlew --no-daemon runClientGameTest
```

Mod Menu 20.0.1 only:

```bash
STEPUP_CAMERA_SMOOTHER_EXPECT_MODMENU=true \
./gradlew --no-daemon -Pinclude_modmenu_runtime=true runClientGameTest
```

The exact StepItUp compatibility fixture only:

```bash
STEPUP_CAMERA_SMOOTHER_EXPECT_STEPITUP=true \
./gradlew --no-daemon -Pinclude_stepitup_runtime=true runClientGameTest
```

StepItUp and Mod Menu together:

```bash
STEPUP_CAMERA_SMOOTHER_EXPECT_STEPITUP=true \
STEPUP_CAMERA_SMOOTHER_EXPECT_MODMENU=true \
./gradlew --no-daemon \
  -Pinclude_stepitup_runtime=true \
  -Pinclude_modmenu_runtime=true \
  runClientGameTest
```

The StepItUp profiles resolve StepItUp and Cloth Config from immutable Modrinth version IDs. The Mod Menu profiles resolve immutable Modrinth versions of Mod Menu 20.0.1 and its required Text Placeholder API, while Fabric API is already part of the test runtime. These dependencies are local-runtime test fixtures and are excluded from the built jar. StepUp Camera Smoother itself does not depend on Cloth Config, Fabric API, or Text Placeholder API.

Minecraft 26.2 uses official unobfuscated names, so Loom exposes these opt-in development fixtures through its `localRuntime` configuration. They are not declared during a normal build and are not exposed as published dependencies.

The opt-in profiles also place Fabric API on their compile-only classpath. Runtime fixture access wideners can expose Fabric lifecycle types in transformed Minecraft class signatures when Gradle recompiles client sources. This compile-only input prevents a missing-type error without adding a production dependency or bundling Fabric API.

Mod Menu's API is present only at compile time for the isolated optional entrypoint. The ordinary client initializer has no Mod Menu link, so the standalone profile also proves that the jar starts without it.

## Release process

1. Finish the code, tests, `CHANGELOG.md`, and `RELEASE_NOTES.md`.
2. Set `release_ready=true` and commit the final candidate.
3. Make the build workflow pass on that exact final commit.
4. Download its verified artifact and manually test it using `docs/TESTING.md`.
5. Record the exact 40-character commit SHA and jar SHA-256 without creating another commit.
6. Tag that same commit as `v<mod_version>`.
7. Dispatch `Publish tested release` on that tag and enter both recorded values.

The release workflow rebuilds, repeats unit tests and all four client boot profiles, audits the jar, compares the rebuilt jar with the manually tested SHA-256, writes provenance evidence, and then creates the GitHub release. Any mismatch fails closed.
