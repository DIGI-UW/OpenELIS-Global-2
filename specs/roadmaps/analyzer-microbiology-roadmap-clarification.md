# Analyzer + Microbiology Roadmap Clarification

**Last updated:** 2026-06-27
**Status:** Analyzer guidance superseded 2026-09-16; microbiology draft retained
**Purpose:** Make the analyzer and microbiology roadmaps executable without
letting product artifacts prescribe implementation architecture.

Microbiology material below is retained from June 27 and has not been
reassessed in this analyzer cleanup.

## Core Rule

Casey's artifacts should describe lab workflow, user intent, observable
behavior, acceptance criteria, and product dependencies.

Engineering artifacts should carry repo constraints, ownership boundaries,
schema/API proposals, implementation order, migration strategy, and test plans.

Mockups are visual workflow aids. They are not binding authority for schema,
API shape, route structure, component boundaries, service ownership, or Bridge
versus OpenELIS responsibilities.

## Artifact Boundaries

| Artifact type | Should contain | Should not contain |
| --- | --- | --- |
| Jira epic/story | Actor, workflow outcome, acceptance behavior, product dependency | Table names, service/class names, route mandates, schema shape, persistence ownership, framework choices |
| Product spec / FRS | Lab behavior, business rules, terminology, UX flow, exception behavior | Required entities, DAOs, controllers, endpoint names, specific columns, implementation sequence |
| Mockup | Interaction shape, information hierarchy, labels, visible states | Binding component library details beyond project standards, data model, API contract, backend ownership |
| Engineering crosswalk | Repo state, architecture decisions, schema/API options, tests, migration plan | New product scope, hidden product requirements, Casey-owned workflow rulings |

## Analyzer Guidance — Superseded

Use the [authoritative analyzer roadmap](./ogc-1054-analyzer-feature-roadmap.md), including the
[OGC-1220 remediation](./ogc-1054-analyzer-feature-roadmap.md#ogc-1220-held-result-remediation). It records the adopted ownership
model and implementation direction. The former open analyzer questions remain
in Git history and are no longer implementation instructions.

## Microbiology Product Statement

Microbiology users can route a culture order, create and work a case, record
culture progress, identify isolates, enter AST, review/report results, and
produce surveillance outputs using one coherent lab workflow.

Product artifacts should describe the lab work and acceptance behavior. They
should not require a specific table layout, service name, route name, result
storage shape, or backend ownership model.

## Microbiology Engineering Notes to Carry Separately

- Case keying and workflow routing belong in the engineering crosswalk.
- Method reuse for culture protocols belongs in the engineering crosswalk.
- AST run/result storage belongs in the engineering crosswalk.
- Critical notification architecture belongs in the engineering crosswalk.
- WHONET reuse belongs in the engineering crosswalk.

Those decisions are important, but they are not the feature. The feature is the
observable microbiology workflow.

## Spec Health Pass

For analyzer and microbiology artifacts, classify findings as:

| Classification | Meaning |
| --- | --- |
| Product gap | Actor, workflow, outcome, acceptance behavior, or dependency is missing |
| Implementation leakage | Table, class, route, service, schema, framework, or ownership decision appears as if it is the feature |
| Real contradiction | Product behavior conflicts across Jira, Confluence, mockups, or repo reality |
| Engineering decision needed | A technical choice is needed, but product artifacts should not make it |

Each cleanup row should include:

- artifact
- problematic wording
- why it is risky
- product-safe rewrite
- engineering note to carry separately

## Roadmap Outputs

- `specs/roadmaps/analyzer-spec-health-cleanup-list.md`
- `specs/roadmaps/microbiology-spec-health-cleanup-list.md`
- `specs/roadmaps/analyzer-microbiology-engineering-crosswalk.md`

## Operating Assumptions

- Casey owns product and workflow intent.
- Piotr owns final engineering direction.
- Bridge/OpenELIS ownership is an engineering architecture decision.
- Mockups are visual workflow aids, not implementation contracts.
- Jira/spec cleanup should reduce technical prescription, not remove useful
  acceptance criteria.
