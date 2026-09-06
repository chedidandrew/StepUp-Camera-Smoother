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

Standalone:

```bash
./gradlew --no-daemon runClientGameTest
```

With the exact StepItUp compatibility fixture:

```bash
STEPUP_CAMERA_SMOOTHER_EXPECT_STEPITUP=true \
./gradlew --no-daemon -Pinclude_stepitup_runtime=true runClientGameTest
```

The second command resolves StepItUp and Cloth Config from immutable Modrinth version IDs. Those dependencies are local-runtime only and are excluded from the built jar.

## Release process

1. Finish the code, tests, `CHANGELOG.md`, and `RELEASE_NOTES.md`.
2. Set `release_ready=true` and commit the final candidate.
3. Make the build workflow pass on that exact final commit.
4. Download its verified artifact and manually test it using `docs/TESTING.md`.
5. Record the exact 40-character commit SHA and jar SHA-256 without creating another commit.
6. Tag that same commit as `v<mod_version>`.
7. Dispatch `Publish tested release` on that tag and enter both recorded values.

The release workflow rebuilds, repeats unit tests and both client boot profiles, audits the jar, compares the rebuilt jar with the manually tested SHA-256, writes provenance evidence, and then creates the GitHub release. Any mismatch fails closed.
