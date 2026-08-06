# Analyzer + Microbiology Engineering Crosswalk

**Audience:** Engineering planning and implementation
**Purpose:** Preserve technical decisions and repo constraints outside
Casey-owned product artifacts.

## Source Evidence

- Repo: current checkout root (`./`)
- Analyzer profile files: `projects/analyzer-profiles/{astm,hl7,file}/`
- Analyzer profile schema:
  `projects/analyzer-profiles/schema/analyzer-defaults-1.0.schema.json`
- Analyzer code: `src/main/java/org/openelisglobal/analyzer/`
- Current analyzer UI: `frontend/src/components/analyzers/`
- Pinned microbiology product authority:
  `https://github.com/DIGI-UW/openelis-work/tree/a1f720d7b3b01db63387361495f4aa6589105003/designs/microbiology`
- Source-to-code-to-UAT status:
  `specs/782-ogc-782-microbiology-mvp-spec/evidence/openelis-work-authoritative-alignment-2026-08-05.md`

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

| Topic                        | Current engineering direction                                                                  | Still open                                                                                        |
| ---------------------------- | ---------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| User-visible analyzer choice | Present ASTM, HL7, File first; hide generic plugin framing where possible                      | Exact IA and route ownership                                                                      |
| Profile authority            | Profiles are authoritative runtime templates today as JSON files under `ANALYZER_PROFILES_DIR` | Whether future authoring is OpenELIS UI, Bridge UI, git-backed files, DB-backed drafts, or hybrid |
| Profile application          | Snapshot on analyzer creation for this iteration                                               | Whether sectioned reapply is needed later                                                         |
| Mapping runtime              | Keep current per-analyzer runtime mappings; add reusable defaults carefully                    | Whether Bridge should own more traffic learning or mapping diagnostics                            |
| FILE mode                    | Do not add an OpenELIS app-side poller                                                         | Whether Bridge should expose watcher/config UI directly                                           |
| Bridge ownership             | Bridge owns transport/runtime adapter behavior today                                           | Whether Bridge should own profile runtime, diagnostics, and its own admin UI                      |

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

- A dedicated `org.openelisglobal.microbiology` backend module and Carbon React
  workflow exist through the M10 follow-on stack. They form a substantial
  vertical slice, not completion of the authoritative microbiology feature;
  human UAT is also still pending.
- Cases are anchored to `SampleItem + workflow` and can represent sibling
  workflows on one physical specimen.
- Test Catalog already has AMR/WHONET groundwork through `test_amr_config`,
  `whonet_antibiotic_codes`, and the existing AMR flag.
- Test Catalog culture workflow configuration and default Method support are
  implemented.
- The MVP stores AST runs/readings in the microbiology workflow and projects
  reviewed reportable interpretations into the standard Result/patient-report
  path.
- The M10 branch extends the existing report/export path with the first manual
  WHONET CSV slice; broader compatibility and delivery remain incomplete.
- The clinical call/read-back record is authoritative and projects lifecycle
  state into the existing generic Alert workflow.
- Configured Add Order uses `/order/enter`. The historical M-03 repair mounts
  only in legacy `/SamplePatientEntry`; modern `SampleTestSection` truncates
  selected tests to ID/name and loses culture workflow metadata.
- The M-04 workbench lacks workflow classification, subculture lineage, full
  two-pass isolate work, sibling navigation, and reuse of the existing
  non-conformance workflow.
- The M-05 AST flow lacks complete ordered-panel provenance, matched-breakpoint
  evidence, override/revert history, analyzer/QC review states, and scoped
  repeat behavior.
- The M-07 worklist has configured navigation, breadcrumbs, and canonical query
  state, but lacks the authoritative Culture/AST view switch, AST-run rows,
  resistance context, recent activity, and complete row actions.
- Shared reagent-lot behavior and the M-NFR accessibility, scale, audit, and
  intermittent-connectivity qualifications remain partial or unproven.

