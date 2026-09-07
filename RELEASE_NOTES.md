# Smart StepUp Camera Smoother 0.1.0

Smart StepUp Camera Smoother 0.1.0 is the first stable release for Minecraft Java Edition 26.2 on Fabric.

## Included

- Smooth upward collision steps on slabs, stairs, snow layers, and full blocks.
- Compatibility with StepItUp 3.0 and its 1.25-block effective step height.
- First-person plus rear and front third-person smoothing by default.
- Configurable recovery duration, easing curve, strength, maximum lag, and third-person behavior.
- Optional Mod Menu 20.0.1 integration with a 0% to 200% Smoothness slider and third-person toggle.
- Original 512 by 512 project icon embedded in the jar and supplied separately for CurseForge project-logo upload.
- Full correction without downward overshoot. Values above 100% extend recovery time up to 2x at 200%.
- Third-person collision calculated by vanilla from the smoothed camera pivot.
- Automatic migration of versionless alpha.2 configurations to third-person smoothing enabled.
- Immediate application after selecting Done, with Reset, Cancel, and Escape behavior that preserves unexposed JSON settings.
- No required library mod beyond Fabric Loader.

## Public rename and upgrades

The public project name changed from StepUp Camera Smoother to Smart StepUp Camera Smoother, and release files now use `smart-stepup-camera-smoother-<version>.jar`.

This is a compatibility-safe public rename. The internal mod ID `stepup_camera_smoother`, resource namespace `stepup_camera_smoother`, Java packages, mixin configuration, translation keys, and `config/stepup-camera-smoother.json` path are unchanged. Existing alpha installations upgrade in place and retain their configuration.

## Configuration

Install Mod Menu 20.0.1 to use the in-game screen. Smoothness at 0% leaves the original camera motion, 50% applies half smoothing, and 100% applies full smoothing. From 101% through 200%, the correction remains full while the recovery lasts progressively longer. The slider controls `smoothing_strength` from `0.0` through `2.0` and defaults to 100%.

Mod Menu is optional. Smart StepUp Camera Smoother starts and works without it, and the mod does not depend on Cloth Config. Cloth Config is needed only when required by an independently installed mod such as the tested StepItUp fixture.

## Verification

The stable artifact must pass the repository build, unit tests, icon-aware jar audit, and all four client boot profiles: standalone, Mod Menu only, StepItUp only, and StepItUp plus Mod Menu. Its SHA-256 must also match the approved candidate recorded by the fail-closed release gate before GitHub publishes the tag and release.

See `docs/TESTING.md` for the manual test matrix and `CHANGELOG.md` for the complete change record.
