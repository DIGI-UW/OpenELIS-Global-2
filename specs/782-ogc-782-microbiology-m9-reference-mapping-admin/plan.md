# Implementation Plan: M3 Reference and Mapping Administration

## Scope

Complete a reviewable administration vertical slice over the microbiology
reference foundation already shipped in the OGC-782 stack. Keep WHONET export,
WHO-TB, and remote catalog sync out of this PR.

## Architecture

### Backend

- Extend existing reference valueholders through one versioned Liquibase file.
- Add activation-event persistence and explicit breakpoint lifecycle values.
- Expand microbiology DAOs with typed HQL queries for filtered pages, counts,
  natural-key collision checks, reference impact, and version lookup.
- Add a `MicrobiologyReferenceAdminService` transaction boundary for compiled
  list/detail DTOs and writes.
- Add a `MicroBreakpointImportService` that parses, validates, previews, and
  applies normalized CSV rows through services.
- Add admin REST controllers that only map requests and derive the actor from the
  authenticated request.
- Preserve the existing read-only `/rest/microbiology/reference/*` contracts.

### Frontend

- Add one reusable admin shell with routed sections for organisms, antibiotics,
  AST panels, culture methods, and breakpoint standards.
- Use shared query-state utilities for `q`, filters, `sort`, `page`, and
  `pageSize`; breakpoint detail additionally carries the selected standard.
- Reuse shared table, status, form, confirmation, and import-result components.
- Add a config array consumed by both the Admin router and AdminSideNav labels so
  section definitions are not duplicated.
- Add only English i18n keys; Transifex remains authoritative for translations.

### UAT and Evidence

- Extend the property-gated UAT scenario service with synthetic reference and
  breakpoint records via services only.
- Publish M3 stories into the existing AMR checklist while retaining M1/M2.
- Add registered Playwright `core-app` journeys for reference editing, panel
  versioning, activation, mixed-validity CSV import, stable URLs, and breadcrumb
  return.
- Capture desktop/mobile screenshots against M-01 and M-02 mock intent and record
  intentional deviations.

## Migration Boundary

One new Liquibase changelog is permitted for accepted data-model changes only.
It must include rollback and tests for update and rollback. No migration is used
for routes, navigation, fixtures, or tests.

## Test Strategy

1. JUnit 4/Mockito service tests written before implementation.
2. Controller tests prove authorization, validation, and authenticated actor.
3. PostgreSQL integration tests prove uniqueness, immutable versions,
   activation exclusivity, import idempotency, and rollback.
4. ORM validation includes every new/changed valueholder.
5. Vitest/RTL proves URL state, Carbon interactions, modal validation, and
   navigation/breadcrumb behavior.
6. Playwright proves the complete authenticated admin journey locally and on the
   exact deployed SHA with no arbitrary waits.

## Constitution Check

- Configuration-driven variation: pass; no country branches.
- Carbon first: pass; installed Carbon primitives only.
- Layered architecture: pass; controllers delegate to transactional services.
- TDD: required by task order.
- Liquibase: required only for actual model changes, with rollback.
- i18n: new keys in `en.json` only.
- Security/audit: admin authorization and server-derived actor are acceptance
  requirements.
- Milestone delivery: separate worktree, branch, PR, UAT, and evidence.
- Legacy removal: no new behavior is added to superseded WHONET or legacy Method
  write paths; existing Method identity is reused.
