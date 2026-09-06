# Testing

Automated client boot tests verify that Fabric loads the project, both mixin targets transform, the title screen starts, the optional Mod Menu entrypoint creates its configuration screen, and the exact StepItUp fixture can coexist. CI runs four profiles: standalone, Mod Menu only, StepItUp only, and StepItUp plus Mod Menu. These tests do not judge camera feel. Complete this manual matrix on the exact final candidate commit after `release_ready=true` has been committed and its build has passed. Do not create another commit between testing and tagging.

## Required setup

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.5 or newer compatible 26.2 build
- Candidate StepUp Camera Smoother jar
- StepItUp 3.0-26.2 Fabric
- StepItUp's required Fabric API and Cloth Config versions
- Mod Menu 20.0.1
- Mod Menu's required Fabric API and Text Placeholder API versions
- A new test world with cheats enabled

Test all four combinations: without StepItUp or Mod Menu, with Mod Menu only, with StepItUp only, and with both StepItUp and Mod Menu. Cloth Config is part of the StepItUp setup only and is not required by StepUp Camera Smoother.

## Configuration screen

- In each profile, confirm the game reaches the title screen without missing-dependency or entrypoint errors.
- With Mod Menu absent, confirm StepUp Camera Smoother still loads and smooths eligible steps.
- With Mod Menu present, open Mods, select StepUp Camera Smoother, and open its configuration screen.
- Set Smoothness to 0%, select Done, and confirm eligible steps retain their original camera motion without restarting.
- Set Smoothness to 50%, select Done, and confirm eligible steps use a visibly partial correction without restarting.
- Set Smoothness to 100%, select Done, and confirm eligible steps receive the full default correction without restarting.
- Set Smoothness to 150%, select Done, and confirm full correction settles over 1.5 times the base recovery duration without a downward dip.
- Set Smoothness to 200%, select Done, and confirm full correction settles over twice the base recovery duration without a downward dip.
- Disable and enable Third Person, select Done after each change, and verify the selected behavior applies to both rear and front views without restarting.
- Change the slider, select Cancel, reopen the screen, and confirm the saved value did not change.
- Change the slider, press Escape, reopen the screen, and confirm the saved value did not change.
- Change another setting manually in JSON, restart, use Reset and Done in the screen, then confirm `smoothing_strength` returned to `1.0`, `smooth_third_person` returned to `true`, and unexposed settings did not change.
- Start once with a versionless alpha.2 configuration containing `smooth_third_person: false`, then confirm it is rewritten as configuration version 1 with third-person smoothing enabled and its other values preserved.
- Start once with a configuration version greater than 1, then confirm safe defaults are active and the newer file remains byte-for-byte unchanged.
- Make the config path temporarily unwritable in a disposable instance and confirm a failed Done operation reports the failure without losing the previous configuration.

Expected result: values save atomically, apply to the next eligible step immediately, and survive a normal restart. The screen works with and without StepItUp, and no Cloth Config installation is needed unless StepItUp itself requires it.

## Core movement

- Walk, sprint, strafe, and walk backward over slabs.
- Walk and sprint over straight, corner, and alternating stairs.
- Cross carpets, paths, snow layers, and other layered collision shapes.
- With StepItUp enabled, walk onto full blocks and confirm the entire rise is smoothed.
- Traverse several consecutive full-block steps without a camera snap or unbounded lag.
- Repeat while crouching with StepItUp's sneaking option both disabled and enabled.

Expected result: the camera stays visually continuous, then settles cleanly at the new height. Player collision, reach, targeting, and movement speed remain vanilla or StepItUp-controlled.

## False-positive rejection

- Jump normally while still and while sprinting.
- Jump into the edge of a block.
- Fall from short and tall ledges.
- Receive knockback and upward impulses.
- Stand on or near piston and shulker movement.
- Climb ladders, vines, and scaffolding.
- Swim, leave water, and move through lava in a controlled creative test.
- Fly in creative mode and glide with elytra.

Expected result: none of these motions gets an artificial step-camera correction.

## Context resets

- Enter and leave boats, minecarts, and rideable mobs.
- Sleep and wake.
- Die and respawn.
- Teleport short and long distances.
- travel through Nether and End portals.
- Enter spectator mode and spectate another entity.
- Disconnect and join another world or server while a transition is active.

Expected result: no old camera offset survives the state change.

## Perspectives and rendering

- Test first-person, rear third-person, and front third-person with the default configuration.
- Disable `smooth_third_person`, then confirm first-person remains smooth while both third-person views retain their original motion.
- Switch perspectives during an active transition.
- Step beside walls, underneath low ceilings, and in tight stairwells in both third-person views.
- Repeat the wall, ceiling, and tight-stair tests at 100%, 150%, and 200%.
- Test at 30, 60, 120, and 144 or higher frames per second when possible.
- Cause a temporary frame-time spike and confirm the offset still settles without overshoot or NaN movement.

Expected result: all perspectives remain stable. The third-person camera must not enter terrain or reveal a new collision regression.

## Compatibility pass

- Run with the intended production mod set, especially camera, perspective, replay, VR, shader, and rendering mods.
- Do not install another step camera smoothing mod during this pass.
- Review `logs/latest.log` for `MixinApplyError`, `InvalidMixinException`, exceptions from `stepup_camera_smoother`, or repeated debug messages.

## Evidence to record

- Full 40-character Git commit SHA.
- Candidate jar SHA-256.
- Minecraft, Fabric Loader, Java, StepItUp, Fabric API, and Cloth Config versions.
- Mod Menu version and all four runtime profiles tested.
- Smoothness results at 0%, 50%, 100%, 150%, and 200%.
- Perspectives tested.
- Result for every group above.
- Any screenshots, video, crash reports, and `latest.log` needed to reproduce a failure.
