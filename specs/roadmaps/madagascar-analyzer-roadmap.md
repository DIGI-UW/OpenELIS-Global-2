# Madagascar Analyzer Integration Roadmap

**Last Updated**: 2026-04-20  
**Status**: MVP architecture shipped (three generic plugins on `develop`);
per-analyzer site validation + post-MVP architecture evolution open.  
**Confluence Tracker (canonical for per-analyzer status)**:
[OpenELIS Global — Analyzer Integration Tracker](https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1097531396)  
**Epic**: OGC-304

> This roadmap covers the **generic analyzer integration architecture**.
> Per-instrument status, spec confidence, vendor-doc availability, and site
> deployment state live on the Confluence tracker linked above — that's the
> single source of truth for "is analyzer X shipped, in validation, or blocked."
> This roadmap intentionally does not duplicate that table.
>
> This roadmap supersedes all previous versions (v1, v2, v3-finishline,
> v4-results-first).

---

## Architecture

All analyzer communication flows through the **openelis-analyzer-bridge**.
OpenELIS never connects directly to analyzers.

```
Analyzer → Bridge (MLLP/ASTM TCP/File Watch) → OE HTTP API → Results
```

Three **generic plugins** handle all analyzer types via JSON profiles:

- **GenericHL7** — HL7 v2.x over MLLP (Pattern A2)
- **GenericASTM** — ASTM LIS2-A2 over TCP (Pattern A)
- **GenericFile** — Flat-file import; CSV / TSV / XLSX / XML readers (Pattern C)

Patterns B (pipeline — e.g., MinION + TB-Profiler) and E (proprietary serial /
BCI — e.g., Stago, VIDAS) use dedicated adapters outside the three generic
plugins.

Profiles live in `projects/analyzer-profiles/{hl7,astm,file}/`. Each profile
defines identifier patterns, test mappings, and communication mode. Integrating
a new analyzer on an already-supported pattern is a **profile-JSON drop**;
per-instrument deployment status lives on the Confluence tracker.

### CommunicationMode

Each analyzer has a `communicationMode` field:

- **ANALYZER_INITIATED** (all current analyzers) — analyzer connects to bridge
- **LIS_INITIATED** (future) — OE connects to analyzer for queries/orders
- **BOTH** (future) — bidirectional

MVP implements ANALYZER_INITIATED only. This is a deliberate phased approach —
vendor docs confirm bidirectional capability for Mindray (OGC-326/327) and
GeneXpert HL7 (OGC-336).

---

## Current State (2026-04-20)

### Architecture / infrastructure merged to `develop`

| PR          | Feature                                                      | Status    |
| ----------- | ------------------------------------------------------------ | --------- |
| #3035       | HL7 MLLP listener + GenericHL7 plugin + initial profiles     | Merged    |
| #3103       | Bridge-owned file watcher                                    | Merged    |
| #3184       | FILE profile verification (sheet detection, locale handling) | Merged    |
| #3188       | Submodule pointers + chmod fix                               | Merged    |
| #3191       | `X-Analyzer-Id` for HL7 + MSH-3/MSH-4 pattern matching       | Merged    |
| #3195       | HL7 test-connection parity + `CommunicationMode` enum        | In Review |
| Mock #18    | HL7 MLLP push + profile adapter                              | Merged    |
| Mock #24    | Mock server fixes                                            | Merged    |
| Plugins #69 | Plugin fixes                                                 | Merged    |
| Plugins #72 | GenericHL7 MSH-3+MSH-4 extraction                            | Merged    |

### E2E verification (generic-plugin flows)

| Pattern | Plugin        | End-to-end flow exercised                      |
| ------- | ------------- | ---------------------------------------------- |
| A2      | `GenericHL7`  | Mock → MLLP → Bridge → OE → Results page       |
| A       | `GenericASTM` | Mock → ASTM TCP → Bridge → OE → Results page   |
| C       | `GenericFile` | File drop → Bridge watcher → OE → Results page |

All three flows are exercised by `analyzer-demo-flow.spec.ts` in the Playwright
suite. Per-profile fixture coverage (which specific analyzers are exercised at
which validation level) lives on the Confluence tracker.

**Architecture note**: the FILE path today parses files on the OE side
(`FileImportServiceImpl` + the plugin readers). Bridge forwards the raw binary.
Post-MVP unification (below) moves parsing to the bridge, delivering FHIR R4
Bundles to OE so OE becomes format-agnostic.

### Per-analyzer status

