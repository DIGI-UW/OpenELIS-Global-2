# Step 10: Finish line

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal (set 6 Oct): "full remediation proved with a re-recording of the Analyzer
workflow evidence package for the full set of analyzer e2e workflows." This
step is the ordered work from the state on 7 Oct to that goal. It replaces the
open tasks of step 7 (T7.1b, T7.2b, T7.4b, T7.5 to T7.8), which point here.

### Decisions (7 Oct, the user's words)

- Outbound orders: "I believe outbout orders are still planned/deffereed? ...
  if not, then we need to defer, and obviously we would use the translation
  for both ways in the future". OE2 sends no orders to analyzers today; that
  feature is deferred. The Bridge already uses the lab's instrument codes both
  ways (one code-to-LOINC table per connection, the lab's code first;
  `aSavedCodeOverrideIsTheCodeTheInstrumentIsOrderedAndTranslatedBy`).
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
- The webapp health limit was the container's own check (start period 2m, now 10m;
  three checks 30 s apart, in `projects/analyzer-harness/docker-compose.base.yml`,
  copied from `build.docker-compose.yml`); Compose stops waiting when Docker
  marks the container unhealthy, before CI's `--wait-timeout 900`.

### Build

```
- [ ] F1 The top PR's CI is honest: the Build + Test failure (AnalyzerAdoptionIntegrationTest, which passes locally) is found and fixed at its cause; every E2E failure is accounted for by a spec this step rewrites
- [x] F1a The adoption integration test creates and deletes its own catalog tests (earlier suites in CI's class order leave the test table empty)
- [x] F1b The harness loads only what the analyzer stories use: the `analyzer-harness-*` files, with the generic rows the baseline profiles resolve (HIV-1 Viral Load, SARS-CoV-2 PCR, Xpert MTB/RIF, Rifampin Resistance) moved into them; the rest of the copied generic dictionary is deleted. Nothing else that runs on the harness (the foundational, storage and reporting fixtures, every harness spec) reads a row only the harness configuration provides or changes; checked by booting the harness and loading its fixtures
- [x] F1c The harness webapp's health grace period lets CI's own wait (15 minutes) govern: a slow start shows as a slow job, never as "unhealthy" at 4 minutes
- [x] F2 Delete the dead code this stack orphaned (`findHeldMappingResultsByProfile` in the DAO, its implementation, the service and its implementation) and the stale M2 entry in playwright.config.ts (the entry went with `5517b2c136`)
- [x] F3 Specs in their end state, each setting up its own analyzers through the setup screens or the shared API sequence and sending only manufacturer fixtures: the MVP journeys (a GeneXpert HIV-1 viral load and a respiratory panel with its components, each to an accepted clinical result; two GeneXperts on one listener; FluoroCycler file to clinical result; a catalog test deactivated after mapping, held, reactivated and recovered, on Influenza B), with the seven per-code scenarios and the unknown-value recovery retired; a new QuantStudio file story to an accepted clinical result; the setup-assays held-result story on fixtures (Influenza B turned off; RSV under a code the profile does not declare); the adoption spec on its own catalog test; the microbiology AST spec with its own source analyzer; the M1 lineage text on the baseline ID; the M3 guided-setup spec (it clicks "Review mappings in Analyzer Types", a link per-analyzer mappings removed). Then `sendGeneXpertAstm` and the setup picker's profile pin are deleted
- [ ] F4 E2E user story: an instrument code changed in the Assays step is the code results arrive under (Cepheid fixture replayed with that code) and they land on the right test. Outbound orders from OE2 are deferred
- [ ] F5 E2E user stories for placement, recordable: a result placed on its tube; a mistyped ID held and placed by the reviewer; a patient mismatch explained before saving; a rerun replacing a held result; a FILE plate with one mistyped sample name. Two tubes, an unordered test and an unknown ID stay proved by the placement integration tests
- [ ] F6 When the Bridge reports an analyzer's profile unavailable, the operator can reset it the way the upgrade migration does (keep identity, name, lab units, connection and history; clear the mapping; inactive) and set it up again on an available type; the message says so. Red first in OE2 (service and setup screen); then an E2E user story that authors a site profile, sets an analyzer up on it, removes that revision's file from the Bridge's data volume and restarts the Bridge, and the operator resets and sets the analyzer up again (derived 7 Oct: the only way to produce the state)
- [ ] F7 The upgrade path is proved: an OE2 integration test (an analyzer left by the migration, its connection pinned to a missing profile, is set up on the baseline profile, the Bridge connection re-pinned, and it activates); a Bridge test (a profile-unavailable connection updated to an available profile is restored and runs); then an upgrade rehearsal on the testing site from today's develop, recorded as the upgrade user story (the deployment itself needs the user's go at the time)
- [ ] F8 The operator-style seed is proved on a stack built from the final pins, and it uses the same API sequence as the specs' setup helper
- [ ] F9 Deployment images come from the submodules: `publish-images.yml` builds and publishes the Bridge and mock images at the submodule commits and writes them into the deployment bundle; the hand-typed tags in `docker-compose.analyzers.yml` and `test_analyzer_overlay.py` are deleted; the testing deployment uses the bundled images
- [ ] F10 Green: the full harness suite on the local stack, then every CI check on the top PR
- [ ] F11 Evidence: the `harness-demo-video` project records three workflows and nothing else. "Setup to clinical result": GeneXpert (HIV-1 viral load; respiratory panel), FluoroCycler, QuantStudio, and the changed instrument code. "Placement and recovery": the placement stories and the held-result recoveries (assay off, undeclared code, deactivated catalog test). "Lifecycle and degraded states": adoption of a newer revision, the stranded-analyzer reset, delivery issues, deactivate and reactivate. Packaged with the evidence-bundle skill (MP4, manifest with the app SHA and checksums, contact sheet checked), drafted as a comment on the top PR, media not committed
- [ ] F12 After the stack lands: step 9 (spec sync) as its own PR on develop
```

Not in this step: OE2 sending orders to analyzers; the 301-2002 parser tests
(assays outside the profile); HL7 result parts (with the first HL7 baseline
profile); distro profiles; opening the OE2 submodule bump automatically.

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
3. The evidence bundle for the three workflows is drafted on the top PR.
4. An upgraded analyzer and a stranded analyzer can both be set up again by an
   operator, proved at each level and recorded.
5. Nobody types an image tag or a commit hash for the Bridge or mock outside
   the submodule.
