from pathlib import Path
import re, subprocess

r = Path('/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup')
m = r / 'designs/microbiology'

def edit(name, transform):
    p = m / name
    p.write_text(transform(p.read_text()))

def v2(s):
    s = s.replace('### Current State', '### Problem addressed by this design')
    s = s.replace('On the test server that reclassification returns an error (user acceptance testing finding F-1), so the case is stuck.', 'Earlier user acceptance testing found that failed reclassification could leave a case stuck.')
    s = s.replace('nothing on it depends on a type .', 'nothing on it depends on a configured culture type.')
    s = s.replace('Accessible through the existing role bundles (existing role bundles).', 'Accessible through the existing role bundles.')
    s = s.replace('today mutations check only the global role.', 'The same case-unit boundary applies throughout the workflow.')
    s = s.replace('from the built analysis and audit records', 'from the result and audit history')
    s = s.replace(' (`StatusService.AnalysisStatus`)', '')
    s = s.replace(' (replaces the hard-coded microscopy exam of )', '')
    s = s.replace('no new catalog attribute is needed and adds each pick', 'no new catalog attribute is needed. It adds each pick')
    s = s.replace(' (the OpenELIS analysis status, :', ' (the ordinary OpenELIS result status:')
    s = re.sub(r'\s+([.;:,])(?=\s|\|)', r'\1', s)
    s = s.replace('; ).', ').').replace('; )', ')')
    return s
edit('amr-micro-v2-amendments.md', v2)

def nfr(s):
    s = re.sub(r'^> This FRS.*\n', '> This is the shared V2 nonfunctional baseline: blocked offline writes, retained history, accessibility and performance. Engineering implementation and verification belong to specs/amr.\n', s, flags=re.M)
    s = s.replace('**• (queue saves)**', '**• (blocked writes)**').replace('• (queue)', '• (blocked writes)')
    s = s.replace(' + M-10 hub', '').replace('(M-01, M-02, M-08, M-10)', '(M-01, M-02, M-08)')
    s = s.replace('The narrative names roughly 200 strings across the bundle. The actual count will be larger after FRS detail.', 'Every visible string in the V2 and shared surfaces must be localizable.')
    s = s.replace('Mobile browsers are **non-goals** for Phase 1. Mobile bottle barcode scanning is Phase 3+ and will be its own surface.', 'V2 keyboard and mobile layouts meet its acceptance criteria; future dedicated barcode-scanning surfaces are separate scope.')
    s = s.replace('All API endpoints honor existing OE session-based auth.', 'Every read and write honors the existing OpenELIS authentication and the V2 case-unit access rules.')
    s = s.replace('Micro respects existing OE auth and adds per-action permission codes per', 'Micro respects existing OpenELIS authentication and the access rules in')
    s = s.replace('Audit log access requires `audit.read` (existing OE permission).', 'Audit history uses the existing audit access rights.')
    s = s.replace('WHONET export requires both `micro.surveillance.export` and `audit.read` (operators of surveillance exports must be able to see audit trail of what was exported).', 'WHONET export uses the shared export access rights; operators can inspect the audit trail of what they exported.')
    s = re.sub(r'- AC-NFR-06-01:.*', '- AC-NFR-06-01: No supported action deletes a case, isolate, original measurement, issued report or callback history.', s)
    start = s.index('## NFR-10 Database and replication')
    end = s.index('## Summary table', start)
    s = s[:start] + '''## NFR-10 Cutover and operational continuity

Required clinical records and attributable history survive the cutover. Ordinary
orders, results and other laboratory work keep their behavior and performance.
Existing backup and recovery workflows continue to preserve this information.
Engineering owns the transformation, final storage decisions and verification.

**Acceptance:**

- AC-NFR-10-01: Rehearse the cutover on a production-scale copy and demonstrate retained clinical meaning and audit history.
- AC-NFR-10-02: Meet the existing worklist performance budgets while ordinary laboratory work continues.

---

''' + s[end:]
    s = s.replace('NFR-10 DB', 'NFR-10 Continuity').replace('| NFR-10 | Database and replication |', '| NFR-10 | Cutover and operational continuity |')
    return s
edit('m-nfr-non-functional-requirements.md', nfr)

