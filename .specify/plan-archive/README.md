# Plan Archive

Retired specs, plans, and research documents. Files are archived (not deleted)
so their history stays discoverable to agents doing archaeology. Git `mv`
preserves blame.

If you land here looking for the live state of the Madagascar analyzer fleet,
stop and read instead:

- **Canonical roadmap:** `specs/roadmaps/madagascar-analyzer-roadmap.md`
- **Analyzer seed authority:** `projects/analyzer-harness/seed-analyzers.sh`
- **Live profile set:** `projects/analyzer-profiles/{astm,hl7,file}/*.json`
  (distro `configs/analyzer-profiles/` is the source of truth; repo copy is a
  mirror)
- **Unified form:**
  `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx`
- **E2E harness:**
  `frontend/playwright/tests/demo/harness/analyzer-demo-flow.spec.ts`
  - `file-import-results.spec.ts`

## Index

| Archived path                                 | Original location                                                      | Retired    | Why retired                                                                                                                                                                                                           |
| --------------------------------------------- | ---------------------------------------------------------------------- | ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `fix-011-analyzer-config-form.md`             | unknown (pre-2026-04)                                                  | earlier    | Feature shipped                                                                                                                                                                                                       |
| `parallel_analyzer_lanes_af342372.plan.md`    | `specs/roadmaps/`                                                      | 2026-04-18 | Superseded by `specs/roadmaps/madagascar-analyzer-roadmap.md` (self-declared canonical 2026-03-26)                                                                                                                    |
| `madagascar-profile-streams-roadmap.md`       | `specs/roadmaps/`                                                      | 2026-04-18 | v1 roadmap; superseded by the v2 canonical roadmap                                                                                                                                                                    |
| `012-013-014-status-2026-03-10.md`            | `specs/roadmaps/`                                                      | 2026-04-18 | Point-in-time status snapshot; reality has moved on                                                                                                                                                                   |
| `011-audit-remediation.md`                    | `specs/011-madagascar-analyzer-integration/plans/audit-remediation.md` | 2026-04-18 | Remediation delivered; historical log                                                                                                                                                                                 |
| `011-mock-server-astm-remediation.md`         | `specs/011-.../plans/mock-server-astm-remediation.md`                  | 2026-04-18 | Remediation shipped; mock server runs `tools/analyzer-mock-server/` with dynamic networks                                                                                                                             |
| `011-protocol-version-enum.md`                | `specs/011-.../plans/protocol-version-enum.md`                         | 2026-04-18 | Enum added; delivery history                                                                                                                                                                                          |
| `011-genexpert-astm-e2e-readiness.md`         | `specs/011-.../plans/genexpert-astm-e2e-readiness.md`                  | 2026-04-18 | GeneXpert ASTM flow validated in `analyzer-demo-flow.spec.ts`                                                                                                                                                         |
| `011-universal-analyzer-bridge-v2.md`         | `specs/011-.../plans/universal-analyzer-bridge-v2.md`                  | 2026-04-18 | Pre-transparent-FHIR-pipe design; superseded by current bridge contract                                                                                                                                               |
| `011-pre-implementation-analysis.md`          | `specs/011-.../research/pre-implementation-analysis.md`                | 2026-04-18 | D1/D2 architectural decisions fossilised into code                                                                                                                                                                    |
| `011-xml-hibernate-migration.md`              | `specs/011-.../research/xml-hibernate-migration.md`                    | 2026-04-18 | Migration complete                                                                                                                                                                                                    |
| `011-plugin-system-unification-assessment.md` | `specs/011-.../research/plugin-system-unification-assessment.md`       | 2026-04-18 | PR #2802 merged                                                                                                                                                                                                       |
| `011-supported-analyzers.md`                  | `specs/011-.../contracts/supported-analyzers.md`                       | 2026-04-18 | Described pre-March fleet (13 required + per-instrument plugin JARs + "GenericFile Plugin: Deferred"). Current architecture is three generic plugins driven by JSON profiles; fleet is tracked in `seed-analyzers.sh` |
| `014-file-analyzer-sprint.md`                 | `.specify/prompts/014-file-analyzer-sprint.md`                         | 2026-04-18 | Sprint closed; profiles shipped                                                                                                                                                                                       |

## Not archived, but bannered

See `.specify/audit-reports/analyzer-specs-reckoning-20260418.md` for the full
classification. Files marked ⚠ DRIFT in areas 011, 013, 014 were bannered in
place with a pointer to the canonical live source rather than archived.

## Pending follow-up

Once PR #3372 merges, the four file-import-profile iteration plans
(`madagascar-file-import-profile-alignment-{v2,v3,v4}.md`) will also belong
here. Only v5 + the base `madagascar-file-import-profile-alignment.md` should
remain live.
