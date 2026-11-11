# Reporting Design Fidelity — Required Iteration Check

## Authority

The [canonical interactive mock](https://github.com/DIGI-UW/openelis-work/blob/5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f/designs/reports/custom-data-export.html)
is the interface source of truth. On 2026-09-14, design PR 322 still pointed to
that revision. The locally rendered mock's Git blob matched the remote blob:
`2076af722d81b1dea5712f44a4e3cfa2adefc618`.

The user clarified that MVP scope limits functional integration, not the supplied
design. Port the full design and connect functions incrementally. Preserve
pending controls with clear unavailable/pending behavior, without fake success
or fictional records presented as live data. Native OpenELIS shell, Carbon
components and translation integration should preserve the supplied presentation.

Accepted adaptations are limited to the decisions recorded in `spec.md`: both
layouts, all repeated results, instance-specific catalogs, per-test turnaround,
shared named reports, existing access, and one background generation path.
Do not restore a fixed mock field count, personal-library restriction, new
permission workflow or fictional queue timing in the name of visual fidelity.

## Verified Drift at Application ebc6983898

This audit compared the rendered mock and public application and inspected
their source. That deployed revision does not pass design fidelity.

| Surface          | Mock                                                                          | Public application                                                        | Result                                                         |
| ---------------- | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------- | -------------------------------------------------------------- |
| Entry            | Equal new-export and saved-report cards                                       | Immediately opens the builder; saved reports behind a button              | Fail                                                           |
| Flow             | Choose columns, Set Filters, Review & Submit                                  | Columns and filters combined without the staged flow                      | Fail                                                           |
| Catalog          | Collapsed groups, counts, explicit expansion                                  | All groups expanded into checkbox lists                                   | Fail                                                           |
| Selection        | Add/Added buttons and independent selected-column controls                    | Checkboxes; every active test selected by default                         | Fail                                                           |
| Search           | Searches field and group names; matching groups open; clearing restores folds | Field-label search only; a visible group-name search returned zero fields | Fail                                                           |
| Ordering         | Grip, insertion marker, compact stacked arrows, separate remove               | Draggable rows and horizontal arrows; no grip or insertion marker         | Fail                                                           |
| Narrow layout    | Available/selected pane switch with retained selection                        | No corresponding view control or responsive rule in source                | Source-confirmed gap; browser validation required after repair |
| Review and queue | Structured review cards and queue table/actions                               | Simplified summary and job cards                                          | Fail                                                           |

The live available catalog contained 1,710 options: 19 common fields, 190 tests,
192 result components, 163 question types and 1,146 per-test/component turnaround
choices. Its 194 default selections were four common fields plus all 190 tests.
The size follows actual instance configuration; exposing all options at once
and selecting all tests by default are the defects. These counts are observed
data, not limits to hardcode.

The cause is traceable: `sample-testing.json` includes `catalog:tests` in
spreadsheet defaults, `ReportingCatalogService.defaultColumns` expands that
whole group, and `CustomDataExport.jsx` renders each variable as a checkbox in
an always-expanded fieldset. Component and browser tests asserted those controls
and output behavior without comparing the design. CSV successes remain valid
for their tested outputs, but do not pass this design check.

## Every UI Iteration

1. Identify the affected mock surface and exact state before editing. Record the
   mock revision, application revision and any explicitly accepted adaptation.
2. Port the existing presentation and interactions; connect the agreed current
   functionality. Keep a clear list of working and pending actions.
3. Render mock and application at matching desktop and narrow widths and in the
   same states. Compare layout, hierarchy, spacing, controls and navigation.
4. Exercise initial entry, group expansion, field/group search, cleared search,
   Add/Added, removal, pointer/keyboard ordering, pane switching and restoration
   where affected. Keep the selected order through real review and CSV download.
5. Run the same user workflow and fixture expectations in automated E2E and UAT.
   Keep screenshots and observed discrepancies with the iteration receipt.
6. Fix unexplained differences and repeat affected checks before claiming the
   iteration complete. A green backend test or screenshot's existence is not a
   design comparison. Record pending functionality separately from failed fidelity.
7. Publish every usable stage to the existing UAT target with its actual scope,
   versions and known findings. Do not delay UAT until all MVP functions exist.

Two attempts without verified progress or new evidence require reassessment.
Unexplained drift stops completion of the affected iteration. Escalate only a
concrete ambiguity that changes product behavior; the existing design is not an
ambiguity and does not require another approval to port.

## Immediate Remediation

- [x] Port the complete mock presentation, including overview, staged builder,
      catalog, ordering, responsive panes, review, saved reports and queue.
- [x] Connect existing exports, shared definitions and completed-job retrieval;
      retain visible pending controls for functionality not yet implemented.
- [x] Remove the all-test default selection; keep instance-aware availability
      and explicitly configured useful defaults without hardcoded field counts.
- [x] Replace checkbox-shaped test assumptions with assertions for the mock's
      actual interactions, preserving all existing record/CSV correctness checks.
- [x] Perform direct desktop/narrow comparison, publish the corrected stage and
      verify the same interactions and downloaded CSVs on public UAT.

These are repairs to M1 (T006, T014–T017), not a new feature or separate redesign.

## Frontend Repair Comparison — 2026-09-14

Direct screenshots at 1280×900 and 390×844 cover the collapsed/selected catalog,
filters with extra controls closed and open, review including the lower save
panel, and the full queue. The native OpenELIS shell replaces the mock's
fictional shell/preview toolbar; the reporting layout retains its structure.
Native Carbon inputs, buttons and tags supply focus and disabled semantics.
Actual configured names/counts replace fictional records. Spreadsheet layout and
shared-report management are the accepted functional adaptations above.

The comparison corrected reversed surface layers, mobile source/header wrapping,
the extra review preview, review section order, mobile queue action placement,
queue separators and the footer's right alignment/divider. The pending file-name
explanation is readable independently of its disabled input. No mock timing,
fictional rows or permission simulation is represented as current capability.

Browser checks exercise pointer and keyboard ordering, retained focus, search
fold restoration, both mobile panes, navigation and zero horizontal page
overflow. Automated accessibility checks cover the connected reporting screens.
The public frontend at `1f2093054e574fec8344443b75530cc7a687e58a` passed the same
responsive/keyboard/accessibility workflow and actual CSV download checks.
Deployment `20260914T084139Z-1f2093054e57` retains backend `ebc6983898`.
The live checklist now includes RPT-005 for narrow layout, ordering and retained
navigation, and every story links to the pinned mock. See `execution.md` for the
separate deployed-stage evidence and remaining functional/human acceptance gaps.
