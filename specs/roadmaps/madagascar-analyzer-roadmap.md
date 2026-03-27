# Madagascar Analyzer Integration Roadmap

**Last Updated**: 2026-03-26 **Status**: MVP code complete, validation in
progress **Confluence Tracker**:
[OpenELIS Global — Analyzer Integration Tracker](https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1097531396)
**Epic**: OGC-304

> This is the canonical roadmap for Madagascar analyzer integration. It
> supersedes all previous versions (v1, v2, v3-finishline, v4-results-first).

---

## Architecture

All analyzer communication flows through the **openelis-analyzer-bridge**.
OpenELIS never connects directly to analyzers.

```
Analyzer → Bridge (MLLP/ASTM TCP/File Watch) → OE HTTP API → Results
```

Three **generic plugins** handle all analyzer types via JSON profiles:

- **GenericHL7** — HL7 v2.x over MLLP (Mindray BC-5380, BS-200, BS-300)
- **GenericASTM** — ASTM LIS2-A2 over TCP (GeneXpert, Sysmex)
- **GenericFile** — Flat file import (QuantStudio, FluoroCycler, Tecan,
  Multiskan)

Profiles live in `projects/analyzer-profiles/{hl7,astm,file}/`. Each profile
defines identifier patterns, test mappings, and communication mode.

### CommunicationMode

Each analyzer has a `communicationMode` field:

- **ANALYZER_INITIATED** (all current analyzers) — analyzer connects to bridge
- **LIS_INITIATED** (future) — OE connects to analyzer for queries/orders
- **BOTH** (future) — bidirectional

MVP implements ANALYZER_INITIATED only. This is a deliberate phased approach —
vendor docs confirm bidirectional capability for Mindray (OGC-326/327) and
GeneXpert HL7 (OGC-336).

---

## Current State (2026-03-26)

### Merged to develop

| PR          | Feature                                                        | Status    |
| ----------- | -------------------------------------------------------------- | --------- |
| #3035       | HL7 MLLP listener + BC-5380 + BS-200/BS-300 profiles           | Merged    |
| #3103       | Bridge-owned file watcher                                      | Merged    |
| #3184       | FILE profile verification (QS5 sheet detection, French locale) | Merged    |
| #3188       | Submodule pointers + chmod fix                                 | Merged    |
| #3191       | X-Analyzer-Id for HL7 + pattern matching                       | Merged    |
| #3195       | HL7 test-connection fix + CommunicationMode enum               | In Review |
| Mock #18    | HL7 MLLP push + profile adapter                                | Merged    |
| Mock #24    | Mock server fixes                                              | Merged    |
| Plugins #69 | Plugin fixes                                                   | Merged    |
| Plugins #72 | GenericHL7 MSH-3+MSH-4 extraction                              | Merged    |

### E2E Verified Locally

| Lane | Analyzers                        | Flow                                           | Video    |
| ---- | -------------------------------- | ---------------------------------------------- | -------- |
| HL7  | BC-5380, BS-200, BS-300          | Mock → MLLP → Bridge → OE → Results page       | Recorded |
| ASTM | GeneXpert                        | Mock → ASTM TCP → Bridge → OE → Results page   | Ready    |
| FILE | QuantStudio 5/7, FluoroCycler XT | File drop → Bridge watcher → OE → Results page | Ready    |

All 7 analyzers covered in unified `analyzer-demo-flow.spec.ts` (FILE analyzers
restored from commented-out state with correct fixture paths).

**Architecture note**: FILE path currently has OE parsing xlsx directly via
`ExcelAnalyzerReader`. Bridge detects and forwards the raw binary. Post-MVP
unification will move parsing to bridge, delivering FHIR R4 Bundles to OE.

### Per-Analyzer Status

| Analyzer             | Protocol     | Profile                | Status             | Notes                                  |
| -------------------- | ------------ | ---------------------- | ------------------ | -------------------------------------- |
| **Mindray BC-5380**  | HL7 v2.3.1   | `hl7/mindray-bc5380`   | E2E verified       | 13 test mappings, OGC-327              |
| **Mindray BS-200**   | HL7 v2.3.1   | `hl7/mindray-bs200`    | E2E verified       | 8 test mappings, OGC-326               |
| **Mindray BS-300**   | HL7 v2.3.1   | `hl7/mindray-bs300`    | E2E verified       | 8 test mappings, OGC-326               |
| **GeneXpert (ASTM)** | ASTM LIS2-A2 | `astm/genexpert-astm`  | E2E verified       | OGC-335                                |
| **QuantStudio 5**    | FILE/Excel   | `file/quantstudio`     | WORKING at LA2M    | OGC-348                                |
| **QuantStudio 7**    | FILE/Excel   | `file/quantstudio`     | WORKING at LA2M    | OGC-348                                |
| **FluoroCycler XT**  | FILE/Excel   | `file/fluorocycler-xt` | WORKING at LA2M    | OGC-420                                |
| **Tecan F50**        | FILE/Excel   | `file/tecan-f50`       | PENDING validation | Herbert testing, OGC-417               |
| **Multiskan FC**     | FILE/CSV     | `file/multiskan-fc`    | PENDING validation | Herbert testing, OGC-418               |
| **Wondfo Finecare**  | ASTM + CSV   | Not started            | Not started        | OGC-344/345                            |
| **Attune CytPix**    | N/A          | N/A                    | DEPRIORITIZED      | Research instrument, no LIS connection |

