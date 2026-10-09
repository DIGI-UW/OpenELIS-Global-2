from pathlib import Path
import re
r=Path('/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup');m=r/'designs/microbiology'
p=m/'amr-micro-v2-amendments.md';s=p.read_text()
s=re.sub(r'\((?:replacing|retiring|amends|following|analyze|Mohamed review|keeps)[^)]*\)','',s)
s=re.sub(r',\s*\)',')',s);s=re.sub(r'\(\s*[;:]?\s*\)','',s)
s=s.replace('** .','**.').replace('** :','**:').replace('  ',' ')
s=re.sub(r'\(V2 [^)]*\)[^)]*§[\d.Aa-z]+\)','',s)
s=s.replace('no culture type: no culture type','no culture type')
s=s.replace('the technician can **Move to related case** (reason required, on both Timelines).','only a member sample with no results may be split into its own related case with a reason on both Timelines. Regrouping existing results and joining cases are deferred.')
s=s.replace('environmental replicate swabs stay one case (M-18)','environmental samples follow the order, sample-type, lab-unit and site rule (M-18)')
s=s.replace('**Environmental isolates** follow (','**Environmental isolates** are excluded from clinical populations (')
s=s.replace(' (`CLINICAL_DIAGNOSTIC`, built)','').replace(' (`ACTIVE_SCREENING`, built)','').replace('(default, `CLINICAL_DIAGNOSTIC`, built)','(default)').replace('(colonization and infection prevention, `ACTIVE_SCREENING`, built)','(colonization and infection prevention)')
s=s.replace(' (V2 susceptibility (A-07) `RESULTS_IN`; accept semantics)','').replace('analyzes','analyzes')
s=s.replace('`micro_isolate`','isolate').replace('existing `MicroCaseAnalysis`','the case’s ordinary test results')
s=re.sub(r'\([^)]*V2 preservation[^)]*\)[^)]*\)','',s)
s=s.replace('as on develop, including','including')
s=s.replace('Macro categories are rebound','Macro categories serve notes')
s=s.replace('structured micro results in electronic result delivery (FHIR DiagnosticReport); M-15 territory','structured micro results in electronic result delivery (FHIR DiagnosticReport); future scope')
s=s.replace('one `critical_callback` row','one shared callback entry')
s=s.replace('Source???','Source')
# Explicit shared source links replace unscoped decisions; keep the source self-contained.
s+='''\n## Shared functional sources

- [Reference administration](m-01-amr-reference-data.md) and [breakpoints](m-02-breakpoint-catalog.md).
- [Expert-rule administration](m-06-expert-rules-engine.md), with review in case validation.
- [Macros](m-08-macro-library.md), used in clinical history, notes and justified overrides.
- [WHONET](m-09-whonet-export.md) and [reagent linkage](m-12-test-reagent-linkage.md).
- [Nonfunctional requirements](m-nfr-non-functional-requirements.md).
- [Reception](../sample-collection/clinical-order-entry-v4.md), [catalog](../admin-config/test-catalog.md),
  [Inventory](../inventory/inventory-redesign.md) and [patient report](../reports/patient-report-redesign.md).
'''
p.write_text(s)
# No technical storage claims in the environmental functional rules.
p=m/'m-18-environmental-microbiology.md';s=p.read_text();s=s.replace('created by the existing post-save hook','using the same all-or-nothing order save')
s=s.replace('stored as today (`envSamplingSiteId`)','selected from Sampling Sites')
s=re.sub(r'\bD-\d{3}\b','',s);s=re.sub(r'\(\s*\)','',s);s=s.replace('**Validation role** releases environmental Case results.','**Results rights** permit partial release; **Validation rights** permit validation, final release and amendments in the case lab unit, as in the V2 Access table.')
s=s.replace('**Results role** works environmental Cases','**Results rights in the case lab unit** permit work on environmental cases')
s=s.replace('Keys follow constitution Principle VII. Run `npm run i18n:find` for each NEW key.','Visible text follows the shared localization conventions. Translation implementation belongs to engineering.')
s=re.sub(r'one [Cc]ase per (?:culture test|specimen)', 'one case per order, sample type, lab unit and site',s)
p.write_text(s)
# Every surviving reference document points to the same source, without dead section citations.
for p in m.glob('*.md'):
 s=p.read_text()
 s=re.sub(r'(\[V2 [^\]]+\]\([^)]+\))\s*§[\d.]+[a-zA-Z]?',r'\1',s)
 s=s.replace('Module Parent Specification','functional baseline')
 p.write_text(s)
# Add real current/future distinctions to retained HTML previews.
for name in ['m-13-antibiogram-prototype.html','m-16-cluster-detection-preview.html','glass-submission-console.html']:
 p=m/name;s=p.read_text();s=re.sub(r'(<body[^>]*>)',r'\1\n<div role="note" style="padding:12px;background:#fff1c7;color:#161616">Future capability — outside Microbiology V2 delivery. This preview is not implementation or acceptance evidence.</div>',s,count=1);p.write_text(s)
# Sync only changed source copies; no generated source can republish the retired paths.
base=r/'mockup-viewer/public/designs'
for p in base.rglob('*'):
 if p.is_file():
  src=r/'designs'/p.relative_to(base)
  if src.exists() and p.suffix in ['.md','.html','.jsx','.js','.svg']:p.write_bytes(src.read_bytes())
