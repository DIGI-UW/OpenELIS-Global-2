# OGC-782 UAT Review Harness Evidence

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

## Deployment Caveat

The Grist checklist and overlay are live. The AMR site was still serving the
older application build during verification and did not yet contain the new
Microbiology menu or canonical routes. Redeploy the feature branch before
expecting those UAT steps to pass; until then, their failure is a deployment
signal rather than a checklist defect.
