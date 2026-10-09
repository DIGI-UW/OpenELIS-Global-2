from pathlib import Path
import re, shutil
root=Path.cwd()
spec=root/'specs/amr/spec.md'
s=spec.read_text().replace('- `openelis-work` and Jira define functionality, design, and observable product\n  acceptance. They do not define entities, persistence, API shapes, routes,\n  process ownership, migrations, or test-layer choices.', '- `openelis-work` defines functional requirements, workflows, mocks and observable\n  acceptance. `specs/amr` owns engineering decisions, migration/removal mapping,\n  implementation order and verification. Jira tracks ownership, status,\n  dependencies, acceptance evidence and links; it is not a functional specification.\n- Design and Jira do not define entities, persistence, API shapes, routes, process\n  ownership, migrations or test-layer choices.')
s=s.replace('Site-based cases; reconcile routing drift first','Site-based cases grouped by order, sample type, lab unit and site')
s=s.replace('## Clean cutover', '''## Agreed V2 scope

Draft 10.4 is the baseline with the following settled corrections. Eligible
catalog tests open cases independently of Program. Automatic grouping, adding
tests to an existing case, blood-culture sets and splitting a member sample with
no results remain in scope. Joining existing cases is deferred. A lab-unit
transfer preserves separate case identities even when both cases now have the
same order, sample type and working unit; it never merges their work or history.
Environmental grouping also distinguishes the sampling site.

Use the draft's Results and Validation rights, including technician placement,
report choices and partial release, validator final release and amendments,
worklist scoping and read-only direct links. Admission date remains optional;
missing admission information must not fabricate infection origin. Patient
reporting choices and surveillance eligibility are separate. Offline writes are
blocked. Future antibiogram, GLASS and cluster delivery is outside this cleanup.

This change updates documentation, gallery registration and gallery tests only.
It changes no application interface, database schema or clinical record.

## Clean cutover''')
spec.write_text(s)
p=root/'specs/amr/plan.md'; s=p.read_text(); start=s.index('## Source and cutover gate'); end=s.index('## Execution rules')
s=s[:start]+'''## Source and cutover gate

The scope and deletion mapping are settled. V00 aligns the functional sources,
removes competing guidance and verifies the gallery and tracking readback.
There is no deletion-confirmation or speculative policy blocker.

Before runtime implementation, freeze the slice's service boundaries and
one-way clinical/audit transformation from current code. V01 rehearses fresh
install, upgrade, collisions and rollback/reapply on disposable PostgreSQL.
Do not combine existing cases because a grouping key now matches. No production
legacy reader, dual write or retained configuration authority is permitted.
Applied Liquibase history remains immutable; future cutover changesets transform
required clinical/audit information and remove obsolete final-schema storage.
This documentation change does not execute those migrations.

## Runtime disposition

| Disposition | Current families | Owner and cutover obligation |
| --- | --- | --- |
| Retire | `MicroWorkflowType`, `MicroCaseWorkflowService*`, workflow forms/actions; `MicroCultureSetup`, DAO/admin form, `MicroCaseProtocolService*`, protocol controller/forms, workflow/protocol UI and seeds | V01/V02/V05: transform attributable history, then remove consumers, configuration and obsolete expectations with their runtime replacement. |
| Retire | Reception micro section, `MicroCaseOrderDetail` reception draft handling, Program guards and micro draft save expectations | V02/V03: preserve shared order-save integrity and required context; remove the competing reception edit path. |
| Rewrite | `MicroCase` membership, `MicroCaseAnalysis`, `MicroOrderRoutingServiceImpl` | V01/V02: one grouping rule for preview/save/edit; preserve member/result ownership, separate transfers and idempotence. |
| Rewrite | `MicroCaseInoculationServiceImpl`, culture timing/events and inventory usage links | V05/V06: row-local timing/lineage, usable lots and traceability with no culture stock transaction. |
| Rewrite | `MicroWorklistServiceImpl`, incoming-result placement, `MicroReportProjectionServiceImpl` | V07/V08/V12: scoped worklists, one attributable result, actual release readiness, structured print output and verified electronic summary continuity. |
| Reuse and extend | Ordinary Analysis/Result/components, organism and antibiotic masters, published panels and breakpoint versions | V04/V09/V10/V11: no second result framework; retain original values and historical versions. |
| Reuse and extend | Amendments, audit, questionnaires, referrals, callbacks, labels, Workplan and export selection | V03/V09/V12/V13/V07/V15: extend their existing owner and preserve shared behavior. |

## Preservation and acceptance mapping

| Surviving requirement | Primary owner | Dependent verification |
| --- | --- | --- |
| Released reports, amendments, immutable history and actual release readiness | V12 | V01 migration; V08 late arrivals; V16 end-to-end. Carry lasting readiness behavior from closed OGC-1384/1385; closure is not acceptance evidence. |
| Identification history and received-isolate provenance | V09 | V12 historical reports and V15 referred-in exports. |
| Original measurements, repeats/retests, overrides/reverts and panel versions | V10 | V11 interpretation, V12 print. Replace whole-run reporting selection with one chosen validated reading per agent. |
| Culture-medium lot traceability and eligibility, including in-house batches | V05 | V06 readings and V07 bulk work; stock remains unchanged on create/edit/undo. |
| Other reagent eligibility, required links, method-specific lots and QC | V04/V10 | Shared Inventory regression; do not infer requiredness from unrelated catalog roles. |
| Organism/antibiotic administration and published panel versions | V09/V10 | V12/V15 retain historical names, interpretations and export mappings. |
| Breakpoint import validation, activation and historical interpretation | V10 | V11 WHO critical concentrations; imports/activation never silently reinterpret history. |
| Export mapping, populations, deduplication, preview/generation parity and audit | V15 | V12 released source data; V07 date boundary (OGC-1411). |
| Accessibility, performance and shared order-save integrity | Each affected slice | V02 order saves; V07 worklist; V16 keyboard/mobile and measured budgets. |

Export period membership uses specimen **collection date**. Independently chosen
first-isolate chronology may use collection or final-release date, with the
existing 7/14/30-day windows or disabled deduplication, same-source choice,
contaminant filtering and changed-interpretation policy. Changing chronology
never changes period membership. Mapping failures exclude only affected rows;
generation is blocked only when no valid rows remain. Preview and generation
use the same population and policy; generation records actor, time, counts,
selection, filename and content fingerprint. Quoted/newline CSV values round-trip.

The complete 112-criterion ownership matrix is in [tasks.md](tasks.md#acceptance-coverage).
It is the acceptance mapping, not a claim that those tests exist or pass.

## Settled functional rules

| Concern | Draft rule and verification owner |
| --- | --- |
| Routing and transfers | Tests trigger grouping; Program does not. Transfers keep cases separate, adding tests remains supported and a no-result member may be split with a reason. V02. |
| Admission and surveillance | Admission date is optional. V03/V15 retain missing values and never invent infection origin or a questionnaire-to-export mapping. Questionnaire CSV export remains later work. |
| Placement and report choices | Keep draft Results rights for placement/moves, In lab only and Report choices. Validation, final release and amendments retain Validation rights. V04/V08/V12 enforce each action and final locking. |
| Access | Worklists show only permitted units; direct links remain read-only without unit rights; mutations are rejected. V02 and each write-bearing slice. |
| Defaults and quantitative cultures | Editable catalog media defaults and existing result components/calculated values; no protocol lane or new Gram result type. V04/V05. |
| Reporting versus surveillance | In lab only controls patient delivery, while exports apply their own eligibility rules. V12/V15. |
| Callbacks | One attributable call attempt in the shared log/report, outcomes and follow-up retained; the micro finding clock begins at validation. V13. |
| Offline work | Previously loaded information may remain readable; all writes are blocked, with no local save/replay queue. V07/V16. |
| Future capabilities | Micro QC, isolate storage, configurable release rules, regrouping existing cases, questionnaire CSV, antibiogram, GLASS and cluster delivery remain outside V2 claims. |

OGC-1386 remains open for V12/V15: distinguish clinical release readiness from
export readiness and investigate displayed antibiotic names using source data.
No display-name fix or deployment is claimed by this cleanup.

''' + s[end:]
s=s.replace('- [Risk reconciliation](#risk-reconciliation)','- [Runtime disposition](#runtime-disposition)\n- [Preservation and acceptance mapping](#preservation-and-acceptance-mapping)\n- [Settled functional rules](#settled-functional-rules)')
s=s.replace('Also review source/mock anchors, the exact removals, one authoritative roadmap\nand the functional-only source audit. Formatting alone does not close V00.', '''In the paired design worktree run `npm test` and `npm run build`. Check source
and generated catalogs, documentation, sitemap and published design copies for
retired files/links; review local anchors and render clinical/environmental mocks
and shared entry points. Read Jira back for dispositions, ownership, replacement
links and dependency direction; verify the Confluence walkthrough is archived.
Formatting alone does not close V00. No application tests run for this cleanup.''')
p.write_text(s)
p=root/'specs/amr/tasks.md';s=p.read_text();s=s.replace('**Status:** `[*]` — blocked on product/source alignment and deletion confirmation.','**Status:** `[*]` — documentation and tracking synchronization in progress; scope settled.')
a=s.index('- [ ] Resolve grouping/set');b=s.index('## 01 — migration rehearsal')
s=s[:a]+'''- [ ] Align the three engineering documents and paired design sources to draft
      10.4 with separate transfers and deferred joining.
- [ ] Remove the mapped superseded guidance without replacement archives.
- [ ] Verify gallery source/generated output, rendered mocks, all 112 criterion
      owners, Jira replacement links/dependencies and archived Confluence history.
- [ ] Pass [V00](plan.md#v00--documentation-and-sources).

**Acceptance:** one functional baseline, one engineering roadmap and tracking
that points to them. Documentation completion is not application acceptance.

'''+s[b:]
s=s.replace('- [ ] Close inpatient admission, missing-origin and prior-antibiotic mapping policy\n      before implementing required-before-final or export rules.','- [ ] Keep admission optional, enforce the draft required-before-final fields and\n      preserve unknown infection origin; no invented export mapping.')
s=s.replace('The agreed admission policy is enforced without fabricating infection origin.','Admission stays optional without fabricating infection origin.')
s=s.replace('apply the approved diagnostic-withholding rights','apply the draft Results rights for report choices')
s=s.replace('withholding follows the approved role','report choices follow the draft role')
s=s.replace('Apply the agreed placement-review rights','Apply the draft placement-review rights')
s=s.replace('- [ ] Confirm report placement and the callback clock with its owner.','- [ ] Verify shared report placement and the draft callback clock from validation.')
s=s.replace('with agreed grouping','with order/sample-type/lab-unit/site grouping')
s=s.replace('Prove the approved inpatient/antibiotic export mappings, including missing\n      values','Prove the export mappings supported by the source, including missing\n      values')
s=s.replace('## Iteration rules','''## Implementation sequence

V00 → V01 → V02 → V03 → V04 → V05 → V06 → V09 → V10 → V11 → V08 →
V12 → V13 → V07 → V14 → V15 → V16.

Identifiers are retained. Isolates and susceptibility targets exist before
incoming placement, labels and bulk work are accepted. Jira children are tracking
only: V15 belongs to OGC-1382; all other slices belong to OGC-1383. Shared epics
remain with their owners and are linked, not copied into new specifications.

## Iteration rules''')
# Add explicit prerequisites and removals to each slice, retaining their source links and gates.
order=[0,1,2,3,4,5,6,9,10,11,8,12,13,7,14,15,16]
removals={0:'Superseded engineering/design guidance and registrations.',1:'Obsolete final-schema storage after required clinical/audit transformation; never applied migration history.',2:'Workflow classification, protocol routing, Program guards and obsolete routing expectations.',3:'Reception micro draft handling and duplicate questionnaire/context authority.',4:'Competing case-owned result editors and obsolete whole-case result assumptions.',5:'Culture protocol defaults and culture stock-consumption paths.',6:'Case-wide culture clocks/outcomes and setup-derived timing.',7:'Old workflow filters, offline save/replay expectations and batch/run assumptions for bench sheets.',8:'Existing-run-only placement assumptions and duplicate result copies.',9:'Workflow-specific isolate creation and duplicate accession/referral paths.',10:'Workflow panel defaults and whole-run reporting selection where agents overlap.',11:'TB-profile execution assumptions; no hard-coded instrument behavior.',12:'Printed REMARK/mapping dependence after electronic continuity proof; linear release/work-state assumptions.',13:'Duplicate call entry and disconnected callback reporting.',14:'Competing history/matching queries and automatic repeat suppression.',15:'Workflow-type export selection and patient assumptions for site subjects.',16:'Any remaining superseded runtime, storage, seed, configuration and test consumer.'}
for i in order:
 pat=rf'(## {i:02d} — [^\n]+\n)'
 prev='None; execute the approved documentation cleanup.' if i==0 else f'V{order[order.index(i)-1]:02d} accepted at its gate; shared owners linked before changing their behavior.'
 s=re.sub(pat,lambda m:m[0]+f'\n**Prerequisites:** {prev}\n\n**Removals:** {removals[i]}\n',s)
