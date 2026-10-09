# Microbiology (AMR) V2 — OpenELIS final state

This document describes OpenELIS Global 2 once Microbiology V2 is complete: what
exists, how it fits the rest of OpenELIS, and the rules it must always obey.
It integrates the V2 functional design into this codebase. The
[roadmap](roadmap.md) orders the work as one dependent PR stack for the complete
replacement, with one PR per outlined implementation milestone. Each milestone
carries its schema, backend, frontend, tests and documentation together.
Individual tasks, fixes and shared prerequisites do not create extra PRs. The
assembled final revision passes clinical migration and integrated acceptance
before the stack merges in dependency order; intermediate PRs are review units,
not independent releases.

**Design source:** `DIGI-UW/openelis-work` at
`c9722f07304c3170913d565876a6dd085c388a7d` —
`designs/microbiology/amr-micro-v2-amendments.md` (draft 10.4),
`designs/microbiology/m-18-environmental-microbiology.md` (v0.9),
`designs/sample-collection/clinical-order-entry-v4.md`,
`designs/reports/patient-report-redesign.md`. Identifiers such as FR-02.4 and
AC-V2-54 refer to anchors in the amendments document (`#fr-02.4`, `#ac-v2-54`);
M-18 identifiers are prefixed `M18-`.

The design owns user-visible behavior. This document owns how that behavior
exists in OpenELIS. The database schema is authoritative in code (Liquibase
changesets and entities); this document states the model and invariants that
code must satisfy. When the design and this document disagree, fix one of them
before implementing.

## 1. The picture

A **micro test** is an ordinary catalog test with _Opens a Microbiology case_
on. Reception orders it like any test. Saving the order opens or joins a
**case**: one per order, sample type and lab unit (plus site for environmental
work). Everything after reception happens on the case: case information,
initial testing, culture rows with readings and subcultures, isolates,
AST/DST runs, additional testing, incoming results, notes, validation,
partial/final/amended releases and critical calls. The **case lab unit** decides
who sees and changes the case. The case's **Program** supplies questions and a
reporting track; it never routes work.

Results on a case are ordinary OpenELIS analyses and results, worked on the case
instead of the Results/Validation pages. V2 adds the case structure around
them; it does not add a second result model.

There is no workflow type, culture type, culture setup, protocol, Unassigned
state, reception Microbiology section, or micro-only report template.

```
 Order entry (shared)            Case (V2)                         Shared OpenELIS
 ─────────────────────           ─────────────────────────         ─────────────────────
 test picker ──save──► routing ─► MicroCase ──members──► sample items (incl. isolate items)
 What this order will open        │  case lab unit, Program  ──► analyses / results
 per-sample site/time/set         │  culture rows, readings  ──► inventory items/lots (no stock)
                                  │  isolates, AST/DST runs  ──► organism/antibiotic/breakpoint refs
 Edit order / cancel ────────────►│  incoming result queue   ◄── analyzer + referral returns
                                  │  report choices          ──► report versions, patient report
                                  │  timeline                ──► notes, callback log, audit
 Worklist (Cultures, AST) ◄───────┘  needs-attention reasons ──► Workplan print records, labels
```

## 2. Engineering decisions

| ID  | Decision                                                                                                                                                                                                                                                                                                                                                  |
| --- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| D1  | V2 replaces the front of V1 and evolves the back (§10). Tables whose concept V2 keeps are extended in place; tables V2 reshapes are restructured in place; new concepts get new tables.                                                                                                                                                                   |
| D2  | What V2 retires is deleted first. Retired columns and tables keep their data until the clinical migration (§11) has used them, then they are dropped; meanwhile they are made nullable so new rows never fabricate a value, and production case creation never writes them. The temporary WHONET read and UAT-only stamp described in §10 end at step 15. |
| D8  | A schema change ships registered in the application changelog in the same change as the code that needs it, and that change boots on a fresh and an upgraded database. Each change is written once against this model, never per increment.                                                                                                               |
| D3  | Case tests are ordinary `Analysis`/`Result` rows on member sample items. V2 stores their case placement, not their values (FR-06.2, FR-06.5).                                                                                                                                                                                                             |
| D4  | Work stage, culture outcome, related cases, number of sets, needs-attention reasons and due times are computed from case parts. They may be cached for queries but are never edited directly (FR-17.1, FR-17.2, FR-02.4a, FR-04.1, FR-12.1).                                                                                                              |
| D5  | The case timeline is an append-only event log on the case (actor, time, event, reason, before/after). The shared audit trail continues to record row changes.                                                                                                                                                                                             |
| D6  | Every write checks the case lab unit and the final-release lock on the server (Access, FR-17.7).                                                                                                                                                                                                                                                          |
| D7  | One routing rule produces the order-entry preview and the saved cases (FR-02.3b).                                                                                                                                                                                                                                                                         |

## 3. Catalog, configuration and dictionaries

**Test catalog** (FR-01.1, FR-01.1a, FR-01.1b, FR-15.3)

