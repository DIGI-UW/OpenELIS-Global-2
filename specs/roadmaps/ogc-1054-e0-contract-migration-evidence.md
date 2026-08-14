# OGC-1054 E0 Contract and Migration Evidence

- **Recorded:** 2026-08-13
- **OpenELIS branch:** `codex/ogc-1054-e0-contract-migration`
- **OpenELIS tested head:** `0f56562f8`
- **Bridge contract baseline:** BR-E0 PR #45, `e17b021`
- **Checkpoint status:** `IN_PROGRESS`

This is checkpoint evidence, not MVP or deployed-UAT acceptance. E0 remains
subject to its prerequisite reviews, PR CI, and the remaining migration and
contract exit criteria in the authoritative roadmap.

## Bridge Client Security TDD

| Stage    | Evidence                                                                                                                     | Result                                                                                                                                                                |
| -------- | ---------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Red      | `366aa2e8c`; `BridgeHttpClientTlsTest` against the pinned Bridge PKCS12 fixture                                              | Failed because the client accepted a certificate outside the configured truststore.                                                                                   |
| Green    | `3e2e88325`; focused `BridgeHttpClientAuthenticationTest,BridgeHttpClientTlsTest`                                            | 4 tests passed. Unknown certificates are rejected, configured trust is accepted, unreadable trust material fails closed, and configured Basic authentication is sent. |
| Refactor | Truststore loading is isolated in `BridgeHttpClient`; the harness no longer disables Java HTTP-client hostname verification. | `mvn spotless:check` passed.                                                                                                                                          |

## Automated Validation

| Gate                       | Command scope                                                                      | Result                                                                                                                       |
| -------------------------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Backend package regression | Analyzer, analyzer-import, and QC package tests                                    | 873 tests passed; 0 failures, 0 errors, 0 skipped.                                                                           |
| Compose contracts          | Dev, base, analyzer-test, and isolated Compose layers                              | All rendered successfully with `config --quiet`.                                                                             |
| Exact artifact build       | Java 21 Maven package with tests already validated separately                      | `target/OpenELIS-Global.war` built successfully; SHA-256 `61205f9ccddef141c4d93705b6dc126826b3bd6b0002c4f7e9375a71a200caff`. |
| UI cross-component smoke   | `harness-foundational`: ASTM result review/acceptance and analyzer connection test | 3 tests passed in 15.6 seconds.                                                                                              |
| OpenELIS PR CI             | PR #4055 backend and frontend workflows                                             | All seven reported jobs passed at `0f56562f8`.                                                                              |
| Bridge stacked PR CI       | BR-M1 PR #46 `Run Tests / test`                                                     | Passed in 1 minute 8 seconds at `5d2664e` after BR-E0 `e17b021` removed the base-branch restriction.                        |

## Isolated Runtime Evidence

The `ogc1054-webapp` container was force-recreated with the WAR bind-mounted
from this permanent E0 worktree and reached `healthy`. Startup registered nine
analyzers over HTTPS; Bridge independently reported nine reconciled
registrations. No Bridge TLS, PKIX, hostname-verification, or HTTP 401 failures
appeared in the reviewed startup logs.

The isolated stack intentionally omits the FHIR service, so FHIR DNS failures
in startup logs are harness-scope noise and are not analyzer contract failures.
They are not accepted as G0 evidence.

## Evidence Limits and Follow-up

- The foundational Playwright project does not emit screenshots for passing
  tests. No visual comparison or final evidence claim is made from this run.
- Browser-console review found Vite HMR WebSocket connection errors and a
  recurring 404 while the three user stories passed. This is tracked as
  `ISSUE-E0-008` and must be resolved or deterministically classified before
  G0 console-clean acceptance.
- The remote analyzer site still runs historical code and only eight historical
  `AN-QC-*` review steps. Exact-RC deployment, all 15 `AN-MVP-*` Grist steps,
  screenshot/trace inspection, and MP4 evidence remain G0 work.
