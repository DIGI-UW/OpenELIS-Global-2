from pathlib import Path
import re, json, shutil
root=Path('/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup'); micro=root/'designs/microbiology'; p=micro/'amr-micro-v2-amendments.md'; s=p.read_text()
# Keep the functional requirements, roles and existing acceptance identifiers, not the delta/history scaffolding.
intro='''# Microbiology (AMR) V2 functional requirements

**Baseline:** draft 10.4, synchronized 2026-10-06. Joining existing cases is deferred;
lab-unit transfers preserve separate cases. This is the self-contained V2 functional
baseline, including behavior carried forward from the retired case/worklist documents.
Historical decisions remain in Git; their identifiers are not invented or renumbered here.

This repository owns functional requirements, workflows and mocks.
[Engineering decisions and roadmap](https://github.com/DIGI-UW/OpenELIS-Global-2/tree/polished-shannon/specs/amr)
belong to the application repository. [OGC-1383](https://uwdigi.atlassian.net/browse/OGC-1383)
and environmental follow-on [OGC-1382](https://uwdigi.atlassian.net/browse/OGC-1382)
track ownership, dependencies and acceptance evidence, not implementation contracts.
Documentation and prototype behavior do not establish implementation or clinical acceptance.

**Mocks:** [clinical case and Worklist](amr-micro-v2-preview.html),
[interactive case](amr-micro-v2-mockup.jsx),
[environmental case](m-18-environmental-microbiology.html).

A **micro test** has Opens a Microbiology case enabled in the catalog. Its
**Case role** is Culture, Direct or Case. A **Program** supplies questions and a
reporting track, never routing. The **case lab unit** determines its worklist and
mutation rights; transferring it does not change its tests or identity.

'''
s=intro+s[s.index('## Lab Context'):s.index('## Localization')]+'''## Localization

Every visible label, help message, status, validation message and dictionary value
must be localized consistently with its owning shared screen. Dates, times and
numbers use the laboratory's configured locale; labels must remain usable at
mobile widths and with a keyboard. Translation implementation belongs to engineering.

'''+s[s.index('## Acceptance Criteria'):s.index('## Crosscheck')]+s[s.index('## Later (not in v2)'):s.index('## Preview follow-ups')]
s=re.sub(r'### Navigation & URL\n.*?(?=## User Stories)', '### Navigation\n\nThe Worklist opens the case at the relevant row. Case search, patient history,\nshared order entry, Inventory, Test Catalog, Workplan and report views remain\nlinked entry points. Filters and focus survive navigation and reload.\n\n',s,flags=re.S)
s=re.sub(r'^> \*\*Contradicts V1\*\*\n(?:>[^\n]*\n|\n(?=>))*','',s,flags=re.M)
s=s.replace('## Amendments','## Functional requirements')
s=s.replace('### A-16. Built capabilities carried forward (nothing on develop is dropped silently)', '### A-16. Preservation boundary')
a=s.index('### A-16.');b=s.index('### A-17.')
s=s[:a]+'''### A-16. Preservation boundary

The requirements below preserve observable behavior, not legacy storage or runtime
configuration. Identification history is owned by A-10; measurements, attempts,
interpretation and panel versions by A-07; amendments and issued history by A-17;
lot eligibility by A-05/A-06/A-07; callback history by A-18. A-12 retains worklist
views, filters, paging, refresh, shared-queue indicators and due actions.

Existing cases keep their identities, members, clinical meaning, issued reports
and attributable history during cutover. No workflow classifier, protocol setup
screen or retained legacy runtime/storage authority remains. Applied migration
history and clinical history are different: engineering owns their transformation.

'''+s[b:]
replacements={
'01.3':'**Culture setups are retired.** Remove the protocol-lane administration and case controls. Previously recorded clinical actions and their attributable history remain readable, without retaining an editable or read-only legacy configuration authority.',
'01.5':'**AST and DST panels** are offered by organism group, with an interpretation model of clinical breakpoints or TB critical concentrations (FR-07.3), never by a case workflow type.',
'01.6':'**Cutover preserves clinical meaning.** Existing cases keep their identities, members, results, issued reports and attributable history. Cases that would group together under the new rules remain separate and are listed for review. Cases missing a Program are assigned the clinically corresponding configured Program and listed for review with a Timeline explanation. Unresolved classifications require review; they must not fabricate clinical meaning. No retained legacy workflow or culture-setup authority remains. Engineering owns the one-way transformation.',
'01.7':'**History stays readable.** Earlier changes to workflow, protocol and purpose remain understandable in the Timeline with their original actor, time and reason.',
'01.8':'**Retired controls:** Change workflow, Set/change protocol, Culture setups administration, workflow-based panel filtering and Unassigned setup. No user is required to classify a workflow to work a case.',
'02.6':'**Change lab unit** transfers the case to the destination Worklist and edit rights. Record who, when, from and to on the Timeline; no reason is required. The case keeps its identity, Program, member samples and tests. Offer only eligible lab units in its domain, and require Results rights in both units. If another case already has the same order, sample type and destination unit, **both remain separate**. Joining existing cases is deferred. Renaming, splitting or merging lab units never merges cases. A deactivated unit leaves its cases searchable with a prompt to transfer. Transfer after final release requires an amendment (A-17).',
'03.6':"**Program questions use the Program questionnaire.** Case information and order entry show the same answers for the order and Program; an edit in either place appears in the other, including its configured FHIR mirror. Each answer shows where it was last changed, by whom and when. Required questions are required before final report; changing Program changes the questions and retains earlier answers in history. TB history, treatment month and specimen register details are questionnaire questions, not additional case fields. Culture purpose, patient origin, optional admission date, diagnosis/history and prior antibiotics remain as described in FR-03.2. CSV export of questionnaire answers is later work.",
'05.5a':'**Culture outcomes drive ordinary configured rules.** A positive, no-growth or contaminated culture outcome is available to the same reflex and critical rules as other results. A positive-bottle rule can add Gram stain, culture on that bottle. Administrators can inspect, change or disable the rule; repeated signals do not add duplicates.',
'10.1c':'**Isolate samples are shared with referral and sequencing.** When an isolate needs labels, referral or tests beyond identification and susceptibility, it is handled as an Isolate sample derived from the original specimen, with its source and producer preserved. It belongs to the same case, is not accessioned twice, and its tests are entered and validated in that case. Isolate storage remains future scope.',
'10.2':'A test on a culture marked **In lab only** never appears on a patient report or requester delivery and does not unlock a partial release. Surveillance eligibility follows A-19 independently; the flag must not silently exclude an otherwise eligible measurement. Turning it off returns it to the Report choices unticked.',
'11.4':'**What prints.** Only selected validated results print, with exactly one chosen reading per agent when attempts overlap. Send with Result notes print beside their target; outside results name their performing laboratory; critical calls print as callback lines. Earlier attempts and issued report versions remain inspectable.',
'11.5':'**Per-agent reporting.** Always, Cascade and Suppress unless resistant provide the defaults in FR-11.2. Where panels or attempts overlap, explicitly choose the validated reading for each agent, never silently replace the entire selected panel. Preserve original readings, versions and every choice with who, when and why.',
'11.6':'**Structured printed content.** Printed reports use the case groups and susceptibility blocks, with no short-text remark limit or mandatory culture-setup report mapping. Existing electronic result delivery must continue without losing content; structured electronic microbiology delivery is separate future work.',
'11.7':'**Shared report lifecycle.** Validated and released case results carry the same release date, signature and Partial/Final/Amended presentation as other laboratory results.',
'13.1b':'**Shared note behavior.** Case, result, culture-row, isolate and drug notes use the same internal/external choices, attribution, validation visibility and report placement as ordinary result notes. No duplicate entry is required.',
'18.1':'**Raising a critical.** Numeric and coded criticals follow the test catalog. Configured organism/phenotype rules also cover CRE, MRSA, meningococci in blood or cerebrospinal fluid, a positive blood-culture Gram, positive AFB smear and TB resistance. Recipients include the responsible clinician, infection prevention and the National TB Programme as configured. The finding identifies its case, isolate or result and appears in Alerts.',
'18.2':'**One shared callback entry per call attempt.** Logging a call records its finding, result/case/isolate context, caller, exact time, recipient and required outcome in the shared callback log. The same attempt is visible on the case and in callback reports; staff never enter it twice.',
'18.2a':'**Shared critical clearance.** Catalog-limit and organism/phenotype criticals both participate in the usual notification and validation acknowledgement workflow. The call retains the actual finding text and its source.',
'18.4':'**Follow-up and corrections.** Retain target, method, contact, message, follow-up needed, acknowledgement and close-with-resolution. Alerts and case badges stay synchronized. Corrections retain the original attempt and attribution.',
'18.5':'**Reports and timing.** The latest call prints under the critical result or isolate it concerns. Attempts count once in shared callback summaries. For a microbiology finding the callback clock begins when that finding is validated.',
'21.1':'**Patient history** lists earlier microbiology cases across orders in lab units the reader may access: date, lab number, specimen/site, Program, organisms, resistance and state, each linked to the case. Use the same patient-results experience; related cases on the same specimen retain their switcher.'}
for key,value in replacements.items():
 s,n=re.subn(r'^\| FR-'+re.escape(key)+r' \|.*$',f'| FR-{key} | {value} |',s,flags=re.M);assert n==1,(key,n)
