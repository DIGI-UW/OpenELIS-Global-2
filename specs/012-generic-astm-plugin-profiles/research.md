# Research: Generic ASTM Plugin Profiles v1.2

## Scope

Research focused on current GeneXpert workflow readiness and how to implement
FR-014..FR-025 without duplicating existing analyzer infrastructure.

## Decision 1: Treat current harness GeneXpert path as the primary regression gate

- **Decision**: Use the existing analyzer harness workflow as the canonical
  validation path for v1.2 rollout.
- **Rationale**: Recent merged work already stabilizes this chain: OE -> bridge
  -> mock/real GeneXpert with contention handling and realistic timing.
- **Evidence**:
  - Superproject commits: `f75556556`, `617155847`, `40dba1510`
  - Bridge submodule HEAD: `6200254` (timeout/contention fixes)
  - Mock submodule HEAD: `4c8f1aa` (TCP listener + simulation API concurrently)
- **Alternatives considered**:
  - Build a new isolated simulator path for 012 only (rejected: duplicates 011
    investments).
  - Rely on unit tests only (rejected: misses real bridge and socket behavior).

## Decision 2: Evolve existing defaults system into profile library semantics

- **Decision**: Reuse existing defaults API and `projects/analyzer-defaults`
  content as the seed for Built-in profiles.
- **Rationale**: Current flow already supports loading curated analyzer
  templates (including GeneXpert ASTM), reducing migration risk.
- **Evidence**:
  - Existing endpoints: `GET /rest/analyzer/defaults`,
    `GET /rest/analyzer/defaults/{protocol}/{name}`
  - Existing defaults include
    `projects/analyzer-defaults/astm/genexpert-astm.json`.
- **Alternatives considered**:
  - Introduce brand-new profile ingestion path with no compatibility bridge
    (rejected: high migration and UX churn).

## Decision 3: Preserve strict separation of profile defaults vs analyzer instance runtime fields

- **Decision**: Keep snapshot-on-apply and no live linkage after analyzer save.
- **Rationale**: Clarified requirement and existing architecture trend already
  moved in this direction (instance identity/connection separated from plugin
  defaults).
- **Evidence**:
  - Clarification in `spec.md` (`FR-024`, reapply out of scope).
  - Analyzer form updates from prior commits that separate plugin-level and
    instance-level values.
- **Alternatives considered**:
  - Live-linked profile inheritance (rejected: operational unpredictability and
    migration complexity).

## Decision 4: Extend current mapping/simulator surface instead of creating a new parser stack

- **Decision**: Implement simulator and advanced mapping behavior by extending
  the existing `preview-mapping` endpoint and field mapping APIs/UI. No new
  `/simulate` endpoint; the existing
  `POST /rest/analyzer/analyzers/{id}/preview-mapping` gains v1.2 outputs
  (transform results, QC rule evaluation, extraction override application,
  aggregation mode, abnormal flag mapping, unmapped code warnings).
- **Rationale**: Existing analyzer mapping controller and frontend already
  provide preview/copy/validation workflows. Adding a parallel endpoint would
  create duplicate logic.
- **Evidence**:
  - Existing endpoint: `POST /rest/analyzer/analyzers/{id}/preview-mapping`
  - Existing UI modules: `frontend/src/components/analyzers/FieldMapping/*`
- **Alternatives considered**:
  - New standalone simulator service and UI (rejected: duplicate logic and
    maintenance cost).
  - New `/simulate` endpoint alongside `preview-mapping` (rejected: contradicts
    guardrail #2 and creates two paths for the same capability).

## Decision 5: Maintain plugin-type test mapping model while adding analyzer-specific runtime overlays

- **Decision**: Keep `analyzer_test_map` as plugin-type-level mapping baseline;
  add v1.2 runtime config entities for per-analyzer behavior (transforms, QC
  rules, extraction, aggregation, flags, pending code queue).
- **Rationale**: 011 intentionally moved mappings to analyzer type to remove
  phantom analyzer coupling; reverting would regress that architecture.
- **Evidence**:
  - Liquibase migration: `3.4.x.x/009-decouple-test-mappings.xml`
  - Plugin submodule cleanup commit removing static analyzer ID coupling.
- **Alternatives considered**:
  - Re-key all mappings back to analyzer instance (rejected: conflicts with 011
    model and introduces duplication).

## Decision 6: Enforce role boundaries at API level from the start

- **Decision**: Apply RBAC checks on all new profile/config endpoints:
  `LAB_USER` read-only, `LAB_SUPERVISOR` configure/simulate/activate,
  `LAB_ADMIN` destructive profile actions.
- **Rationale**: Explicitly clarified in spec and required by Constitution
  CR-007.
- **Alternatives considered**:
  - Defer RBAC enforcement to later hardening pass (rejected:
    security/compliance risk).

## Decision 7: Keep implementation within existing module boundaries

- **Decision**: Add valueholders/DAOs/services/controllers/forms under existing
  analyzer package; no new service boundary.
- **Rationale**: Matches existing project architecture and reduces integration
  cost.
- **Alternatives considered**:
  - New module or separate microservice for profiles (rejected: disproportionate
    complexity for current scope).

## Decision 8: Use GeneXpert-first acceptance to prevent overengineering

- **Decision**: Each milestone must prove one concrete GeneXpert scenario before
  expanding generic breadth.
- **Rationale**: User requested focus on currently tested workflow and concise,
  valid plan.
- **Alternatives considered**:
  - Build full generic abstraction first then validate later (rejected: delays
    feedback and increases risk).

## Known Risks and Mitigations

1. **Risk**: Schema overlap confusion between existing analyzer mapping tables
   and new v1.2 config tables.  
   **Mitigation**: Define strict ownership in `data-model.md` and enforce
   through service boundaries.

2. **Risk**: UI scope creep across many analyzer tabs.  
   **Mitigation**: Prioritize workflows already exercised by GeneXpert path;
   defer non-critical polish.

3. **Risk**: Regressing test-connection behavior via bridge.  
   **Mitigation**: Run harness regression gate in every milestone merge cycle.

4. **Risk**: Profile version semantics drift (`id` lineage vs `version`
   artifact).  
   **Mitigation**: Encode uniqueness and designated-latest behavior in DB
   constraints and service tests.
