#!/usr/bin/env python3
"""Interrupt real exports in a disposable local reporting instance."""

import argparse, http.cookiejar, json, os, ssl, subprocess, time, urllib.error, urllib.parse, urllib.request, uuid
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--app-container', required=True)
parser.add_argument('--db-container', required=True)
parser.add_argument('--project', required=True)
parser.add_argument('--base-url', required=True)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
APP, DB, ROOT, out = args.app_container, args.db_container, args.base_url.rstrip('/'), args.output
assert urllib.parse.urlparse(ROOT).hostname in {'localhost', '127.0.0.1', '::1'}
for name in (APP, DB):
    info = json.loads(subprocess.check_output(['docker', 'inspect', name], text=True))[0]
    assert info['State']['Running']
    assert info['Config']['Labels']['com.docker.compose.project'] == args.project
API = '/rest/reports/data-export'
out.mkdir(parents=True, exist_ok=False)
http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()), urllib.request.HTTPSHandler(context=ssl._create_unverified_context()))
csrf = ''

def call(path, body=None, raw=False, form=False):
    data = urllib.parse.urlencode(body).encode() if form else json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(ROOT + path, data=data, headers={'Content-Type': 'application/x-www-form-urlencoded' if form else 'application/json', 'X-CSRF-Token': csrf})
    with http.open(req, timeout=15) as response:
        payload = response.read()
        return payload if raw else json.loads(payload)

def sql(query):
    return subprocess.check_output(['docker','exec',DB,'psql','-X','-v','ON_ERROR_STOP=1','-U','clinlims','-d','clinlims','-Atc',query],text=True).strip()

def submit():
    return call(API+'/jobs', {'schemaVersion':1,'reportType':'SAMPLE_TESTING','layout':'SPREADSHEET','selectedVariables':['accessionNumber','test:174'],'filterSpec':{'dateFrom':'2026-05-05','dateTo':'2026-05-05','labSectionIds':[],'testIds':[],'resultStatuses':['FINALIZED']},'clientRequestId':'restart-'+str(uuid.uuid4())})

call('/ValidateLogin?apiCall=true', {'loginName':os.environ.get('TEST_USER','admin'),'password':os.environ.get('TEST_PASS','adminADMIN!')},form=True)
session = call('/session')
assert session['authenticated']
csrf = session['csrf']
assert sql("select count(*) from clinlims.reporting_export_job where state in ('QUEUED','GENERATING')") == '0'
baseline = submit()
deadline = time.monotonic()+30
while baseline['state'] in ['QUEUED','GENERATING'] and time.monotonic()<deadline:
    time.sleep(.5)
    baseline = call(API+'/jobs/'+baseline['id'])
assert baseline['state']=='READY',baseline
blob = call(API+'/jobs/'+baseline['id']+'/download',raw=True)
assert blob == b'\xef\xbb\xbfAccession Number,Viral Load\r\nREPORTING-MVP-REPEAT,450\r\nREPORTING-MVP-REPEAT,450\r\n'
(out/'baseline.csv').write_bytes(blob)
stall_name = 'reporting-restart-'+uuid.uuid4().hex
locklog = (out/'stall.log').open('w')
stall = subprocess.Popen(['docker','exec','-i','-e','PGAPPNAME='+stall_name,DB,'psql','-X','-v','ON_ERROR_STOP=1','-U','clinlims','-d','clinlims'],stdin=subprocess.PIPE,stdout=locklog,stderr=subprocess.STDOUT,text=True)
jobs=[]
try:
    stall.stdin.write("BEGIN; SET LOCAL lock_timeout='5s'; LOCK TABLE clinlims.result IN ACCESS EXCLUSIVE MODE; SELECT 'REPORTING_STALL_READY'; SELECT pg_sleep(60); ROLLBACK;\n")
    stall.stdin.close()
    deadline=time.monotonic()+10
    while 'REPORTING_STALL_READY' not in (out/'stall.log').read_text():
        assert stall.poll() is None,'Read-stall process exited'
        assert time.monotonic()<deadline,'Read-stall readiness expired'
        time.sleep(.2)
    jobs=[submit() for _ in range(3)]
    observations=[]
    deadline=time.monotonic()+8
    while time.monotonic()<deadline:
        states=[call(API+'/jobs/'+job['id']) for job in jobs]
        observations.append({'at':time.time(),'states':[s['state'] for s in states]})
        time.sleep(.5)
    ids=','.join("'"+j['id']+"'" for j in jobs)
    workers=sql('select id,state,worker_id,lease_until from clinlims.reporting_export_job where id in ('+ids+') order by submitted_at')
    result={'baseline':baseline,'jobs':states,'observations':observations,'workers':workers,'expectedWorkersPerApplication':1,'observedGenerating':sum(s['state']=='GENERATING' for s in states),'project':args.project}
    (out/'worker-probe.json').write_text(json.dumps(result,indent=2)+'\n')
    assert any(s['state']=='GENERATING' for s in states) and any(s['state']=='QUEUED' for s in states)
    paths=subprocess.check_output(['docker','exec',APP,'find','/var/lib/openelis-global/reporting','-maxdepth','1','-type','f'],text=True).splitlines()
    partials=[p for p in paths if any(p.rsplit('/',1)[-1].startswith(j['id']+'.') for j in states if j['state']=='GENERATING')]
    assert len(partials)==result['observedGenerating'],partials
    result['partialPaths']=partials
    for job in states:
        if job['state']=='GENERATING':
            try: call(API+'/jobs/'+job['id']+'/download',raw=True)
            except urllib.error.HTTPError as error: assert error.code==409
            else: raise AssertionError('Generating output was downloadable')
    before=json.loads(subprocess.check_output(['docker','inspect',APP],text=True))[0]
    result['beforeProcess']={k:before['State'][k] for k in ['Pid','StartedAt','Running']}
    result['beforeContainerId']=before['Id']
    result['databaseContainer']=subprocess.check_output(['docker','inspect','--format','{{.Id}}',DB],text=True).strip()
    subprocess.run(['docker','kill','--signal','KILL',APP],check=True)
    after=json.loads(subprocess.check_output(['docker','inspect',APP],text=True))[0]
    assert not after['State']['Running']
    result['stoppedProcess']={k:after['State'][k] for k in ['Pid','StartedAt','FinishedAt','Running','ExitCode']}
    result['persistedAfterKill']=sql('select id,state,worker_id,lease_until from clinlims.reporting_export_job where id in ('+ids+') order by submitted_at')
    (out/'interruption.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'stopped':result['stoppedProcess'],'states':[s['state'] for s in states],'partialFiles':len(partials),'evidence':str(out/'interruption.json')},indent=2),flush=True)
finally:
    sql("select pg_cancel_backend(pid) from pg_stat_activity where application_name='"+stall_name+"'")
    stall.wait(timeout=10)
    locklog.close()
