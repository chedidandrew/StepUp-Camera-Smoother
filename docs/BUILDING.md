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

The playable jar is created under `build/libs`. The audit writes its SHA-256 file under `build/checksums`. It also verifies that the metadata points to the bundled 512 by 512 PNG icon and validates the icon's PNG structure and dimensions.

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

The StepItUp profiles resolve StepItUp and Cloth Config from immutable Modrinth version IDs. The Mod Menu profiles resolve immutable Modrinth versions of Mod Menu 20.0.1 and its required Text Placeholder API, while Fabric API is already part of the test runtime. These dependencies are local-runtime test fixtures and are excluded from the built jar. Smart StepUp Camera Smoother itself does not depend on Cloth Config, Fabric API, or Text Placeholder API.

Minecraft 26.2 uses official unobfuscated names, so Loom exposes these opt-in development fixtures through its `localRuntime` configuration. They are not declared during a normal build and are not exposed as published dependencies.

The opt-in profiles also place Fabric API on their compile-only classpath. Runtime fixture access wideners can expose Fabric lifecycle types in transformed Minecraft class signatures when Gradle recompiles client sources. This compile-only input prevents a missing-type error without adding a production dependency or bundling Fabric API.

Mod Menu's API is present only at compile time for the isolated optional entrypoint. The ordinary client initializer has no Mod Menu link, so the standalone profile also proves that the jar starts without it.

## CurseForge package

Use `build/libs/smart-stepup-camera-smoother-<version>.jar` as the CurseForge mod file. Upload `src/main/resources/assets/stepup_camera_smoother/icon.png` separately as the CurseForge project logo. The website does not import the embedded Fabric icon automatically.

The build workflow stages the same 512 by 512 PNG as `smart-stepup-camera-smoother-icon-512.png` in its verified artifact. A successful release attaches that standalone file to the GitHub release alongside the jar, checksum, source snapshot, and provenance.

## Release process

1. Finish all jar-affecting code and resources, then finish the tests, `CHANGELOG.md`, and `RELEASE_NOTES.md`. Keep `release_ready=false` and leave `release_jar_sha256` empty.
2. Commit and push the release candidate. Let the build workflow finish its unit tests, jar audit, and all four client boot profiles.
3. Download that workflow's verified `Smart-StepUp-Camera-Smoother-<commit>` artifact and manually test its playable jar using `docs/TESTING.md`.
4. Record the exact candidate jar SHA-256 from the verified artifact.
5. Make a gate-only commit that sets `release_jar_sha256` to that lowercase 64-character value and changes `release_ready` to `true`. Do not change any input that affects the jar in this commit.
6. Push the gate commit to `main`. The build workflow rebuilds and retests the source, requires the rebuilt jar to match `release_jar_sha256`, and only then creates tag `v<mod_version>` and its GitHub release.

The final gate commit may update release status documentation and the two release-gate properties only. Matching artifact hashes prove that the jar published from the final commit is byte-for-byte identical to the candidate that was approved. An invalid version, missing or malformed SHA-256, failed test, existing tag or release, or hash mismatch fails closed without publishing.

After publication, verify that the GitHub release contains `smart-stepup-camera-smoother-<version>.jar`, its adjacent `.sha256` file, `source-snapshot.zip`, `PROVENANCE.txt`, and `smart-stepup-camera-smoother-icon-512.png`.
