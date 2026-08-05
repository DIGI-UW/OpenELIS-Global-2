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

The pinned evidence bundler produced six screenshots, one standard H.264 MP4,
a completed narrative index with no placeholders, and a zip at
`/tmp/ogc-782-m4-evidence-local/`. This local bundle will be regenerated against
the exact deployed SHA before publication.

All backend runs used Java 21. Persistence, ORM, and Liquibase checks used
Testcontainers PostgreSQL. The service-created M4 fixture uses no SQL, fixed
persisted IDs, or DAO bypass.

## Interaction Quality

The foundational and accessibility tests use Carbon roles, labels, visible
state, response readiness, and download events. There are no arbitrary waits.
Presentation-only pauses are isolated to `core-demo-video` through
`createDemoPresentation`.

The disposable Vite stack reports its known HMR WebSocket connection warning and
one unrelated notification-resource `404`. Neither affected the feature request,
the CSV download, axe results, or assertions. The Vite build also reports existing
repository-wide CSS-module, chunk-size, and direct-eval warnings outside this
slice.

## UAT State

Grist contains story `AMR-S14` with required steps `AMR-45` through `AMR-50`.
The combined pre-deployment checklist revision is
`2c50adaa394ee252cd775a87383c70d5af672b42530614c9bc1ad201dac27ba8`, with
14 stories, 38 required steps, and the optional TB reflection. Deployment-time
revision verification and human rulings remain open.

## Environment Issue Found And Fixed

Restarting the existing M4 database initially produced a Liquibase checksum
failure because a cleanup had edited the original export-run changeset. The
original changeset was restored byte-for-byte and a second cleanup changeset was
added. The same existing database then started successfully, and the standalone
update/rollback/reapply test passed.