# Place sections in executable order, preserve numbered anchors and contents.
head=s[:s.index('## 00 —')]; refs=s[s.index('[mock]:'):]; body=s[s.index('## 00 —'):s.index('[mock]:')]
sections={int(m.group(1)):m.group(0) for m in re.finditer(r'## (\d\d) — .*?(?=\n## \d\d — |\Z)',body,re.S)}
s=head+'\n'.join(sections[i].rstrip() for i in order)+'\n\n'+refs
p.write_text(s)
for name in ['782-ogc-782-microbiology-mvp-spec','782-ogc-782-microbiology-m8-clinical-completeness','782-ogc-782-microbiology-m9-reference-mapping-admin','782-ogc-782-microbiology-m10-whonet-export']:
 path=root/'specs'/name
 if path.exists():shutil.rmtree(path)
for name in ['data-model.md','research.md','quickstart.md','playwright-plan.md','checklists','contracts','research']:
 path=root/'specs/amr'/name
 if path.is_dir():shutil.rmtree(path)
 elif path.exists():path.unlink()
for path in [root/'specs/roadmaps/microbiology-spec-health-cleanup-list.md', root/'.devin/artifacts/plan.md.pre-prettier']:
 if path.exists():path.unlink()
print('Engineering baseline consolidated; specified obsolete documents removed.')
