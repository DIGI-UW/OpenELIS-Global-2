# Feature 014: FILE Analyzer Stream Alignment

**Status:** Architecture shipped; contract validation and instrument profiles
continue by milestone
**Governing ownership:** [AGENTS.md](../../AGENTS.md)

## Purpose

Provide one profile-driven FILE analyzer architecture without duplicating
runtime watching inside OpenELIS.

## Ownership Contract

### Bridge owns

- directory watching and polling;
- runtime watch registration/removal;
- file stability detection;
- transport, retries, and delivery;
- transport/runtime dead-letter behavior;
- operational watcher/parser state.

### OpenELIS owns

- FILE analyzer and import configuration;
- shipped FILE profile selection;
- bridge registration and synchronization;
- the authenticated direct-ingestion endpoint;
- format normalization and profile-driven result processing;
- result/QC persistence, audit, and review UI.

No OpenELIS application-side FILE poller is implemented. A future fallback
would require a separate specification and must be disabled by default.

## User Stories

### US1 - Configure a FILE analyzer

As a laboratory administrator, I can select a shipped FILE profile, configure
its directories/format and lab context, save it, and have OpenELIS register the
configuration with the bridge.

### US2 - Receive a bridge-delivered file

As a laboratory user, I can review results from a file delivered to the
OpenELIS direct-ingestion endpoint after profile-driven normalization and
mapping.

### US3 - Add another compatible instrument

As an implementer, I can support a compatible analyzer by adding and validating
a shipped profile rather than a WAR-local per-instrument poller or adapter.

## Requirements

- FILE is a transport/profile category, not an ASTM protocol alias.
- Profile configuration declares file format, delimiter/header/skip behavior,
  file pattern, import/archive/error directories as applicable, and mapping
  defaults.
- Bridge registration create/update/delete changes live bridge watch state.
- Bridge delivery is idempotent under the direct-ingestion contract.
- OpenELIS validation rejects unsupported or incomplete FILE configuration.
- Format readers normalize delivered content; profiles own instrument-specific
  interpretation.
- Analyzer QC/configuration follows
  [OGC-1054](../OGC-1054-analyzer-qc-config/spec.md).
- Production result import and Results/Validation v4 acceptance remain separate
  from the OGC-1054 configuration MVP.

## Acceptance

1. A FILE analyzer can be created from a shipped profile through visible UI.
2. The exact saved configuration is represented in bridge registration.
3. Contract tests prove empty/update/delete registration behavior.
4. Bridge tests prove watching and delivery; OpenELIS tests do not simulate an
   application-owned poll loop.
5. Direct ingestion proves authentication, idempotency, format dispatch,
   processing, and audit.
6. Repository search finds no enabled OpenELIS FILE watcher/poller path.

## Current Profile Direction

QuantStudio, Wondfo, FluoroCycler, and other compatible instruments are profile
work unless real export evidence proves the generic format/plugin contract is
insufficient. Unsupported formats remain blocked on representative vendor
exports and a validated mapping specification.
