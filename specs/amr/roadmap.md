# Microbiology (AMR) V2 — roadmap

Ordered work to reach the [final state](spec.md). Check an item when it is done.
Evidence belongs in the pull request, not here.

Delivery is one dependent PR stack for the whole V2 replacement, rooted at
baseline PR #4646. Steps 1–3 belong to that baseline. Each implementation
milestone below is one PR above its predecessor, carrying its schema, backend,
frontend, tests and documentation together. Individual tasks, dependency work
and fixes stay in their owning milestone; they do not create extra PRs. Final
acceptance is a gate on the assembled stack, not a separate implementation PR.

An item is done when its contribution is verified on its branch: the application
boots with the registered application changelog on fresh and upgraded databases,
the change's own tests pass, and every user-facing item has a recorded browser
run compared with the pinned design mock. This does not make an intermediate PR
independently mergeable. The complete stack must pass clinical migration and
final acceptance at its final revision before it merges in dependency order.
Schema changes ship with the step that needs them
([D8](spec.md#2-engineering-decisions)); there is no test-only schema.

Acceptance criteria (AC-V2-nn) are listed once under their primary milestone.
Criteria spanning milestones are exercised as complete journeys at final
acceptance; their primary assignment is not evidence that all parts already work.

## 1. Spec and roadmap

- [ ] [spec.md](spec.md) reviewed and accepted
- [ ] V1 specifications under `specs/782-*` removed
- [ ] Jira OGC-1383 / OGC-1382 children point at these steps

## 2. Dependencies and open decisions

- [ ] Each dependency in [§14](spec.md#14-shared-openelis-dependencies) verified against current code and assigned to its owning milestone; evidence and any existing delivery PR recorded in that milestone's PR
- [ ] Required shared behavior delivered before its consumer, in the owning milestone unless already delivered; no silent scope reductions or microbiology-only duplicates
- [ ] Approved behavior clarifications reflected in the engineering spec and pinned functional specs/mocks ([§15](spec.md#15-clarified-behavior-and-delivery))

## 3. Retire the V1 front

[§10 Retire](spec.md#10-v1-disposition-retire-restructure-evolve)

- [ ] Workflow type, culture setups and protocols removed, backend and frontend
- [ ] Reception Microbiology section, micro draft pipeline and Program guards removed
- [ ] V1 order routing removed
- [ ] Retired columns made nullable; no other schema change
- [ ] Order entry, Results, Validation and existing cases work

## 4. Case structure

[§4.1](spec.md#41-case), [§10 Restructure](spec.md#10-v1-disposition-retire-restructure-evolve)

- [ ] Case lab unit, Program, member samples and case analysis roles, restructured in place
- [ ] Requested-test ownership before collection; explicit attachment to the eventual sample, cancellation history and retry rules
- [ ] Membership constraints permit retained separate cases after transfer; split relationships are preserved independently of shared samples
- [ ] Existing cases load and display after upgrade
- [ ] Existing-data requirements for later access and routing documented and rehearsed against the registered schema; missing clinical mappings are never fabricated

## 5. Routing and case creation

[§5](spec.md#5-routing-and-case-membership)

- [ ] Catalog switch, case role, collected in sets
- [ ] Routing on order save, electronic orders, reflex, case tests
- [ ] An order without a received sample opens its case against requested work; later sample recording attaches to the same case without duplicate cases, samples or ownership
- [ ] What this order will open preview
- [ ] Set numbers and set warnings
- [ ] Shared per-sample fields and configured container classification support all specified set warnings
- [ ] Edit order: add, cancel, last-test confirmation and reason
- AC: 01, 02, 03, 05, 54, 58, 88, 91, 103, 104

## 6. Case, case information and access

[§4.1](spec.md#41-case), [§6](spec.md#6-case-work-rules), [§9](spec.md#9-access)

- [ ] Case view shell, header, related cases, samples list, timeline
- [ ] Related-case switcher covers shared samples and split relationships, including within one lab unit and after transfer; labels identify case, samples and current lab unit
- [ ] Cases awaiting samples are visible to the responsible lab unit without implying collection or receipt
- [ ] Case information, order-level details, Program and questionnaire
- [ ] Case-lab-unit access on every read and write; read-only direct links
- [ ] Case search and worklist listing by lab unit
- [ ] Transfer
- [ ] Split a no-result sample
- AC: 04, 06, 07, 49, 60, 63, 90, 92, 99, 111

## 7. Case tests and results

[§4.3](spec.md#43-case-tests)

- [ ] Initial testing and Additional testing with the shared chooser
- [ ] Shared chooser supports the specified compatible/"used as" sample types
- [ ] One result table and inline editor, multi-component results
- [ ] Tested elsewhere, reagent lots, In lab only
- [ ] Notes on case and results
- [ ] Per-result validation, Block self-validation
- [ ] Shared result runs, reagent/control policy and quality-control holds apply to typed case results; the shared self-validation rule applies on both case and Validation screens
- AC: 08, 09, 24, 30, 31, 32, 33, 38, 39, 40, 51, 59, 61, 67, 68, 100, 105, 108

## 8. Culture rows and media

[§4.4](spec.md#44-culture-rows)

- [ ] Inoculation from media links, medium and lot without stock change, tracked-media setting
- [ ] Shared Inventory lot-tracking property and medium type tags delivered before culture entry uses them
- [ ] Readings, check due, incubation complete, extensions, positive time
- [ ] Instrument negatives, late growth
- [ ] Culture tree: tests on a culture, Gram stain shortcut, subcultures
- [ ] Seeded positive-bottle reflex rule
- [ ] Used on cultures in Inventory
- AC: 10, 13, 23, 55, 64, 65, 66, 69, 70, 71, 72, 77, 82, 83, 84, 89, 93, 95, 97, 101, 102, 109, 110

## 9. Isolates and referral

[§4.5](spec.md#45-isolates-and-referral)

- [ ] Isolates picked from rows, identification history, significance
- [ ] Isolate sample items
- [ ] Received isolates
- [ ] Refer remaining work, a test or an isolate
- AC: 15, 26, 57, 62, 75, 106

## 10. AST/DST and TB classification

[§4.6](spec.md#46-astdst)

- [ ] Runs, default panel, added panels, standards and reasons
- [ ] Readings, attempts, overrides, QC, expert flags
- [ ] Use for reporting per agent
- [ ] TB classification, discordance gate, NTM off-ramp
- AC: 21, 27, 43, 47, 56, 98

## 11. Incoming results

[§4.7](spec.md#47-incoming-results)

- [ ] Results for existing rows go to the row
- [ ] Incoming queue, one-click placement, moves, duplicate sends
- [ ] Reflex no-duplicate rule
- AC: 16, 17, 18, 25, 34, 35, 36, 37

## 12. Releases, report and calls

[§4.8](spec.md#48-notes-report-choices-releases-calls), [§7](spec.md#7-output)

- [ ] Work stage and culture outcome
- [ ] Report choices
- [ ] Partial and final release, amendments, server-side final lock
- [ ] Shared report version/print-queue behavior and required patient-report support verified and extended before case releases use them
- [ ] Patient report micro block and environmental certificate
- [ ] Critical calls through the shared callback log
- AC: 14, 19, 20, 28, 29, 44, 45, 46, 48, 52, 53, 96

## 13. Worklist, bench and labels

[§8](spec.md#8-worklist-and-bench), [§7 Labels](spec.md#7-output)

- [ ] Needs attention reasons and sorting
- [ ] Cultures filters, No growth, Inoculate many, Extend 24 h, Undo
- [ ] Bench sheet as Workplan print, Open sheet
- [ ] Shared Workplan print records and per-container label support delivered with these consumers
- [ ] Per-container label scope and presets, label scanning
- AC: 11, 12, 73, 76, 78, 79, 80, 81, 85, 86, 87, 94, 112

## 14. Patient history

- [ ] Patient history, repeat isolate, TB follow-up ([§6](spec.md#6-case-work-rules))
- AC: 74

## 15. Environmental cases and surveillance

[§12](spec.md#12-surveillance-populations), [§13](spec.md#13-environmental-cases)

- [ ] Site-subject cases, environmental fields and purposes
- [ ] Purpose changes affect only the selected case; replicates are shared across the order's cases, with helper text and timeline entries on the correct scope
- [ ] Purpose and track populations in the WHONET export, replacing its retired workflow-type scope
- [ ] M-18 acceptance criteria
- AC: 50, 107

## 16. Clinical migration

[§11](spec.md#11-existing-data)

- [ ] Lab unit and Program assignment with review lists and timeline notes
- [ ] All fields needed by V2 routing, access, membership and reporting populated before the migrated application serves case work; ambiguous mappings stop the migration without partial clinical changes
- [ ] Verified on a copy of a V1 database
- [ ] Retired columns and tables dropped
- AC: 42

## 17. Final acceptance

- [ ] Complete journeys for all 112 core criteria verified on the assembled final revision, including criteria whose behavior spans several milestones
- [ ] Environmental acceptance criteria verified against the corrected pinned design
- [ ] Fresh installation and full V1-to-V2 upgrade verified with the real application changelog and clinical migration
- [ ] Existing case identities, results, issued reports, amendments, provenance and attributable history remain usable after upgrade
- [ ] Localization, desktop/mobile and keyboard review
- [ ] Offline reads and blocked writes, audit, access, ordinary laboratory workflow continuity and pinned performance requirements verified with representative data
- [ ] Preservation capabilities (A-16) and retained worklist list (A-12) present
- [ ] Browser evidence compared with the pinned design mocks for the integrated user journeys
- [ ] All three GitHub checkpoints pass on the final revision; the whole stack is ready to merge in dependency order
- AC: 22, 41
