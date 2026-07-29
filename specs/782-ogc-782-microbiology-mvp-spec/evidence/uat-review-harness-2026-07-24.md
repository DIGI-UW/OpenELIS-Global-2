# OGC-782 UAT Review Harness Evidence

> Historical checkpoint. Current deployment, checklist, and fixture provenance
> are recorded in
> [live-deployment-2026-07-28.md](live-deployment-2026-07-28.md).

## Live Contract

- Authoring source: Grist `UAT Checklists`, instance `amr`
- Jira: `OGC-782`
- Live checklist:
  `https://amr.openelis-global.org/__review/uat-amr.json`
- Review surface: `https://amr.openelis-global.org` -> `Review`
- Delivery behavior: Grist edits appear in the overlay through the live read
  service; there is no static publish step.

## Checklist

The live checklist contains 10 steps across four sections:

1. Open the worklist through configured Microbiology navigation.
2. Preserve workflow and sort state across refresh.
3. Clear filters and stale query parameters.
4. Open a seeded case while retaining worklist context.
5. Preserve the active case section across refresh.
6. Return to the prior filtered worklist.
7. Record an isolate.
8. Enter and interpret manual AST.
9. Carry the reviewed S/I/R result through release to the patient report.
10. Review the sibling TB case and identify the highest-value remaining gap.

## Verification

On 2026-07-24:

- the live endpoint returned one `amr` checklist with 10 steps;
- Playwright opened the actual AMR `Review` panel;
- the panel rendered `Microbiology MVP - review` and `0/10 checked`;
- accessibility-tree assertions confirmed the configured-navigation,
  canonical-route, stable-filter, report-propagation, and shared-specimen
  checks;
- all seven targeted overlay assertions passed.

The deployed feature was then verified with the committed
`core-live-uat` Playwright project:

```bash
cd frontend
BASE_URL=https://amr.openelis-global.org \
EXPECTED_APP_SHA=c44c3ad0ce35d1502d9ed217f3e26aa88e309dc1 \
PLAYWRIGHT_VIDEO=on \
npx playwright test \
  playwright/tests/manual-only/core/ogc-782-live-uat.spec.ts \
  --project=core-live-uat --no-deps --reporter=html
```

Result: `1 passed (4.0s)`. The complete authentication plus feature run passed
both tests in `6.6s`.

The registered product E2E flow also passed locally:

```bash
cd frontend
BASE_URL=https://localhost:48443 \
DB_CONTAINER=ogc-782-microbiology-db \
npm run pw:test -- \
  playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts \
  --project=core-app
```

Result: authentication setup plus the microbiology navigation and stable-state
test both passed in 9.4 seconds. The isolated Vite proxy logged its known HMR
WebSocket connection warning and a missing development-only resource; neither
affected the production behavior under test.

## Live Deployment

- Deployment: `20260725T003108Z-c44c3ad0ce35`
- OpenELIS SHA: `c44c3ad0ce35d1502d9ed217f3e26aa88e309dc1`
- Review-tooling SHA: `2cf9dd4e5794e2ce46a568c295d8093ae51bf33e`
- Scope: `app`
- Schema-affecting: `false`
- Verification: health and route smoke passed
- Checklist revision:
  `364c75677839c0aed225ecfa75e5c0912c07840a1925a14c9dd30bf2ed2d7d95`

The application now contains the configured Microbiology navigation and
canonical routes. The production review widget uses an inspectable open shadow
root and preserves panel state when an initial checklist refresh finishes after
the reviewer opens it.

Evidence is packaged under
`evidence/live-uat-2026-07-24/`, including three inspected screenshots, WebM
and MP4 video, the Playwright HTML report, and a machine-readable manifest.

At this checkpoint, all ten Grist rows had `required=false` and nested routes
emitted route-relative service-worker noise. Both observations are superseded
by the current deployment evidence linked above: all ten steps are now required,
and service-worker registration is rooted at the application origin.