- `opensMicroCase` (default off). Off means the test never opens or joins a case.
- `caseRole`: `CULTURE`, `DIRECT` or `CASE` (default `DIRECT`); shown only when
  the switch is on. A `CASE`-role test holds no result and prints nothing; it is
  complete when the case is finally released.
- `collectedInSets` (culture-role tests only, for example Blood culture).
- The existing **Reportable** setting is the In-lab-only default. No separate
  attribute.
- **Reagents and media** (FR-05.2a): the existing reagent links also accept
  Microbiology medium items, with optional sample type, order, duration and
  unit, check interval, loop volume, and atmosphere/temperature overrides.
  Media links never consume stock and never gate anything.

**Programs admin** (FR-03.7): `showOnMicroCase` and `reportingTrack` (a
dictionary value). Exports declare which tracks they take (FR-19.3).

**Lab units** (FR-02.1, M18-FR-A2, FR-05.1f): a lab unit is eligible for cases
when it has at least one active micro test; it keeps exactly one domain
(clinical or environmental); `requireTrackedMedia` (default off).

**Site settings**: `blockSelfValidation` (FR-17.5); culture-set interval,
default 30 minutes (FR-02.4a); repeat-isolate window, default 14 days
(FR-21.2).

**Dictionary categories, seeded and lab-extendable** (No Hard Delete):
Reporting track (Bacterial, TB, Mycology); Culture atmosphere (FR-05.1a);
Culture reading and Culture quantity (FR-05.4); Extend incubation reason
(FR-05.3a); Microscopy grade (FR-06.1a). Deactivated values stay on old records
and leave new choices (AC-V2-101).

**Inventory** (FR-05.1d): a seeded "Microbiology medium" type tag, the per-item
_Track lots_ property, and optional usual atmosphere and temperature on medium
items.

**Seeded reflex rule**: "Blood culture result = Positive: add Gram stain,
culture on that bottle, not In lab only", an ordinary reflex rule
(FR-10.1g, AC-V2-102).

## 4. Domain model

Each case-owned record belongs to exactly one case. Shared samples, order
clinical details and Program questionnaire responses can be referenced by
several cases as specified below. §10 maps each V1 concept to its disposition.

### 4.1 Case

| Concept                    | Holds                                                                                                                                                                                                                                                                                            | Rules                                                                                                                                                                                                                                                                                                                                                                              |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Case**                   | order, sample type, case lab unit, domain, subject (patient, or site for environmental), site (environmental), Program, culture purpose, collection method, original specimen type (received isolates), status (active, cancelled, rejected), released-final flag, opened by (test or case test) | Opening grouping = order + requested sample type + lab unit (+ site when environmental), with the collected-in-sets exception in §5. This groups new work, not a permanent uniqueness constraint: transfer, split and migrated cases can leave separate cases with the same grouping. Identity never changes on transfer (FR-02.6). Cancelled and rejected are terminal (FR-17.8). |
| **Case member sample**     | case, sample item, joined at/by, split-out at/by/reason                                                                                                                                                                                                                                          | A sample item has at most one active membership in a given case. Separate cases can share it, including in the same current lab unit after transfer; membership is not unique by sample and current lab unit. Aliquots (TB decontamination) belong to their parent's case and are not members of a related case (FR-05.8).                                                         |
| **Case analysis**          | case, analysis, role snapshot, collected-in-sets snapshot, placement (§4.3), added by (order, user, rule + rule id, incoming placement), cancelled at/by/reason                                                                                                                                  | An analysis has at most one active case. Cancelled ownership is kept; only active ownership is unique (FR-02.8, FR-07.5).                                                                                                                                                                                                                                                          |
| **Order clinical details** | order, patient origin, admission date, clinical diagnosis, reason for test, clinical history (≤ 4000 chars), prior antibiotics (agent + date, repeatable), replicates (environmental)                                                                                                            | One record per order, shared by every case on that order (FR-03.3a, M18-FR-C5a). Admission date is optional and never inferred (FR-03.5). Clinical fields never exist for environmental cases (M18-FR-C7).                                                                                                                                                                         |
| **Related cases**          | —                                                                                                                                                                                                                                                                                                | Cases sharing a member sample, plus cases linked by a recorded split. The switcher identifies each case, its samples and its current lab unit, including cases in the same unit and after transfers (FR-04.1, AC-V2-63). Sharing an order alone does not create this relationship.                                                                                                 |
| **Number of sets**         | —                                                                                                                                                                                                                                                                                                | Computed: distinct set numbers on member samples carrying a collected-in-sets test (FR-02.4a).                                                                                                                                                                                                                                                                                     |
| **Timeline event**         | case, actor, time, type, reason, details                                                                                                                                                                                                                                                         | Append-only. Every case write records one (FR-01.7, FR-12.6).                                                                                                                                                                                                                                                                                                                      |

