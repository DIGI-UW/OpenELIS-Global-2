from pathlib import Path
import re,json
r=Path('/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup');m=r/'designs/microbiology'
p=r/'mockup-viewer/src/App.jsx';s=p.read_text();s=s.replace("{ name: 'Test Catalog — Microbiology Workflow Attribute', blurb: 'A test can carry a workflow type that routes it down a specialized path — for example bacterial culture vs TB in the AMR module. This is where that attribute is set on the test.' }", "{ name: 'Microbiology v2 Amendments', blurb: 'The catalog switch Opens a Microbiology case sends eligible tests to a case, independently of the AMR surveillance flag. The V2 preview includes the catalog controls, reception summary and case workflow.' }")
s=s.replace('Amended by Microbiology v2 (2026-09-28): see Microbiology v2 Amendments for the reversals. ','')
s=s.replace('Phase 1B.','Shared V2 dependency.').replace('First Phase 1A+ feature.','Used by V2 notes and clinical history.')
s=s.replace('Depends on the OGC-782 microbiology module (stacked, not yet on develop).','Future capability, outside V2 delivery; population rules follow V2.')
# Future labels on all registered future artifacts.
blocks=re.split(r'(?=^  \{\n)',s,flags=re.M)
for i,b in enumerate(blocks):
 if re.search(r"specPath: 'designs/microbiology/(?:m-13-|m-15-|m-16-|glass-)",b.split('\n  },')[0]):
  b=re.sub(r"description: '","description: 'Future capability, outside V2 delivery. ",b,count=1);blocks[i]=b
s=''.join(blocks);p.write_text(s)
p=r/'MANIFEST.yaml';s=p.read_text();s=re.sub(r'^  (?:analyze|breakdown): designs/microbiology/m-18[^\n]*\n','',s,flags=re.M)
for id,desc in [('microbiology-v2-amendments','Self-contained Microbiology V2 draft 10.4 functional baseline. Eligible-test grouping, separate transfers, row-local culture work, per-agent reporting and shared clinical history. Joining existing cases is deferred.'),('m-18-environmental-microbiology','Environmental follow-on to V2: cases grouped by order, sample type, lab unit and site; explicit populations, site certificates and culture-media traceability without stock changes.')]:
 pat=r'(- id: '+id+r'\n.*?  description: >\n).*?(?=  links:)';s=re.sub(pat,lambda mt:mt[1]+'    '+desc+'\n',s,flags=re.S)
s=s.replace('jira: ["OGC-1383", "OGC-782"]','jira: ["OGC-1383"]')
s=s.replace('Depends on the OGC-782 microbiology module (not yet on develop).','Future capability, outside V2 delivery.')
p.write_text(s)
# No obsolete protocol administration or workflow-based future selection.
p=m/'m-01-amr-reference-data.md';s=p.read_text();s=re.sub(r', plus \*\*Culture Protocols.*?not a new master\.','.',s)
s=re.sub(r'^- .*culture protocol.*\n','',s,flags=re.M|re.I);s=s.replace('Four admin masters','Three admin masters').replace('four reference-data vocabularies','three reference-data vocabularies').replace('antibiotic, panel, culture protocol','antibiotic and panel')
s=s.replace('M-10\'s bespoke hub is retired — see m-10 v3.0','shared catalog administration owns updates').replace('flagged for the Catalog Subscription owner in m-10 §2','tracked with the Catalog Subscription owner')
p.write_text(s)
for p in m.glob('*.md'):
 s=p.read_text()
 s=s.replace('`workflow_type = MYCOBACTERIOLOGY_TB`','a Program on the TB reporting track').replace('`workflow_type`s','Program reporting tracks').replace('workflow_type = BACTERIOLOGY','Program reporting track = Bacterial')
 s=s.replace('M-03 Order Entry Hook','V2 Case information').replace('Clinical History field uses macros (per Micro Order Entry hook)','Clinical history uses macros on the case')
 s=s.replace('Expert Review section','case validation step').replace('### 5.0 Expert Review','### 5.0 Case validation').replace('a separate Expert Review','the case validation')
 # Old named modules no longer teach a separate contract. Scoped functional links replace them.
 mapping={'M-00':'[V2 baseline](amr-micro-v2-amendments.md)','M-03':'[V2 reception](amr-micro-v2-amendments.md#fr-02.3)','M-04':'[V2 case](amr-micro-v2-amendments.md#fr-17.6)','M-05':'[V2 susceptibility](amr-micro-v2-amendments.md#fr-07.2b)','M-07':'[V2 Worklist](amr-micro-v2-amendments.md#fr-12.1)','M-11':'[V2 callbacks](amr-micro-v2-amendments.md#fr-18.1)','M-14':'[V2 DST](amr-micro-v2-amendments.md#fr-14.1)'}
 if p.name!='amr-micro-v2-amendments.md':
  for old,new in mapping.items():s=re.sub(r'(?<![\w-])'+old+r'(?![\w-])',new,s)
 s=s.replace('Culture Protocol masters','published Panel versions').replace('day count from `max_incubation_days`','due times from individual culture rows')
 # Eliminate decision-log links from active guidance without renumbering history.
 s=re.sub(r'\[([^\]]+)\]\([^)]*(?:decisions|decision-log)[^)]*\)', '[V2 functional requirements](amr-micro-v2-amendments.md)',s)
 p.write_text(s)
