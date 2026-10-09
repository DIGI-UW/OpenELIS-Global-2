from pathlib import Path
import re,json,subprocess
r=Path('/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup');m=r/'designs/microbiology'
v2='amr-micro-v2-amendments.md'
p=m/v2;s=p.read_text()
s=re.sub(r'\(,?\s*(?:keeps|following|amends|as amended by|supersedes|replaces|withdrawing)?\s*[,;]?\s*\)','',s)
s=re.sub(r'\(,\s*','(',s);s=re.sub(r'\(;\s*','(',s);s=s.replace('(: no new per-action permission keys)','(existing role bundles)')
s=s.replace('**Culture sets** .','**Culture sets**.').replace('**Microbiology medium items** (following ).','**Microbiology medium items**.')
s=re.sub(r'\(`panelProvenance: ORGANISM_DEFAULT`[^)]*\)(?: A-REUSE-2, AC-M01[^)]*\))?','',s)
s=s.replace('(`report_behavior`: Always on; Cascade per the cascade rule; Suppress unless resistant on only when R)','(Always on; Cascade per its rule; Suppress unless resistant on only when R)')
s=s.replace('(built `OrderReferOutSection`, `OrderReferOutForm` and `ReferralStatusTag`)','')
s=s.replace('(`critical_callback`, OGC-714)','(OGC-714)').replace('(`status=attention`)','').replace('(`/PatientResults`)','').replace('`/PatientResults`','the patient results view').replace('read through `/fhir`','in the configured FHIR store')
s=s.replace('(`ORGANISM_DEFAULT`)','').replace(' (`CLINICAL_DIAGNOSTIC`, built)','').replace(' (`ACTIVE_SCREENING`, built)','').replace(' (`TREATMENT_FOLLOW_UP`)','').replace(' (`SURVEY_STUDY`)','').replace(' (`EQA`; a purpose tag only until the EQA V2 controller exists, )',' (a purpose tag; EQA scoring is out of scope)').replace('(built `includeScreening`)','')
s=re.sub(r'(AC-V2-22\*\*)[^\n]*',r'\1 Every visible label, help message, validation message, status and dictionary value is localized consistently with its owning shared screen; dates, times and numbers use the configured locale.',s)
s=s.replace(' (the server returns 403)',' (the change is rejected)').replace(' (403)','').replace('the V1 behaviour','the required behavior').replace('v2 A-01','A-01')
s=s.replace('**Program** (FR-03.7), and the questions','**Program** (FR-03.7), and the questions')
s=s.replace('All other Case information fields are optional.','All other Case information fields, including admission date, are optional. Missing admission date leaves infection origin unknown; it never blocks release or invents a value.')
s=s.replace('This amendment says what the case sends to it.','This section defines the report choices and visible output.')
s=s.replace('the reportable AST attempt (**Use for reporting**)','the selected validated reading per agent (**Use for reporting**)')
s=s.replace('(`microbiology.*`, or the `order.*`, `catalog.*`, `inventory.*`, `validation.*` and `siteInfo.*` keys this table lists for those screens)','')
s=s.replace('V2 case (A-10/A-17) §4.6','A-17').replace('V2 DST (A-14) §7.1','A-19').replace('V2 DST (A-14) categories','A-14 categories')
s=s.replace('Readiness','Readiness')
p.write_text(s)
# Environmental requirements: keep functional sections, remove obsolete engineering/decision sections.
p=m/'m-18-environmental-microbiology.md';s=p.read_text();s='''# Environmental Microbiology functional requirements

**Baseline:** environmental follow-on aligned to Microbiology V2 draft 10.4.
[OGC-1382](https://uwdigi.atlassian.net/browse/OGC-1382) is blocked by
[OGC-1383](https://uwdigi.atlassian.net/browse/OGC-1383). This is functional
scope, not a schema or routing implementation. See the [V2 baseline](amr-micro-v2-amendments.md)
and [preview](m-18-environmental-microbiology.html).

An eligible test opens a case; Program does not. Group samples by **order,
sample type, lab unit and site**, retaining each sampling point. Transfers keep
cases separate. Joining existing cases is deferred; a member without results
may be split with a reason. Media defaults come from catalog test media links;
culture rows record lots without changing stock.

'''+s[s.index('## Lab Context'):]
s=re.sub(r'### Navigation & URL\n.*?(?=## User Stories)','',s,flags=re.S)
s=re.sub(r'## Information & Data\n.*?(?=## Access)','',s,flags=re.S)
s=re.sub(r'## Crosscheck\n.*?(?=## Out of Scope)','',s,flags=re.S)
s=re.sub(r'## Dependencies\n.*?(?=## Out of Scope)', '## Dependencies\n\nThe V2 case, shared environmental order entry, Inventory, Test Catalog,\nLocations & Organizations and environmental certificate remain shared owners.\nFuture cluster detection, antibiogram and GLASS are not V2 dependencies or\ndelivery claims.\n\n',s,flags=re.S)
rows={
'C1':'An environmental test with **Opens a Microbiology case** enabled opens or joins a case on the same order, sample type, lab unit and site. Different sites or lab units produce separate cases. Sampling points remain visible per sample and do not replace the site grouping rule. Program is not set or read for routing. | [V2 grouping](amr-micro-v2-amendments.md#fr-02.4).',
'C2':'A sample with no eligible micro test opens no case. Quantitative water/food tests whose switch is off stay in environmental Results with their regulatory limits. | [Catalog switch](amr-micro-v2-amendments.md#fr-01.1).',
'C4':'Purpose and Replicates are entered in Case information. Environmental order entry retains its normal sample/site fields and adds no Microbiology section or protocol. | [Case information](amr-micro-v2-amendments.md#fr-03.1).',
'C5':'Environmental purposes are **Routine monitoring** (default), **Post-cleaning check**, **Outbreak investigation** and **Complaint**. Each case offers its domain’s values, with no duplicate order-entry culture-purpose field. | [Populations](amr-micro-v2-amendments.md#fr-19.6).',
'D1':'An environmental case has a **site**, not a patient, as its subject. Grouping follows order, sample type, lab unit and site. Replicates retain their sampling points and collection details. | [V2 grouping](amr-micro-v2-amendments.md#fr-02.4).',
'D7':'Related cases show each other as in the clinical case. A transfer changes worklist/rights but preserves each case’s identity, members and history even when the destination has a matching case. Joining existing cases is deferred. | [Transfers](amr-micro-v2-amendments.md#fr-02.6).',
'B3':'Tests whose Opens a Microbiology case switch is off remain on ordinary Results. Case-owned tests never acquire a second editing path. | No retained legacy routing authority.'}
for k,v in rows.items():s,n=re.subn(r'^\| FR-'+k+r' \|.*$',f'| FR-{k} | {v} |',s,flags=re.M);assert n==1,k
s=s.replace('A generic culture test opens a Case in any domain','Any eligible micro test opens a case in its domain')
s=re.sub(r'^3\. \*\*Routing follows.*$', '3. **Routing follows eligible tests.** Group by order, sample type, lab unit and site; Program is independent. Transfers preserve separate cases.',s,flags=re.M)
s=s.replace('(`micro_case_analysis`)','').replace('Case\'s preliminary and final release','case’s partial and final release').replace('Culture, Growth work-up, Isolates','Culture (with tests and subcultures beneath each row), Isolates')
s=re.sub(r'\bD-\d{3}\b','',s);s=re.sub(r'\(\s*\)','',s)
for old,new in [('M-00','V2'),('M-03','V2 reception'),('M-04','V2 case'),('M-07','V2 Worklist'),('M-11','V2 callbacks')]:s=s.replace(old,new)
s=s.replace('### G. Cluster detection (M-16)','### G. Cluster detection (future, outside V2 delivery)')
s=re.sub(r'one Case per sample and culture type','one case per order, sample type, lab unit and site',s,flags=re.I)
s=re.sub(r'keyed on the sample and culture type','grouped by order, sample type, lab unit and site',s)
s=s.replace('plating templates','catalog media links').replace('Plating templates','Catalog media links')
p.write_text(s)
# Environmental mock text follows the same grouping and catalog language.
for ext in ['html','jsx']:
 p=m/f'm-18-environmental-microbiology.{ext}';s=p.read_text();s=s.replace('Growth work-up','Culture work-up').replace('plating templates','catalog media links').replace('Plating templates','Catalog media links')
 s=re.sub(r'one [Cc]ase per sample and culture type','one case per order, sample type, lab unit and site',s)
 s=s.replace('sample and culture type','order, sample type, lab unit and site')
 p.write_text(s)
