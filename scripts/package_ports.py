"""Audit and collect selected playable port artifacts; never collect smoke output."""
import argparse,hashlib,json,shutil,tomllib,zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--only',nargs='*');p.add_argument('--require-smoke',action='store_true');p.add_argument('--output-label',default='0.3.1');a=p.parse_args()
root=Path(__file__).resolve().parents[1];versions=json.loads((root/'ports/versions.json').read_text())
assert a.output_label and all(c.isalnum() or c in '.-_' for c in a.output_label)
records=[];out=root/'build/releases'/a.output_label;out.mkdir(parents=True,exist_ok=True)
icon=(root/'src/main/resources/assets/stepup_camera_smoother/icon.png').read_bytes()
for v in versions:
 mc=v['minecraft']
 for loader in v.get('loaders',['fabric','neoforge']):
  if a.only and mc+'/'+loader not in a.only:continue
  project=root/'ports'/mc/loader
  name=f'smart-stepup-camera-smoother-{loader}-mc{mc}-0.3.1.jar'
  jar=project/'build/libs'/name
  with zipfile.ZipFile(jar) as z:
   names=z.namelist();assert len(names)==len(set(names)),name
   assert not any('/smoke/' in n or '.smoke.' in n or n.endswith(('.java','.jar')) for n in names),name
   assert 'LICENSE_smart-stepup-camera-smoother' in names,name
   assert z.read('assets/stepup_camera_smoother/icon.png')==icon,name
   assert z.read('assets/stepup_camera_smoother/lang/en_us.json')==(root/'src/main/resources/assets/stepup_camera_smoother/lang/en_us.json').read_bytes(),name
   mixin=json.loads(z.read('stepup_camera_smoother.client.mixins.json'))
   assert mixin['required'] and mixin['injectors']['defaultRequire']==1,name
   assert mixin['compatibilityLevel']=='JAVA_'+str(v['java']),name
   assert mixin['client']==['CameraMixin','LocalPlayerMixin'],name
   if loader=='fabric':
    meta=json.loads(z.read('fabric.mod.json'));assert meta['version']=='0.3.1' and meta['environment']=='client',name
    assert meta['depends']['minecraft']==mc and meta['depends']['java']=='>='+str(v['java']),name
    assert 'META-INF/neoforge.mods.toml' not in names,name
   else:
    metadata_path='META-INF/mods.toml' if loader=='forge' or mc in ['1.20.3','1.20.4'] else 'META-INF/neoforge.mods.toml'
    meta=tomllib.loads(z.read(metadata_path).decode())
    assert meta['mods'][0]['version']=='0.3.1',name
    assert meta['mods'][0]['modId']=='stepup_camera_smoother',name
    assert meta['mods'][0]['logoFile' if mc.startswith('1.') or mc in ['26.1','26.1.1'] else 'iconFile']=='assets/stepup_camera_smoother/icon.png',name
    deps=meta['dependencies']['stepup_camera_smoother'];assert any(d['modId']=='minecraft' and d['versionRange']=='['+mc+']' for d in deps),name
    assert 'fabric.mod.json' not in names,name
   for n in names:
    if n.endswith('.class'):
     data=z.read(n);assert int.from_bytes(data[6:8],'big')==v['java']+44,(name,n)
     assert b'STEPUP_PORT_SMOKE_PASS' not in data,name
     if loader!='fabric':assert b'net/fabricmc/' not in data and b'com/terraformersmc/' not in data,(name,n)
     if loader=='fabric':assert b'net/neoforged/' not in data and b'net/minecraftforge/' not in data,(name,n)
     if loader=='forge':assert b'net/neoforged/' not in data,(name,n)
   if loader=='fabric' and mc.startswith('1.'):
    camera=z.read('dev/chedidandrew/stepupcamerasmoother/client/mixin/CameraMixin.class')
    assert b'net/minecraft/class_' in camera and b'net/minecraft/client/Camera' not in camera,name
   if loader=='forge':
    assert mixin['refmap']=='stepup_camera_smoother.refmap.json',name
    refmap=json.loads(z.read(mixin['refmap']));assert refmap.get('mappings'),name
    assert b'MixinConfigs: stepup_camera_smoother.client.mixins.json' in z.read('META-INF/MANIFEST.MF'),name
    assert b'm_' in z.read('dev/chedidandrew/stepupcamerasmoother/client/CameraMotion.class'),name
  reports=list((project/'build/test-results/test').glob('TEST-*.xml'));assert reports,name
  suites=[ET.parse(p).getroot() for p in reports];tests=sum(int(s.attrib['tests']) for s in suites)
  assert tests==25 and all(int(s.attrib.get('failures',0))+int(s.attrib.get('errors',0))+int(s.attrib.get('skipped',0))==0 for s in suites),name
  log=root/'build/ports'/f'{mc}-{loader}-smoke.log'
  text=log.read_text(errors='replace') if log.exists() else ''
  smoke='STEPUP_PORT_SMOKE_PASS' in text and 'BUILD SUCCESSFUL' in text
  if a.require_smoke:assert smoke,'Missing successful real-client test: '+name
  shutil.copy2(jar,out/name)
  records.append(dict(minecraft=mc,loader=loader,version='0.3.1',java=v['java'],file=name,bytes=jar.stat().st_size,sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),unit_tests=tests,client_smoke=smoke))
shutil.copy2(root/('docs/releases/0.3.1-additional.md' if a.output_label=='0.3.1-additional' else 'docs/releases/0.3.1.md'),out/'CHANGELOG.md')
(out/'manifest.json').write_text(json.dumps(records,indent=2)+'\n')
(out/'SHA256SUMS.txt').write_text(''.join(r['sha256']+'  '+r['file']+'\n' for r in records))
if not a.only or a.output_label!='0.3.1':
 with zipfile.ZipFile(out.parent/('smart-stepup-camera-smoother-'+a.output_label+'-all-ports.zip'),'w',zipfile.ZIP_DEFLATED) as z:
  for name in [r['file'] for r in records]+['manifest.json','SHA256SUMS.txt','CHANGELOG.md']:z.write(out/name,name)
print(f'PASS: {len(records)} audited playable JARs, exact version metadata, Java levels, loader isolation, unit results, and release checksums')
