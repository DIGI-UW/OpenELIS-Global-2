# R0 — Finish the merged #4332 analyzer test foundation

Updated: 29 September 2026. [OE2 #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332)
merged as `e0c98726ba`; its nine accepted review corrections belong to
[#4470](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4470). Broader
scope: [core analyzer roadmap](ogc-1054-analyzer-feature-roadmap.md).

## Implementation and validation

#4332 merged into `develop`; the obsolete GitHub stack grouping is removed. The
[merged PR checks](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332/checks)
passed; passing tests do not establish deployment
or full release qualification.

The
[review at `760536edb8`](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332#pullrequestreview-5345266867)
confirms that the earlier ordinary-startup catalog-overwrite blocker is
resolved: molecular test/result CSVs are harness-owned, and the repeated-load
test preserves the existing COVID report label. The review still identifies
test, runner, harness-configuration and documentation work. Its disposition and
acceptance checks are recorded below; completed code still needs final-head
validation and workflow evidence.

The full local command is `scripts/run-ci-checks.sh`: backend, frontend and E2E
lanes start from the same committed source, with E2E suites on isolated stacks.
Maven's dependency cache is reused. Call a revision locally green only when
every required lane finishes successfully on that revision; compare it with
GitHub's checks at the same head.

The earlier stopped local run diagnosed a proxy redirect that lost its random
host port; #4470 corrected it. GitHub then passed the actual backend, frontend,
both core Playwright shards, both analyzer-harness shards and all Cypress jobs
at #4470 head `99384069e4`. The branch was subsequently rebased onto newer
`develop`, so that green run supports the change but is not final-head proof.
The separate Cypress repair is outside this analyzer stack. Do not edit Cypress
tests here; report its status from required GitHub checks and keep analyzer
validation focused on the affected workflows. Mock 0.1.3 includes merged mock
#50's FILE permission fix, and the OE2 source and image pins match it.

Analyzer assertions use observable conditions within the existing whole-test
deadline. Do not add sleeps or raise deadlines to repair a failure. Result
stories use unique sender identities and watched directories; the disposable
stack owns their teardown. The dedicated guided-setup story exercises
deactivation. Review corrections must preserve those isolation and evidence
boundaries.

#4470 now supplies the missing Compose volume declaration, checks the exact
count value, and makes certificate generation a completed one-shot dependency.
The subsequent proxy restart showed another startup-contract gap: nginx names
both frontend and OE2 upstreams, but Compose did not wait for them. The proxy
now starts after frontend starts and OE2 is healthy. A fresh local seed run
reached login and created the default analyzer connections with this ordering.
The ordinary Catalog Import preview/apply endpoint was verified on a fresh,
source-built #4470 stack: it added a CSV, replaced that same file, and retained
the replacement bytes and updated catalog state across a webapp restart. The
exact-source analyzer run passed 10/10 registered scenarios with separate
GeneXpert ASTM and FluoroCycler watched-FILE recordings and clinical readback.
Human review is separate from these technical checks. Read current-head check
state from each PR's checks, not from this document.

The [GitHub stack #4472](https://github.com/DIGI-UW/OpenELIS-Global-2/stack/4472)
records the merge sequence **#4470 → #4448 → #4449**. #4470 targets `develop`;
#4448 contains #4470 and targets its branch; #4449 contains #4448 and targets
its branch. Each PR must retain only its own stack-relative changes and be
rechecked when `develop` advances.
#4332 established the stable harness catalog and faithful test foundation
without installing harness clinical CSVs on ordinary OE2 sites. Obsolete demo
test copies were removed while syncing the follow-ups. #4448 passed 11/11
source-built registered scenarios, including one usable GeneXpert observation
beside a held one, mapping correction, preserved review edits and two
independent clinical readbacks. #4449 passed 12/12 registered scenarios: when
one confirmed mapping becomes invalid, the valid sibling reaches clinical
review and the original held row recovers without duplication after correction.
The recordings show the functional heads; subsequent changes to the persistence
test fixture and stack ancestry did not alter those runtime paths. Every head
that is pushed still requires its own current-head CI. Broader FILE/HL7 qualification,
outage/replay, populated upgrades and general catalog additions are separate
follow-ups.

#4448 additionally rejects adopting an unconfirmed or stale analyzer binding
revision in its service layer, matching the UI's Apply guard. Its focused
service tests passed, and the persistence test fixture was corrected to supply
a real catalog profile and current confirmation; the full persistence class
passes 7/7 locally. #4449 is ready for review.

## Landing the stack

**Goal.** Stack #4472 (#4470 → #4448 → #4449) is mergeable and ready for the
maintainer's review: current on `develop`, required checks green on every
current head, no open review threads, and each PR's claimed workflow shown in a
paced, presented recording from its final head. Merging is the maintainer's
action, not a step of this plan.

Acceptance, checked by the agent:

1. Each branch is current on `develop` and carries only its own patches
   (`git range-diff` against the previous head).
2. Required checks (Backend, Frontend and E2E checkpoints) pass on each
   current head; `gh stack view` shows no rebase warning.
3. No unresolved review thread on any PR, including a Codex review of the
   final heads.
4. L1 is resolved: the two-visit test fails before the fix and passes after,
   or passes on the current code and L1 is dropped as not reproduced.
5. One `pw:test:harness-demo-video` run from the top of the stack records
   every workflow the three PRs claim (GeneXpert ASTM, FluoroCycler FILE,
   held-result recovery, invalid binding) at the documented pacing, each video
   45–90 seconds with opening, story and outcome cards.
6. `tools/code-qa/skills/evidence-bundle` has packaged those videos as MP4
   with a manifest naming the tested commits, and one PR-comment draft per PR
   is ready. No media is committed.
7. The R0-REV3 interrupt result is recorded in #4470's thread.

Left to the maintainer: posting the evidence comments, requesting or waiving
approvals, and the merge itself. An atomic `gh stack merge 4472 --squash`
needs one approval per PR because stack merge does not bypass review; merging
each PR separately instead needs one CI cycle per PR, since `develop` is
`strict`.

**Restack onto #4471/#4478 (29 September).** #4471 landed the same `certs`
restart policy, `certs`/proxy dependencies and nginx `$http_host` change as
#4470. A three-way merge of each branch was clean; the commit-by-commit rebase
stopped once, in `projects/analyzer-harness/docker-compose.base.yml`, and was
resolved to the union: develop's `certs`, `fhir`, Bridge and proxy dependency
blocks plus #4470's `harness-catalog-init` service and its dependency on
`oe.openelis.org`. The duplicated nginx commit dropped as already upstream.
Each rebased branch's tree equals a merge of its previous head with `develop`,
so the restack changed ancestry only. When `develop` moves again, update each
branch in its own worktree, bottom to top, and verify the same tree equality.

**Why the earlier recordings were not usable evidence.** Since #4430
(28 September) the parity runner exports `PLAYWRIGHT_SLOWMO=0` unless one is
passed, which overrides the video projects' 500 ms default, so harness videos
recorded at full speed. The analyzer video tests also called none of the
video-gated presentation helpers, so the video toggle had no cards to show.
The recordings were 8–10 second test captures, not walkthroughs.

| Step | Work                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Gate                                                                                                                                                                                                                                                                    | Status                                                                                              |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| L1   | `AnalyserResults` cleared its draft edits whenever results reloaded, so a choice made before one mapping visit was lost after a second visit.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | `worklistDraftRoundTrip.test.jsx` walks the real worklist through two mapping visits: it failed on the second visit before the fix and passes after; the `analyserResults` and mapping-editor suites (40 tests) pass. `history` is declared in `frontend/package.json`. | Done 29 September (#4448)                                                                           |
| L2   | Restack onto current `develop`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Tree equality as above; no conflict markers; formatting clean after the last edit.                                                                                                                                                                                      | Done 29 September (`91db9a80aa`); L1/L3 cascaded the same way                                       |
| L3   | Evidence prerequisites, one commit set per PR: the runner defaults `PLAYWRIGHT_SLOWMO` to empty so the Playwright config's 500 ms is the only default (#4470); presentation cards on the claimed workflows, each PR carrying its own (#4470 GeneXpert and FluoroCycler, #4448 held-result recovery, #4449 invalid binding), labelled by ticket and milestone (`OGC-1054 R0`/`R1`/`R2`); the Playwright README names `evidence-bundle` (#4470). The M3 guided-setup demo gets no cards: none of the three PRs claims it, and the harness demo guard rejects the title-card waits in `demo/harness` specs, which needs its own decision.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Cards appear only in video projects; `npm run pw:guard` and formatting pass; `git range-diff` shows each branch's earlier commits unchanged.                                                                                                                            | Done 29 September                                                                                   |
| L4   | Push all three with `gh stack push`; one CI cycle.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Acceptance 1–2.                                                                                                                                                                                                                                                         | Pushed 29 September; current-head checks on each PR are the record                                  |
| L5   | While L4 runs: record once from the top of the stack with `npm run pw:test:harness-demo-video`, check durations and representative frames, package with `evidence-bundle`, draft one PR comment per PR.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | Acceptance 5–6.                                                                                                                                                                                                                                                         | Bundle in `~/.codex/evidence/analyzer-stack-4472-20260929/`; its manifest names the recorded commit |
| L6   | R0-REV3 interrupt proof: interrupt a disposable `scripts/run-ci-checks.sh` run after L5 finishes.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | No owned process or Compose project remains, unrelated stacks keep running, the run reports interrupted; result in #4470's thread.                                                                                                                                      | Result in #4470's thread                                                                            |
| L7   | Codex review of the final heads; resolve findings in one batch (a fix means re-cascade, push and another CI cycle). First pass on 29 September found two P2 defects, both fixed with tests or a dry run: #4448 rolled back a whole accept when a sibling had an inactive test (skipped rows now stay staged), and #4470's `--build` could not provision the image-only catalog initializer (build mode now pulls image-only services). The second pass found two P1 defects in #4448's release of awaiting-specimen results, fixed with red-then-green integration tests: a released row trusted the submitted order, and the chosen specimen was ignored on an order with an entered sample or dropped when a grouping's results share one new specimen. The third pass found four more, all in lines this stack changed and all fixed: the staged component was not restored, the one-specimen check ignored orders with an entered sample even though every new analysis of a grouping shares one sample item, a grouping of skipped rows still created an empty order (#4448), and the local runner dropped a failed step's blob report (#4470). The fourth pass found two more on #4448, both fixed with red-then-green tests: a restored tick could sit on a hidden row after its held sibling was recovered, and the completion time and other review-page fields were still taken from the submitted row. | Acceptance 3.                                                                                                                                                                                                                                                           | Re-review of the final heads requested                                                              |
| L8   | Maintainer: post evidence, approvals or bypass, merge. Afterwards `gh stack sync --prune` and mark rows 1, 3 and 4 below done.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Merged.                                                                                                                                                                                                                                                                 | Maintainer                                                                                          |

**Intermittent develop failure seen on #4448's first rebased head.**
`ogc-1192-environmental-orders` ("Modify Order sends an environmental order to
its own Enter Order page", from #4459) failed once: the trace shows
`window.location.replace()` requested `/order/environmental/enter` with a 200,
but the navigation never committed and the page kept rendering the clinical
wizard. The same test passed on #4470 and #4449 at the same `develop` code.
If it recurs on a stack head, reproduce it locally before rerunning.

Rejected alternatives: detaching worktrees or creating a separate one to run
`gh stack rebase` (each branch is already checked out in its own worktree and
is updated there); stashing or committing another agent's uncommitted edits
without review (they ride through rebases with `--autostash`); hand-built
evidence pages and ad hoc MP4 conversion (the repo already has
`evidence-bundle` and a documented evidence format); a hard-coded 500 ms
runner default (it would duplicate the config's default).

## Review disposition and pre-merge acceptance

Source:
[review at `760536edb8`](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332#pullrequestreview-5345266867),
checked against that source revision on 28 September. Owner for every R0 item
below: OE2's bounded #4332 review follow-up, #4470. Status: corrections implemented in #4470; R0-REV3 still needs its interrupt-cleanup proof (step L6 above). This is the
bounded correction scope before the final CI/evidence checkpoint; the deferred
table is not an additional merge gate.

| ID      | Valid finding and decision                                                                                                                                                                     | Smallest remediation                                                                                                                                                                                                                                                              | Acceptance                                                                                                                                                                                                                                                           |
| ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| R0-REV1 | Search/count evidence can pass before filtering commits; `toContainText("1")` also matches `21`.                                                                                               | Wait for the committed search URL inside `AnalyzerListPage.search`; use exact count assertions in guided setup.                                                                                                                                                                   | The registered guided-setup story waits for its actual filter and proves exactly one matching analyzer and setup record. No sleep or new step timeout.                                                                                                               |
| R0-REV2 | Full local runner's language pathspec is relative to `frontend/` and matches no locale files.                                                                                                  | Use a repository-root pathspec and keep the existing translation-source rule.                                                                                                                                                                                                     | A controlled non-English change is rejected; an English-only change is allowed. Missing or failed validation cannot produce overall PASS.                                                                                                                            |
| R0-REV3 | Cancelling the runner signals lane shells but can leave Maven/Docker descendants using checkouts that cleanup removes.                                                                         | Isolate each lane's process group; stop and wait for its descendants and scoped Compose cleanup before removing its checkout. Keep this inside the existing runner.                                                                                                               | Interrupt a disposable validation run; no owned child processes or Compose projects remain, unrelated stacks remain running, and the run reports interrupted/non-success.                                                                                            |
| R0-REV4 | Deployment instructions still imply molecular clinical defaults ship in every image; rebase instructions call a deleted script/unsupported option; PR validation text calls an old head final. | Correct catalog packaging and retention guidance, point full validation to the one runner, and link live checks instead of maintaining a moving final-head claim.                                                                                                                 | No instruction removes a still-needed site catalog or calls the deleted runner/removed flag. Deployment smoke proves receipt only unless clinical acceptance/readback is explicitly performed. PR body and roadmap state the same current scope and evidence limits. |
| R0-REV5 | The shared-listener result poll parses JSON without checking the response and can throw outside retry handling.                                                                                | Use the established response check and retryable assertion pattern; retain useful diagnostics.                                                                                                                                                                                    | Transient unsuccessful/non-JSON responses do not end the poll immediately; valid results still require independent analyzer/order attribution and the exact expected count.                                                                                          |
| R0-REV6 | Delivery-issue mock cleanup in the test body's `finally` can be interrupted when that body exhausts its deadline.                                                                              | Give the temporary source an owned fixture lifecycle and reliable teardown context.                                                                                                                                                                                               | A failing/timed-out body still removes its temporary source before retry; a passing run does too. Keep the existing whole-test deadline; do not introduce a shorter arbitrary poll deadline.                                                                         |
| R0-REV7 | Whole-directory read-only catalog mounts prevent supported Catalog Import uploads in the harness.                                                                                              | Initialize the harness's canonical CSVs into its existing writable configuration volume, then use the ordinary OE2 loader. Preserve intentional edits/uploads on a retained development volume. Individual read-only file mounts alone do not support replacement of those files. | Fresh local/CI stacks load the same catalog; normal Catalog Import can add and replace a file; restart retains the supported upload. Fresh CI resets remain isolated. No SQL setup, mapping repair or global loader-precedence change.                               |
| R0-REV8 | Catalog integration proof reads GeneXpert v5 and manually adds hints already shipped in v7.                                                                                                    | Read the real profile revision from the pinned Bridge catalog and remove the test-only hint patch.                                                                                                                                                                                | Existing catalog/default assertions pass against the actual shipped profile data without manufacturing defaults in the test.                                                                                                                                         |
| R0-REV9 | The order helper guesses date layout for an optional next-visit date unrelated to analyzer acceptance.                                                                                         | Confirm omission through the production order API, then remove the unnecessary date and guessing helper.                                                                                                                                                                          | The existing native result workflow creates and accepts the intended order without `nextVisitDate`; patient/order/specimen/value assertions stay intact.                                                                                                             |

Execution: first repair the test/runner/doc items, then validate writable
catalog initialization. Reuse existing tests; add only the focused
failure/cleanup checks needed to prove the changed behavior. Push each coherent
revision and run the authorized local lanes alongside GitHub for that same
revision. A run for an earlier revision is supporting evidence only. Do not add
competing CI entry points or repair Cypress in this stack. Final acceptance
requires analyzer-controlled local checks and actual downstream GitHub checks
to complete, plus accessible reviewed video from the same registered workflow
tests when their behavior changes.

## Explicitly deferred review items

These items remain open after #4332; recording them does not claim that a
follow-up PR or issue has been created. Ownership below names the roadmap
workstream responsible for creating that bounded follow-up. None justifies
changing clinical semantics without evidence.

| ID     | Finding / disposition                                                                                                                                                 | Owner and reason for deferral                                                                                                                                                        | Follow-up acceptance                                                                                                                                                                                                    |
| ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| F-REV1 | Harness filesystem catalogs suppress bundled Horiba CBC/vector CSVs for those domains. Confirmed loader behavior; intended coverage still needs an explicit decision. | R5/R7 core catalog and profile qualification. Current #4332 corpus is bounded molecular coverage. Do not silently merge classpath and site catalogs or expand this PR to all assays. | State the supported corpus; qualify additional profiles against its normal catalog load and native clinical readback. Missing coverage stays visible. Preserve the documented site-override contract.                   |
| F-REV2 | Transactional TRUNCATE can retain locks that block a second connection or `REQUIRES_NEW` work. Credible risk; no current failing case demonstrated by this review.    | R0 backend test-infrastructure follow-up. Diagnose transaction ownership separately from analyzer browser setup. A lock timeout only improves failure diagnostics.                   | Reproduce the cross-connection case; fix fixture isolation/transaction ownership and verify it finishes or fails diagnostically without hanging a job. Do not solve it by lengthening timeouts.                         |
| F-REV3 | A single local-code candidate bypasses specimen disambiguation. Behavior confirmed; a defect is not yet established.                                                  | R4 populated-catalog reconciliation. A unique code can legitimately identify a test whose specimen support is being extended.                                                        | Define identity/update semantics with existing catalog-loader expectations; cover valid extension and conflicting identity. Add a restriction only if that contract requires it.                                        |
| F-REV4 | Repeated `SpringContext.getBean` access and reference-table lookup add indirection/query work.                                                                        | OE2 service-maintenance follow-up under R6. No functional failure or measured performance blocker shown.                                                                             | Prefer injected/context-scoped dependencies where appropriate; verify lifecycle/context correctness and query reduction. Do not restore stale static caches to satisfy a style preference.                              |
| F-REV5 | Historical 2.3.x seed assigns the COVID LOINC to HIV viral-load variants. Source defect predates this PR; deployed effective values require separate audit.           | R4 general catalog correction. Harness-only CSVs are not a production migration policy; keep broad catalog management out of #4332.                                                  | Verify affected records and intended concept/specimen/unit identities; apply a narrowly scoped supported correction preserving IDs, history, report labels and site activation intent. Prove fresh and populated cases. |
| F-REV6 | Duplicate raw values render one hint editor because hints are keyed by raw value. Authoring validation improvement; no loss of distinct mappings established.         | R3 profile-authoring follow-up. A second identical key cannot have an independent hint under the existing contract.                                                                  | Explain/reject duplicate value entries visibly while preserving valid values and hints; do not create a second hint contract.                                                                                           |

Already resolved: ordinary installs no longer receive the harness molecular test
CSVs; the COVID report label is asserted across two loads; the stale-search
product race, released mock FILE permissions and `nc_event` fixture sequence
mapping are fixed. The old sequence review thread can be resolved during PR
housekeeping. These resolved items do not certify the remaining acceptance
above.

## What this PR delivers

| Area                         | Built in #4332                                                                                                                                                                                                                                                                               | Evidence and limit                                                                                                                                                                                                                                                                                                                   |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Faithful workflow setup      | Removed analyzer SQL order fixtures and hidden mapping selection/exclusion/confirmation. The harness loads its stable molecular CSVs through OE2's normal configuration loader; synthetic clinical prerequisites use production APIs, while analyzer setup and result acceptance use the UI. | Retained native MTB/RIF workflows independently assert patient, order, specimen, test and saved value. The FILE numeric workflow also passes. These OE2 workflows do not establish HL7, populated upgrade or outage behavior.                                                                                                        |
| Harness clinical catalog     | Kept the molecular test and result-choice CSVs inside the harness and mounted them for local and CI runs; corrected COVID fresh-load identity and added generic specimen and answer-hint consumption/editor support.                                                                         | Catalog tests cover fresh/repeated loading and preservation of existing IDs. The harness CSV is test configuration, not a globally packaged catalog update. HIV/COVID hints are supplied by companion Bridge #69; the same dependency combination must pass the native workflow tests. Existing-site duplicate cleanup remains open. |
| Result visibility            | Corrected mixed numeric/categorical result display so staged COVID observations do not disappear because a numeric definition was interpreted as a dictionary choice.                                                                                                                        | Existing pending rows reappeared locally without resend. This is display evidence, not completion of all recovery paths.                                                                                                                                                                                                             |
| Persistent harness storage   | Persisted Bridge connections/profiles, delivery queue and FILE state in a named volume through shared local/CI configuration.                                                                                                                                                                | Container replacement retained 26 connections and 15 delivered records. The FILE store was empty; queued retry and processed-file deduplication were not proved. Mock network reattachment required a manual step.                                                                                                                   |
| Real-service test foundation | Real parsing/history in integration tests, transaction-aware fixture loading, owned audit records, catalog isolation and context-correct service access.                                                                                                                                     | Focused suites passed. Earlier full local backend validation passed; the current full run remains incomplete at this checkpoint. Final-revision local and GitHub checks remain required. No failing assertion or required check is waived.                                                                                           |

## One dependency and evidence boundary

| Use                       | OE2 source                                   | Bridge source / profiles                            | Mock source          | What it proves                                                                                                                                                                                                                                       |
| ------------------------- | -------------------------------------------- | --------------------------------------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Current #4332 integration | #4332 merged; corrections in #4470           | Bridge 3.2.4 `1eff7b4`; GeneXpert 7, FluoroCycler 4 | Mock 0.1.3 `8c64750` | Released pins align on `develop` and throughout stack #4472. Current-head checks on each PR are the merge signal.                                                                                                                                    |
| Current result evidence   | Application `dbc266a484`; tests `311af6006e` | `da675c9`; GeneXpert 7 and FluoroCycler 4           | `2ad1082`            | Nine passing native result recordings: MTB negative, three RIF outcomes, HIV concentration, COVID positive/negative, shared-listener isolation and watched-FILE delivery. Independent clinical readback passes. Five stock-default checks also pass. |
| Current setup evidence    | Application `dbc266a484`; tests `311af6006e` | `da675c9`; full GeneXpert 7 duplicated through UI   | `2ad1082`            | Passing first-time confirmation, activation, connection editing, QC navigation and deactivation. Setup plus the nine result workflows provide ten recordings on one application/dependency combination.                                              |

Released Bridge 3.2.4 differs from the recorded `da675c9` only in
acceptance-test readiness and release version metadata. Mock 0.1.2 matched the
earlier recorded `2ad1082`; released mock 0.1.3 adds the readable FILE
publication fix. The diagnostic showed host health UP six seconds before Bridge
forwarding health UP. Waiting for forwarding recovery preserved the assertion
that one operator retry delivers once. Operator retry currently grants one
delivery attempt after automatic retries are exhausted; resilience of that
behavior to another transient failure remains an explicit recovery follow-up.

These are source identities. Attach the actual image IDs/digests and test-run
identity to final deployment evidence rather than inferring running images from
Git pins. Bridge changes are staged deliberately with their consuming workflows;
verify the committed gitlink before deployment.

## Ordered remaining work and acceptance

The R labels retain their existing capability ownership; the numbered rows below
give the execution order. PR numbers identify existing owners rather than new
parallel implementations.

| Order | Owner / current state                                  | Next work                                                                                                                                                                                                                                                                                                             | Acceptance                                                                                                                                                                                                                                                                                                                                 |
| ----- | ------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1     | R0 — #4332 merged; #4470 stack bottom                  | Catalog Import add/replace/restart and 10/10 recorded analyzer workflows are proved on the preceding head. Pass current-head checks and the L6 interrupt proof; keep F-REV1 through F-REV6 with named owners. Cypress repair is owned elsewhere.                                                                      | Analyzer-controlled local checks and actual downstream GitHub E2E pass on the submitted revision. Setup and clinical readback prove the claimed workflows; a conditional confirmation branch is not first-time setup proof. Record numeric FILE coverage limits.                                                                           |
| 2     | R5 — Bridge #69 / mock #49; merged and released        | GeneXpert/FluoroCycler numeric compatibility passes across OE2 setup, Bridge runtime and native mock traffic. Bridge 3.2.4 and mock 0.1.3 are released with aligned OE2 source and image pins. Retain the exact recorded source identities; a source-built workflow is not a deployment test of the published images. | Correct profile defaults, specimen/answer hints and shared-listener attribution. Preserve published revisions. No production instrument-specific branches or invented assay semantics.                                                                                                                                                     |
| 3     | R1/R3 — OE2 #4448; stacked on #4470                    | The native GeneXpert mixed-result workflow passed 11/11; its paced recording is part of L5. The confirmed-binding persistence test now passes 7/7 locally. Resolve L1 (two-visit test first) and pass current-head checks while preserving API-based prerequisites and existing demo-test scope.                      | Accept a usable observation while its held sibling remains intact; select a valid missing specimen; correct mappings in the UI and recover the original row without resend or duplication. Preserve unsaved worklist edits and the original QC lot when recovering held controls. Passing current-head CI and a recorded focused workflow. |
| 4     | R2 — OE2 #4449; ready, stacked on #4448                | Its per-observation catalog-validity workflow passed 12/12 with recorded clinical readback. Reuse existing confirmation, catalog validation and recovery services; pass current-head checks before the stack merge.                                                                                                   | In a previously confirmed configuration, invalidate one binding: unaffected observations still reach review, only the affected observation is held. Correct it, recover that original observation, and replay without duplicates. Passing current-head CI and a recorded focused workflow; then merge.                                     |
| 5     | R5 — broader core FILE/HL7 qualification; open         | The reusable FILE mock and numeric Plasma workflow are complete. Qualify additional supported FluoroCycler exports/assays, units and status/control semantics, plus a shipped core HL7 profile. This does not delay the three-PR merge sequence.                                                                      | UI directory configuration reaches Bridge watching; native files/HL7 save correct clinical values. Archive/error retention is verified. No distro mount or fabricated concentration makes the test pass.                                                                                                                                   |
| 6     | R4 — OE2 #4347/#4421 plus upgrade/catalog owners; open | Prove OE2 queue outage/restart/replay, review operator retry after another transient failure, restore mock attachment after Bridge replacement, reconcile populated catalogs and upgrade a supported previous version.                                                                                                | Retained pending messages deliver once after recovery; processed FILE inputs are not duplicated. Existing analyzer IDs, clinical history and deliberate mappings survive upgrade. No SQL feature setup or resend is needed for claimed recovery.                                                                                           |
| 7     | R6/R7 — core qualification; open                       | Reconcile remaining PR deltas against merged code, close superseded work with lineage, run supported full workflows and review/present recordings from the same tests.                                                                                                                                                | Every supported ASTM/FILE/HL7 workflow and required recovery/upgrade scenario has passing independent readback, exact image identities and accessible reviewed video.                                                                                                                                                                      |
| 8     | R8 — Madagascar distro; later                          | Publish qualified core versions, update distro pins and run limited packaging/configuration checks.                                                                                                                                                                                                                   | The distro consumes working core profiles/defaults without site-specific mapping repair. Its checks prove packaging differences, not replacement core acceptance.                                                                                                                                                                          |

## Remaining PR cleanup ownership

OE2, mock and Bridge PR states were rechecked on 29 September; the
Madagascar-harness and review-tooling rows were not. The dispositions below are proposed
work, not claims that these PRs have already been cleaned up or closed. They
follow the ordered work above; none expands the bounded current #4332 merge
gate.

| PRs                                    | Remaining action                                                                                                                                                                                                          |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| OE2 #4336                              | Compare its remaining lifecycle, authorization and query tests with #4332/current develop; retain useful cases in the recovery follow-up, then close or narrow. Do not merge the old implementation wholesale over #4433. |
| OE2 #4347 / #4421                      | Reuse existing receipts and services for outage/replay proof and durable operator retry/dismiss audit.                                                                                                                    |
| OE2 #4429                              | Open; its last Backend run failed. Remove the blanket recognition gate; retain useful nonblocking guidance, or close if no useful delta remains.                                                                          |
| OE2 #4072                              | #4430 and #4382 (CI-parity isolation) merged. Reconcile the remaining worktree development stack with the supported dev-stack path; keep CI and interactive entry points separate without competing launchers.            |
| OE2 #4197 / #3974                      | Compare older workflow work and optional type conversion with current implementation; close superseded work or retain a justified small residual.                                                                         |
| Bridge #69 / mock #49 and #50          | Merged and released as Bridge 3.2.4 / mock 0.1.3. OE2 sources and image tags align. Broader assay and HL7 qualification remains owned by R5.                                                                              |
| Mock #39 / #41                         | Compare with merged #47 and close if no useful delta remains.                                                                                                                                                             |
| Madagascar harness #4 / #10 / #9 / #11 | After core qualification: reconcile #4 with merged #15; narrow #10; review outbound proof in #9 and its child #11.                                                                                                        |
| Review tooling #16 / #31               | Compare #16 with merged #17; update #31 evidence manifests to the tested pins.                                                                                                                                            |
| OE2 #3793 / #4447                      | #3793 remains separate operational-QC planning. #4447 is merged order-entry work, not an analyzer release blocker.                                                                                                        |

#4448 and #4449 remain the explicit recovery and per-observation isolation
owners in steps 3 and 4. Populated upgrade/catalog acceptance uses the existing
#4433 migration and normal catalog-loading services; its reconciliation work
still needs a bounded follow-up, not a second migration implementation.

## #4256 closure: useful work and remaining proof

The original manual per-result reprocess endpoint and button were superseded by
#4433's adoption-triggered recovery; they should not be restored. Receipt
protection landed in #4241 and migration 104. The alternative history-key
migration 109 preserves numeric and UUID audit references. Test-foundation work
continues in the #4332 review follow-up.

#4448 owns useful outstanding recovery behavior, including updated reasons for
still-held rows. #4449 owns isolating invalid mappings to affected observations.
Preserve two acceptance checks from the old work: recovery must retain the
original QC lot across transactions and must not discard unsaved worklist edits.
Closing #4256 did not certify complete recovery acceptance.

## Rules for faithful tests and evidence

- Start the ordinary core application/catalog and Bridge-shipped profiles
  through `scripts/dev-stack`; use CI's supported runner for CI parity.
- Prepare synthetic patients, orders and specimens through existing validated
  APIs. Assert expected catalog identity and specimen independently. Do not
  select, exclude or repair analyzer mappings in fixtures.
- Exercise the UI for any setup, confirmation, correction, activation or
  acceptance that the story claims. Supplemental API readiness/readback is
  allowed; it must not perform the behavior being demonstrated.
- Keep Bridge responsible for protocols, parsing, listeners, directory watching
  and delivery. Keep OE2 responsible for clinical catalog bindings,
  orchestration, review, history and QC.
- Use the same registered scenarios and assertions for CI and videos. The
  evidence page presents the current workflow set with exact commit identities
  and limits; Git retains implementation history. Generating a new file is not
  evidence review.
- Track code, CI, merge, deployed build, publication and human acceptance
  separately. Do not describe a PR as merge-ready while required checks are
  failing or unknown.
