# Step 10: Finish line

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal (set 6 Oct): "full remediation proved with a re-recording of the Analyzer
workflow evidence package for the full set of analyzer e2e workflows." This
step is the ordered work from the state on 7 Oct to that goal, including what
was left of steps 1b and 7.

### Decisions (7 Oct, the user's words)

- QuantStudio: "Yes, add a QuantStudio file story (Recommended)".
- Legacy E2E stories: "Keep journeys, retire duplicates (Recommended)".
- Seeding: "Yes, as described (Recommended)": the environment seed
  (`seed-analyzers.sh`) is for people using dev, demo and testing stacks; every
  spec sets up the analyzers it needs itself, through the same API sequence the
  seed uses (a story that starts from "a lab already running" does it in a
  before-step); only the testing deployment's delivery check uses the seed's
  output.
- E2E: "e2e should be a user story that can be recorded as a video proof".
  Rules are proved where they live (Bridge, OE2 backend); E2E proves journeys.
- Bridge-only behavior: "if this is purely a bridge concern, likely should stay
  with the bridge. if this is a user-story type thing, then we should cover it
  with an e2e test".
- Deployment images: "Derive from submodules, in this stack (Recommended)".
  The submodule commit is the one record of which Bridge and mock go with this
  OpenELIS; nobody types an image tag.
- Upgrade path: "Tests at each level + rehearsal (Recommended)".
- An analyzer whose profile disappears later: "Offer the same reset
  (Recommended)".
- Harness configuration (7 Oct, after the harness shards failed to start):
  "Only what analyzers use (Recommended)", and "I want to make sure that the
  config we're loading is not being tested by other tests that run on the
  analyzer harness, and I want to raise the 4 minute timeout since that seems
  arbitrary".
- Evidence (10 Oct): "we should have clear admin of profiles separated from
  configuring a new instance of an analyzer, from normal analyzer workflows";
  no copied profile in any video; "make sure we do record full validation of
  analyzer results as one of the scenarios"; "make sure we show hl7, astm, and
  file analyzer workflows since those are the 3 paths we need to support".
- HL7 baseline: "Mindray BC-5380 (Recommended)", a shipped Bridge profile and a
  mock replay of the CBC message its LIS protocol manual prints.
- BC-5380 alarms and manual counts (10 Oct): "Alarms as flags now, drop manual
  count (Recommended)": the alarms an instrument raises about a sample travel
  with that sample's results as flags, never as results, for ASTM and HL7
  alike. Its manual count part is replaced by the next decision.