# Preserve omitted runtime behaviors as functional requirements in their owning sections.
def add_before(section,text):
 global s
 s=s.replace('### A-'+section, text+'\n### A-'+section,1)
add_before('08.', '''#### Susceptibility history and review

Record method and original measurement separately from source: MIC in µg/mL or
zone diameter in mm, plus the selected breakpoint standard/version and matched
level. Changing the active standard or published panel never changes old results.
Retain panel provenance, instrument/software version, instrument interpretation,
expert flags, completion time and message codes. A mismatched organism,
interpretation, missing breakpoint, expert flag or QC failure needs review before
validation; receipt alone is not validation. Show unresolved rows and explain why.

Overrides and reverts require a validator and reason, retain the original and
show the reading history. QC failure supports invalidation and a new attempt or
a justified authorized override. Repeat/retest may cover a whole panel or one
agent, with reason, method and the original attempt retained. Lot eligibility
and requiredness follow the reagent link and method. Panel adjustment records its
reason and preserves the published version used by each attempt. Per-agent
report selection follows FR-11.5. Missing values cannot silently pass review.
''')
add_before('11.', '''#### Identification history and specimen disposition

An isolate can begin with preliminary identification and then receive final
identification, method, date, significance and its source culture. Identification
history retains each previous organism and author. Reidentification requires a
validator and reason; after final release it also requires an amendment. It
never silently reinterprets historical susceptibility readings. Preserve the
sender's reported identification separately from the identification made here.
An unassigned organism is clearly pending; susceptibility setup requires an
identified organism. Default significance may be overridden with attribution.

The case header retains last activity, related cases, progress/next-step guidance,
nonconformance counts and amendment banners. Report nonconformance uses the
shared workflow. Mark specimen lost records the reason and rejects the affected
tests without discarding their work/history; repeat/retest dispositions link back
to the source work. Cancellation and rejection are attributable terminal actions.
''')
add_before('18.', '''#### Issued history and readiness

Opening, cancelling and releasing an amendment each records a reason and actor.
An amended report identifies the version it corrects; the issued original remains
immutable and readable. A released no-growth report remains available when late
growth reopens work through an amendment. No-growth recording alone never
publishes a final report. Readiness names the missing outcome rather than an
isolate when no isolate is applicable; isolate-only checks show Not applicable.
Clinical-release readiness reflects the release action actually available.
Export readiness is separate, clearly labelled, and never implied by final release.
''')
# Remove stale decision-log tokens rather than invent missing historical decisions.
s=re.sub(r'\bD-\d{3}\b','',s)
s=re.sub(r'\(\s*[,;]?\s*\)','',s)
s=s.replace('offers to join when the target lab unit already has a case for the same order and sample type','keeps both cases separate when the target lab unit already has a case for the same order and sample type')
s=s.replace('never appears on a report, a WHONET export or a surveillance submission','never appears on the patient report or requester delivery; WHONET and surveillance apply their own eligibility rules independently')
s=s.replace('every capability marked Kept in A-16 and in the A-12 Worklist list','every capability required by A-16 and the A-12 Worklist list').replace('Every capability marked Kept in A-16 and in the A-12 Worklist list','Every capability required by A-16 and the A-12 Worklist list')
s=s.replace('and its route redirects','and no obsolete setup control is offered').replace(' (409 `FINAL_CASE_LOCKED`)','').replace('(the server returns 403)','(the change is rejected)').replace('(409)','')
s=s.replace('writes one `critical_callback` row with the culture result\'s analysis','records one attributable call in the shared callback log linked to the finding')
s=s.replace('Microbiology medium; an item','Microbiology medium tag; an item').replace('offers the item type Microbiology medium','offers the Microbiology medium type tag')
s=s.replace('with the Urine template','with the culture test’s linked media for Urine').replace('No template: write the media','No media linked: write the media')
s=s.replace('with culture "No culture ordered"','with the culture side shown as "No culture ordered"')
s=s.replace('on an interim report','on a partial report')
s=s.replace('Required before final report, to confirm with CPHL:','Required before final report:')
s=s.replace('Every read and every edit checks the case lab unit','Every worklist read and every edit checks the case lab unit')
s=re.sub(r'The M-00 §4\.1 permission codes.*?\n','Direct links remain read-only for users without rights in the case lab unit.\n',s)
# Replace engineering-only incidental prescriptions while retaining user outcomes.
s=s.replace('The built auto-set, the save guard in `ProgramSection` and the order entry Microbiology section are removed.','There is no automatic Program change, Program save guard or order-entry Microbiology section.')
s=s.replace('the case key','the grouping rule').replace('part of the key','part of the grouping')
s=re.sub(r'\([^)]*`(?:Micro\w+|source_inoculation_id|parent_sample_item_id|micro_isolate|test\.is_reportable|/Results\?run=)[^)]*\)','',s)
s=s.replace('A Run view (`/Results?run=`, OGC-1200)','A Run view').replace('(`LabelPreset`, : no print tracking)','(without print tracking)')
s=s.replace('The Test catalog\'s built Reagents section (`test_reagent_link`, `/rest/test-catalog/{testId}/reagents`)','The Test catalog’s Reagents section')
s=s.replace('(`ComplianceReportRestController`)','').replace('`ComplianceReportRestController`','the shared environmental report')
s=s.replace(' (M-04 §7, matched by container identifier)',' (matched by container identifier)')
s=s.replace('and the M-04 §4.6 checks that still apply','and the validation, QC, identification and required-result checks above')
s=s.replace('The final-report checklist (M-04 §4.6)','The final-report checklist (A-17)')
s=s.replace('with the M1', 'with the M1')
s=s.replace('Microbiology v2 draft 10.4','Microbiology v2 draft 10.4')
# Remove remaining obsolete-module cross-references, retaining surviving module references.
for old,new in [('M-00','V2 preservation (A-16)'),('M-03','V2 reception (A-02)'),('M-04','V2 case (A-10/A-17)'),('M-05','V2 susceptibility (A-07)'),('M-07','V2 Worklist (A-12)'),('M-11','critical-call (A-18)'),('M-14','V2 DST (A-14)')]:
 s=s.replace(old,new)