# Shared reception section: actual explicit set assignment, preview and received-isolate provenance.
p=r/'designs/sample-collection/clinical-order-entry-v4.md';s=p.read_text()
s=re.sub(r'^\| FR-B12a \|.*$', '''| FR-B12a | **Microbiology V2.** Pick ordinary catalog tests with **Opens a Microbiology case** on, including direct tests, culture tests and case tests. The first eligible test opens a case; group by order, sample type and lab unit (also site for environmental samples). Program is never set or locked by this choice. There is no Microbiology reception section. Each blood-culture bottle is its own sample with site, collection time and an explicit required Set number; sets on one order group into one case, with count derived from those numbers and nonblocking set warnings. Show **What this order will open** beside Ordered tests as samples/tests change, including ordinary Results rows and relevant reflex rules, without saving. The same grouping applies to the all-or-nothing Save and Edit order. A received Isolate sample can have a culture marked Tested elsewhere with the sending laboratory and optional reported organism; it opens the case with received-isolate provenance, not a primary inoculation. | [V2 reception](../microbiology/amr-micro-v2-amendments.md#fr-02.3b), [sets](../microbiology/amr-micro-v2-amendments.md#fr-02.4a), [received isolates](../microbiology/amr-micro-v2-amendments.md#fr-02.13). |''',s,flags=re.M)
s=s.replace('Microbiology v2 reuses this picker for Previous report (its FR-06.3)','Microbiology V2 reuses this picker for Tested elsewhere (its FR-06.3)')
s=s.replace('A test marked tested elsewhere needs no sample.','A test marked tested elsewhere can be recorded without a sample; a received isolate retains its Isolate sample and sending-laboratory provenance ([V2 FR-02.13](../microbiology/amr-micro-v2-amendments.md#fr-02.13)).')
p.write_text(s)
# Shared catalog in its existing Basic Info and Reagents sections.
p=r/'designs/admin-config/test-catalog.md';s=p.read_text()
heads=[x for x in re.findall(r'^#{2,4} .*$',s,re.M) if 'Basic' in x or 'Reagent' in x];print('catalog heads',heads)
# Insert at the substantive section, not the localization-key table.
for term,text in [('Basic Info','''**Microbiology V2:** Basic Info includes **Opens a Microbiology case** (default
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
 matches=[mt for mt in re.finditer(r'^#{2,4} .*'+term+r'.*$',s,re.M) if mt.start()>s.find('## Localization')]
 if matches:mt=matches[-1];s=s[:mt.end()]+'\n\n'+text+s[mt.end():]
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
