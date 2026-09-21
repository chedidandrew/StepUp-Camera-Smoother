"""Generate an isolated port source set from shared sources and reviewed API adapters."""
import argparse,json,re,shutil
from pathlib import Path
p=argparse.ArgumentParser()
for name in ['minecraft','loader','version','java','modmenu','neo','smoke','output']:p.add_argument('--'+name,required=True)
p.add_argument('--forge',default='')
a=p.parse_args();root=Path(__file__).resolve().parents[1];out=Path(a.output).resolve()
assert out.is_relative_to(root/'ports') and out.name=='port', 'Output must be an isolated generated port directory'
if out.exists():shutil.rmtree(out)
for source in ['src/main/java','src/client/java']:
 shutil.copytree(root/source,out/'java',dirs_exist_ok=True)
shutil.copytree(root/'src/main/resources',out/'resources',dirs_exist_ok=True)
if a.minecraft.startswith('1.20'):
 patch=int(a.minecraft.split('.')[2]) if len(a.minecraft.split('.'))>2 else 0
 (out/'resources/pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':15 if patch<=1 else 18 if patch==2 else 22 if patch<=4 else 32,'description':'Smart StepUp Camera Smoother resources'}}))
base=out/'java/dev/chedidandrew/stepupcamerasmoother'
if a.loader in ['neoforge','forge']:
 shutil.rmtree(base/'platform/fabric')
 (base/'client/StepUpCameraSmootherModMenu.java').unlink()
 shutil.copytree(root/'neoforge/src/main/java',out/'java',dirs_exist_ok=True)
 (out/'resources/fabric.mod.json').unlink()
 meta=(root/'neoforge/src/main/resources/META-INF/neoforge.mods.toml').read_text()
 meta=meta.replace('${version}',a.version).replace('[26.3.0.4-beta,)',f'[{a.neo},)').replace('[26.3,26.4)',f'[{a.minecraft}]')
 if a.minecraft.startswith('1.'):
  meta='modLoader="javafml"\nloaderVersion="[4,)"\n'+meta
 if a.minecraft.startswith('1.') or a.minecraft in ['26.1','26.1.1']:
  meta=meta.replace('iconFile=', 'logoFile=')
 (out/'resources/META-INF').mkdir(exist_ok=True)
 if a.minecraft in ['1.20.5','1.20.6']:
  meta=meta.replace('loaderVersion="[4,)"','loaderVersion="[3,)"')
 if a.loader=='forge':
  meta=meta.replace('loaderVersion="[4,)"', 'loaderVersion="[47,)"' if a.minecraft=='1.20.1' else 'loaderVersion="[49,)"')
  meta=meta.replace('modId="neoforge"','modId="forge"').replace(f'[{a.neo},)',f'[{a.forge.split("-")[1]},)')
  meta=meta.replace('type="required"','mandatory=true').replace('[[mixins]]\nconfig="stepup_camera_smoother.client.mixins.json"\n','')
  (out/'resources/META-INF/mods.toml').write_text(meta)
 else:
  if a.minecraft in ['1.20.3','1.20.4']:
   meta=meta.replace('loaderVersion="[4,)"','loaderVersion="[1,)"' if a.minecraft=='1.20.3' else 'loaderVersion="[2,)"')
  if a.minecraft=='1.20.3':meta=meta.replace('type="required"','mandatory=true')
  (out/'resources/META-INF'/('mods.toml' if a.minecraft in ['1.20.3','1.20.4'] else 'neoforge.mods.toml')).write_text(meta)
else:
 meta=json.loads((out/'resources/fabric.mod.json').read_text())
 meta['version']=a.version;meta['depends']={'fabricloader':'>=0.19.5','minecraft':a.minecraft,'java':'>='+a.java};meta['suggests']['modmenu']='>='+a.modmenu
 (out/'resources/fabric.mod.json').write_text(json.dumps(meta,indent=2))
