#!/usr/bin/env python3
"""Fail-closed structural audit for the playable Fabric jar."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import struct
import sys
import zipfile


PROJECT_CLASS_PREFIX = "dev/chedidandrew/stepupcamerasmoother/"
EXPECTED_MOD_ID = "stepup_camera_smoother"
EXPECTED_CLIENT_ENTRYPOINT = (
    "dev.chedidandrew.stepupcamerasmoother.client.StepUpCameraSmootherClient"
)
EXPECTED_CLASS_MAJOR = 69


def fail(message: str) -> None:
    raise AssertionError(message)


def find_playable_jar(build_directory: pathlib.Path) -> pathlib.Path:
    candidates = [
        path
        for path in (build_directory / "libs").glob("*.jar")
        if not any(
            marker in path.name
            for marker in ("-sources.jar", "-dev.jar", "-javadoc.jar")
        )
    ]
    if len(candidates) != 1:
        fail(f"Expected exactly one playable jar, found: {candidates}")
    return candidates[0]


def is_path_safe(name: str) -> bool:
    path = pathlib.PurePosixPath(name)
    return (
        bool(name)
        and not name.startswith(("/", "\\"))
        and "\\" not in name
        and "\x00" not in name
        and ".." not in path.parts
    )


def read_json(archive: zipfile.ZipFile, path: str) -> dict:
    try:
        return json.loads(archive.read(path).decode("utf-8"))
    except KeyError as exception:
        fail(f"Missing required file: {path}")
        raise exception
    except (UnicodeDecodeError, json.JSONDecodeError) as exception:
        fail(f"Invalid UTF-8 JSON in {path}: {exception}")
        raise exception


def audit_metadata(archive: zipfile.ZipFile, names: set[str]) -> None:
    metadata = read_json(archive, "fabric.mod.json")
    if metadata.get("id") != EXPECTED_MOD_ID:
        fail(f"Unexpected mod id: {metadata.get('id')}")
    if metadata.get("environment") != "client":
        fail("The published mod must be client-only")
    if "${" in str(metadata):
        fail("Unexpanded Gradle placeholder found in fabric.mod.json")

    client_entrypoints = metadata.get("entrypoints", {}).get("client", [])
    if client_entrypoints != [EXPECTED_CLIENT_ENTRYPOINT]:
        fail(f"Unexpected client entrypoints: {client_entrypoints}")
    if set(metadata.get("entrypoints", {})) != {"client"}:
        fail("The jar must not declare common or server entrypoints")

    dependencies = metadata.get("depends", {})
    if dependencies.get("minecraft") != ">=26.2 <26.3":
        fail(f"Unexpected Minecraft range: {dependencies.get('minecraft')}")
    if dependencies.get("fabricloader") != ">=0.19.5":
        fail(f"Unexpected Fabric Loader range: {dependencies.get('fabricloader')}")
    if dependencies.get("java") != ">=25":
        fail(f"Unexpected Java requirement: {dependencies.get('java')}")
    if metadata.get("suggests", {}).get("stepitup") != "*":
        fail("StepItUp must remain an optional suggested dependency")

    mixin_entries = metadata.get("mixins", [])
    if mixin_entries != ["stepup_camera_smoother.client.mixins.json"]:
        fail(f"Unexpected mixin declaration: {mixin_entries}")

    mixin_config = read_json(archive, mixin_entries[0])
    if not mixin_config.get("required"):
        fail("Mixin configuration must fail closed")
    package = mixin_config.get("package")
    for mixin_name in mixin_config.get("client", []):
        class_path = f"{package}.{mixin_name}".replace(".", "/") + ".class"
        if class_path not in names:
            fail(f"Declared mixin class is missing: {class_path}")


def audit_classes(archive: zipfile.ZipFile, names: set[str]) -> None:
    project_classes = sorted(
        name
        for name in names
        if name.startswith(PROJECT_CLASS_PREFIX) and name.endswith(".class")
    )
    if not project_classes:
        fail("No project classes found")

    for name in project_classes:
        header = archive.read(name)[:8]
        if len(header) != 8 or header[:4] != b"\xca\xfe\xba\xbe":
            fail(f"Invalid class header: {name}")
        major = struct.unpack(">H", header[6:8])[0]
        if major != EXPECTED_CLASS_MAJOR:
            fail(f"{name} uses class major {major}, expected {EXPECTED_CLASS_MAJOR}")


def audit_jar(jar_path: pathlib.Path) -> None:
    with zipfile.ZipFile(jar_path) as archive:
        infos = archive.infolist()
        names_list = [entry.filename for entry in infos]
        names = set(names_list)

        if len(names) != len(names_list):
            fail("Duplicate ZIP entry names found")
        unsafe = [name for name in names if not is_path_safe(name)]
        if unsafe:
            fail(f"Unsafe ZIP paths found: {unsafe}")
        if any(name.endswith(".jar") for name in names):
            fail("Nested jars are not allowed")
        if any(name.endswith(".java") for name in names):
            fail("Java source files must not be in the playable jar")
        if any(name.startswith("org/spoorn/") for name in names):
            fail("StepItUp classes must never be bundled")
        if any(name.startswith("assets/stepitup/") for name in names):
            fail("StepItUp assets must never be bundled")
        if "LICENSE_stepup-camera-smoother" not in names:
            fail("Renamed MIT license is missing from the jar")

        # Reading every file verifies CRCs and catches truncated entries.
        for entry in infos:
            archive.read(entry)

        audit_metadata(archive, names)
        audit_classes(archive, names)


def write_checksum(jar_path: pathlib.Path, build_directory: pathlib.Path) -> pathlib.Path:
    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    checksum_directory = build_directory / "checksums"
    checksum_directory.mkdir(parents=True, exist_ok=True)
    checksum_path = checksum_directory / f"{jar_path.name}.sha256"
    checksum_path.write_text(f"{digest}  {jar_path.name}\n", encoding="utf-8")
    return checksum_path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=pathlib.Path)
    parser.add_argument("--build-directory", type=pathlib.Path, default=pathlib.Path("build"))
    arguments = parser.parse_args()

    jar_path = arguments.jar or find_playable_jar(arguments.build_directory)
    if not jar_path.is_file():
        fail(f"Jar does not exist: {jar_path}")

    audit_jar(jar_path)
    checksum_path = write_checksum(jar_path, arguments.build_directory)
    print(f"Audited {jar_path}")
    print(f"Wrote {checksum_path}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (AssertionError, OSError, zipfile.BadZipFile) as exception:
        print(f"JAR AUDIT FAILED: {exception}", file=sys.stderr)
        sys.exit(1)
