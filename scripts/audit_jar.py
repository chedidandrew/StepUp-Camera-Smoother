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
import zlib


PROJECT_CLASS_PREFIX = "dev/chedidandrew/stepupcamerasmoother/"
EXPECTED_MOD_ID = "stepup_camera_smoother"
EXPECTED_CLIENT_ENTRYPOINT = (
    "dev.chedidandrew.stepupcamerasmoother.client.StepUpCameraSmootherClient"
)
EXPECTED_MODMENU_ENTRYPOINT = (
    "dev.chedidandrew.stepupcamerasmoother.client.StepUpCameraSmootherModMenu"
)
EXPECTED_CLASS_MAJOR = 69
EXPECTED_ICON_PATH = "assets/stepup_camera_smoother/icon.png"
EXPECTED_ICON_SIZE = (512, 512)
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
REQUIRED_CONFIG_FILES = {
    "assets/stepup_camera_smoother/lang/en_us.json",
    (
        "dev/chedidandrew/stepupcamerasmoother/client/"
        "StepUpCameraSmootherModMenu.class"
    ),
    (
        "dev/chedidandrew/stepupcamerasmoother/client/config/"
        "SmootherConfigScreen.class"
    ),
}
EXPECTED_TRANSLATION_KEYS = {
    "stepup_camera_smoother.config.title",
    "stepup_camera_smoother.config.smoothness",
    "stepup_camera_smoother.config.description",
    "stepup_camera_smoother.config.description_range",
    "stepup_camera_smoother.config.third_person",
    "stepup_camera_smoother.config.on",
    "stepup_camera_smoother.config.off",
    "stepup_camera_smoother.config.reset",
    "stepup_camera_smoother.config.cancel",
    "stepup_camera_smoother.config.done",
    "stepup_camera_smoother.config.save_failed",
}


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


def audit_icon(archive: zipfile.ZipFile, names: set[str]) -> None:
    if EXPECTED_ICON_PATH not in names:
        fail(f"Missing declared mod icon: {EXPECTED_ICON_PATH}")

    icon = archive.read(EXPECTED_ICON_PATH)
    if not icon.startswith(PNG_SIGNATURE):
        fail("Mod icon does not have a valid PNG signature")

    offset = len(PNG_SIGNATURE)
    chunk_number = 0
    image_header: tuple[int, int, int, int, int] | None = None
    compressed_image = bytearray()
    saw_image_end = False

    while offset < len(icon):
        if len(icon) - offset < 12:
            fail("Mod icon contains a truncated PNG chunk header")

        chunk_length = struct.unpack(">I", icon[offset:offset + 4])[0]
        chunk_type = icon[offset + 4:offset + 8]
        chunk_data_start = offset + 8
        chunk_crc_start = chunk_data_start + chunk_length
        chunk_end = chunk_crc_start + 4
        if chunk_end > len(icon):
            fail(f"Mod icon contains a truncated {chunk_type!r} PNG chunk")

        chunk_data = icon[chunk_data_start:chunk_crc_start]
        expected_crc = struct.unpack(">I", icon[chunk_crc_start:chunk_end])[0]
        actual_crc = zlib.crc32(chunk_type)
        actual_crc = zlib.crc32(chunk_data, actual_crc) & 0xFFFFFFFF
        if actual_crc != expected_crc:
            fail(f"Mod icon has an invalid {chunk_type!r} PNG chunk CRC")

        if chunk_number == 0 and chunk_type != b"IHDR":
            fail("Mod icon PNG must begin with an IHDR chunk")
        if chunk_type == b"IHDR":
            if image_header is not None or chunk_length != 13:
                fail("Mod icon has an invalid IHDR chunk")
            width, height, bit_depth, color_type, compression, filtering, interlace = (
                struct.unpack(">IIBBBBB", chunk_data)
            )
            if (width, height) != EXPECTED_ICON_SIZE:
                fail(
                    "Mod icon dimensions are "
                    f"{width}x{height}, expected "
                    f"{EXPECTED_ICON_SIZE[0]}x{EXPECTED_ICON_SIZE[1]}"
                )
            valid_bit_depths = {
                0: {1, 2, 4, 8, 16},
                2: {8, 16},
                3: {1, 2, 4, 8},
                4: {8, 16},
                6: {8, 16},
            }
            if bit_depth not in valid_bit_depths.get(color_type, set()):
                fail("Mod icon has an invalid PNG color type or bit depth")
            if compression != 0 or filtering != 0 or interlace != 0:
                fail("Mod icon must use standard compression, filtering, and no interlace")
            image_header = (width, height, bit_depth, color_type, interlace)
        elif chunk_type == b"IDAT":
            compressed_image.extend(chunk_data)
        elif chunk_type == b"IEND":
            if chunk_length != 0:
                fail("Mod icon has an invalid IEND chunk")
            saw_image_end = True
            if chunk_end != len(icon):
                fail("Mod icon contains data after its IEND chunk")

        offset = chunk_end
        chunk_number += 1
        if saw_image_end:
            break

    if image_header is None or not compressed_image or not saw_image_end:
        fail("Mod icon is missing required PNG chunks")

    width, height, bit_depth, color_type, _ = image_header
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    row_size = (width * channels * bit_depth + 7) // 8
    expected_decoded_size = height * (row_size + 1)
    inflater = zlib.decompressobj()
    try:
        decoded = inflater.decompress(compressed_image, expected_decoded_size + 1)
        decoded += inflater.flush()
    except zlib.error as exception:
        fail(f"Mod icon contains invalid compressed PNG image data: {exception}")
    if (
        len(decoded) != expected_decoded_size
        or not inflater.eof
        or inflater.unused_data
        or inflater.unconsumed_tail
    ):
        fail("Mod icon PNG image data does not match its declared dimensions")
    if any(decoded[row * (row_size + 1)] > 4 for row in range(height)):
        fail("Mod icon contains an invalid PNG row filter")