**Requested-test ownership** links the case to the saved requested sample/test
before a physical sample or analysis exists. It keeps the requested sample type,
ordered test, role, collected-in-sets choice and cancellation history needed by
routing and later collection. Recording the actual sample resolves that explicit
request into the case membership and analysis ownership; retries reuse those
links. No placeholder physical sample, analysis, collection time or receipt time
is fabricated to open a case. Cancellation and reorder preserve the earlier
request history and do not reactivate cancelled ownership.

A split records the source and resulting case, moved memberships, actor, time
and reason. This relationship remains available when the cases no longer share
a sample or a lab unit.

Program questionnaire answers stay in the existing questionnaire storage, one
response per order and Program, shared with order entry and its FHIR mirror
(FR-03.6, AC-V2-111).

### 4.2 Samples (shared, extended)

Each sample item carries, from order entry v4 section N: container type, body
site and side, collection date/time, collector, collection timing (Spot, Early
morning), and set number when it carries a collected-in-sets test (FR-02.4a,
FR-04.3). Corrections go through the order's steps and are recorded on the
case timeline (FR-04.2).

An **isolate sample item** (sample type Isolate, parent = source sample item)
is created when an isolate needs a label, referral or tests beyond
identification and susceptibility. It belongs to the same case and is never
accessioned twice (FR-10.1c, AC-V2-57).

### 4.3 Case tests

A case test is an ordinary analysis with a case placement:

| Placement          | Target                                                       |
| ------------------ | ------------------------------------------------------------ |
| Initial testing    | the specimen                                                 |
| Test on a culture  | a culture row                                                |
| AST/DST            | an isolate (through runs, §4.6)                              |
| Additional testing | the specimen or an isolate (through its isolate sample item) |

Per case test: In lab only (defaults from catalog Reportable; never on the
generic culture test, FR-15.5), tested elsewhere (performed by an organization
or a user, date performed; organization means **external**, FR-06.3), added by
rule (rule name), reagent lot (required when the catalog link says so,
FR-06.2a). Statuses are the ordinary analysis statuses: Not started, Awaiting
validation, Returned, Validated, Cancelled (FR-06.5, FR-06.5a). A result typed
on the case is a run of one, so reagent lots, control policy and QC holds apply
as on Results (FR-17.5, AC-V2-59). Flags, critical ranges and reflex rules are
evaluated exactly as on Results Entry (FR-07.6).

### 4.4 Culture rows

| Concept                          | Holds                                                                                                                                                                                                                                                                                                                              | Rules                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Culture row**                  | case, source sample item or aliquot, parent row (subculture), subculture purpose, container identifier, medium item, lot or _not tracked_, atmosphere, temperature, duration + unit, check interval, loop volume, inoculated at, positive at + source (analyzer or user), outcome (growth, no growth, contaminated), outcome by/at | Duration, unit and atmosphere are required (AC-V2-10). Inoculated at is editable until the first reading, never before receipt or after now (FR-05.1e). Positive at is not before inoculated at and not in the future (FR-05.4b). Saving a row never changes stock (FR-05.1b). A tracked medium needs a usable lot; with `requireTrackedMedia` a not-tracked medium is refused (FR-05.1f). Each row has its own readings and outcome (FR-05.4c). |
| **Reading**                      | row, coded reading, quantity, note, incubation day, by, at                                                                                                                                                                                                                                                                         | Kept as a read log, never overwritten (FR-05.4, AC-V2-23).                                                                                                                                                                                                                                                                                                                                                                                       |
| **Extension**                    | row, extend by + unit, reason, note, by, at                                                                                                                                                                                                                                                                                        | Extensions add time; the clock always runs from inoculated at (FR-05.3a, FR-05.6).                                                                                                                                                                                                                                                                                                                                                               |
| **Instrument negative proposal** | row, signal, received at, confirmed by/at                                                                                                                                                                                                                                                                                          | Nothing is recorded until a person confirms (FR-05.4d).                                                                                                                                                                                                                                                                                                                                                                                          |
| **Positive-time edit**           | row, old, new, by, at                                                                                                                                                                                                                                                                                                              | Kept on the timeline (FR-05.4b).                                                                                                                                                                                                                                                                                                                                                                                                                 |

Computed per row: incubation ends, next check, check due, final read due,
overdue, time to positivity (FR-05.3, FR-12.4a). Late growth on a no-growth row
reopens the culture; after final release it requires an amendment (FR-05.7).
Media links prefill new rows; otherwise the previous row does (FR-05.2).
Inventory shows a computed _Used on cultures_ list per lot (FR-05.1g).

### 4.5 Isolates and referral

| Concept                     | Holds                                                                                                                       | Rules                                                                                                                                                                                  |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Isolate**                 | case, number (ISO-n), picked-from culture row or _received from_ organization, significance, isolate sample item (optional) | Picked from a row with growth (FR-10.1b). A received isolate starts as ISO-1 with culture outcome _Growth (received isolate)_ (FR-02.13).                                              |
| **Identification**          | isolate, organism, status (preliminary, final), method, date, by, reason                                                    | History is kept. Reidentification needs a validator and reason, and an amendment after final release. It never reinterprets old susceptibility readings (A-10 identification history). |
| **Reported identification** | isolate, organism from the sender                                                                                           | Kept separate. A mismatch must be acknowledged at validation (FR-02.13, AC-V2-106).                                                                                                    |

