"""Build the complete version/loader matrix with bounded parallelism."""
import argparse,concurrent.futures,json,os,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--workers',type=int,default=2);p.add_argument('--only',nargs='*');p.add_argument('--smoke',action='store_true');a=p.parse_args()
root=Path(__file__).resolve().parents[1];logs=root/'build/ports';logs.mkdir(parents=True,exist_ok=True)
versions=json.loads((root/'ports/versions.json').read_text())
plans=[(v['minecraft'],l) for v in versions for l in ['fabric','neoforge'] if not a.only or v['minecraft']+'/'+l in a.only]
def run(plan):
 mc,loader=plan;key=mc+'-'+loader;log=logs/(key+('-smoke' if a.smoke else '')+'.log')
 java=str(Path(os.environ['JAVA_HOME'])/'bin/java.exe') if os.name=='nt' else str(Path(os.environ['JAVA_HOME'])/'bin/java')
 cmd=[java,'-classpath',str(root/'gradle/wrapper/gradle-wrapper.jar'),'org.gradle.wrapper.GradleWrapperMain','-p',str(root/'ports'/mc/loader),'--no-daemon','--console=plain']
 cmd+=['-PsmokeTest=true','runClient'] if a.smoke else ['build']
 with log.open('w') as f:
  process=subprocess.Popen(cmd,cwd=root,stdout=f,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0,start_new_session=os.name!='nt')
  try:code=process.wait(timeout=300 if a.smoke else 1200)
  except subprocess.TimeoutExpired:
   if os.name=='nt':subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],stdout=f,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
   else:
    import signal
    os.killpg(process.pid,signal.SIGKILL)
   process.wait();code=124
 success=code==0 and (not a.smoke or 'STEPUP_PORT_SMOKE_PASS' in log.read_text(errors='replace'))
 print(('PASS' if success else 'FAIL')+': '+key,flush=True)
 return dict(minecraft=mc,loader=loader,passed=success,exit_code=code,log=str(log.relative_to(root)))
with concurrent.futures.ThreadPoolExecutor(max_workers=a.workers) as pool: results=list(pool.map(run,plans))
(logs/('smoke-results.json' if a.smoke else 'build-results.json')).write_text(json.dumps(results,indent=2)+'\n')
raise SystemExit(0 if all(r['passed'] for r in results) else 1)