mixin=out/'resources/stepup_camera_smoother.client.mixins.json';meta=json.loads(mixin.read_text());meta['compatibilityLevel']='JAVA_'+a.java;
if a.loader=='forge':meta['refmap']='stepup_camera_smoother.refmap.json'
mixin.write_text(json.dumps(meta,indent=2))
# Reviewed API adapters only; smoothing math/configuration remain shared.
for path in base.rglob('*.java'):
 s=path.read_text();s=s.replace('initialized for Minecraft 26.3.',f'initialized for Minecraft {a.minecraft}.')
 if a.minecraft.startswith('1.'):
  s=s.replace('camera.entity()', 'camera.getEntity()').replace('camera.position()', 'camera.getPosition()')
  s=s.replace('this.minecraft.gui.setScreen(', 'this.minecraft.setScreen(')
  s=s.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(').replace('graphics.centeredText(', 'graphics.drawCenteredString(')
 if a.minecraft in ['1.21.11']:
  s=s.replace('camera.getEntity()', 'camera.entity()')
 if a.minecraft.startswith('26.1'):
  s=s.replace('this.minecraft.gui.setScreen(', 'this.minecraft.setScreen(')
 if path.name=='NeoForgeClientEntrypoint.java' and (a.loader=='forge' or a.minecraft in ['1.20.3','1.20.4']):
  ns='net.minecraftforge' if a.loader=='forge' else 'net.neoforged'
  factory=ns+('.client.ConfigScreenHandler' if a.loader=='forge' else '.neoforge.client.ConfigScreenHandler')
  s=s.replace('import net.neoforged.neoforge.client.gui.IConfigScreenFactory;', 'import '+factory+';')
  s=s.replace('@Mod(value = StepUpCameraSmootherClient.MOD_ID, dist = Dist.CLIENT)', '@Mod(StepUpCameraSmootherClient.MOD_ID)')
  s=s.replace('public NeoForgeClientEntrypoint(ModContainer container)', 'public NeoForgeClientEntrypoint()')
  s=s.replace('        StepUpCameraSmootherClient.initialize', '        if (!'+ns+'.fml.loading.FMLEnvironment.dist.isClient()) return;\n        StepUpCameraSmootherClient.initialize')
  s=s.replace('container.registerExtensionPoint(IConfigScreenFactory.class,\n                (ignored, parent) -> new SmootherConfigScreen(parent));', ns+'.fml.ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,\n                () -> new ConfigScreenHandler.ConfigScreenFactory((ignored, parent) -> new SmootherConfigScreen(parent)));')
  if a.loader=='forge': s=s.replace('net.neoforged.', 'net.minecraftforge.')
 if path.name=='NeoForgeClientEntrypoint.java' and a.loader=='neoforge' and a.minecraft in ['1.20.5','1.20.6']:
  s=s.replace('@Mod(value = StepUpCameraSmootherClient.MOD_ID, dist = Dist.CLIENT)', '@Mod(StepUpCameraSmootherClient.MOD_ID)')
  s=s.replace('        StepUpCameraSmootherClient.initialize', '        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return;\n        StepUpCameraSmootherClient.initialize')
 path.write_text(s)
# Older camera pipeline adapter is kept as readable source rather than broad substitutions.
if a.minecraft.startswith('1.'):
 shutil.copyfile(root/('ports/compat/CameraMixin120.java' if a.minecraft.startswith('1.20') else 'ports/compat/CameraMixinLegacy.java'),base/'client/mixin/CameraMixin.java')

if a.smoke=='true':
 source=(root/'ports/compat/SmokeTitleMixin.java').read_text()
 if a.minecraft not in ['26.2','26.3']:
  source=source.replace('client.gui.setScreen(', 'client.setScreen(').replace('client.gui.screen()', 'client.screen')
 if a.minecraft.startswith('1.') and tuple(map(int,a.minecraft.split('.'))) < (1,21,9):
  source=source.replace('import net.minecraft.client.input.MouseButtonEvent;', '').replace('import net.minecraft.client.input.MouseButtonInfo;', '')
  source=source.replace('MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));', '')
  source=source.replace('screen.mouseClicked(event, false)', 'screen.mouseClicked(x, y, InputConstants.MOUSE_BUTTON_LEFT)').replace('screen.mouseReleased(event)', 'screen.mouseReleased(x, y, InputConstants.MOUSE_BUTTON_LEFT)')
 p=base/'smoke/SmokeTitleMixin.java';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source)
 if a.loader=='forge':
  shutil.copyfile(root/'ports/compat/SmokeScreenMixin.java',base/'smoke/SmokeScreenMixin.java')
 config='stepup_camera_smoother.smoke.mixins.json'
 (out/'resources'/config).write_text(json.dumps({'required':True,'minVersion':'0.8','package':'dev.chedidandrew.stepupcamerasmoother.smoke','compatibilityLevel':'JAVA_'+a.java,'client':['SmokeTitleMixin']+(['SmokeScreenMixin'] if a.loader=='forge' else []),'injectors':{'defaultRequire':1}}))
 if a.loader=='fabric':
  p=out/'resources/fabric.mod.json';meta=json.loads(p.read_text());meta['mixins'].append(config);p.write_text(json.dumps(meta,indent=2))
 else:
  if a.loader=='neoforge':
   p=out/'resources/META-INF'/('mods.toml' if a.minecraft in ['1.20.3','1.20.4'] else 'neoforge.mods.toml');p.write_text(p.read_text()+'\n[[mixins]]\nconfig="'+config+'"\n')
