# OGC-1054 Implementation Plan

## Technical Context

The implementation extends the current Java 21/Spring MVC analyzer services and
React 17/Carbon application. Shipped profile files remain authoritative.
Analyzer-specific result mappings and verification metadata remain in existing
plugin config JSON; durable verification is also recorded through existing
analyzer audit services.

Carbon patterns follow the repository's installed version. This work adopts the
current Carbon composition and accessibility approaches without a dependency
upgrade.

## Architecture Decisions

1. Keep `/analyzers` as the single setup entry surface.
2. Represent setup progress with saved-resource URLs, not component-only state.
3. Centralize query parsing, serialization, safe `returnTo`, and setup-route
   construction in pure route helpers.
4. Replace analyzer-only `PageTitle` usage with a reusable Carbon `PageHeader`.
5. Use a shared `AnalyzerSetupProgress` component for Instrument, Verify,
   Connect, and Review.
6. Use Carbon `DataTable` composition for mapping and profile tabular data.
7. Keep controllers focused on transport and services responsible for
   validation, transactions, fingerprints, audit, and bridge sync.
8. Keep QC on `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and Westgard.
9. Keep FILE runtime ownership on the bridge.

## Checkpoints

### C0 - Normative baseline

- Rebase on current `develop`.
- Rewrite the current spec, route contract, acceptance checklist, and roadmap.
- Reconcile Feature 004, Feature 012, Feature 014, and OGC-41 references.
- Update PR title/body to show current, not historical, status.

### C1 - URL and page-shell contract

- Write failing pure route-helper tests.
- Write failing page-header and list/profile URL-state tests.
- Implement canonical query serialization, safe return paths, semantic
  headings, breadcrumbs, and contextual actions.
- Remove analyzer/QC dependence on the old `PageTitle`.

### C2 - Guided setup

- Write failing RTL tests using real `MemoryRouter` navigation.
- Add route-aware `AnalyzerSetupProgress`.
- Navigate create → Verify from the returned analyzer ID.
- Preserve setup context through QC rule/control-lot detours.
- Add Connect and Review step routes with explicit readiness presentation.

### C3 - Carbon and responsive remediation

- Write failing component tests for table semantics, action accessibility, and
  narrow viewport behavior.
- Convert profile/mapping tables and status/actions to current Carbon patterns.
- Ensure primary actions remain available without incoherent overlap at desktop
  and mobile widths.

### C4 - Acceptance closure

- Run focused backend, frontend, formatting, lint, and Playwright guards.
- Run `digi-uw/code-qa` spec/code, coverage, simplicity, cross-repo, and
  evidence gates.
- Deploy the exact PR SHA to the analyzer UAT host.
- Synchronize stable Grist steps and run UI-only dry-run then video projects.
- Inspect screenshots, trace, console, runtime state, and compare with
  `openelis-work@4c0e1a28`.
- Replace pending evidence with the build-bound report and MP4 bundle.

## TDD Rule

Each behavioral checkpoint records:

1. the focused failing test;
2. the smallest passing implementation;
3. refactoring with the test still green;
4. final commands and evidence.

Unit/service tests own pure logic and backend contracts. RTL owns component
composition and route transitions. Playwright owns the complete visible user
story. Remote UAT owns review acceptance, not API correctness.

## Risks

- The PR is already large. Remediation must remove duplicate or obsolete paths
  when touched instead of adding parallel setup surfaces.
- Query state can diverge if components read `window.location` directly.
  Router location is the sole UI source.
- `returnTo` is an open-redirect risk. Only same-origin application paths are
  accepted.
- Older mocks describe persisted/forkable profile management and richer analyzer
  dashboards. Those are deliberately deferred and must not leak into MVP claims.