### As-Built MVP Decisions

| Topic                     | Engineering decision                                                                                                                                                                                          | Product-safe expression                                                                                              |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Workflow routing          | Test Catalog persists `culture_workflow_type`; case persists `workflow_type`; backend routing is implemented. R1 must preserve this metadata through modern order state and visibly derive Program/details    | The ordered test determines the microbiology workflow and visibly reveals the details needed for culture work        |
| Case anchor               | Case is keyed by one physical specimen workflow, implemented as `SampleItem + workflow`                                                                                                                       | A specimen can hold distinguishable sibling workflow records without duplicate accessioning                          |
| Culture protocol          | Use test default Method plus micro-specific method metadata                                                                                                                                                   | The selected test provides a default culture setup recipe                                                            |
| AST storage and reporting | Use microbiology AST runs/readings, then project reviewed reportable content into standard Result/patient reporting                                                                                           | Users enter AST readings, review interpretations, and see the result on the patient report                           |
| Final safety              | Keep the existing final-release mutation lock and add the authoritative amend/re-identify history workflow in R1                                                                                              | Final results cannot be silently changed; later corrections preserve what was released                               |
| Critical communications   | Keep the clinical critical log authoritative and synchronize its lifecycle with existing Alert records                                                                                                        | Critical communications are logged and surfaced in the existing operational alert workflow                           |
| WHONET                    | Reuse existing mapping/reference/report services. M9/M10 add organism/antibiotic administration and first manual export; wider packaging, remaining vocabularies, scheduling, and delivery remain future work | Users can validate and manually export the supported finalized result set; unsupported export depth remains explicit |

### Product Authority Versus Engineering Choice

OpenELIS Work is binding for visible workflow order, information, control
meaning, defaults, state transitions, and acceptance behavior. It is not
binding for schema, API, service, route names, or component structure. An
engineering implementation may differ internally, but it may not silently omit
the product behavior.

For M-03 this means the current Add Order path must visibly derive Program,
show the ruled Culture Method/details controls, confirm destructive workflow
changes, and persist to the case. Reusing a legacy route or choosing a shared
state abstraction is engineering work; whether the user sees the behavior is
not optional.

### Microbiology Remediation Boundary

- Deliver the M-03/M-04/M-05/M-07/M-12 and M-NFR drift repair in one official
  remediation PR stacked on the exact M10 head.
- Keep every roadmap status explicit about code, automated evidence, deployed
  review state, and human acceptance.
- Treat Macro Library runtime and administration as a separate cross-cutting
  feature, PR stack, UAT project, and preferably review deployment.
- Add only the later microbiology consumer integration to this feature after
  Macro Library is independently accepted.

### Microbiology Implementation Readiness Gate

Before a later micro slice begins, engineering should confirm:

- Whether the slice extends the current bacteriology workflow or introduces a
  distinct operational workflow such as TB.
- Whether it changes the standard patient-report projection or requires
  versioned amendment behavior.
- Which WHONET export columns and mapping administration belong in the export
  slice beyond current readiness.
- Whether reagent/card lot or expert-rule behavior requires new persistence.
- Whether a planned slice has a complete pinned-source -> requirement -> task
  -> code -> automated evidence -> Grist UAT trace.

## Spec Cleanup Workflow

1. Rewrite product titles and summaries around workflow outcomes.
2. Move table/class/route/service details into engineering notes or linked
   implementation tasks.
3. Keep product acceptance criteria observable by a user or reviewer.
4. Keep engineering tests in the crosswalk or implementation plan.
5. Re-check contradictions only when they change workflow, schema, API, or
   user-facing behavior.

## Non-Goals

- Do not create a governance process.
- Do not erase useful technical context; move it to the right artifact.
- Do not treat mockups as schema/API/component contracts; do treat their
  observable workflow behavior as authoritative unless a deviation is ruled.
- Do not make Bridge/OpenELIS ownership a product-spec decision.
