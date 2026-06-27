# Analyzer + Microbiology Engineering Crosswalk

**Last updated:** 2026-06-27
**Audience:** Engineering planning and implementation
**Purpose:** Preserve technical decisions and repo constraints outside
Casey-owned product artifacts.

## Source Evidence

- Repo: `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`
- Analyzer profile files: `projects/analyzer-profiles/{astm,hl7,file}/`
- Analyzer profile schema:
  `projects/analyzer-profiles/schema/analyzer-defaults-1.0.schema.json`
- Analyzer code: `src/main/java/org/openelisglobal/analyzer/`
- Current analyzer UI: `frontend/src/components/analyzers/`
- Microbiology Confluence narrative:
  `https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1315209256`
- Public design repo:
  `https://github.com/DIGI-UW/openelis-work/tree/main/designs/microbiology`

## Analyzer Engineering Crosswalk

### Current Repo Reality

- `AnalyzerType` is an existing OpenELIS plugin/protocol capability model.
- Bundled analyzer setup profiles are JSON files under
  `projects/analyzer-profiles`.
- OpenELIS currently reads profiles from `ANALYZER_PROFILES_DIR`, defaulting to
  `/data/analyzer-profiles`.
- Current analyzer creation applies a profile as a one-time template:
  profile defaults seed analyzer instance config, test mappings, FILE config,
  QC rules, and bridge registration.
- Runtime mappings are currently per analyzer, not purely per profile/type.

### Engineering Decisions to Keep Out of Product Specs

| Topic | Current engineering direction | Still open |
| --- | --- | --- |
| User-visible analyzer choice | Present ASTM, HL7, File first; hide generic plugin framing where possible | Exact IA and route ownership |
| Profile authority | Profiles are authoritative runtime templates today as JSON files under `ANALYZER_PROFILES_DIR` | Whether future authoring is OpenELIS UI, Bridge UI, git-backed files, DB-backed drafts, or hybrid |
| Profile application | Snapshot on analyzer creation for this iteration | Whether sectioned reapply is needed later |
| Mapping runtime | Keep current per-analyzer runtime mappings; add reusable defaults carefully | Whether Bridge should own more traffic learning or mapping diagnostics |
| FILE mode | Do not add an OpenELIS app-side poller | Whether Bridge should expose watcher/config UI directly |
| Bridge ownership | Bridge owns transport/runtime adapter behavior today | Whether Bridge should own profile runtime, diagnostics, and its own admin UI |

### Analyzer Implementation Readiness Gate

Before changing analyzer product tickets, engineering should answer:

- What does OpenELIS need to know to set up an analyzer?
- What does Bridge need to own to run and diagnose the connection?
- Which profile fields are runtime adapter config versus OpenELIS catalog
  mapping?
- What state must be visible in OpenELIS, Bridge UI, or both?
- What is the migration path from current per-analyzer mappings?

Do not encode those answers in Casey-facing tickets until engineering has made
and documented the architecture decision.

## Microbiology Engineering Crosswalk

### Current Repo Reality

- There is no dedicated microbiology backend module yet.
- `SampleItem` exists and is the correct granularity to evaluate for case
  anchoring.
- Test Catalog already has AMR/WHONET groundwork through `test_amr_config`,
  `whonet_antibiotic_codes`, and the existing AMR flag.
- `test_method` exists and supports a default Method per test.
- Result components exist and include `allow_multiple_readings`, but AST
  storage must still be proven against result-entry/reporting behavior.
- Existing WHONET export services exist and should be extended before building
  a parallel exporter.
- Generic `Alert` infrastructure exists, but M-11 clinical call/read-back
  logging may still need its own audited log model.

### Engineering Defaults

| Topic | Engineering default | Product-safe expression |
| --- | --- | --- |
| Workflow routing | Test Catalog field likely persists as `culture_workflow_type`; case field likely persists as `workflow_type` | The ordered test determines the microbiology workflow |
| Case anchor | Case likely keyed by one physical specimen workflow, implemented as `SampleItem + workflow` | A specimen can have separate bacteriology and TB workups without duplicate accessioning |
| Culture protocol | Use test default Method plus micro-specific method metadata | The selected test provides a default culture setup recipe |
| AST storage | Use AST run/header plus existing result rows/components if proven feasible | Users enter AST readings and see interpretations with override history |
| Critical communications | Use clinical critical log plus existing alerts surface | Critical communications are logged and surfaced in the existing operational alert workflow |
| WHONET | Extend existing WHONET services and Test Catalog mapping data | Finalized microbiology cases can be exported for surveillance |

### Microbiology Implementation Readiness Gate

Before a micro slice begins, engineering should confirm:

- Which existing service or hook owns order-save side effects.
- How a case DTO can compile SampleItem, patient, test, method, timeline, and
  result data inside service transactions.
- Whether existing result components/multiple readings can represent AST
  without breaking validation/reporting.
- Which critical notification fields are clinical record requirements versus
  dashboard alert metadata.
- Which WHONET columns can be populated from existing export services and which
  require new micro data.

## Spec Cleanup Workflow

1. Rewrite Casey-facing Jira titles and summaries around workflow outcomes.
2. Move table/class/route/service details into engineering notes or linked
   implementation tasks.
3. Keep product acceptance criteria observable by a user or reviewer.
4. Keep engineering tests in the crosswalk or implementation plan.
5. Re-check contradictions only when they change workflow, schema, API, or
   user-facing behavior.

## Non-Goals

- Do not create a governance process.
- Do not erase useful technical context; move it to the right artifact.
- Do not treat mockups as implementation contracts.
- Do not make Bridge/OpenELIS ownership a product-spec decision.
