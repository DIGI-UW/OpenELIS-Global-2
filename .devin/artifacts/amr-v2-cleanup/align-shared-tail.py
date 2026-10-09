from pathlib import Path
import re,json,subprocess
r=Path("/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup");m=r/"designs/microbiology";v2="amr-micro-v2-amendments.md"
# Shared catalog in its existing Basic Info and Reagents sections.
p=r/'designs/admin-config/test-catalog.md';s=p.read_text()
heads=[x for x in re.findall(r'^#{2,4} .*$',s,re.M) if 'Basic' in x or 'Reagent' in x];print('catalog heads',heads)
# Insert at the substantive section, not the localization-key table.
for term,text in [('Basic','''**Microbiology V2:** Basic Info includes **Opens a Microbiology case** (default
No), **Case role** (Culture, Direct or Case; shown when enabled, default Direct)
and **Collected in sets** for culture tests. These replace the workflow/culture-type
attribute. This switch is independent of the existing **AMR** surveillance flag
and its WHONET fields (OGC-936/952 keep their owners). Case tests route a sample
to their lab unit but hold no result. Reportable supplies the default In lab only
choice. See [catalog behavior](../microbiology/amr-micro-v2-amendments.md#fr-01.1).

'''),('Reagent','''**Microbiology V2 — Reagents and media:** culture tests link Inventory media
by optional sample type, display order, duration/unit, check interval and optional
loop volume, atmosphere and temperature. Start inoculation proposes these editable
rows. Media links carry no quantity and never consume stock. Other reagent links
retain their method, requiredness and eligibility behavior. There is no separate
plating-template or culture-protocol administration. See [media defaults](../microbiology/amr-micro-v2-amendments.md#fr-05.2a).

''')]:
 matches=[mt for mt in re.finditer(r'^#{2,4} .*'+term+r'.*$',s,re.M|re.I) if mt.start()>s.find('## Localization')]
 if matches:mt=matches[0];s=s[:mt.end()]+'\n\n'+text+s[mt.end():]
 else:raise Exception('missing catalog section '+term)