- Each lab decides (10 Oct): "this will run at various labs - and ideally we
  don't want to force or block workflows but let the lab operators make these
  calls"; agreed: the shipped BC-5380 profile does not leave out the microscope
  exam group (it arrives held until a lab maps or excludes its codes on the
  analyzer's mapping page), and Validation's release rules do not change for
  flags (a lab already chooses bulk release and per-test validation).
- Recording and pins (11 Oct): "we don't need to pin specific commits
  ourselves on evidence packages": recording does not wait for the Bridge and
  mock merges; the pin check is the merge gate.
- Plan before recording (11 Oct): "Build the 🔧 items, then record
  (Recommended)": every story is built and green before anything is recorded.
- Test design (11 Oct): "that's bad test design", on a story whose release step
  ran only when the screen showed a lane count and whose final count came from
  the same screen. A story states what the order holds and reads it back.
- Evidence lanes (11 Oct): "each analyzer type needs it's own evidence lane or
  else it's both confusing and not satisfying for the actual requirements. each
  video can be it's own workflow, we don't have to merge." One lane per
  analyzer type, videos by stage inside the lane, one MP4 per story.
- The empty call part (11 Oct): "is this realistic? why are we not simulating
  things properly if not?": the mock replays Cepheid's example quantified
  message (303-0251 Rev. A, 2.1.1), which carries a number and no call text.
  What the report prints for the empty part is checked before anything changes.
- Misfiled notes (11 Oct): "is this an error? if so we should fix it": the
  repair of the rows the old code wrote ships in the same PR as the code fix.
- Test connection for file analyzers (11 Oct): "Keep one label (Recommended)";
  the Bridge's answer line says what it checked.
- Draft issues (11 Oct): "Fix now on top (Recommended)", on the authoring
  dialog showing `$: required property 'controlResultRecognition' not found`
  inside the control recognition step while the author filled it in.

### Facts

- Coverage split (inventory 7 Oct): the Bridge (about 970 tests and a Docker
  acceptance suite) owns vendor parsing against Cepheid's examples, HL7 and
  FILE parsing, the bundle, code translation both ways, outbound orders, the
  outbox, listeners and routing, the catalog and startup degrade; the mock
  (216 tests) owns its replay, generation and listeners; OE2's
  `analyzer-components.yml` runs the tests that need both against the
  submodules; OE2's backend owns binding, mappings, adoption, import, placement
  (20 unit, 13 integration tests), review and accept, setup and activation,
  delivery issues and the dictionary; OE2's E2E proves journeys.
- The legacy MVP stories: seven per-code GeneXpert scenarios on retired codes
  (MTB-RIF, RIF with values, COVID19); two instruments on one listener;
  recovery of an unknown value (fixtures cannot send one; covered by
  `AnalyzerNormalizedResultImportIntegrationTest` and `worklistDraftRoundTrip`);
  FluoroCycler file import; recovery of a deactivated catalog test on RIF.
  They are the only journeys that reach an accepted clinical result.
- The upgrade migration (changeset 124) keeps every analyzer's id, name, lab
  units, Bridge connection and activation history, clears its mapping and
  leaves it inactive. OE2 lets an analyzer with no mapping choose a profile
  (`AnalyzerInstanceLocalStateServiceImpl.update`, `isInactiveWithoutMapping`),
  and the Bridge accepts a new profile pin on a connection update
  (`AnalyzerConnectionCatalog.update`). Nothing proves the whole path.
- An analyzer that keeps its mapping and loses its profile later cannot change
  type (`analyzer.setup.error.profileKept`), yet its message
  (`analyzer.connection.readiness.profileUnavailable`) says to choose the type
  again.
- Deployments pull published images named by hand in
  `docker-compose.analyzers.yml` (Bridge 3.2.6, mock 0.1.3);
  `.github/scripts/test_analyzer_overlay.py` checks they are releases at the
  submodule commits; `publish-images.yml` already reads the Bridge submodule
  commit to bundle its profiles.
- The harness configuration (`projects/analyzer-harness/dictionary/`) is all
  32 files of `volume/configuration/backend/` (Indonesian address hierarchy,
  water standards, vector, environmental, QA, roles, OCL) plus 6
  `analyzer-harness-*` files; develop's harness loads 2. Each dictionary, test,
  sample type, test section, test result and panel file ends with a full
  display-list rebuild (`DisplayListService.refreshLists()`, about 16 s on a CI
  runner), so the webapp started in over 4 minutes and the harness shards
  failed at "Start containers" (run 37639207944). `example-test-results.csv`
  also gives the base catalog's DNA PCR a second Positive answer, so the
  harness fixture `reporting-field-values.sql` fails ("query returned more
  than one row"). The baseline profiles resolve eight LOINCs: 20447-9,
  94500-6, 85362-2 and 89372-7 (rows of `example-tests.csv`), and 85477-8,
  85478-6, 85479-4 and 89578-3 (`analyzer-harness-tests.csv`).
- The BC-5380 LIS manual (appendix C) groups a sample's message by OBR-4
  (Table 9): `00001` Automated Count is what the analyzer measured, `00002`
  Manual Count is the microscope exam a technician enters on the analyzer
  (manual differential, morphology, blood type). Table 10's "Abnormal alarm
  information" (12000 to 12018 and LOINC-coded suspect flags such as 15180-3
  Hypochromia) is sent as `IS` with `T` or `F`. The manual's example message
  raises Anemia and Hypochromia beside its low HGB and MCHC, and sends all 19
  manual counts as 0.0, which no counted differential is; the manual does not
  say whether an empty microscope exam is sent.
- OE2 shows an instrument's flags and notes on the review row only
  (`InstrumentReported.jsx`); accepting a result writes the reviewer's note as
  an internal note on the analysis (`AnalyzerResultsAcceptServiceImpl`), and
  Validation lists an analysis's notes.
- The webapp health limit was the container's own check (start period 2m, now 10m;
  three checks 30 s apart, in `projects/analyzer-harness/docker-compose.base.yml`,
  copied from `build.docker-compose.yml`); Compose stops waiting when Docker
  marks the container unhealthy, before CI's `--wait-timeout 900`.