def audit_metadata(archive: zipfile.ZipFile, names: set[str]) -> None:
    metadata = read_json(archive, "fabric.mod.json")
    if metadata.get("id") != EXPECTED_MOD_ID:
        fail(f"Unexpected mod id: {metadata.get('id')}")
    if metadata.get("environment") != "client":
        fail("The published mod must be client-only")
    if "${" in str(metadata):
        fail("Unexpanded Gradle placeholder found in fabric.mod.json")
    if metadata.get("icon") != EXPECTED_ICON_PATH:
        fail(f"Unexpected mod icon metadata: {metadata.get('icon')}")

    audit_icon(archive, names)

    entrypoints = metadata.get("entrypoints", {})
    client_entrypoints = entrypoints.get("client", [])
    if client_entrypoints != [EXPECTED_CLIENT_ENTRYPOINT]:
        fail(f"Unexpected client entrypoints: {client_entrypoints}")
    modmenu_entrypoints = entrypoints.get("modmenu", [])
    if modmenu_entrypoints != [EXPECTED_MODMENU_ENTRYPOINT]:
        fail(f"Unexpected Mod Menu entrypoints: {modmenu_entrypoints}")
    if set(entrypoints) != {"client", "modmenu"}:
        fail(f"Unexpected entrypoint keys: {set(entrypoints)}")

    dependencies = metadata.get("depends", {})
    expected_dependency_keys = {"fabricloader", "minecraft", "java"}
    if set(dependencies) != expected_dependency_keys:
        fail(f"Unexpected required dependency keys: {set(dependencies)}")
    if dependencies.get("minecraft") != ">=26.2 <26.3":
        fail(f"Unexpected Minecraft range: {dependencies.get('minecraft')}")
    if dependencies.get("fabricloader") != ">=0.19.5":
        fail(f"Unexpected Fabric Loader range: {dependencies.get('fabricloader')}")
    if dependencies.get("java") != ">=25":
        fail(f"Unexpected Java requirement: {dependencies.get('java')}")
    suggestions = metadata.get("suggests", {})
    if set(suggestions) != {"stepitup", "modmenu"}:
        fail(f"Unexpected suggested dependency keys: {set(suggestions)}")
    if suggestions.get("stepitup") != "*":
        fail("StepItUp must remain an optional suggested dependency")
    if suggestions.get("modmenu") != ">=20.0.1":
        fail("Mod Menu must remain an optional suggested dependency")

    missing_config_files = REQUIRED_CONFIG_FILES - names
    if missing_config_files:
        fail(f"Missing Mod Menu configuration files: {missing_config_files}")
    translations = read_json(archive, "assets/stepup_camera_smoother/lang/en_us.json")
    if set(translations) != EXPECTED_TRANSLATION_KEYS:
        fail(f"Unexpected English translation keys: {set(translations)}")

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
        if any(name.startswith("com/terraformersmc/modmenu/") for name in names):
            fail("Mod Menu classes must never be bundled")
        if any(name.startswith("assets/modmenu/") for name in names):
            fail("Mod Menu assets must never be bundled")
        if any(name.startswith("net/fabricmc/fabric/") for name in names):
            fail("Fabric API classes must never be bundled")
        if any(name.startswith("me/shedaniel/clothconfig2/") for name in names):
            fail("Cloth Config classes must never be bundled")
        if any(name.startswith("me/shedaniel/autoconfig/") for name in names):
            fail("Auto Config classes must never be bundled")
        if any(name.startswith("eu/pb4/placeholders/") for name in names):
            fail("Text Placeholder API classes must never be bundled")
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
