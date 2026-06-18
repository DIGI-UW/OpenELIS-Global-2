---
name: meaningful-test-coverage
description: >
  Author thorough, no-theater tests across every layer a change touches — via a
  multi-agent workflow that designs the RIGHT-level test per layer, adversarially
  verifies each for test theater / over-mocking, then implements and runs the
  survivors. Use after a substantive multi-layer change (DB/catalog + DAO/HQL +
  service + frontend) when you want coverage that actually catches the bug, not
  green-theater. Enforces the inversion test, level-correctness (data/HQL →
  integration, pure logic → unit, rendering → component, story → E2E), and the
  Constitution Principle V.6 test-quality invariants. MOCKUP — refine before use.
---

# Meaningful Test Coverage (no theater)

Use this skill when:

- a change spans multiple layers (DB/catalog, DAO/HQL, service, controller, frontend, E2E)
- you need confidence the tests genuinely guard the fix (i.e. they FAIL on the old/buggy code)
- you want to avoid over-mocking and assert-on-mock-return theater

## The four litmus tests (apply to every proposed test)

1. **Right level for what can actually fail.** Put the guard where the bug lives.
   A bug in HQL/SQL or a column mapping can only be caught against a real DB — a
   mocked service test asserting the same thing is theater. Pure arithmetic →
   unit; rendering/degraded-state → component; full story → E2E.
2. **The inversion test (Constitution V.6).** A guard is only meaningful if it
   would FAIL against the old/buggy behavior. State, per test, "would this fail
   if I reverted the fix?" — and verify it (revert locally, watch it go red), at
   least for the load-bearing guard.
3. **No theater.** No assert-on-mock-return (asserting a stub returned what you
   stubbed). No over-mocking the unit under test. No `verify(...)`-only tests that
   still pass if the behavior is dropped. Assert on real produced values
   (ArgumentCaptor on the real entity, real rendered DOM, real query results).
4. **Honest disclaimers.** A level that *cannot* cover something must say so, not
   pretend. A mocked-DAO service test does NOT guard an HQL inversion — write that
   in the test's javadoc so nobody trusts it for the wrong thing.

## OpenELIS test levels & infrastructure

| Level | When | Infra | Run |
|---|---|---|---|
| **Integration** | HQL/SQL, column mappings, Liquibase, anything that can only fail against a real DB | extend `BaseWebContextSensitiveTest` (Testcontainers Postgres + full Liquibase); seed via dbunit fixture + `jdbcTemplate` for shapes FlatXml can't express | `mvn -Dtest=X -Dsurefire.failIfNoSpecifiedTests=false test` (JDK 21 + Docker) |
| **Service unit** | arithmetic, branching, mapping, passthrough | Mockito (mock the DAO); assert the real math on known aggregate inputs | `mvn -Dtest=X test` |
| **Frontend component** | rendering, degraded/empty states, no-fake-zeros | vitest + `@testing-library/react` (NOT jest); real `en.json`; assert rendered output for BOTH happy and degraded payloads | `npx vitest run <file>` |
| **E2E** | the assembled story across the stack | Playwright; assert visible state (`toBeVisible`/`toHaveText`), never `response.ok()`; stable `data-testid` hooks | `npm run pw:test:*` scripts only |

Run **one `mvn` at a time** — the shared `target/` corrupts under concurrent builds.

## How to run it (the workflow)

Author and launch a `Workflow` with three phases (this is an explicit opt-in to
multi-agent orchestration — invoking it from this skill is allowed):

- **Design** — `pipeline`/`parallel`, one agent per changed layer. Each reads the
  actual changed files + existing test conventions and returns the right-level
  test design + concrete code, plus a `catchesInversion` field answering "would
  this fail on the old code?".
- **Verify** — an adversarial skeptic per design (schema-structured verdict):
  over-mocked? wrong level? asserts on mock returns? for the data layer, would it
  actually fail on the reverted bug? Default to skepticism; `verdict ∈
  {solid, needs-revision, theater-reject}`.
- **Synthesize** — merge designs + verdicts into a prioritized plan naming WHICH
  single layer genuinely guards the core bug (usually the integration test).

Then **in the main loop** (not in parallel agents): implement the `solid`
designs, apply the skeptic's `needs-revision` fixes, run them sequentially, and
revert-to-verify the inversion guard goes red on the old code.

A starting workflow script lives alongside this skill at
`workflow.template.js` — pass the changed files + the bug being fixed as context,
one layer per array entry, and structured schemas for the design + verdict +
synthesis (see the `vector-test-coverage` run for a worked example).

## Output contract

- One meaningful test per layer that can independently fail, committed.
- An explicit statement of which layer guards the core bug (and proof it bites).
- Honest javadoc on each test about what it does and does NOT cover.
- Any layer flagged `theater-reject` is rewritten or dropped — never shipped to
  pad a coverage number.
