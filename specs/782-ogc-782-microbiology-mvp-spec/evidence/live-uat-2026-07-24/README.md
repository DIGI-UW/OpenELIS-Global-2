# OGC-782 Live AMR UAT Evidence

> Historical media bundle. Current deployment, checklist, and fixture
> provenance are recorded in
> [../live-deployment-2026-07-28.md](../live-deployment-2026-07-28.md).

## Provenance

- Target: `https://amr.openelis-global.org`
- Deployment: `20260725T003108Z-c44c3ad0ce35`
- OpenELIS SHA: `c44c3ad0ce35d1502d9ed217f3e26aa88e309dc1`
- Review-tooling SHA: `2cf9dd4e5794e2ce46a568c295d8093ae51bf33e`
- Checklist revision:
  `364c75677839c0aed225ecfa75e5c0912c07840a1925a14c9dd30bf2ed2d7d95`
- App deployment scope: `app`
- Schema-affecting: `false`

## Playwright Result

The committed `core-live-uat` test passed against the deployed target:

```bash
cd frontend
BASE_URL=https://amr.openelis-global.org \
EXPECTED_APP_SHA=c44c3ad0ce35d1502d9ed217f3e26aa88e309dc1 \
PLAYWRIGHT_VIDEO=on \
npx playwright test \
  playwright/tests/manual-only/core/ogc-782-live-uat.spec.ts \
  --project=core-live-uat --no-deps --reporter=html
```

Result: `1 passed (4.0s)`. Authentication was validated separately in the
complete run, where setup plus live UAT both passed in `6.6s`.

The test verifies:

- exact deployed application SHA and ready target state;
- the ten stable Grist checklist step keys and checklist revision;
- the live Review overlay and AST-to-report review step;
- configured Microbiology sidenav navigation;
- canonical worklist filter/sort state across refresh;
- case URL context and `section=isolates` across refresh;
- return to the filtered worklist and canonical clear-filter behavior.

## Visual Review

- [Review overlay](screenshots/01-review-overlay.png): panel is readable,
  stable, and does not obscure its controls.
- [Filtered worklist](screenshots/02-filtered-worklist.png): configured
  Microbiology navigation remains locked and the canonical filters are visible.
- [Case workbench](screenshots/03-case-isolates-section.png): progress rail,
  case context, and content remain aligned without overlap.
- [Slowed MP4](videos/ogc-782-live-uat.mp4)
- [Raw Playwright WebM](videos/ogc-782-live-uat.webm)
- [Playwright HTML report](playwright-report/index.html)

## Historical Follow-Up Signals

At this checkpoint, all ten Grist rows were optional and nested routes produced
route-relative service-worker requests. The current deployment evidence
supersedes both observations: all ten steps are required, and service-worker
registration is rooted at the application origin.