s=re.sub(r'AC-(?:V2 case \(A-10/A-17\)|V2 reception \(A-02\)|V2 DST \(A-14\))-\d+','the owning requirement above',s)
s=s.replace('the V1 reconciliation gate','the reconciliation gate').replace('kept from V1','required').replace(' (developer to confirm the field mapping)','').replace('(developer to confirm it applies to case results)','').replace('developer to confirm on develop;','')
s=s.replace('Each criterion also meets the M-NFR baseline it touches (NFR-01 offline reads, NFR-02 audit, NFR-04 localization).','Each criterion also meets the [nonfunctional baseline](m-nfr-non-functional-requirements.md) it touches: offline reads/blocked writes, audit, localization, accessibility and performance.')
s=s.replace('Nothing here is built in v2.','Nothing here is included in V2 delivery acceptance.\n\n- Joining existing cases, including at lab-unit transfer, is deferred. Automatic grouping, adding tests and the no-result split remain in V2.\n- [Antibiogram](m-13-antibiogram.md), [GLASS](m-15-glass-fhir-surveillance.md) and [cluster detection](m-16-cluster-detection.md) are future capabilities; their population boundaries inform V2 exports but their delivery is not claimed.')
# Stable source anchors for engineering/Jira links.
s=re.sub(r'^\| (FR-[\d.]+[a-z]?) \|',lambda m:'| <a id="'+m[1].lower()+'"></a>'+m[1]+' |',s,flags=re.M)
s=re.sub(r'^- \*\*(AC-V2-\d+)\*\*',lambda m:'- <a id="'+m[1].lower()+'"></a>**'+m[1]+'**',s,flags=re.M)
p.write_text(s)
for name in ['amr-micro-v2-mockup.jsx','amr-micro-v2-preview.html']:
 p=micro/name;s=p.read_text();s=s.replace('if that lab unit already has a case for this order and sample type, you can join it.','if that lab unit already has a case for this order and sample type, both cases remain separate. Joining existing cases is deferred.').replace('if that lab unit already has a case for this order and sample type, it offers to join it.','if that lab unit already has a case for this order and sample type, both cases remain separate. Joining existing cases is deferred.')
 p.write_text(s)
