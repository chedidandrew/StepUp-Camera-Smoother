# StepUp Camera Smoother 0.1.0-alpha.1

This is the first public test build for Minecraft Java Edition 26.2 on Fabric.

## Included

- Smooth upward collision steps on slabs, stairs, snow layers, and full blocks.
- Compatibility with StepItUp 3.0 and its 1.25-block effective step height.
- First-person smoothing by default, with opt-in rear/front third-person testing support.
- Configurable recovery duration, easing curve, strength, maximum lag, and third-person behavior.
- No required library mod beyond Fabric Loader.

## Test status

The repository build, unit tests, jar audit, and standalone plus StepItUp client boot tests must pass before publication. This alpha must also be tested manually in Minecraft at the exact tagged commit before the release workflow can publish it.

See `docs/TESTING.md` for the manual test matrix and `CHANGELOG.md` for the complete change record.
