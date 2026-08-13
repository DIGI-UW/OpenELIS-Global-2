# OGC-1054 Analyzer QC/Configuration Foundation Plan

**Status:** Historical implementation plan for PR #3792
**Current roadmap:**
[OGC-1054 Analyzer Feature](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)

## Purpose

This plan records the branch-local implementation that produced the Analyzer
QC/configuration foundation. It is not the implementation plan for reusable
Analyzer Types, complete mapping, safe traffic learning, or the full MVP.

## Source Boundary

`openelis-work` is functional/visual reference material only. It does not
govern the technical context, architecture, persistence, APIs, routes, runtime
ownership, migration, or tests in this plan. Those decisions are grounded in
current repository code, engineering specifications, `AGENTS.md`, and explicit
contracts.

## Foundation Technical Context

The branch extended the existing Java 21/Spring MVC analyzer services and React
17/Carbon application. It used the current create-time profile bootstrap,
per-analyzer mappings/plugin configuration, existing analyzer audit services,
and existing operational QC entities. Those choices describe the branch; they
are not the target reusable-profile architecture.

## Branch Decisions

1. Use `/analyzers` as the foundation setup entry.
2. Represent saved-analyzer setup progress with URL state.
3. Centralize branch query parsing, safe `returnTo`, and route construction.
4. Use reusable Carbon page header, progress, and table components.
5. Keep controllers focused on transport and services on validation,
   transaction, fingerprint, audit, readiness, and Bridge sync behavior.
6. Keep operational QC on `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and
   Westgard.
7. Keep FILE runtime ownership on Bridge.

## Completed Foundation Checkpoints

### C0 - Branch baseline

- Reconciled the then-current branch spec, route contract, and checklist.
- Ran focused analyzer/QC baselines.

### C1 - URL and Carbon page shell

- Added route/query helpers, URL-backed filters, semantic headings, linked
  breadcrumbs, and shared Carbon composition.

### C2 - Setup shell

- Added Instrument, Verify, Connect, and Review routes.
- Preserved saved analyzer context through QC rule/control-lot detours.

### C3 - Mapping and responsive safeguards

- Added catalog-bound result-value selection and Carbon tabular composition.
- Exercised desktop/mobile foundation layouts.

### C4 - Historical foundation evidence

- Ran branch-focused automated validation.
- Deployed application SHA `2c840a55b03b238a2ad00c987181504c2bef6ef6`.
- Recorded the eight-step foundation Grist story and MP4.

This evidence did not include reusable profile lifecycle, complete mapping,
production unknown-traffic capture/hold, or mock-to-Bridge result flow, so it
does not accept the OGC-1054 MVP.

## Current Disposition

Follow F0 in the authoritative roadmap:

1. rebase and range-diff PR #3792 against current `develop`;
2. retain compatible foundation work without extending the large PR;
3. remove or migrate touched legacy/duplicate paths;
4. merge the branch as a clearly named foundation or extract its reusable
   commits into the milestone PRs and supersede it.

## TDD Rule

Future feature work follows the test ownership and red-green-refactor gates in
the authoritative roadmap. Branch-level component/API tests cannot stand in for
Bridge transport contracts, integrated harness tests, or UI-only MVP evidence.
