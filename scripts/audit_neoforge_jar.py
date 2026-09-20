"""Audit NeoForge loader isolation and parity of shared camera classes."""
import hashlib
import json
from pathlib import Path
import tomllib
import zipfile

root = Path(__file__).resolve().parents[1]
props = dict(line.split('=', 1) for line in (root / 'neoforge/gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
jar = root / 'neoforge/build/libs' / ('smart-stepup-camera-smoother-neoforge-' + props['mod_version'] + '.jar')
fabric = root / 'build/libs' / ('smart-stepup-camera-smoother-' + props['mod_version'] + '.jar')
with zipfile.ZipFile(jar) as z, zipfile.ZipFile(fabric) as f:
    names = z.namelist()
    assert len(names) == len(set(names)), 'Duplicate ZIP entries'
    assert 'fabric.mod.json' not in names
    assert not any(n.endswith(('.jar', '.java')) or '/gametest/' in n or '/platform/fabric/' in n or 'ModMenu' in n for n in names)
    metadata = tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
    assert metadata['mods'][0]['modId'] == 'stepup_camera_smoother'
    assert metadata['mods'][0]['version'] == props['mod_version']
    assert metadata['mixins'][0]['config'] == 'stepup_camera_smoother.client.mixins.json'
    mixins = json.loads(z.read(metadata['mixins'][0]['config']))
    assert mixins['required'] and mixins['injectors']['defaultRequire'] == 1
    assert mixins['client'] == ['CameraMixin', 'LocalPlayerMixin']
    assert z.read('assets/stepup_camera_smoother/icon.png') == f.read('assets/stepup_camera_smoother/icon.png')
    assert 'LICENSE_smart-stepup-camera-smoother' in names
    for name in names:
        if name.endswith('.class'):
            data = z.read(name)
            assert int.from_bytes(data[6:8], 'big') == 69, name
            assert b'net/fabricmc/' not in data and b'com/terraformersmc/' not in data, name
            if '/platform/neoforge/' not in name:
                assert data == f.read(name), 'Shared class differs: ' + name
checksum = hashlib.sha256(jar.read_bytes()).hexdigest()
output = root / 'neoforge/build/checksums' / (jar.name + '.sha256')
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(checksum + '  ' + jar.name + '\n')
print('PASS: NeoForge metadata, loader isolation, Java 25, mixins, and byte-identical shared classes; SHA-256 ' + checksum)