def reagent(s):
    start=s.index('## 1. Lab Context')
    s='''# M-12 Reagent and medium lot selection — functional requirements

**V2 baseline, synchronized 2026-10-06.** Test Catalog defines allowed reagents,
methods and requiredness; Inventory defines lots and their eligibility. Case
result entry uses the shared lot-selection behavior. Engineering decisions and
storage contracts belong to specs/amr, not this functional source.

**Culture media are traceability only:** selection records which medium and lot
were used, with no quantity, stock consumption or automatic credit on undo.
Other reagents retain their method, requiredness and stock policies. See
[V2 media](amr-micro-v2-amendments.md#fr-05.1b) and
[shared Inventory](../inventory/inventory-redesign.md).

''' + s[start:]
    a=s.index('### 3.1 Tables');b=s.index('### 3.3 Linkage semantics',a)
    s=s[:a]+'''### 3.1 Information shown to laboratory users

The test's allowed reagents and media show their name, method restrictions,
requiredness, permitted substitutions and active state. A lot shows its number,
expiry and eligibility with a reason when unavailable. The result history names
the selected reagent lot; the culture row names its medium and tracked lot.
Culture-medium links have editable timing and atmosphere defaults but no quantity.

''' + s[b:]
    s=s.replace('## 3. Data model','## 3. Linkage behavior')
    a=s.index('### 5.1 Component contract');b=s.index('**FIFO tooltip',a)
    s=s[:a]+'''### 5.1 Shared selection behavior

1. Offer the test's permitted reagents, filtered by its method where configured.
2. List usable lots first by earliest expiry. Show expired, failed, quarantined or consumed lots disabled with their reason.
3. Require a lot for required links, allow optional links, and allow one permitted alternative for a substitute group.
4. Recheck eligibility on save. Culture-media selection records traceability only, with no stock change.

''' + s[b:]
    s=s.replace('Expired and locked lots are **not selectable** in the dropdown in the first place (they\'re filtered out per §5.1)', 'Expired and locked lots are **not selectable** in the dropdown (shown disabled with a reason per §5.1)')
    s=s.replace('If the lot\'s QC has failed recently, surfaces a warning (not blocking — supervisor can override).', 'Failed, quarantined and consumed lots are ineligible; pending QC follows the shared Inventory policy and shows its explanation.')
    a=s.index('## 7. Migration considerations');b=s.index('## 8. Permissions',a)
    s=s[:a]+'''## 7. Shared setup

Laboratory managers configure the permitted media for Blood Culture, Urine
Culture and Wound Culture, and reagent links for the susceptibility setup tests.
The same shared catalog behavior applies to other laboratory sections. A test
with no reagent links requires no reagent selection. A tracked culture medium
requires a valid lot; a Not tracked medium records its name without a lot.
Engineering owns cutover and any storage transformation.

---

''' + s[b:]
    s=s.replace('`micro.case.edit`','Results rights in the case lab unit')
    # Retain visible outcomes; remove implementation mandates from integrations.
    s=re.sub(r'^- \*\*Test Catalog v2.5.*$', '- **[Shared Test Catalog](../admin-config/test-catalog.md)** owns the allowed reagent/media definitions, methods and requiredness.',s,flags=re.M)
    s=re.sub(r'^- \*\*Inventory module.*$', '- **[Shared Inventory](../inventory/inventory-redesign.md)** owns lots, their eligibility, stock and traceability policies; culture-row selection never changes stock.',s,flags=re.M)
    return s
edit('m-12-test-reagent-linkage.md', reagent)