### Build

```
- [x] F1 The top PR's CI is honest: the Build + Test failure (AnalyzerAdoptionIntegrationTest, which passes locally) is found and fixed at its cause; every E2E failure is accounted for by a spec this step rewrites
- [x] F1a The adoption integration test creates and deletes its own catalog tests (earlier suites in CI's class order leave the test table empty)
- [x] F1b The harness loads only what the analyzer stories use: the `analyzer-harness-*` files, with the generic rows the baseline profiles resolve (HIV-1 Viral Load, SARS-CoV-2 PCR, Xpert MTB/RIF, Rifampin Resistance) moved into them; the rest of the copied generic dictionary is deleted. Nothing else that runs on the harness (the foundational, storage and reporting fixtures, every harness spec) reads a row only the harness configuration provides or changes; checked by booting the harness and loading its fixtures
- [x] F1c The harness webapp's health grace period lets CI's own wait (15 minutes) govern: a slow start shows as a slow job, never as "unhealthy" at 4 minutes
- [x] F2 Delete the dead code this stack orphaned (`findHeldMappingResultsByProfile` in the DAO, its implementation, the service and its implementation) and the stale M2 entry in playwright.config.ts (the entry went with `5517b2c136`)
- [x] F3 Specs in their end state, each setting up its own analyzers through the setup screens or the shared API sequence and sending only manufacturer fixtures: the MVP journeys (a GeneXpert HIV-1 viral load and a respiratory panel with its components, each to an accepted clinical result; two GeneXperts on one listener; FluoroCycler file to clinical result; a catalog test deactivated after mapping, held, reactivated and recovered, on Influenza B), with the seven per-code scenarios and the unknown-value recovery retired; a new QuantStudio file story to an accepted clinical result; the setup-assays held-result story on fixtures (Influenza B turned off; RSV under a code the profile does not declare); the adoption spec on its own catalog test; the microbiology AST spec with its own source analyzer; the M1 lineage text on the baseline ID; the M3 guided-setup spec (it clicks "Review mappings in Analyzer Types", a link per-analyzer mappings removed). Then `sendGeneXpertAstm` and the setup picker's profile pin are deleted
- [x] F5 E2E user stories for placement, recordable: a result placed on its tube; a mistyped ID held and placed by the reviewer; a patient mismatch explained before saving; a rerun replacing a held result; a FILE plate with one mistyped sample name. Two tubes, an unordered test and an unknown ID stay proved by the placement integration tests
- [x] F6 When the Bridge reports an analyzer's profile unavailable, the operator can reset it the way the upgrade migration does (keep identity, name, lab units, connection and history; clear the mapping; inactive) and set it up again on an available type; the message says so. Red first in OE2 (service and setup screen); then an E2E user story that authors a site profile, sets an analyzer up on it, removes that revision's file from the Bridge's data volume and restarts the Bridge, and the operator resets and sets the analyzer up again (derived 7 Oct: the only way to produce the state)
- [x] F7a The upgrade path is proved at each level: an OE2 integration test (an analyzer left by the migration, its connection kept, is set up on a baseline type, the Bridge connection re-pinned, and it activates), a Bridge test (a connection whose profile revision is gone is re-pinned by an update, becomes ready and runs), and the stranded-analyzer user story of F6
- [x] F8 The operator-style seed is proved on a stack built from the branch pins (7 Oct: a fresh `scripts/dev-stack up` created the GeneXpert, two QuantStudios and the FluoroCycler on their baseline profiles, then confirmed, applied and activated each through the API the setup screens use). The specs set their analyzers up through the setup screens themselves, so the two paths are the same flow, not one shared script; the seed is re-run on the final pins at the end of F10
- [x] F9 Deployment images come from the submodules: `publish-images.yml` publishes the Bridge and mock images that E2E built from the submodule commits and tested, beside the application images and with the same tags, and puts their digests in the deployment manifest; the hand-typed tags in `docker-compose.analyzers.yml` and the release check in `test_analyzer_overlay.py` are deleted; the testing deployment runs the manifest's digests (8 Oct, the user: "we need the PR-based e2e to be based on the submodule pins, not on any other pinning")
- [ ] F10 Green (after [step 11](11-review-remediation.md)): the full harness suite on the local stack, then every CI check on the top PR
- [x] F11a The screens the recordings read are right: the review row keeps its lab number on one line, Results Entry names an analyzer set up after startup, and Validation and the patient report give each part of a result its own unit (`35aaa1e7ee`)
- [x] F11b HL7 baseline, Bridge: a shipped Mindray BC-5380 profile built from its LIS protocol manual (HL7 2.3.1 `ORU^R01`, LOINC-coded OBX), its evidence note, and a test replaying the manual's message; its alarms declared as sample flags; released in Bridge 3.4.0
- [x] F11b2 Sample flags, Bridge (#90), for every protocol: a profile names the codes its instrument sends as warnings about the sample under `sample_flags` (`raisedValue`, `codes`, `names`), read where a result is read; the shape and the rule behind it are [step 12](12-record-kinds.md). Such a record is never a result: a raised one goes in the bundle as an Observation classified `SAMPLE_FLAG` with the code and its name, and one not raised is dropped
- [x] F11c HL7 baseline, mock: the manual's message as a replay fixture sent over MLLP, checked against the profile; released in mock 0.3.0
- [x] F11d HL7 baseline, OE2 (#4663): both pinned, the harness loads the CBC tests the message names, and BC-5380 stories for setup and for a CBC to release. OE2 reads `SAMPLE_FLAG` Observations: the flags show once with their sample on the review screen, and accepting the sample's results writes them as an internal note on each analysis, which Validation lists. The CBC story's analyzer has its microscope exam codes excluded on its mapping page before the clip
- [x] F11e Evidence (agreed 11 Oct): one lane per analyzer type, videos by stage inside each lane (admin, setup, results, exceptions), one MP4 per story, nothing concatenated. Every recorded story carries three tags, `@evidence`, its lane (`@astm`, `@hl7`, `@file`) and its stage (`@admin`, `@setup`, `@results`, `@exceptions`); `harness-demo-video` records the `@evidence` tests and nothing else; the packager reads the tags from the JSON report and writes `<lane>/<stage>/<story>.mp4`. Each story opens on a card that says what it shows, is narrated by captions shown once the page holds its content, and closes on the outcome its assertions read back; results and exceptions stories set their analyzers up before the clip and their results come from the mock. No video copies a profile. The stack is reset and started with `dev-stack up --no-scenarios` and `seed-analyzers.sh --networks-only`. A hole in the matrix is a story to build. The lanes:
  - GeneXpert (ASTM). Admin: the shipped types and their mapping status; authoring and publishing an ASTM profile; publishing a new revision through Update shared; deactivating and reactivating a type. Setup: guided setup; the Assays step on a lab's own catalog; adopting a newer revision; resetting an analyzer whose type the Bridge lost. Results: the HIV-1 viral load through Validation and release; the respiratory panel with its parts; two GeneXperts on one listener. Exceptions: the four placement stories; held results recovered once mapped; a deactivated catalog test; a result the Bridge could not deliver; deactivating and reactivating an analyzer
  - Mindray BC-5380 (HL7). Admin: authoring and publishing an HL7 profile. Setup: guided setup. Results: the CBC with its alarms through Validation and release. Exceptions: the microscope exam the lab does not do is held, excluded on the mapping page, and the held rows clear
  - QuantStudio and FluoroCycler (FILE). Admin: authoring and publishing a file profile. Setup: the QuantStudio guided setup watching a folder. Results: the QuantStudio workbook; the FluoroCycler file. Exceptions: a plate with one mistyped sample name
  - In order:
    - [x] F11e-1 GeneXpert results: the HIV-1 viral load story states its valued parts, all in Needs review, released with their one analysis, each final on the order (257c52ccc4); the internal quantitation standards are flags, so six parts and seven final ([step 12](12-record-kinds.md), R8)
    - [x] F11e-2 The patient report for a quantified and a not-detected HIV-1 viral load, printed on the stack: an empty show-on-report part prints nothing (the quantified report shows only `HIV-1 viral load: 1010` and `Log viral load: 3.00`), so the harness catalog stays as it is
    - [x] F11e-3 Repair changeset with its test: an accept note keyed to a result ID (the code before ca7ce0562c) moves to that result's analysis, found by the result written in the same transaction
    - [x] F11e-4 FILE setup: the QuantStudio joins the guided setup story beside the GeneXpert and the BC-5380, filling the folder the Bridge watches; Test connection keeps its one label
    - [x] F11e-5 HL7 exceptions: the BC-5380 reports the microscope exam the lab does not do; its rows are held, the operator excludes the codes on the mapping page, the held rows clear and the CBC is accepted
    - [x] F11e-6 Setup off screen in the results and exceptions stories that still run it inside the clip (FluoroCycler, QuantStudio, the catalog test switched off, the second GeneXpert on one listener)
    - [x] F11e-7 Admin stage: captions on the types catalog and the authoring story (ASTM, HL7, FILE); on the GeneXpert type, publishing a new revision through Update shared and deactivating then reactivating a type
    - [x] F11e-8 Tags on every recorded story, `harness-demo-video` selecting `@evidence` across the harness directories, the packager writing one MP4 per story by lane and stage, README and docs saying so
    - [x] F11e-9 Every `@evidence` story green on the reset stack without recording; then recorded, every frame reviewed, packaged, and posted as the evidence comment on the top PR (media not committed), after [step 12](12-record-kinds.md)
- [x] F11f A profile draft's issues read as the author's next step. Bridge (#91): a part the draft has not saved yet is reported as the step that saves it, "Save control recognition before publishing." for `controlResultRecognition` and "Save the profile settings before publishing." for anything else; every other issue keeps its text. OE2 (#4663): the profile editor shows the draft's issues once, after the control recognition step and above Publish, and only while neither step has unsaved edits; the control recognition step shows none of its own. Then the three authoring clips are recorded again
```

### Verify

```bash
gh pr checks <top PR>                                   # every check green
cd frontend && npm run pw:test:harness-foundational     # local stack, final pins
grep -rn "sendGeneXpertAstm\|genexpert-astm\"\|fluorocycler-xt\"" frontend/playwright   # none
grep -n "image:" docker-compose.analyzers.yml           # no hand-typed Bridge or mock tag
```

### Done when

1. Every CI check on the top PR is green.
2. Every harness spec sets up its own analyzers and sends only manufacturer
   fixtures.
3. The evidence bundle, one lane per analyzer type (ASTM, HL7, file) with
   admin, setup, results and exceptions videos, is drafted on the top PR.
4. An upgraded analyzer and a stranded analyzer can both be set up again by an
   operator, proved at each level; the stranded one is recorded.
5. Nobody types an image tag or a commit hash for the Bridge or mock outside
   the submodule.
