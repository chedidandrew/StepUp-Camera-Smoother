# Security policy

Report a suspected security issue privately through GitHub's security advisory feature when it is available. Do not include access tokens, private server addresses, or unrelated personal data in logs.

Only the latest Smart StepUp Camera Smoother release for the supported Minecraft 26.2 line receives security fixes.

Official release jars are published only by the fail-closed GitHub Actions release path. Before publication, the rebuilt `smart-stepup-camera-smoother-<version>.jar` must pass the complete automated test matrix and match the approved candidate SHA-256 recorded in the release gate. Verify the adjacent `.sha256` file before installing a jar. Do not trust a jar whose hash, tag commit, and `PROVENANCE.txt` do not agree.