def whonet(s):
    s=re.sub(r'### 0.1 Reuse.*?(?=### 0.2)', '### 0.1 Shared export entry point\n\nThe existing WHONET report entry point remains the shared export surface. V2 adds its population and versioned measurement behavior there.\n\n',s,flags=re.S)
    s=re.sub(r'> \*\*Open reconciliation.*?\n\n', '',s,flags=re.S)
    s=re.sub(r'\(`SELECT DISTINCT.*?\)', '',s)
    s=s.replace('workflow_type','Program reporting track')
    s=re.sub(r'\*\(Open: WHONET codes.*?\)\*', 'Engineering owns the catalog update contract.',s)
    s=s.replace('### 1.5 AST-Worklist quick-action contract','### 1.5 Worklist export action')
    a=s.index('The AST-Worklist "Export to WHONET" quick action');b=s.index('## 2. WHONET Mapping admin',a)
    s=s[:a]+'''The Worklist export action opens the shared export surface with the current
date scope, specimen, origin, organism and significance filters filled in. It
identifies the Worklist as the source of those filters and offers Clear filters.
The operator can review or change the scope before Preview and Generate.
The V2 Worklist uses the current rows and filters, not a separate AST worklist.

---

''' + s[b:]
    a=s.index('Every export run writes:');b=s.index('## 7. Phase 2',a)
    s=s[:a]+'''Each export retains an immutable record of the operator, start/completion
time, reporting period, population filters, deduplication options, exclusions and
reasons, isolate/no-growth counts, generated file and integrity evidence. Preview
and generation use the same selection and validation behavior. A history entry
supports re-download and retains the original parameters for at least five years.
Future delivery records also show their destination, attempts and outcome.

---

''' + s[b:]
    s=s.replace('`whonet_export_run` row written (per §6 audit).','Export history recorded (per §6 audit).')
    s=s.replace('### 0.5 Configure-once, then unattended (the real endgame)', '### 0.5 Scheduled delivery — future, outside V2')
    s=s.replace('### 0.6 Where a consolidated FHIR server exists, the lab does almost nothing (M-15)', '### 0.6 Consolidated surveillance — future, outside V2')
    s=s.replace('This is the same rule M-15 §4.10 applies to final release; the two paths must not disagree about what "ready" means.', 'Future surveillance must preserve the distinction between final-release eligibility and export eligibility.')
    return s
edit('m-09-whonet-export.md', whonet)

for p in m.glob('*.md'):
    if p.name == 'amr-micro-v2-amendments.md': continue
    s=p.read_text()
    s=re.sub(r'\(M-10(?:\u2019s|\'s)? bespoke hub is retired[^)]*\)', '(Uses the shared Catalog Subscription workflow)',s)
    s=s.replace('m-10 §2','the shared Catalog Subscription workflow').replace('see m-10 v3.0; ', '')
    s=s.replace('(`project_no_multitenancy`)','').replace('`workflow_type`', 'Program reporting track')
    # These are functional sources, never a competing implementation contract.
    note='> Functional authority: the V2 baseline owns case behavior; this document owns its scoped laboratory outcomes. Technical examples are non-normative. Engineering decisions and verification belong to specs/amr.\n\n'
    if 'Functional authority:' not in s:
        split=s.index('\n')+1;s=s[:split]+'\n'+note+s[split:]
    p.write_text(s)

p=m/'m-18-environmental-microbiology.md';s=p.read_text().replace('Results ;','Results;').replace(' | .',' |').replace('patient isolates. .','patient isolates.');p.write_text(s)

# Keep shared additions at their existing functional sections, outside localization tables.
p=r/'designs/admin-config/test-catalog.md';s=p.read_text()
a=s.index('**Microbiology V2:**');b=s.index('| Key |',a);basic=s[a:b].strip();s=s[:a]+s[b:]
a=s.index('**Microbiology V2 — Reagents and media:**');b=s.index('| Key |',a);media=s[a:b].strip();s=s[:a]+s[b:]
s=s.replace('The Basic Info section includes the following status flags:',basic+'\n\nThe Basic Info section includes the following status flags:')
pos=s.index('## New Features Summary');s=s[:pos]+media+'\n\n'+s[pos:]
p.write_text(s)

# Restore unrelated public-copy drift caused by the broad earlier sync.
changed=subprocess.check_output(['git','diff','--name-only','--diff-filter=M'],cwd=r,text=True).splitlines()
for name in changed:
    if name.startswith('mockup-viewer/public/designs/'):
        src='designs/'+name.removeprefix('mockup-viewer/public/designs/')
        if src not in changed:
            (r/name).write_bytes(subprocess.check_output(['git','show','HEAD:'+name],cwd=r))
        elif (r/src).exists():
            (r/name).write_bytes((r/src).read_bytes())