# Reagent catalog examples must not imply culture stock consumption.
p=m/'m-12-test-reagent-linkage.md';s=p.read_text();s=s.replace('Blood Culture     │ REQUIRED │ 1 PER_RUN','Blood Culture     │ Tracked  │ Trace only').replace('Blood Culture (Ped)│ REQUIRED│ 1 PER_RUN','Blood Culture (Ped)│ Tracked │ Trace only')
s=s.replace('Inoculation modal','inoculation section').replace('AST Setup modal','susceptibility setup section')
s=re.sub(r'^- \*\*AC-M12-01\*\*.*$', '- **AC-M12-01** *(Shared Test Catalog)*: administrators can link a test to its allowed reagents and media, with method and requiredness shown; culture media carry no consumption quantity.',s,flags=re.M)
s=s.replace('on save, an `InventoryUsage`','on non-media reagent save, an `InventoryUsage`')
p.write_text(s)
p=m/'m-12-test-reagent-linkage-prototype.html';s=p.read_text();s=re.sub(r'<span class="w">.*?</span></div>', '<span class="w">Shared Inventory lots show expiry and QC eligibility. Selecting a culture-medium lot records traceability only and never changes stock. Other reagents retain their own consumption policy.</span></div>',s,count=1)
s=re.sub(r'<div class="note">.*?</div>','<div class="note"><strong>Culture media never consume stock.</strong> The culture records the chosen medium and lot. Inventory shows Used on cultures separately from stock. Required reagent links and non-media consumption retain their shared policy.</div>',s)
s=s.replace('<td>MGIT 960 tube</td><td><span class="tag pri">primary</span></td><td>1</td>','<td>MGIT 960 tube</td><td><span class="tag pri">culture medium</span></td><td>Traceability only</td>')
s=s.replace('Link the reagents a test consumes, with usage type and quantity.','Link culture media for traceability and other reagents with their usage type and quantity.').replace('ReagentLotPicker — choosing a lot at inoculation (M-04)','Choose a culture-medium lot')
p.write_text(s)
for name in ['amr-micro-v2-preview.html','amr-micro-v2-mockup.jsx']:
 p=m/name;s=p.read_text().replace('A suggestion from the plating templates;','A suggestion from the culture test’s media links;').replace('Apply template: {name}','Linked media: {name}').replace('Plating template proposal','Catalog media proposal')
 # Current scoped requirements replace orphan historical decisions in active mock prose/comments.
 s=re.sub(r'\(D-\d{3}(?:[^)]*)\)','',s)
 p.write_text(s)
# Remove remaining obsolete paired references in manifests and indexes.
retired=json.loads(Path(__file__).with_name('retired-design-paths.json').read_text())
for rel in ['MANIFEST.yaml','INDEX.md','README.md']:
 p=r/rel;s=p.read_text()
 for path in retired['paths']:
  name=Path(path).name
  s='\n'.join(line for line in s.splitlines() if name not in line)+'\n'
 p.write_text(s)
# Keep tracked source and publication copies identical; remove stale public design copies.
for base in [r/'mockup-viewer/public/designs']:
 for p in base.rglob('*'):
  if p.is_file():
   src=r/'designs'/p.relative_to(base)
   if src.exists() and p.suffix in ['.md','.html','.jsx','.js','.svg']:p.write_bytes(src.read_bytes())
print('Registry journey and remaining retained guidance corrected.')