Referral is the existing referral record on sample items, opened from the case
with the order-entry Refer out panel (FR-08.0 to FR-08.5). An isolate is
referred through its isolate sample item (FR-08.1a). The case shows _Referred_
while any referral is open (FR-17.4).

### 4.6 AST/DST

| Concept                | Holds                                                                                                                                                                                                                            | Rules                                                                                                                                                                                          |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Run**                | isolate, published panel version, method, interpretation model (clinical breakpoints or TB critical concentrations), standard + version, reason when not the active default, instrument and software version, status, validation | Several runs per isolate (FR-07.2a). The default panel is added when an isolate is identified, except NTM (FR-07.1b, FR-14.4). A panel already on an isolate cannot be added again (FR-07.1a). |
| **Agent reading**      | run, agent, raw MIC or zone, matched breakpoint level, interpretation, instrument interpretation, expert flags, QC state                                                                                                         | Original values are never overwritten. Changing the active standard or panel never changes old results (A-07 susceptibility history).                                                          |
| **Attempt / override** | reading or run, type (repeat, retest, override, revert, invalidation), reason, validator, before/after                                                                                                                           | Originals are kept (A-07 susceptibility history).                                                                                                                                              |
| **Use for reporting**  | isolate, agent, chosen run                                                                                                                                                                                                       | Exactly one validated reading per agent on the report; defaults to the organism default panel's run (FR-07.2b, FR-11.5).                                                                       |

Molecular DST on the specimen (line probe assay, Xpert MTB/XDR) is an
Initial-testing case test whose per-drug results count toward TB classification
(FR-06.1b).

**TB resistance classification** (FR-14.1 to FR-14.5): computed per case from
phenotypic and genotypic per-drug results (RR, MDR, pre-XDR, XDR,
pan-susceptible, mono-, poly-resistant). A supervisor edit is stored with its
reason. A drug with disagreeing molecular and phenotypic results is
_Discordant_, and the case cannot pass validation until a supervisor records a
resolution. NTM isolates are excluded.

### 4.7 Incoming results

| Concept           | Holds                                                                                                                                                           | Rules                                                                                                                                                                                                                                                                                                   |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Incoming item** | case, source (analyzer, referral), test, value, run/source key, received at, state (waiting, placed, failed + reason, needs amendment), placed by/at, placement | Only results for tests **not** on the case wait here. Results for existing rows go to the row, marked for review (FR-09.1, FR-09.2). Nothing is placed without a person (AC-V2-16). Identical repeats are not queued twice (FR-09.4a). After final release, items wait as _needs amendment_ (FR-09.4b). |

Placing adds the test to the case and the order in one action (FR-09.3,
FR-07.4a). A move needs a reason and leaves one copy (FR-09.4). A reflex rule
never adds a test already on the case or waiting here (FR-07.4c).

### 4.8 Notes, report choices, releases, calls

- **Notes** use the shared note mechanism with targets: case, case test result,
  culture row, isolate, agent reading. Type is In Lab Only (default) or Send
  with Result (FR-13.1 to FR-13.1c).
- **Report choice** per reportable result and per agent: selected, changed
  by/at. Defaults per FR-11.2. In-lab-only tests are not listed (FR-11.1 to
  FR-11.3).
- **Release** = a shared report version: Partial, Final or Amended, with
  validator and time (FR-17.6, FR-11.7). **Amendment**: open, cancel and release,
  each with reason and actor; the issued original stays immutable (A-17 issued
  history).
- **Critical call** = one shared callback-log entry per attempt, linked to its
  case, result or isolate, with finding text, caller, time, recipient, required
  outcome and follow-up (FR-18.1 to FR-18.5).
- **Surveillance re-export flag** on the case, set by a Program change or a late
  revival (FR-19.7).

## 5. Routing and case membership

1. Saving an order (or accepting an electronic order) runs one routing rule
   inside the order's all-or-nothing save (FR-02.3, FR-02.9, M18-FR-C8). A
   physical sample is not required: eligible requested work opens or joins its
   case using the requested sample type. The responsible lab unit sees that
   case as **Awaiting sample**, without implying collection or receipt. Later
   sample recording attaches to that same case through requested-test
   ownership; it does not create a second case or duplicate a sample.
2. A test joins a case only when `opensMicroCase` is on. Its key is order +
   sample type + the test's lab unit (+ site when environmental). An existing
   active case with that key is reused; otherwise a case opens (FR-02.4,
   FR-02.5).
3. Collected-in-sets tests: all samples of one order carrying that test form one
   case, whatever their sample type, site or time (FR-02.4a, AC-V2-54).
4. A `CASE`-role test opens an empty case in its lab unit (FR-02.3a,
   AC-V2-88).
5. A test in another lab unit on the same sample opens that unit's related case
   (FR-02.4, AC-V2-03). When a test could join two related cases, it joins the
   one opened first (FR-02.5).
