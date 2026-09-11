# Quickstart: Running the Analyzer-Independent QC/Results Roadmap

This quickstart is for the developer starting the next milestone from this
roadmap.

## 1. Start From a Clean Worktree

Never use the active OGC-1054 worktree for this roadmap.

```bash
git fetch origin develop
git worktree add /Users/pmanko/.codex/worktrees/<id>/OpenELIS-Global-2 \
  -b feat/427-811-qc-results-m1-batch-foundation origin/develop
cd /Users/pmanko/.codex/worktrees/<id>/OpenELIS-Global-2
```

For later milestones, replace the branch suffix with the milestone branch from
[plan.md](./plan.md).

## 2. Confirm Mock Baseline

Use the pinned openelis-work commit:

```bash
OPENELIS_WORK=/Users/pmanko/code/openelis-work
MOCK_COMMIT=6c24b6ee04aab4ecef96e8daa57dc88ae8821356
git -C "$OPENELIS_WORK" fetch origin main
git -C "$OPENELIS_WORK" rev-parse origin/main
```

Capture baseline screenshots from the relevant mock artifact before implementing
UI:

- M1-M3: `designs/quality/batch-workplan-reagent-qc.jsx`
- M4-M5: `designs/results-validation/results-entry-v4.html`
- M5: `designs/results-validation/results-page-reagent-usage-v2.1-preview.html`
  and `designs/nce/nce-results-entry.jsx`
- M6: `designs/results-validation/validation-page-v4.html`

Store local artifacts outside git unless the milestone PR intentionally commits
text-only evidence:

```text
specs/427-811-qc-results-entry-roadmap/evidence/<milestone>/
```

## 3. Develop With Layered Tests

Use the right test layer:

- Backend business rules: JUnit 4 service/controller/integration tests.
- ORM/schema: mapping tests and Liquibase migration tests.
- React behavior: Vitest/RTL component tests.
- User-story proof: Playwright browser tests and MP4 video.

Do not use Playwright REST calls as proof. If setup or cleanup needs API/fixture
help, keep it outside the asserted story path and document it.

## 4. Run Focused Validation

Backend examples:

```bash
mvn "-Dtest=org.openelisglobal.<module>.**" -Dsurefire.failIfNoSpecifiedTests=false test
mvn spotless:check
```

Frontend examples:

```bash
cd frontend
npm test -- --run <focused-test-files>
npm run pw:guard
npm run lint -- --quiet
npm run check-format
```

Video proof example:

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' \
  npm run pw:test:core-demo-video -- \
  playwright/tests/demo/core/<milestone-user-story>.spec.ts --workers=1
```

Review the generated screenshots, MP4, browser console logs, and trace before
calling the milestone done.

## 5. Fill Evidence Notes

Each milestone must produce:

- `code-qa.md`
- `mock-comparison.md`
- MP4 path
- screenshot paths
- validation command list

Use [contracts/evidence-template.md](./contracts/evidence-template.md) as the
required PR evidence shape.

## 6. PR Body Minimum

Every milestone PR body must include:

- Scope and explicit non-scope.
- Jira links.
- Mock baseline commit and artifacts used.
- Validation commands and results.
- MP4 evidence path.
- Screenshot comparison summary.
- Code-qa gate summary.
- Any legacy route/removal decision.
