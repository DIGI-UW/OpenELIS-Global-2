# Quickstart: Unified Test Catalog Management Editor (OGC-949)

Developer onboarding + per-milestone Independent-Test walkthroughs. Only M0 and
M1 are detailed now; a subsection is appended for each milestone at its ELABORATE
gate (the tasks.md gate task links `quickstart.md#m{N}` as the Independent Test).

## Orientation

- **Spec** ([spec.md](./spec.md)) integrates Jira + FRS + mockups. **Plan**
  ([plan.md](./plan.md)) is the canonical roadmap (milestone tables + graph).
  **Detailed acceptance criteria live in Jira** child stories (linked from the
  Source Map); **prose lives in the FRS** (pinned at openelis-work `@f04cce54`).
- **Branch lane**: every milestone PR targets `develop` as
  `feat/ogc-949-m{N}-{desc}`. Never target `demo-silnas`.
- **Before any milestone**: `git fetch origin`; branch from latest `develop`;
  open a **draft PR early**; keep it ≤30 files / ≤2,500 LOC.
- **Formatting before every commit**: `mvn spotless:apply` (backend) and
  `cd frontend && npm run format` (frontend). Clear the spotless cache if CI
  flags a file local says is clean: `rm -rf target/spotless-* && mvn spotless:apply`.
- **Tests**: backend JUnit 4 + `BaseWebContextSensitiveTest`; frontend Jest +
  RTL; E2E **Playwright** via `npm run pw:test` (never raw `npx playwright`, no
  new Cypress).

## M0 — Reconciliation

Goal: clear the four cross-cutting blockers before M1. M0 is mostly small PRs +
decision records (in [research.md](./research.md)); no user story.

1. **Port Methods (#3706) to develop.**
   ```bash
   git fetch origin
   git checkout -b feat/ogc-949-m0-methods-port origin/develop
   # cherry-pick the OGC-750 Methods commit from demo-silnas (single squashed commit c9b623391)
   git cherry-pick c9b623391    # resolve conflicts: keep test_method table, 039 changeset, TestMethod*, MethodsSection.jsx
   ```
   - Verify the Liquibase changeset `039-test-method-links.xml` applies on a clean
     develop DB; run the Methods service/controller tests; smoke the
     `GET /rest/api/tests/{testId}/methods` endpoint.
   - **Independent Test**: Methods code green on develop (its existing tests pass);
     the editor Methods section (M6) later mounts this unchanged.
2. **Record the OGC-285 ↔ OGC-761 boundary** (already resolved — see
   research.md R2). No code; a decision note + a comment on
   [OGC-761](https://uwdigi.atlassian.net/browse/OGC-761) that it consumes
   OGC-285's `test_label_preset_link`.
3. **Sequence PR #3546 vs OGC-927** (research.md R4): confirm whether the admin
   SideNav consolidation lands before M2; if so, M2 builds on it; else M2 is
   sequenced after. Decision note only.
4. **Confirm the permission gate** (research.md R3): v1 uses
   `@PreAuthorize("hasRole('ADMIN')")` + UI menu hide; **no** dependency on
   OGC-384 / #3443. Decision note only.

## M1 — Schema migrations + backend foundation (OGC-747)

Goal: land every v1 schema object (see [data-model.md](./data-model.md)) with a
**lossless** migration. This blocks all section work, so it ships first and is
the most carefully tested milestone.

1. Branch: `feat/ogc-949-m1-schema` from develop.
2. Author Liquibase changesets `040+` under
   `src/main/resources/liquibase/3.5.x.x/`, in the OGC-936→939 order
   (data-model.md § "Migration sequencing"). Wire each into the changelog include.
3. Add/extend valueholders (JPA/Hibernate annotations) for
   `test_result_component`, `test_sample_handling`, junctions,
   `test_activation_acknowledgment`; reconcile `unit_of_measure` with the existing
   `org.openelisglobal.unitofmeasure` entity rather than duplicating.
4. **Write the losslessness test first** (TDD red): seed a baseline catalog (use
   `src/test/resources/load-test-fixtures.sh`), run the migration, assert
   pre/post row counts match for `test`, `test_range`, `test_interpretation`,
   `test_select_list_option`, and that every row carries a non-null `component_id`
   pointing at its test's PRIMARY component.
5. ORM validation tests for the new entities (must run <5s, no DB).
6. **Dry-run** the migration against a production-like dump before opening for
   review.

   **Independent Test (M1)**: on a populated DB, `liquibase update` then
   `rollback` run clean; the losslessness test is green; a freshly created test via
   the new backend service requires `domain` and resolves units to the master
   list. See spec US1 acceptance scenarios.

## Milestone elaboration protocol (M2–M12)

Each section milestone is **story-level** in tasks.md until it starts. When you
pick it up:

1. Create `feat/ogc-949-m{N}-{desc}` from develop; open a draft PR.
2. Run the **ELABORATE gate**: re-run `/speckit.tasks` scoped to that milestone to
   expand its Jira-story tasks into TDD sub-tasks and extend
   `contracts/openapi.yaml` with that section's request/response schemas. Append a
   `## M{N}` subsection to this quickstart with the section's Independent Test.
3. Only then begin implementation tasks (tests first).

> v2 milestones (M13–M20) are not elaborated here; they require a `/speckit.plan`
> revision at v2 kickoff before any tasks exist.
