# Analyzer + Microbiology Roadmap Clarification

**Last updated:** 2026-06-27
**Status:** Draft for roadmap/spec cleanup
**Purpose:** Make the analyzer and microbiology roadmaps executable without
letting product artifacts prescribe implementation architecture.

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

## Analyzer Product Statement

Lab/admin users can choose an analyzer connection path, select or manage a
reusable analyzer profile, verify mappings against the test catalog, configure
exceptions, and safely set up an analyzer without needing developer support.

Product artifacts may name user-visible choices such as ASTM, HL7, and File.
They should not decide whether a profile is implemented as an OpenELIS entity,
a JSON file, Bridge-owned runtime config, a git-backed catalog, or a Bridge UI.

## Analyzer Engineering Questions to Carry Separately

- Should Bridge own more runtime/profile behavior than it owns today?
- Does Bridge need its own UI for diagnostics, profile/runtime state, traffic
  inspection, or connection health?
- What is the authoritative profile store for each environment: mounted files,
  git checkout, Bridge config, OpenELIS config, or a combination?
- Where should traffic learning, diagnostics, and unmapped-code resolution live?
- What is the right OpenELIS/Bridge contract for FILE mode beyond the visible
  setup workflow?

These are engineering architecture decisions. They should not be smuggled into
Casey's Jira titles, FRS language, or mockups.

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
