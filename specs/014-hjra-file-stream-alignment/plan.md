# Feature 014 Implementation Plan

## Architecture

1. OpenELIS persists FILE analyzer/profile configuration.
2. OpenELIS sends deterministic registration to the bridge.
3. The bridge creates, updates, and removes live watches.
4. The bridge delivers a stable file to the OpenELIS direct-ingestion endpoint.
5. OpenELIS validates, normalizes, processes, persists, and audits the
   submission.

There is no OpenELIS polling loop in this plan.

## Milestones

### M1 - Configuration contract

- Persist protocol-appropriate FILE fields.
- Validate the analyzer/profile configuration.
- Include FILE fields in bridge registration.
- Provide lab-facing profile-driven setup through OGC-1054.

### M2 - Direct ingestion

- Authenticate bridge delivery.
- Enforce idempotency and size/format validation.
- Dispatch delivered bytes to the generic format/plugin path.
- Return deterministic success/error responses.

### M3 - Bridge runtime

- Prove register/update/delete watch lifecycle in the bridge.
- Prove file stability, delivery, retry, and dead-letter behavior.
- Run cross-repository contract tests against OpenELIS.

### M4 - Instrument profiles

- Validate representative vendor exports.
- Add or amend profile JSON and fixtures.
- Prove mapping/result behavior without per-instrument application code.
- Mark formats without representative evidence as blocked.

## Testing Ownership

- OpenELIS JUnit: configuration, registration payload, ingestion, normalization,
  processing, persistence, audit.
- Bridge tests: watcher lifecycle, polling, stability, transport, retry.
- Contract/harness: compatibility between registration/delivery schemas.
- UI E2E: visible analyzer setup and result review only.

## Exit Gate

The architecture is accepted only when repository/code review finds no enabled
OpenELIS FILE poller, bridge runtime tests own watching behavior, and the
cross-repository contract proves delivery into the OpenELIS-owned processing
path.
