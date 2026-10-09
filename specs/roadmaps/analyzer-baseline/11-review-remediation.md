# Step 11: Review remediation

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal (8 Oct, the user): "getting this stack clean again": every review thread
on the stack is fixed or answered, and every CI check on the top PR is green,
before step 10's final green (F10) and evidence recording (F11).

### Decisions (8 Oct, the user's words)

- Where fixes go: "New PRs on top (Recommended)". A finding filed on a lower
  PR is fixed in a new stacked PR above #4657, never by amending the PR it was
  filed on; the stack lands as a unit, so the fix ships with the code it
  corrects. The reply on the thread links the fixing commit.
- Ownership: "you own this now". One session owns every stack branch, the
  restacks and the pins.

### Facts

- The inventory (8 Oct, against `b693f7958f`) holds 114 unresolved review
  threads from Copilot and Codex: 3 high, 22 medium and 34 low still open, 5
  where AGENTS.md contradicts the code, and 50 to answer and resolve
  (already fixed, a duplicate, or not a defect).
- CI: the only red check on the top PR was the mock pin to unreleased
  `38d9b645`; the mock is pinned to its v0.2.0 release on #4657. The lower
  PRs' pin checks stay red by design: each pins the Bridge and mock it was
  built with.
- Reviews are filed as `<PR>#<n>` below, the n-th unresolved thread of that
  PR in the inventory.

### Build

PR A, mapping and adoption:

```
- [x] R1 (4611#1, high) Adopting a newer revision keeps each row's assay switch and instrument code: AnalyzerAdoptionServiceImpl.adopt fills a missing enabled or instrumentCode from the current row, as the editor does; red first with an adoption after an Assays-step override
- [x] R2 (4593#1, high) A row whose profile declares a component cannot be confirmed or saved without one: validateConfirmable and save refuse it; the editor keeps the row unresolved until a target is picked
- [x] R3 (4604#2) Adopt checks baseMappingFingerprint under the lock, as the editor does
- [x] R4 (4604#0, 4604#6) A failed Bridge update after the connection moved pins it back; a failed pin-back reports "reconcile required", not "nothing was applied"
- [x] R5 (4615#0) A Bridge refusal during mapping sync rolls the mapping back and reports it, so the analyzer is not left inactive with a committed mapping
- [x] R6 (4628#1, 4628#2) Defaults load active components always, and options on deactivated components do not count as primary
- [x] R7 (4616#1) A component-only import leaves the primary's result type and significant digits alone
- [x] R8 (4599#4) The OCL mapper reloads an existing answer by name, so its codes are kept
- [x] R9 (4603#0, 4603#1, 4603#3) Dictionary save and LOINC sync are one transaction in a service; the select-list save is transactional
```

PR B, results and import:

```
- [x] R10 (4583#3) Two tubes of one order and test are kept apart: the duplicate and held-match identity includes the instrument specimen id (null matches legacy rows)
- [x] R11 (4592#6) A held call with no call component is staged on no test, not on the number's component
- [x] R12 (4592#1) A recovered control's extra rows go through control processing
- [x] R13 (4631#0) An existing result gets significant digits applied on accept
- [x] R14 (4583#0, 4583#1) Save All skips rows with no matched patient; held mismatch rows show their note
- [x] R15 (4593#2) Held component parts offer Review mapping and Dismiss failed run
- [x] R16 (4625#0) An analyzer whose restore failed offers Activate again
- [x] R17 (4583#6) Placement runs once per page, not once per row before paging
- [x] R24 (4611 review, 9 Oct) OE2 stops holding the code an instrument sends: `instrument_code` leaves the mapping (changeset 133), the Assays step and Apply's push to the Bridge; settling codes at setup is analyzers roadmap item 6
```

PR C, deploy, harness and docs:

```
- [ ] R18 (4632#1, 4644#5, high) The testing deployment's delivery check reaches an active GeneXpert: the seed ignores rows on assays that are off; the smoke analyzer activates on the bundled catalog; a kept connection whose profile is gone is recreated. Rehearsed against a local stack built like the deployment
- [ ] R19 (4579#0, 4579#1, 4584#0, 4593#0, 4611#0) AGENTS.md says each analyzer owns its mapping and editor, the types page previews defaults read-only, and changeset 124 is the one-time baseline migration
- [ ] R20 Low findings in files PRs A to C already touch are fixed there; the rest are filed as issues with the thread linked
```

PR D, pairing proof:

```
- [ ] R21 (4657#1) The pairing code never crosses the wire: OE2 sends an HMAC of the code over the observed Bridge certificate and both of its own; the Bridge checks it and answers with its own HMAC, which OE2 checks. Bridge 3.3.1 first, then OE2
```

Review of the top three PRs, 9 Oct (fixed on the top PR):

```
- [x] R25 (4663 review, high) A test is decided by its head row: the page carries the head's choice to every row of the test, so a part's loaded tick cannot save an unticked test
- [x] R26 (4663 review, high) A main result binds to the option on no component or the primary component, and takes its precision, whatever order the options load in
- [x] R27 (4657 review, high) Both web contexts read the Bridge pin from the database, so pairing again applies to delivery and to calls to the Bridge without a restart
- [x] R28 (4657 review) A configured code the Bridge refuses is not tried again
- [x] R29 (4657 review) The pairing status sends pairedAt as ISO-8601 text
- [x] R30 (4661 review) A test proves an answer and its LOINC mapping roll back together
- [x] R31 (4657 review) bridge-pairing.md says the first pairing trusts whichever certificate answers
```

Threads:

```
- [ ] R22 Replies drafted for the 50 threads to answer and for every thread fixed above, shown to the user, then posted and resolved
- [ ] R23 Every CI check on the top PR is green; then step 10 resumes at F7b
```

### Verify

```bash
gh pr checks <top PR>     # every check green
# unresolved review threads on the stack: 0 (scripted GraphQL count per PR)
```

### Done when

1. No unresolved review thread on any stack PR.
2. Every CI check on the top PR is green.
3. Each high finding has a regression test that failed before its fix.