6. Program never determines routing (FR-02.2). Independently of choosing the
   case, its initial Program defaults from the order's Program only when that
   Program has `showOnMicroCase`; routing never changes the order's Program.
7. Resaving is idempotent: no duplicate case, membership or analysis ownership.
8. Reflex: a rule adding a micro test of another lab unit opens that unit's
   related case; an ordinary test added by a rule joins the case whose result
   triggered it (FR-02.10).
9. A Tested-elsewhere test with no sample is not part of a case (FR-02.11). A
   received isolate is (FR-02.13).
10. **Preview** (FR-02.3b, AC-V2-103): the same rule evaluated over the unsaved
    order, writing nothing. It lists each case that would open with sets and
    bottles, tests that stay in Results, referred and tested-elsewhere tests,
    and named reflex rules that may add tests. It warns, without blocking, about
    split lab units and lab units with no other work.
11. **Set warnings at save** (non-blocking, FR-02.4a, AC-V2-104): a set with one
    bottle, two bottles of the same container type in a set, one set at
    different sites or more than the configured interval apart, and a
    paediatric bottle in a set with adult bottles.

**Edit and cancel** (FR-02.8, FR-07.5): additions route before removals are
evaluated. Removing the last active micro test of a case asks for confirmation.
With results it also needs a reason, and the case keeps its results as
cancelled. Cancelled ownership is kept. Reordering a cancelled test creates a
new analysis.

**Transfer** (FR-02.6, AC-V2-04): changes only the case lab unit. Needs Results
rights in both units. Only eligible units in the same domain are offered. Never
merges with a destination case that has the same key. Refused after final
release unless an amendment is open. Recorded on the timeline.

**Split** (FR-02.5, AC-V2-63): a member sample with no results moves to its own
related case, with a reason on both timelines and a retained split relationship
for navigation. Both cases remain linked in the switcher even within one lab
unit, without needing to share a remaining sample. Not offered for a sample with
results. Joining and regrouping are deferred.

## 6. Case work rules

- **Case information** (FR-03.1 to FR-03.7): required to save is culture purpose
  only (defaults Diagnostic; environmental defaults Routine monitoring).
  Required before final: patient origin, Program and the questionnaire's
  required questions. Nothing blocks the case from opening.
- **Adding tests** (FR-07.1 to FR-07.5): the standard test/panel chooser,
  pre-filtered to the specimen's sample type (including "used as" types), the
  case lab unit first. In AST/DST it is scoped to an isolate and its organism's
  panels.
- **Culture work** (A-05, A-10): Start inoculation proposes the culture test's
  media for the sample type; Add on any row offers Gram stain, Test on this
  culture and Subculture; subcultures require _From culture_; the tree is
  parent → tests → subcultures.
- **Work stage** (FR-17.1): the furthest point any open part has reached —
  Received, Initial testing, Incubating, Growth detected, Identification,
  AST/DST in progress, Results entered, Validated. It moves back when work
  reopens.
- **Culture outcome** (FR-17.2, FR-17.3): Growth, No growth (every row), Contaminated
  (every row, no repeat pending), NTM identified, No culture ordered (automatic),
  No culture performed (reason).
- **Validation** (FR-17.5, AC-V2-47): results are validated per result; runs
  with Review run / Accept results. Expert flags, QC failures, organism
  mismatch, missing breakpoints, sender identification mismatch and TB
  discordance block validation until resolved. With `blockSelfValidation`, the
  entering user cannot validate, here or on the Validation page.
- **Partial release** (FR-17.6, AC-V2-20, AC-V2-29): any number of times before
  final; needs at least one validated, selected result.
- **Final release** (FR-17.6): needs a culture outcome, every selected result
  validated, required-before-final fields, open referrals for culture or AST/DST
  work returned or cancelled, and validation checks resolved. Pending
  Additional tests and In-lab-only tests never block (FR-15.7). Waiting incoming
  items ask for confirmation.
- **After final** (FR-17.7, AC-V2-45): every case write is refused by the server
  until an amendment is open.
- **Patient history** (FR-21.1 to FR-21.3, AC-V2-74): earlier cases across orders
  in lab units the reader may access. A repeat isolate (same patient, organism
  and specimen type with AST inside the repeat-isolate window, matched by the
  WHONET first-isolate matcher) offers _Refer to previous susceptibility_, which
  cancels the default panel with a reason for the validator to review; the report
  then prints "Susceptibility as reported on {labNumber} ({date})". A TB-track
  treatment follow-up case shows the baseline TB case and follow-up results by
  treatment month, read-only.

## 7. Output

- **Patient report** (FR-11.4, FR-11.4a, AC-V2-53): only selected validated
  results, inside the patient report under the case lab unit's section, grouped
  Initial testing → Culture → AST/DST → Additional testing; one susceptibility
  block per reported isolate. Send with Result notes print beside their target.
  Callback lines print under the finding. Earlier versions stay inspectable.
- **Environmental certificate** (FR-11.8, M18-FR-D6): environmental cases print
  on the existing Laporan Hasil certificate, never on the patient report.