# Exactly the mapped retirement scope, including copies and archive.
prefixes=('m-00-','m-03-','m-04-','m-05-','m-07-','m-10-','m-11-','m-14-')
extra={'amr-micro-narrative.md','amr-micro-workflow-flow.html','amr-micro-dependency-graph.svg','OGC-782-amr-uat-findings-20260814.md','OGC-782-amr-uat-findings-part2-20260814.md','amr-end-to-end-walkthrough-20260813.md','spec-delta-OGC-782-amr-20260813-1620.md','uat-review-run-OGC-782-amr-20260813.md','m-18-environmental-microbiology-analyze.md','m-18-environmental-microbiology-breakdown.md','m-06-expert-review-prototype.html','m-09-whonet-painless-prototype.html'}
retired=[p for p in micro.iterdir() if p.is_file() and (p.name.startswith(prefixes) or p.name in extra)]
assert len(retired)==31,len(retired)
arch=list((root/'designs/_archive/2026-06-26/microbiology').glob('*'));assert len([p for p in arch if p.is_file()])==18
retired+=arch+[root/'designs/admin-config'/('test-catalog-microbiology-workflow-attribute.'+e) for e in ['md','html']]
paths=[str(p.relative_to(root)) for p in retired];names={p.name for p in retired}
# Remove registry objects by registered source file, retaining unrelated objects byte-for-byte.
p=root/'mockup-viewer/src/App.jsx';s=p.read_text(); removed_names=[]
def entry(m):
 t=m[0]
 if any(name in t for name in names):
  n=re.search(r"name: '([^']+)'",t);removed_names.append(n[1] if n else 'unknown');return ''
 return t
