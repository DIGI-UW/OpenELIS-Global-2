# Feature 014 Tasks

## OpenELIS configuration

- [x] Persist FILE format/configuration metadata.
- [x] Expose profile-driven FILE analyzer configuration.
- [x] Keep FILE profile source under `projects/analyzer-profiles/file/`.
- [ ] Maintain deterministic bridge registration contract tests.
- [ ] Keep OGC-1054 setup/readiness acceptance current.

## Direct ingestion and processing

- [x] Provide bridge-facing direct ingestion.
- [x] Dispatch supported delivered formats to generic processing.
- [ ] Maintain idempotency, validation, persistence, and audit tests.
- [ ] Add production result-import acceptance in its own milestone.

## Bridge companion work

- [ ] Prove runtime watch add/update/remove.
- [ ] Prove stable-file detection and exact delivery.
- [ ] Prove retry/dead-letter behavior.
- [ ] Run cross-repository registration/delivery contracts.

## Profile work

- [ ] Require representative export fixtures and mapping specs.
- [ ] Add compatible instruments as profile/fixture changes.
- [ ] Record explicit blockers for unsupported formats.
- [ ] Add custom code only after the generic contract is disproved by evidence.

## Prohibited work

- [x] No OpenELIS application-side FILE poller.
- [x] No FILE-to-ASTM protocol default.
- [x] No duplicate per-instrument watcher.
- [x] No bridge change inferred without contract evidence.
