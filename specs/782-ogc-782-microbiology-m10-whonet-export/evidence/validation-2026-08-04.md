# OGC-782 M4 Validation - 2026-08-04

## Automated Results

| Check | Result |
| --- | --- |
| Focused Java unit/security | 20 passed: controller 3, Spring Security 6, dataset compiler 9, report service 2 |
| Real persistence integration | 1 passed |
| ORM validation | 2 passed |
| Liquibase update/rollback/reapply | 1 passed |
| Frontend Vitest/RTL | 12 passed across 3 files |
| Foundational Playwright | 2 passed including auth setup; the single M4 journey passed |
| Focused accessibility | 3 passed including auth setup; desktop and Pixel 5 M4 states have no WCAG 2.1 AA axe violations |
| Standard desktop demo | 2 passed including auth setup |
| Mobile evidence/overflow | 2 passed including auth setup |
| Java 21 package build | Passed |
| Vite production build | Passed |

The pinned evidence bundler produced six screenshots, one 34-second standard
H.264/yuv420p MP4, a contact sheet, a completed narrative index with no
placeholders, a manifest, and a zip at
`/tmp/ogc-782-m4-evidence-f57064ec5b4f/` and
`/tmp/ogc-782-m4-evidence-f57064ec5b4f.zip`.

All backend runs used Java 21. Persistence, ORM, and Liquibase checks used
Testcontainers PostgreSQL. The service-created M4 fixture uses no SQL, fixed
persisted IDs, or DAO bypass.

## Interaction Quality

The foundational and accessibility tests use Carbon roles, labels, visible
state, response readiness, and download events. The demo uses visible headings,
rows, URLs, and status messages rather than backend-response synchronization.
There are no arbitrary interaction waits. Presentation-only pauses are isolated
to `core-demo-video` through `createDemoPresentation`.

The disposable Vite stack reports its known HMR WebSocket connection warning and
one unrelated notification-resource `404`. Neither affected the feature request,
the CSV download, axe results, or assertions. The Vite build also reports existing
repository-wide CSS-module, chunk-size, and direct-eval warnings outside this
slice.

## UAT State

Grist contains story `AMR-S14` with required steps `AMR-45` through `AMR-50` and
a link to PR #3984. The live Grist source and routed AMR overlay are byte-identical
at revision `90a25a2ee19e0282611845eca163159e84cc2bbfe55309ebf0d77d6ec7edea43`,
with 14 stories, 38 required steps, and one optional TB reflection. Human
Pass/Fail/N/A rulings remain open.

## Exact Deployment

- Instance: `amr.openelis-global.org`
- Deployment: `20260805T013759Z-f57064ec5b4f`
- Application SHA: `f57064ec5b4f2f797eee3566938cb69efaa79022`
- Review harness SHA: `72eb003155db91f08a90d5e853e7811f86d3c642`
- Target verification: health and smoke passed; `/` and
  `/Microbiology/worklist` returned `200`
- Deployed functional Playwright: 2/2 including auth setup
- Deployed desktop/mobile accessibility: 3/3 including auth setup, with no
  WCAG 2.1 AA axe violations
- Deployed standard desktop video: 2/2 including auth setup
- Deployed mobile evidence/overflow: 2/2 including auth setup

PR #3984 initially failed the blocking Playwright lint rule because the demo
used `waitForResponse`. Commit `f57064ec5b4f2f797eee3566938cb69efaa79022`
replaced both uses with visible Preview-state assertions. `npm run lint` and the
desktop/mobile demo run passed before deployment. The GitHub web session available
to this workspace is signed out, so the generated binaries have not yet been
attached to the PR; no release or committed-media workaround was used.

## Environment Issue Found And Fixed

Restarting the existing M4 database initially produced a Liquibase checksum
failure because a cleanup had edited the original export-run changeset. The
original changeset was restored byte-for-byte and a second cleanup changeset was
added. The same existing database then started successfully, and the standalone
update/rollback/reapply test passed.
