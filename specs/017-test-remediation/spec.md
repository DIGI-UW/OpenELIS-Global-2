# Draft specification: OE2 test remediation

**Created:** 2026-09-17

**Status:** Draft; reusable principles agreed, broader rollout deferred.

**Current implementation:** analyzer remediation only.

## Purpose and source of truth

Make test results trustworthy by matching each test's setup and assertions to
what it is intended to prove. Start with the existing analyzer work and extend
the proven approach to other OE2 modules in bounded future changes.

The [analyzer catalogue and execution plan](../roadmaps/ogc-1054-analyzer-feature-roadmap.md#analyzer-test-remediation-execution-plan)
owns its inventory, findings, T1/T2/T3 progress and acceptance. This specification
does not copy that task list or create a second active programme.
The [testing roadmap](../../.specify/guides/testing-roadmap.md) and
[constitution](../../.specify/memory/constitution.md) remain authoritative.
Contradictory guide/template instructions must be corrected explicitly in the
owning implementation change; this draft does not silently replace them.

## Evidence motivating the proposal

The analyzer catalogue records real internal behavior replaced by shared mocks,
fixture damage hidden by dependency repairs, inconsistent fixture connections,
retained static dependencies, weak query assertions and incomplete test
selection. Current source also shows the common base has 437 direct subclasses,
317 fixture-loader callers and 33 cleanup-helper callers. Those counts establish
potential reach, not 437 defective tests or a completed OE2-wide audit.

The guide currently steers ordinary controller tests into the broad database
base, mandates destructive DBUnit setup, and describes only named tables as
affected even though the helper uses cascading truncation. Correcting code
without aligning that guidance would perpetuate the same problems.

## Test levels

| Level       | Required proof                                           | Appropriate boundaries                                                                                                                                                                                                                            |
| ----------- | -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Unit        | One rule or transformation fails when its logic is wrong | Isolated real logic; collaborators may be mocked; no shared mutable application/database state.                                                                                                                                                   |
| Component   | One component behaves correctly through its interface    | Real UI/router, HTTP controller, parser or client; dependencies at its boundary may be substituted. No claim of persisted backend effects.                                                                                                        |
| Integration | Connected real components honor their contracts          | Actual internal service/query/history/transaction chain under test; controlled external systems. Database state and transaction closure are checked where relevant. Framework model-building remains a fast integration check without a database. |
| End-to-end  | The assembled product completes a user workflow          | Real browser/application/services/database and required integrations; an external instrument simulator is acceptable. No fake success for the mutation being proved.                                                                              |

Use subject tags for authorization, contracts, history, migrations, concurrency
and performance. Preserve the dedicated ORM validation execution required by the
constitution. Human acceptance is separate from automated test levels.

## Required ownership and proof

1. Every audited file has a level, intended proof, real/substituted dependencies,
   data/state ownership, known problem, and correction/acceptance decision.
   Unknowns are explicit; filenames and a green result are not classification.
2. Ordinary database checks own records, normally roll back, and flush/reload
   before claiming saved effects. They preserve migrated seeds and unrelated rows.
3. Commit, concurrency, after-commit and transaction-close tests retain actual
   commits with uniquely owned prerequisites and cleanup. Adding an outer
   rollback transaction must not remove the behavior the test needs to prove.
4. Destructive schema/migration scenarios own an isolated schema or database.
   Sharing a container is acceptable when data and connection ownership are clear.
5. All fixture operations obey the declared connection/transaction contract.
   No silent independent connection, global seed repair, arbitrary sequence reset,
   or dependency swap conceals damage caused by another test.
6. Reused contexts must not retain another context's services or stale fixture
   state. Audit the dependencies actually reached; do not launch an indiscriminate
   rewrite of every singleton or shared mock.
7. Query tests assert exact relevant rows, exclusions and ordering; mutation tests
   inspect actual effects. Verify that a representative wrong implementation
   makes the owning test fail. Counts and coverage percentages alone are not proof.
8. Test selection is reconciled with the inventory and actual runner discovery.
   Relevant class orders, affected callers and final-commit CI accompany shared
   changes. Hangs get bounded diagnostics and preserved thread/lock evidence;
   retries and longer timeouts do not constitute a correction.

## Proposed adoption and acceptance

**First: complete the analyzer pilot.** Its existing roadmap owns implementation.
Prove the fixture contract in each ownership mode; remove the demonstrated
workarounds; reconcile the full analyzer selection; complete the real mapping
and recovery workflow. Keep foundation changes independently reviewable.

**Then: consolidate the established approach.** Align the specific guide/template
sections with working examples of unit, component, rollback integration,
committed integration, migration isolation and end-to-end proof. Reuse current
JUnit 4, traditional Spring, PostgreSQL, Vitest and Playwright tooling; no new
test framework, blanket renaming or repository-wide file move is required.

**Later: select another bounded module from evidence.** Prioritize failed
isolation, false-positive assertions, shared dependency repairs or costly broad
contexts. Catalogue that module before changing it; give each coherent milestone
its own reviewed change and explicit before/after validation. Do not turn the
current analyzer goal into an OE2-wide rewrite.

For each adopted module, completion requires an enumerated surface, meaningful
assertions at the appropriate levels, no outstanding ownership defect within the
agreed boundary, preserved unrelated data/state, reproducible targeted and
regression execution, and passing applicable CI on the final commits. Remaining
external/human acceptance and deliberately deferred work stay explicit.

## Non-goals of this draft

No OE2-wide implementation is authorized by this document alone. No replacement
of legitimate unit/component mocks, no weakening of assertions to preserve green
runs, no automatic deletion based on duplicate-looking names, and no change to
analyzer ownership, merge authority or deployment scope. Specific code changes
continue to follow the active analyzer goal and its acceptance criteria.
