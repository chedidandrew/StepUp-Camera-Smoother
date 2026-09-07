# Contributing

Keep changes focused on client camera presentation. Do not add gameplay movement, server packets, copied third-party code, or bundled third-party jars.

Before opening a pull request:

1. Add or update focused tests.
2. Run `./gradlew clean test build`.
3. Run `python3 scripts/audit_jar.py`.
4. Run all four client test profiles from `docs/BUILDING.md` when the change touches mixins, dependencies, or Minecraft integration.
5. Record the user-visible and technical change in `CHANGELOG.md`.
6. Update design, compatibility, configuration, or testing documentation when its claims change.

Pull requests should explain the movement case, expected camera behavior, exact Minecraft and dependency versions, and manual evidence when visual behavior changes.