---

## Remaining Work

### Immediate (this week)

- [ ] PR #3195 merged (HL7 test-connection + CommunicationMode + Copilot review
      fixes)
- [ ] HJRA site networking configured (Mindray analyzers → bridge MLLP)
- [ ] Tecan F50 / Multiskan FC profiles validated with Herbert's site samples
- [ ] Wondfo Finecare assessment (ASTM capture needed)
- [ ] Add `communication` blocks to remaining 7 ASTM/HL7 profiles (only 5 of 12
      have them)

### Video Evidence

- [ ] Record HL7 E2E demo (BC-5380 → MLLP → Bridge → OE → Accept results)
- [ ] Record ASTM E2E demo (GeneXpert → Bridge → OE → Accept results)
- [ ] Record FILE E2E demo (QuantStudio file drop → OE → Accept results)
- [ ] Package Playwright HTML reports for stakeholder review

### Post-MVP

- [ ] **Unified FHIR R4 bridge interface** — bridge parses all formats (ASTM,
      HL7, xlsx, CSV), sends FHIR R4 transaction Bundles (DiagnosticReport +
      Observation) to OE. OE becomes format-agnostic. See
      `specs/roadmaps/pr-3195-remediation-plan.md` Phase 3B.
- [ ] ASTM bidirectional (#3032, deferred) — query-initiated result requests
- [ ] HL7 bidirectional — ORM^O01 worklist download (OGC-327), QRY^Q02 order
      download (OGC-326)
- [ ] GeneXpert HL7 mode (OGC-336) — QBP queries
- [ ] Bridge outbound MLLP/ASTM client (required for LIS_INITIATED mode)
- [ ] `autoCreateTestMappings` fix — profile field naming (analyzer_code vs
      analyzerCode)
- [ ] Stago ST art (instrument down on-site)
- [ ] DNA Technology DT-Prime XML parser
- [ ] TLS consolidation — extract shared `BridgeSslUtil`, add
      `analyzer.bridge.tls.verify` config
- [ ] `@Scheduled` periodic bridge sync (currently only fires on OE startup)

---

## Jira Tickets

| Ticket  | Summary                   | Status                                 |
| ------- | ------------------------- | -------------------------------------- |
| OGC-325 | HL7 MLLP Listener Service | Code merged, HJRA validation pending   |
| OGC-326 | BS-Series HL7 Adapter     | Code merged, HJRA validation pending   |
| OGC-327 | BC-5380 HL7 Adapter       | Code merged, HJRA validation pending   |
| OGC-329 | Flat File Config          | Code merged, LA2M working              |
| OGC-335 | GeneXpert ASTM            | Code merged, VM testing planned        |
| OGC-348 | QuantStudio 5/7           | WORKING at LA2M                        |
| OGC-417 | Tecan F50                 | Profile on develop, pending validation |
| OGC-418 | Multiskan FC              | Profile on develop, pending validation |
| OGC-420 | FluoroCycler XT           | WORKING at LA2M                        |

---

## Key Specs

| Spec                                      | Scope                                                       |
| ----------------------------------------- | ----------------------------------------------------------- |
| `specs/012-generic-astm-plugin-profiles/` | ASTM generic plugin + profiles                              |
| `specs/013-hjra-hl7-stream-alignment/`    | HL7 stream coordination, readiness gates, CommunicationMode |
| `specs/014-hjra-file-stream-alignment/`   | FILE stream coordination (handled on separate machine)      |

---

## Risks

| Risk                                | Impact | Mitigation                                           |
| ----------------------------------- | ------ | ---------------------------------------------------- |
| HJRA networking not ready           | HIGH   | Code is merged; can test locally via harness mock    |
| autoCreateTestMappings field naming | MED    | Results still stage (unmapped); manual mapping works |
| Wondfo Finecare no ASTM capture     | MED    | CSV fallback path available                          |
| Bridge outbound not yet built       | LOW    | Post-MVP; CommunicationMode enum prepares data model |