p.write_text(s)
p=r/'designs/inventory/inventory-redesign.md';s=p.read_text();pos=s.index('### 4.3 Receive');s=s[:pos]+'''**Microbiology media:** **Microbiology medium** is a type tag and **Track lots**
is the shared per-item setting. Tracked media need a usable lot; in-house batches
are received as lots with batch number, preparing laboratory, preparation/expiry
information, QC status and storage. Optional usual atmosphere and temperature
pre-fill culture rows. A lab unit may require tracked media; then the bench cannot
create an untracked medium or save a new culture row without a usable lot.

**Used on cultures** shows each lot's date-filtered count and culture rows (lab
number, container, date, technician). This is traceability: creating, editing or
undoing a culture row never changes stock. Receipt, consumption and adjustment
remain Inventory actions; non-media reagent consumption keeps its own policy.
See [V2 media](../microbiology/amr-micro-v2-amendments.md#fr-05.1b).

'''+s[pos:];p.write_text(s)
p=r/'designs/reports/patient-report-redesign.md';s=p.read_text();a=s.index('| FR-A42 |');b=s.index('The legend (FR-A24)',a);part=s[a:b]
part=part.replace('Previous report','Tested elsewhere').replace("whose defaults come from each agent's report rule, `report_behavior`, and which the validator can change","whose defaults come from each agent’s report rule and which a user with Results rights in the case lab unit can change before final")
part=part.replace("Agents come from all the isolate's reportable runs, one reading per agent","Agents come from all the isolate’s validated attempts, with one explicitly selected reading per agent")
part=re.sub(r'The call is read from `critical_callback`.*?only its placement uses the micro record\'s target\.', 'The same call appears once in the shared callback log and report, at the finding it belongs to.',part)
part=re.sub(r'Case release writes each reportable analysis\'s validated and released status, release date and signature, so FR-A11 and FR-A36 treat micro rows like any other\.', 'Case release carries the shared validated/released status, release date and signature, so FR-A11 and FR-A36 treat micro rows like any other.',part)
part=re.sub(r'\bD-\d{3}\b','',part);part=re.sub(r'\(\s*\)','',part)
s=s[:a]+part+s[b:];s=s.replace('The legend (FR-A24)', '**Separate eligibility:** In lab only and Report choices control patient delivery.\nThey do not decide surveillance inclusion. See [V2 report choices](../microbiology/amr-micro-v2-amendments.md#fr-15.6)\nand [release history](../microbiology/amr-micro-v2-amendments.md#fr-17.7).\n\nThe legend (FR-A24)',1);p.write_text(s)
# Nonfunctional baseline: preserve budgets, replace offline writes and retired Hub requirements.
p=m/'m-nfr-non-functional-requirements.md';s=p.read_text();s=re.sub(r'^- Data-entry surfaces.*$', '- All case and worklist writes are blocked while offline, with an explanation. Previously loaded data remain readable; there is no local save queue or replay on reconnect. Reconnect and refresh before trying the action again.',s,flags=re.M)
s=re.sub(r'\*\*M-10 Hub Subscription — offline behavior.*?(?=\*\*Out of scope)', '',s,flags=re.S)
s=re.sub(r'^\*\*Out of scope for Phase 1:.*$', '**Offline boundary:** previously loaded information is readable; no offline writes, save queues or automatic replay. A new export requires a connection.',s,flags=re.M)
s=re.sub(r'^- AC-NFR-01-02:.*$', '- AC-NFR-01-02: Disconnect, attempt a case/worklist edit, and verify the action is disabled and no success or saved change is shown.',s,flags=re.M)
s=re.sub(r'^- AC-NFR-01-03:.*$', '- AC-NFR-01-03: Reconnect after another user changed the row; refresh and verify there is no queued overwrite or automatic replay.',s,flags=re.M)
s=re.sub(r'^.*(?:AC-NFR-01-06|\| M-10 Hub|M-09/M-10).*(?:\n|$)','',s,flags=re.M)
s=re.sub(r'^\*\*Reuse vs\. build:.*$', '**Audit outcome:** attributable, immutable history records each action and its original values. Engineering owns persistence.',s,flags=re.M)
s=s.replace('Every critical-result notification writes a `micro_critical_notification` row per M-11.','Every critical call is attributable and appears once in the shared callback log (V2 A-18).')
p.write_text(s)
# Reference data: protocols are retired, organism/antibiotic/panel behavior remains.
p=m/'m-01-amr-reference-data.md';s=p.read_text();s=re.sub(r'## 6\. Culture Protocols.*?(?=## 7\.)','## 6. Culture media defaults\n\nCulture protocols and workflow-based setup administration are retired. Editable\nmedia defaults live on the culture test’s Reagents and media links; see\n[V2 media defaults](amr-micro-v2-amendments.md#fr-05.2a).\n\n',s,flags=re.S)
s=re.sub(r'^.*\|.*(?:culture_protocol|Culture Protocol|Culture-workflow|workflow_type).*\n','',s,flags=re.M)
s=s.replace('Organism, Antibiotic, AST Panel, and Culture Protocol','Organism, Antibiotic and published AST Panel').replace('Organism, Antibiotic, AST Panel, Culture Protocol','Organism, Antibiotic and published AST Panel')
s=re.sub(r'^- \*\*AC-M01-C-.*\n','',s,flags=re.M)
s=s.replace('AST Runs keep the version they were set up against','susceptibility attempts keep the published version used at setup')
p.write_text(s)
# Reagent functional exception: no stock events for culture media.
p=m/'m-12-test-reagent-linkage.md';s=p.read_text();s=s.replace('\n## 1.', '\n**V2 culture-media boundary:** culture medium/lot selection is traceability only: no quantity, stock consumption or automatic credit on undo. Other reagents keep their requiredness, method and stock policies. See [V2 media](amr-micro-v2-amendments.md#fr-05.1b) and [shared Inventory](../inventory/inventory-redesign.md).\n\n## 1.',1);p.write_text(s)
# Future artifacts remain clearly future, not acceptance evidence.
for name in ['m-13-antibiogram.md','m-15-glass-fhir-surveillance.md','m-16-cluster-detection.md','glass-amr-dashboard-indicators.md','glass-on-aspect-parity.md']:
 p=m/name;s=p.read_text();idx=s.index('\n');s=s[:idx]+'''\n\n> **Future capability, outside V2 delivery.** This document defines follow-on
> outcomes; it is not a V2 prerequisite or implementation/acceptance claim.
> Population boundaries follow [V2 selection](amr-micro-v2-amendments.md#fr-19.2).
'''+s[idx:];p.write_text(s)
# Explicit export preservation after engineering doc consolidation.
p=m/'m-09-whonet-export.md';s=p.read_text();idx=s.index('\n');s=s[:idx]+'''\n\n## V2 population and history baseline

Select by Program reporting track, purpose and producer, never workflow type.
Reporting-period membership uses specimen collection date. First-isolate chronology
is independently configurable (collection or final release), with 7/14/30-day
windows or disabled deduplication, same-source choice, contaminant filtering and
changed-interpretation policy; it never changes period membership. Missing mappings
exclude affected rows with reasons; Generate is blocked only when no valid rows
remain. Preview and generation use the same eligible population and policy.
Each generated file records authenticated actor, time, selection, counts, filename
and content fingerprint. Clinical release readiness and export readiness differ.
In lab only affects patient delivery, not surveillance eligibility. Received isolates
use original specimen type and local-result provenance; sender results remain external.
See [V2 populations](amr-micro-v2-amendments.md#fr-19.2).
'''+s[idx:];p.write_text(s)
# Retired links in active source: point to scoped V2 requirements, not redirects.
retired=json.loads(Path(__file__).with_name('retired-design-paths.json').read_text());mapping={}
for path in retired['paths']:
 name=Path(path).name
 anchor=''
 if name.startswith('m-03'):anchor='#fr-02.3'
 elif name.startswith('m-04'):anchor='#fr-17.6'
 elif name.startswith('m-05'):anchor='#fr-07.2b'
 elif name.startswith('m-07'):anchor='#fr-12.1'
 elif name.startswith('m-11'):anchor='#fr-18.1'
 elif name.startswith('m-14'):anchor='#fr-14.1'
 mapping[name]=v2+anchor
for folder in ['designs','features-inventory','docs-manual']:
 for p in (r/folder).rglob('*'):
  if not p.is_file() or p.suffix not in ['.md','.yaml','.json','.html','.jsx']:continue
  if '_archive' in p.parts:continue
  s=p.read_text();old=s
  for name,target in mapping.items():
   if name in s:s=s.replace(name,target)
  s=s.replace('https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1315209256','https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/'+v2)
  s=s.replace('https://uwdigi.atlassian.net/wiki/spaces/OG/pages/1315209256','https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/'+v2)
  if s!=old:p.write_text(s)
# Remove obsolete narrative image payloads wherever published; generator history identifies location.
old=subprocess.check_output(['git','show','HEAD:mockup-viewer/scripts/gen-narrative-figures.py'],cwd=r,text=True)
print('figure directories', [line for line in old.splitlines() if 'DIR' in line or 'figures' in line][:12])
print('Shared functional sections aligned.')