- **Patient results views** show only the current version of each micro
  result, labelled Amended after an amendment (FR-11.9).
- **Electronic delivery** continues without losing content; structured micro
  delivery is future work (FR-11.6).
- **Labels** (FR-20.1 to FR-20.3): presets Culture plate, Isolate and AST panel.
  A new _per container_ preset scope extends the shared label presets. Scanning
  a plate, isolate or panel label opens the case at that row.

## 8. Worklist and bench

- Shows only cases whose lab unit the user holds rights in, with a Lab unit
  filter when several apply; the workflow filter becomes Program (FR-12.3,
  M18-FR-E1a).
- **Needs attention** reasons: Check due, Incubation complete, Incoming results,
  Referral returned, Needs amendment, Instrument negative to confirm, Missing
  required-before-final fields. Sorted STAT first, then most overdue, then
  earliest due (FR-12.1, FR-12.1b, AC-V2-112).
- **Cultures view filters**: Awaiting inoculation (one row per case), Check due
  and Final read due (one row per culture row) (FR-12.6).
- **Bench writes**, each an ordinary write to each case with one timeline entry
  per case and Undo: No growth (one or many, with named skips), Inoculate many
  (one sample type and culture test only), Extend 24 h. Online only (FR-12.6a to
  FR-12.6e, AC-V2-78 to AC-V2-81). No run, batch or Bench view.
- **Bench sheet** = a Workplan print with a Microbiology bench layout. The sheet
  number and its rows are the Workplan print record; Open sheet replays it.
  Printing changes no case (FR-12.7 to FR-12.7c).
- Retained from the built worklist: Cultures and AST views, their summary tiles,
  filters, Due column, paging, row actions, Refresh and the shared-queue
  indicator (A-12 kept list).
- **Due action** per case follows FR-12.5; "Day n of N" follows FR-12.4.

## 9. Access

| Action                                                                                                               | Right                           |
| -------------------------------------------------------------------------------------------------------------------- | ------------------------------- |
| Order or cancel micro tests, order received isolates                                                                 | Order entry (Reception)         |
| Everything on the case before final, partial release, bench writes, split                                            | Results in the case lab unit    |
| Transfer                                                                                                             | Results in both lab units       |
| Validate, override, reidentify, resolve discordance, edit TB classification, final release, amendments               | Validation in the case lab unit |
| Refer out                                                                                                            | Existing referral access        |
| Catalog switches, media links, Programs flags, dictionaries, reflex rules, label presets, site and lab-unit settings | Existing admin rights           |
| Microbiology medium items, lots, Used on cultures                                                                    | Existing Inventory rights       |

Every worklist read and every write checks the case lab unit. A user without
rights does not see the case on the worklist; a direct link opens it read-only
and the server rejects any change (Access, AC-V2-49, AC-V2-60). Writes are
blocked offline. No micro-specific permission key is introduced.

## 10. V1 disposition: retire, restructure, evolve

The design marks Isolates, AST/DST, Critical communication, Nonconformance,
Report, Amendment, Timeline and the Worklist views as existing; Case
information as moved; Culture as restructured; Initial testing, Referral point,
Additional testing and Incoming results as new; and the workflow, culture-setup
and reception model as retired.

**Retire** (deleted first, D2):

- workflow type and every consumer (catalog field, case field, panel and
  worklist filters, Change workflow);
- culture setups, protocols, their admin, controls and seeds (Set protocol);
- the reception Microbiology section, micro draft pipeline and Program guards;
- V1 order routing (workflow/protocol/Program based); V2 routing replaces it
  in roadmap step 5;
- creation of the Unassigned state;
- V1 specifications under `specs/782-*`.

The temporary exceptions are the read-only `MicroCase.workflowType` mapping
and WHONET bacteriology filter, plus the UAT seeder stamp for WHONET scenarios
representing stored V1 cases. They do not route new clinical work. Step 15
replaces them with purpose/Program-track populations and removes both exceptions.

Retired runtime behavior is replaced in its owning step: linear stage and
Unassigned behavior (step 12); the inventory stock usage link (step 8); Growth
work-up, Mark checked and the "No change" reading (steps 8 and 13); Previous
report fields (step 7); and the remark projection as printed output (step 12).
Retired stored values and tables needed to preserve clinical meaning remain
until the clinical migration has consumed them (step 16, D2).

**Restructure in place**:

| V1                            | V2                                                                           |
| ----------------------------- | ---------------------------------------------------------------------------- |
| `MicroCase` (one sample item) | Case with lab unit, Program, member samples (§4.1)                           |
| `MicroCaseAnalysis`           | Case analysis with role, placement and cancelled ownership (§4.1, §4.3)      |
| `MicroCaseOrderDetail`        | Order clinical details shared by the order's cases, plus case fields (§4.1)  |
| `MicroCaseInoculation`        | Culture rows with media, lots, timing, readings, extensions, outcomes (§4.4) |
| `MicroCulturePurpose`         | V2 purpose values (§12)                                                      |

