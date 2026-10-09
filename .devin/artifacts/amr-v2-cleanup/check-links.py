from pathlib import Path
import re, subprocess, json
from urllib.parse import unquote, urlsplit

root=Path('/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup')
app=Path('/Users/pmanko/.windsurf/worktrees/OpenELIS-Global-2/OpenELIS-Global-2-polished-shannon')
changed=subprocess.check_output(['git','diff','--name-only','--diff-filter=AM'],cwd=root,text=True).splitlines()
files=[root/x for x in changed if x.endswith('.md') and not x.startswith('mockup-viewer/dist/')]
files+=list((app/'specs/amr').glob('*.md'))

def ids(path):
    s=path.read_text(errors='replace')
    anchors=set(re.findall(r'\bid=["\']([^"\']+)',s))
    for heading in re.findall(r'^#{1,6}\s+(.+)$',s,re.M):
        heading=re.sub(r'<[^>]+>', '', heading).lower()
        heading=re.sub(r'[^\w\- ]', '', heading).replace(' ', '-')
        anchors.add(heading)
    return anchors

checked=0; errors=[]; preexisting=[]
for file in files:
    s=file.read_text()
    targets=re.findall(r'\]\(([^\s)]+)',s)+re.findall(r'^\[[^\]]+\]:\s*(\S+)',s,re.M)
    for href in targets:
        href=unquote(href.strip('<>'))
        if href.startswith('https://github.com/DIGI-UW/openelis-work/blob/'):
            part=href.split('/designs/',1)
            if len(part)!=2: continue
            target=root/'designs'/part[1].split('#',1)[0]
        elif href.startswith('https://github.com/DIGI-UW/OpenELIS-Global-2/') and '/specs/amr' in href:
            target=app/'specs/amr'/href.split('/specs/amr/',1)[1].split('#',1)[0] if '/specs/amr/' in href else app/'specs/amr'
        elif urlsplit(href).scheme or href.startswith('/'):
            continue
        else:
            target=(file.parent/href.split('#',1)[0]).resolve() if not href.startswith('#') else file
        checked+=1
        if not target.exists():
            if file.is_relative_to(root) and target.is_relative_to(root):
                prior=subprocess.run(['git','show','HEAD:'+str(file.relative_to(root))],cwd=root,capture_output=True,text=True)
                prior_target=subprocess.run(['git','cat-file','-e','HEAD:'+str(target.relative_to(root))],cwd=root,capture_output=True)
                if href in prior.stdout and prior_target.returncode:
                    preexisting.append(href)
                    continue
            errors.append(f'{file.relative_to(root) if file.is_relative_to(root) else file.relative_to(app)}: missing {href}')
        elif '#' in href and target.suffix=='.md':
            fragment=href.split('#',1)[1]
            if fragment and fragment not in ids(target):
                errors.append(f'{file.name}: missing anchor {href}')

print(json.dumps({'checked':checked,'errors':len(errors),'preexisting_unrelated_missing_targets':len(preexisting),'details':errors},indent=2))
raise SystemExit(bool(errors))
