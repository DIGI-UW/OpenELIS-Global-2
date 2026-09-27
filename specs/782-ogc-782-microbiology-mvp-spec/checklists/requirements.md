# Specification Quality Checklist: Microbiology MVP Workflow

**Purpose**: Validate that the microbiology product spec is clear enough for
planning and implementation without leaking implementation decisions into
Casey-owned product artifacts.
**Created**: 2026-06-27
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No unresolved clarification markers remain
- [x] Product behavior is separated from schema, API, route, service, and
      storage decisions
- [x] User value and lab workflow outcomes are stated before technical notes
- [x] Mockups and M-* design files are treated as workflow references, not
      implementation contracts
- [x] OpenELIS constitutional constraints are captured separately from feature
      behavior

## Requirement Completeness

- [x] Primary actors and workflows are represented
- [x] P1 MVP path covers order routing, case workbench, organism
      identification, manual AST, review readiness, and reporting
- [x] P2 shared worklist and critical communication behavior are represented
- [x] P3 WHONET/export readiness is represented without overclaiming scope
- [x] Key edge cases are listed
- [x] Success criteria are measurable or independently testable
- [x] Planning notes call out engineering decisions without making them product
      requirements

## Artifact Grounding

- [x] Confluence microbiology workflow page is referenced
- [x] openelis-work microbiology design bundle is referenced
- [x] Relevant M-* design/spec files are referenced
- [x] Jira references are listed at the roadmap/spec level
- [x] Local engineering crosswalk is referenced for technical decisions
- [x] Local cleanup list is referenced for product/implementation leakage review

## Feature Readiness

- [x] Spec can feed `/speckit.plan` without asking Casey to decide table names,
      service names, or API shapes
- [x] Spec has enough detail to identify first implementation milestones
- [x] Spec has enough detail to name useful tests during planning
- [x] Remaining ambiguity is intentionally moved to engineering planning rather
      than hidden in product requirements

## Notes

- This checklist validates the feature specification only. Architecture,
  milestone design, data model, API contracts, and task sequencing belong in
  the subsequent SpecKit planning artifacts.