Per-instrument deployment status, spec confidence, real-file availability, and
site-validation progress live on the
[Confluence Analyzer Integration Tracker](https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1097531396).
That's the canonical source — do not duplicate here.

---

## Remaining Work (architecture-level only)

### Immediate (architecture + cross-cutting)

- [ ] PR #3195 merged (HL7 test-connection + `CommunicationMode` enum + review
      fixes)
- [ ] Add `communication` blocks to the remaining 8 ASTM/HL7 profiles (5 of 13
      have them today)
- [ ] `autoCreateTestMappings` fix — profile field naming (`analyzer_code` vs
      `analyzerCode`)

### Video evidence (one per generic plugin)

- [ ] Record GenericHL7 E2E demo flow
- [ ] Record GenericASTM E2E demo flow
- [ ] Record GenericFile E2E demo flow
- [ ] Package Playwright HTML reports for stakeholder review

### Post-MVP (architecture evolution)

- [ ] **Unified FHIR R4 bridge interface** — bridge parses all formats (ASTM,
      HL7, xlsx, CSV), sends FHIR R4 transaction Bundles (`DiagnosticReport` +
      `Observation`) to OE. OE becomes format-agnostic. See
      `specs/roadmaps/pr-3195-remediation-plan.md` Phase 3B.
- [ ] ASTM bidirectional (#3032, deferred) — query-initiated result requests
- [ ] HL7 bidirectional — `ORM^O01` worklist download + `QRY^Q02` order download
- [ ] GeneXpert HL7 mode (OGC-336) — QBP queries
- [ ] Bridge outbound MLLP / ASTM client (required for `LIS_INITIATED` mode)
- [ ] TLS consolidation — extract shared `BridgeSslUtil`, add
      `analyzer.bridge.tls.verify` config
- [ ] `@Scheduled` periodic bridge sync (currently only fires on OE startup)

### Not tracked here (see Confluence)

Per-instrument deployment work — HJRA site networking, per-analyzer field
validation, real-file collection, vendor-doc reviews, site-specific blockers,
individual analyzer spec/companion versioning — lives on the Confluence tracker,
not in this roadmap.

---

## Jira Tickets (architecture / shared infrastructure only)

| Ticket  | Summary                            | Scope                                    |
| ------- | ---------------------------------- | ---------------------------------------- |
| OGC-304 | Madagascar Analyzer Integration    | Epic                                     |
| OGC-325 | HL7 MLLP Listener Service          | Shared HL7 infrastructure (GenericHL7)   |
| OGC-324 | Analyzer File Upload + Review UI   | Shared Upload / Preview foundation       |
| OGC-329 | Flat File Config                   | Shared GenericFile config + registration |
| OGC-336 | GeneXpert HL7 Mode                 | Post-MVP — QBP query support             |
| OGC-573 | GeneXpert Host Query + Result Push | Post-MVP — bidirectional Xpert workflow  |

Per-analyzer Jira tickets (e.g. OGC-326 BS-series adapter, OGC-327 BC-5380,
OGC-335 GeneXpert ASTM, OGC-344/345 Wondfo, OGC-348 QuantStudio, OGC-417 Tecan
F50, OGC-418 Multiskan FC, OGC-420 FluoroCycler XT, OGC-350 Attune, and others)
are tracked on the
[Confluence Analyzer Integration Tracker](https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1097531396)
with spec/companion confidence ratings and deployment status.

---

## Key Specs

| Spec                                      | Scope                                                       |
| ----------------------------------------- | ----------------------------------------------------------- |
| `specs/012-generic-astm-plugin-profiles/` | ASTM generic plugin + profiles                              |
| `specs/013-hjra-hl7-stream-alignment/`    | HL7 stream coordination, readiness gates, CommunicationMode |
| `specs/014-hjra-file-stream-alignment/`   | FILE stream coordination (handled on separate machine)      |

---

## Risks (architecture-level)

| Risk                                       | Impact | Mitigation                                                                          |
| ------------------------------------------ | ------ | ----------------------------------------------------------------------------------- |
| `autoCreateTestMappings` field naming      | MED    | Results still stage (unmapped); manual mapping works; fix on post-MVP slate         |
| Bridge outbound not yet built              | LOW    | Post-MVP; `CommunicationMode` enum already prepares the data model                  |
| Spotless or format drift on profile schema | LOW    | CI format checks catch most; no schema-level contract yet for `communication` block |

Per-site and per-analyzer risks (HJRA networking readiness, instrument
availability, missing vendor exports for specific analyzers) are tracked on the
Confluence tracker.
