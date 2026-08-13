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
- operational watcher/parser state;
- profile-driven FILE parsing and normalization to the versioned analyzer FHIR
  contract.

### OpenELIS owns

- FILE analyzer and import configuration;
- lab-facing FILE profile selection and site-specific catalog binding;
- bridge registration and synchronization;
- the authenticated direct-ingestion endpoint;
- normalized result binding and clinical processing;
- result/QC persistence, operational QC evaluation, audit, alerts, and review
  UI.

An OpenELIS application-side FILE poller is outside the target architecture and
must not be added. Any proposal to change this requires an explicit architecture
decision that supersedes this ownership contract.

## User Stories

### US1 - Configure a FILE analyzer

As a laboratory administrator, I can select a shipped FILE profile, configure
its directories/format and lab context, save it, and have OpenELIS register the
configuration with the bridge.

### US2 - Receive a bridge-delivered file

As a laboratory user, I can review results normalized by Bridge and delivered
to the OpenELIS direct-ingestion endpoint for local catalog binding, QC, and
clinical processing.

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
- Bridge format readers normalize delivered content; portable profiles own
  instrument-specific interpretation.
- Analyzer QC/configuration follows
  [the authoritative OGC-1054 roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md).
- Production result import and Results/Validation v4 acceptance remain separate
  from OGC-1054.

## Acceptance

1. A FILE analyzer can be created from a shipped profile through visible UI.
2. The exact saved configuration is represented in bridge registration.
3. Contract tests prove empty/update/delete registration behavior.
4. Bridge tests prove watching and delivery; OpenELIS tests do not simulate an
   application-owned poll loop.
5. Direct ingestion proves authentication, idempotency, normalized FHIR
   handling, local binding, processing, and audit; it does not parse a raw FILE
   format.
6. Repository search finds no enabled OpenELIS FILE watcher/poller path.

## Current Profile Direction

QuantStudio, Wondfo, FluoroCycler, and other compatible instruments are profile
work unless real export evidence proves the generic format/plugin contract is
insufficient. Unsupported formats remain blocked on representative vendor
exports and a validated mapping specification.