**Evolve** (kept and extended):

| V1                                                                    | V2 additions                                                                 |
| --------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| Organism, antibiotic, breakpoint standard/rule/activation, AST panels | Interpretation model per panel; content unchanged (§4.6)                     |
| `MicroIsolate`, identification events, significance                   | Picked-from row, received isolates, reported identification, isolate samples |
| `MicroAstRun`, readings, attempts, overrides                          | Several runs per isolate, standard reason, use for reporting (§4.6)          |
| `MicroCaseAmendment`, `MicroReportVersion`, final release state       | Partial/Final/Amended versions and report choices (§4.8)                     |
| `MicroCriticalCommunication`                                          | Shared callback-log entry per attempt (§4.8)                                 |
| `MicroCaseActivity`                                                   | The case timeline (D5)                                                       |
| `MicroWhonetExportRun`, selection                                     | Purpose and track populations (§12)                                          |
| Worklist Cultures and AST views                                       | Needs attention, bench filters and writes (§8)                               |
| Patient-origin defaults                                               | Unchanged                                                                    |

New tables cover requested-test ownership, readings, extensions, incoming
results, report choices, use-for-reporting choices and TB classification edits.

## 11. Existing data

Structural changes travel with the code that needs them (D8). Because evolved
and restructured tables keep their rows, existing cases keep their identities,
results, isolates, identification history, AST readings and attempts, issued
reports, amendments, calls and history without a copy step.

Structural changes are rehearsed throughout the stack against the real
application changelog and a V1 database copy. Preserving rows alone is not proof
that V2 can read or authorize them correctly. Each milestone identifies the
existing-data requirements of its new behavior and keeps their verification in
its upgrade evidence. No intermediate stack revision is deployed as a completed
cutover.

One dedicated clinical migration (roadmap step 16), before V2 is released, handles what needs
meaning rather than structure (FR-01.6, FR-01.7, A-16, AC-V2-42):

- give every case a lab unit; assign a Program mapped from its retired workflow
  type where missing, with a timeline note, and list it for review;
- keep cases that now share a grouping key separate and list them for review;
- present earlier workflow, protocol and purpose changes as readable timeline
  history with their original actor, time and reason;
- clean up the duplicate Microbiology program and the misrouted Bacteriology
  program (FR-02.12);
- refuse to run when a mapping is ambiguous, changing nothing;
- then drop the retired columns and tables.

Before the migrated application serves case work, every mapping and membership
required by V2 routing, lab-unit access and reporting must be established. The
complete stack is accepted only after this migration on an upgraded database as
well as a fresh installation (roadmap step 17). Ambiguous clinical mappings stop
the migration without partial clinical changes; they never trigger a legacy
fallback or a fabricated assignment.

Applied Liquibase changesets are never edited.

## 12. Surveillance populations

- Purpose values: clinical Diagnostic (default), Screening, Treatment follow-up,
  Survey or study, EQA/proficiency; environmental Routine monitoring (default),
  Post-cleaning check, Outbreak investigation, Complaint (FR-19.1, M18-FR-C5).
- Inclusion by purpose follows FR-19.2. Inclusion by track follows FR-19.3, one
  rule owned by the WHONET export with one first-isolate de-duplication.
- External results are marked and excluded from antibiogram and GLASS; received
  isolates count here as referred-in under their original specimen type
  (FR-19.5, FR-19.5a).
- Contaminated TB exports nothing; contaminant-only bacterial exports as
  negative; NTM exports as NTM; No culture performed is excluded (FR-19.4).
- Environmental isolates: never in antibiogram or GLASS; WHONET only with
  _Include environmental isolates_, coded environmental, no first-isolate
  de-duplication (FR-19.6, M18-FR-F1 to M18-FR-F4).
- In lab only never decides export eligibility (FR-15.6).

Antibiogram, GLASS and cluster delivery are future capabilities; V2 only
supplies their population rules.

## 13. Environmental cases

Environmental work uses its own lab unit with the Environmental domain
(M18-FR-A1). The case subject is a site (a sampling site until Locations &
Organizations lands) with sampling points per sample (M18-FR-C9, M18-FR-D1). The
layout is the same; clinical-only fields are absent (M18-FR-D3). Purpose belongs
to each case: changing one case from Routine monitoring to Outbreak
investigation does not change another case on that order. Replicates belong to
the order and update all of its cases, with a timeline entry on each. The
"Applies to all cases on this order" helper belongs to Replicates, not Purpose
(M18-FR-C5a, corrected AC-M18-07b). Expert rules
needing patient data are skipped and shown as skipped (M18-FR-D4). Critical
notifications go to the order's requester contact (M18-FR-D5). Case search also
matches site name, site code and sampling point (M18-FR-E3).

## 14. Shared OpenELIS dependencies

