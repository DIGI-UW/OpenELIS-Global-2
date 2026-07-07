# OGC-782 Microbiology MVP Gap Remediation Checkpoint

Generated on 2026-07-06 from the local worktree dev stack. Supersedes nothing;
extends [mvp-checkpoint-2026-06-27.md](./mvp-checkpoint-2026-06-27.md) (the M7
checkpoint) with evidence for the three MVP-scope gaps closed on top of it, per
[mvp-gap-analysis-2026-07-03.md](./mvp-gap-analysis-2026-07-03.md).

## Headline Result

FR-002 (order-detail capture), M-05 (per-run breakpoint-standard selection),
and M-11 (Alerts Dashboard integration) are implemented, TDD-covered, and
proven against a real dev stack. While collecting Playwright evidence, one
pre-existing production bug (final-release stage/readiness mismatch) and two
pre-existing stale Playwright selectors were found and fixed; see
`mvp-gap-analysis-2026-07-03.md` "Discovered during acceptance gate".

## Validation Summary

| Check | Result | Why it matters |
|---|---|---|
| Backend focused suite (`Micro*Test`, `*Micro*IntegrationTest`, alert regression) | 30 test classes, 0 failures | Covers FR-002/M-05/M-11 unit + real-DB integration tests, plus Freezer/Equipment/QC/EQA alert regression proving the `alert_entity_ref` addition didn't disturb numeric-keyed alerts. |
| Frontend focused suite (`Microbiology`, `AlertsDashboard`) | 8 files / 19 tests passed | Covers `OrderDetailPanel`, `AstEntryPanel` breakpoint selector, `AlertsDashboard` microbiology-critical filter/row, and full case-view wiring. |
| Playwright `microbiology-case-workbench.spec.ts` (`core-app`) | 2 passed | Real browser proof the `OrderDetailPanel` renders and the case workbench flow still works end to end. |
| Playwright `microbiology-worklist-critical.spec.ts` (`core-app`) | 2 passed | Real browser proof of critical-communication logging/acknowledging and worklist visibility. |
| Playwright `ogc-782-microbiology-mvp.spec.ts` (`core-demo`) | 2 passed | Full happy path through final release, including the release-fix regression proof. |

## Real Liquibase Migrations Applied

Confirmed via webapp container startup logs against a live Postgres instance
(not just a test DB):

- `055-microbiology-order-detail.xml` (`micro_case_order_detail` table)
- `056-microbiology-ast-breakpoint-standard.xml` (`micro_ast_run.breakpoint_standard_id`)
- `057-alert-entity-ref.xml` (`alert.alert_entity_ref`, `chk_alert_entity_id_or_ref`, `chk_alert_type` update)

## Environment Notes

- Dev stack was stood up locally (`.env` created from `.env.example`, WAR
  rebuilt, `docker compose -f dev.docker-compose.yml up -d`).
- A host port conflict on `8443` with another active worktree's analyzer
  harness stack (`harness-proxy`) was resolved with a temporary, untracked
  local compose override remapping only `oe.openelis.org`'s direct Tomcat port
  (`18443`); Playwright's `baseURL` (`https://localhost`, routed through the
  nginx proxy on port 443) was never affected. The override file was deleted
  and the stack torn down after evidence collection; the harness stack was
  left untouched throughout.

## Known Console Noise

Same pre-existing React Intl missing-message noise as the M7 checkpoint
(unrelated analyzer/navigation labels); no new console errors introduced by
the gap work.
