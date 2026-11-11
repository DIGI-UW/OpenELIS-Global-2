# Selected-sample FHIR replay

## Purpose and ownership

Retained reporting fixtures can be read through OpenELIS's native FHIR mapping
while absent from its configured FHIR store. Re-saving results is unsuitable:
the result-entry path can change dates and advance workflow state. The existing
`transformPersistObjectsUnderSamples` service already transforms retained data
and its dependencies, but its only HTTP caller scans all samples or all samples
missing local UUIDs. Batch size is not a selection limit.

This repair exposes that service for an explicit selection. Native OpenELIS owns
T042–T044 in `tasks.md`; the Catalyst integration task owns cohort verification,
the resulting FHIR/Spark/Catalyst path and its acceptance. No new replay queue,
result-writing path or reporting-builder capability is introduced.

## API

`POST /api/OpenELIS-Global/rest/fhir/replay`

Use existing administrator authentication and the normal CSRF mechanism for
session-based requests. Existing Basic authentication remains available.
Send `Content-Type: application/json` and `Accept: application/json`.

```json
{ "sampleIds": ["1154", "1157"] }
```

- The request contains 1–100 positive integer sample IDs. Null, malformed,
  oversized and missing selections are rejected. Repeated IDs are replayed once.
- All selected samples must exist before any transformation or emission starts.
- Replay covers all results and related resources under the selected samples,
  including Patient, Practitioner, Specimen, ServiceRequest, DiagnosticReport
  and Task resources where applicable. It is not an arbitrary result-ID filter.
- Existing UUIDs are retained. The native service assigns UUIDs where absent.
  It does not call result-entry persistence or recreate results from Observations.
- The endpoint has no shared mutable job state. It waits up to 120 seconds for
  the existing asynchronous service and checks the returned transaction response.

Example completed response (the count depends on the selected samples):

```json
{ "status": "completed", "sampleIds": ["1154", "1157"], "resourceCount": 15 }
```

Success means the FHIR store returned a nonempty transaction response with a
successful status for every entry. It is not downstream Catalyst acceptance.
Unknown sample IDs return 404; invalid input returns 400; transform/store failure
or an invalid response returns 502. A timeout returns 504 with
`FHIR_REPLAY_COMPLETION_UNKNOWN`; interruption returns 503 with the same code.
The worker may still finish after either event. Inspect the store before retrying;
these responses do not claim completion or cancellation.

## Focused validation and handoff

`FhirReplayRestControllerTest` exercises authentication, malformed/oversized
requests, exact selection, completed receipts, missing samples, asynchronous
failure, invalid transaction responses and timeout behavior.
`FhirSampleReplayTest` exercises whole-selection preflight before UUID assignment,
selected-sample-only transformation with retained identities, and propagation of
FHIR store errors through the existing service.

Local validation on September 15: all 13 focused tests passed with zero failures,
errors or skips. The Java 21 WAR build passed with both test-skipping flags after
the focused test run. The HTTP test context uses the existing EL-free validator
pattern because Tomcat supplies EL in the deployed runtime. No frontend code or
dependencies changed.

The first local startup exposed an existing circular referral/transform dependency:
eager controller construction requested the transform before Spring had wrapped
its asynchronous proxy. Constructor injection now resolves lazily.
`FhirReplayWiringTest` reproduced the original startup error before the fix and
passes afterward using the real transform implementation and Spring async proxy.
The corrected WAR was rebuilt; native startup and integration acceptance remain
separate from these focused checks.

The first [full backend check](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/35035861477)
ran 6,338 tests and reported six errors in `SubcontractAutoTransitionTest` while
all 13 replay checks passed. The new wiring test's nested `AsyncConfig` escaped
`AppTestConfig`'s established exclusion for nested `TestConfig` classes, enabling
async proxies in the shared integration context. Renaming the test configuration
to `TestConfig` fixes that isolation defect without changing production code.
The six errors reproduced locally before the rename; afterward all 23 targeted
tests passed (10 referral tests and 13 replay tests), with no failures, errors or
skips. Full hosted CI must pass on the corrected PR head before merge.

The agreed synthetic cohort is sample 1154 (`REPORTING-MVP-REPEAT`, two results)
and sample 1157 (`REPORTING-MVP-TURNAROUND`, two results). The integration owner
verified that these samples contain only the four intended results. Deployment
must preserve the existing database/report volumes, proxy connection and disabled
startup imports. Broad `/OEToFhir` backfill and same-resource Observation PUT are
not part of this procedure.

## Verified native result

[PR #4323](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4323), implementation
`fa70a8c001811ea69b3020cdd2390d249c1b2ea6`, is deployed on the local reporting
instance. Built, staged and active WAR SHA-256:
`34ed37b11ceef8c91d14c2d102260bcbf2f6bc8c956dda41f75628261ed51052`.
Native and proxied login, the reporting page and authenticated FHIR metadata
returned HTTP 200; metadata reports FHIR 4.0.1. Database/web container identities
and start times, mounts and runtime configuration hashes were preserved.

At 2026-09-15 23:25 UTC, the integration owner submitted the two selected sample
IDs and received HTTP 200 with `status: completed` and `resourceCount: 15`.
Store reads matched all four expected Observations to the earlier native facade
outputs, including identities, status, code, quantity, patient/specimen/order
references and effective/issued dates. Before/after full-row snapshots were
byte-identical for four results, three analyses, two samples and two sample items.
The native task independently checked the replay response and snapshot equality.

Evidence is retained by the integration owner under
`catalyst-evidence/four-pathways/2026-09-15/native-fhir-read/`:
`replay-response.json`, `replay-verified.json` and the before/after table snapshots.
Verification receipt SHA-256:
`711c3248e1c036896ffce81c62bb15bac0410a88fe5085539a84233e0623cab8`.
An earlier client request used an unsupported Accept header and returned 406
without emission; the corrected JSON request required no product change.

T044's native delivery is complete. Downstream Data Pipes/Spark/Catalyst
verification is separate and remains with the integration task. This checkpoint
does not claim a merge, public deployment or human UAT acceptance.