s=re.sub(r'^  \{\n.*?^  \},?\n',entry,s,flags=re.M|re.S)
s=s.replace('Organism / Antibiotic / AST Panel / Culture Protocol masters','Organism / Antibiotic / published AST Panel administration')
s=re.sub(r"(name: 'Microbiology v2 Amendments'.*?description: )'[^\n]*'",r"\1'Self-contained draft 10.4 functional baseline: test-driven case grouping, separate lab-unit transfers, row-local cultures, per-agent reporting, incoming placement, shared callbacks and immutable report history. Joining existing cases is deferred.'",s,flags=re.S)
s=re.sub(r"(name: 'M-18 Environmental Microbiology'.*?description: )'[^\n]*'",r"\1'Environmental follow-on to V2: cases grouped by order, sample type, lab unit and site; no fake patients; culture-media traceability without stock changes; explicit export populations.'",s,flags=re.S)
s=s.replace('a delta over M-00 to M-18','the self-contained V2 baseline')
p.write_text(s)
# YAML manifest object boundaries.
p=root/'MANIFEST.yaml';s=p.read_text();blocks=re.split(r'(?=^- id: )',s,flags=re.M);s=''.join(b for b in blocks if not any(re.search(r'^  path: ["\']?'+re.escape(path)+r'["\']?$',b,re.M) for path in paths));p.write_text(s)
p=root/'INDEX.md';s=p.read_text();s='\n'.join(line for line in s.splitlines() if not any(name in line for name in names) and not any(name in line for name in ['amr-module.','case-workbench.md']))+'\n';s=s.replace('Microbiology v2 Amendments (draft 9)','Microbiology V2 (draft 10.4)');p.write_text(s)
p=root/'mockup-viewer/src/App.test.jsx';s=p.read_text();s=re.sub(r"^    '#/microbiology/m-(?:00|03|04|05|07|10|11|14)-[^\n]*\n",'',s,flags=re.M);s=s.replace("    '#/microbiology/m-nfr-non-functional-requirements',","    '#/microbiology/microbiology-v2-amendments',\n    '#/microbiology/m-18-environmental-microbiology',\n    '#/microbiology/m-nfr-non-functional-requirements',");p.write_text(s)
p=root/'mockup-viewer/scripts/thumbnail-targets.json'; obj=json.loads(p.read_text());print('thumbnail shape',type(obj).__name__)
# file may be a list of target objects; filter using registered name/slugs and retired path.
slugs={re.sub('[^a-z0-9]+','-',n.lower()).strip('-') for n in removed_names}
def dead(x):
 t=json.dumps(x)
 return any(n in t for n in names) or any('/'+slug in t or '__'+slug in t for slug in slugs)
if isinstance(obj,list):obj=[x for x in obj if not dead(x)]
elif isinstance(obj,dict):
 for k,v in obj.items():
  if isinstance(v,list):obj[k]=[x for x in v if not dead(x)]
p.write_text(json.dumps(obj,indent=2)+'\n')
for p in retired:
 if p.exists():p.unlink()
for base in [root/'mockup-viewer/public',root/'mockup-viewer/dist']:
 for p in list(base.rglob('*')):
  if p.is_file() and (p.name in names or ('microbiology' in str(p) and any(slug in p.name for slug in slugs))):p.unlink()
 for folder in [base/'designs/_archive/2026-06-26/microbiology',base/'narrative-figures']:
  if folder.exists():shutil.rmtree(folder)
p=root/'mockup-viewer/scripts/gen-narrative-figures.py'
if p.exists():p.unlink()
Path(__file__).with_name('retired-design-paths.json').write_text(json.dumps({'paths':paths,'registryNames':removed_names,'slugs':sorted(slugs)},indent=2)+'\n')
print('Retired current 31 + archive 18 + shared pair 2; surviving current files:',len(list(micro.iterdir())))