The status column records the original name search of `develop` at
`53a8f78d68`; it is not a verified current capability assessment. Before its
consumer is implemented, the owning milestone verifies the actual shared code
path and records the revision, behavior evidence and any existing delivery PR
in that milestone's PR. Where the capability is absent, that milestone delivers
the required reusable portion in the shared module before its consumer. This
work stays within the outlined milestones and the same implementation stack.
The target behavior is not reduced because a dependency is unfinished, and no
microbiology-only duplicate substitutes for a shared capability.

| Dependency                                                          | Required behavior                                                                          | Original search result                        | Owning milestone                                                    |
| ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ | --------------------------------------------- | ------------------------------------------------------------------- |
| Multi-component results (OGC-1126/1127)                             | Gram stain, Xpert, LPA (FR-06.1a)                                                          | present                                       | 7 — case tests/results                                              |
| Program questionnaires + FHIR mirror                                | Case information questions (FR-03.6)                                                       | present                                       | 6 — case information                                                |
| Critical callback log (OGC-714)                                     | Calls (A-18)                                                                               | present                                       | 12 — releases/report/calls                                          |
| Order entry Tested elsewhere (OGC-1424)                             | Sampleless external tests and received isolates (FR-02.11, FR-02.13); case tests (FR-06.3) | present                                       | 5 — routing; extended use in 7 and 9                                |
| Referral on sample items                                            | Refer out (A-08)                                                                           | present                                       | 9 — isolates/referral                                               |
| Reflex rules                                                        | Rule-added tests (FR-07.4)                                                                 | present                                       | 5 — routing; culture rule in 8 and incoming de-duplication in 11    |
| Label presets                                                       | Existing per-order/per-sample scopes plus per-container scope (FR-20.2)                    | present; scope to add                         | 13 — worklist/bench/labels                                          |
| Inventory items and lots                                            | Media and lot selection without stock consumption (FR-05.1b)                               | present                                       | 8 — cultures/media                                                  |
| Inventory Track lots and type tags (Inventory v1.9)                 | Shared per-item lot tracking and medium tag (FR-05.1d)                                     | not found                                     | 8 — shared Inventory support before culture entry                   |
| Result run / run of one (OGC-1200)                                  | Reagent/control policy and QC holds on typed results (FR-17.5)                             | not found                                     | 7 — shared result processing before case entry                      |
| "Used as" sample types                                              | Shared compatible-type chooser behavior (FR-07.1)                                          | not found                                     | 5 — order-entry routing/chooser; reused in 7                        |
| Block self-validation                                               | Same rule on case and ordinary Validation screens (FR-17.5)                                | not found                                     | 7 — shared validation                                               |
| Report versions / print queue (OGC-1031 r4)                         | Shared version and release behavior (FR-17.6)                                              | present by name; shared ownership unconfirmed | 12 — shared reporting before case release                           |
| Redesigned patient report (OGC-1111)                                | Required microbiology report block (A-11)                                                  | not found                                     | 12 — shared report support                                          |
| Workplan print record                                               | Persisted bench sheet and Open sheet (FR-12.7)                                             | partial; confirm                              | 13 — shared Workplan support                                        |
| Order-entry per-sample body site/time/set and bottle classification | Culture sets and all nonblocking warnings (FR-02.4a)                                       | partial; classification not found             | 5 — shared order entry/catalog                                      |
| Sampling sites; Laporan Hasil certificate                           | Environmental subject and report                                                           | present                                       | 5 — site routing; report in 12; full environmental acceptance in 15 |

## 15. Clarified behavior and delivery

- **Environmental scope:** purpose is per case; replicates are per order. The
  environmental requirement, acceptance criterion and mock helper must say the
  same thing (§13).
- **Related-case navigation:** retain shared-sample and split relationships
  across transfers, including within one current lab unit (§4.1, §5).
- **Before sample arrival:** saving eligible requested work opens its case and
  shows Awaiting sample; later recording attaches the actual sample to that
  case (§4.1, §5). Order saving never depends on a physical sample existing.
- **Bottle warnings:** retain the specified paediatric/adult warning. Verify or
  add configured container classification in the shared catalog in milestone 5;
  do not infer it from a display name or silently omit the warning.
- **Shared dependencies:** verify and deliver the required shared behavior in
  the assigned milestone (§14), without task-sized extra PRs or unapproved
  scope reductions.
- **One stack:** baseline #4646 followed by one PR per outlined implementation
  milestone. Validate each contribution, then clinical migration and full
  integrated acceptance at the final stack revision before merging in order.
  Final acceptance includes every core and environmental criterion, complete
  cross-milestone journeys, retained clinical history, access, offline behavior,
  localization, accessibility, ordinary laboratory workflow continuity and the
  pinned nonfunctional performance requirements.

## 16. Deferred and out of scope

Deferred (Later, not in V2): joining cases, regrouping after results,
configurable final-release rules, six Worklist areas, questionnaire CSV export,
blood culture fill volume, separately assignable release rights, hiding patient
names on worklists, antibiogram, GLASS and cluster detection delivery.

Out of scope: micro-specific colony count fields, breakpoint content changes, a
lab-unit type or micro marker, automatic placement of incoming results, micro
QC, isolate storage, structured micro electronic delivery.
