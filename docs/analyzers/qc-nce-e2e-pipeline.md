# QC → Westgard → NCE: end-to-end pipeline

How a quality-control result travels from a (simulated) analyzer all the way to
an auto-created Non-Conforming Event (NCE), and how to drive and verify it.

This spans **three repositories**:

| Repo                                                                                 | Role                                                                |
| ------------------------------------------------------------------------------------ | ------------------------------------------------------------------- |
| `analyzer-mock-server` (`~/Dev/analyzer-mock-server`)                                | Simulates an ASTM/HL7/FILE analyzer; generates + pushes QC messages |
| `openelis-analyzer-bridge` (`~/Dev/openelis-analyzer-bridge`, container `oe-bridge`) | Receives analyzer messages, converts to FHIR R4, forwards to OE     |
| `OpenELIS-Global-2` (this repo)                                                      | Ingests FHIR, runs Westgard evaluation, auto-creates NCEs           |

> The `tools/analyzer-mock-server` and `tools/openelis-analyzer-bridge` git
> submodules in this repo can lag the standalone checkouts that build the
> running containers. When reasoning about runtime behavior, read the standalone
> `~/Dev/...` checkouts, not the submodules.

---

## 1. Architecture at a glance

```
 mock server                    bridge (oe-bridge)                 OpenELIS-Global
 ───────────                    ──────────────────                 ───────────────
 server.py --qc --push  ──ASTM/TCP──▶  ASTM/HL7/FILE listener
   (template qc_controls)    :12001        │
                                           │ FhirRoutingConfig.useFhir = true (default)
                                           ▼
                                    ASTMResultParser  (detects QC: O.12 == "Q")
                                    FhirBundleBuilder (meta.tag "QC" + lot/level extensions)
                                           │
                                           └──HTTP POST──▶  POST /analyzer/fhir
                                                            AnalyzerFhirImportController
                                                              │ meta.tag "QC" ⇒ isControl
                                                              ▼
                                                            QCResultProcessingService.processQCResult
                                                              ▼
                                                            QCResultServiceImpl.createQCResult
                                                              │ persists QCResult + z-score
                                                              │ publishes QCResultCreatedEvent
                                                              ▼
                                    (async, AFTER_COMMIT, REQUIRES_NEW)
                                    QCResultCreatedEventListener
                                      → evaluateAllRules → per violated rule:
                                          QCRuleViolationServiceImpl.createViolation
                                            → createAlertsForViolation
                                            → if severity == REJECTION:
                                                QcViolationNceServiceImpl.createNceForViolation
                                                  → NcEvent (CRITICAL / Pending) + linked samples
```

Key принцип: **QC only reaches Westgard/NCE through the FHIR ingest path
(`/analyzer/fhir`).** The legacy `/analyzer/astm` reader deliberately does _not_
run QC — see `ASTMAnalyzerReader` ("QC processing is now handled by the FHIR
import pipeline"). The bridge's default FHIR routing is what makes ASTM QC work.

---

## 2. The mock server — templates & QC generation

Repo: `~/Dev/analyzer-mock-server`. Templates live in `templates/*.json`.

### Template shape (relevant parts)

```jsonc
{
  "analyzer":   { "name": "Horiba ABX Pentra 60", "category": "HEMATOLOGY" },
  "protocol":   { "type": "ASTM", "version": "LIS2-A2" },
  "identification": { "astm_header": "ABX^PENTRA60^V2.0" },   // matched to OE analyzer.identifier_pattern
  "fields":     [ { "code": "WBC", "loinc": "6690-2", "unit": "10^3/uL", ... }, ... ],
  "qc_controls": [                                            // ← QC analytes come from HERE
    { "field_code": "WBC", "lot_number": "LOT-WBC-N", "level": "N", "target": 5.8 },
    { "field_code": "RBC", "lot_number": "LOT-RBC-N", "level": "N", "target": 4.92 },
    ...
  ]
}
```

- **`fields`** drive normal patient-result messages.
- **`qc_controls`** drive QC messages — **one entry = one analyte**. A push
  emits one `R`+`Q` ASTM segment pair per `qc_controls` entry. So the analyte
  count is `len(qc_controls)`, not anything the command controls (Pentra = 5;
  GeneXpert = 1).

### QC message generation (`protocols/astm_handler.py::generate_qc`)

For each `qc_controls` entry it computes `value = target + deviation × sd`,
where `sd` is a category fraction of target (`SD_PCT`: ~5% hematology, ~10%
molecular), and emits:

```
O|1|<qcSampleId>|||||||||Q|...       ← O.12 action code "Q" marks the message as QC
R|1|^^^WBC|6.81|10^3/uL|...          ← measured result
Q|1|WBC^LOT-WBC-N^N|6.81|...         ← QC identity: testCode^lotNumber^level
R|2|^^^RBC|...   Q|2|RBC^LOT-RBC-N^N|...   (and so on per analyte)
```

### Running it

```bash
cd ~/Dev/analyzer-mock-server
# Push a rejection-level QC run (5 analytes for pentra) to the bridge:
python3 server.py --push tcp://localhost:12000 --template horiba_pentra60 --qc --qc-deviation 3.5
```

| Flag                 | Effect                                               |
| -------------------- | ---------------------------------------------------- |
| `--qc`               | Build a QC message from the template's `qc_controls` |
| `--qc-deviation 0`   | Exactly on target → z=0 → ACCEPTED (no violation)    |
| `--qc-deviation 2.5` | +2.5 SD → `1₂ₛ` WARNING (no NCE)                     |
| `--qc-deviation 3.5` | +3.5 SD → `1₃ₛ` REJECTION (→ NCE)                    |
| (omit deviation)     | Realistic Gaussian scatter around target             |
| `--dry-run`          | Print the ASTM message without sending               |

`tcp://localhost:12000` is the bridge's ASTM listener (host `12000` → container
`12001`). There is also an HTTP API: `POST /simulate/astm/{template}` with body
`{ "destination": "tcp://bridge:12001", "qc": true, "qc_deviation": 3.5 }`.

---

## 3. The bridge — analyzer message → FHIR

Repo: `~/Dev/openelis-analyzer-bridge`. Runtime config is mounted as
`configuration.yml` (forward target = `…/api/OpenELIS-Global/analyzer`).

- Listens for ASTM (LIS1-A `:12001`), HL7 (MLLP), and watches FILE drops.
- **`FhirRoutingConfig.useFhir = true` (default):** every protocol is parsed to
  a FHIR R4 transaction Bundle (`HttpForwardingRouter.routeAsFhir`) and POSTed
  to OE's `/analyzer/fhir`. (Set `useFhir=false` for legacy raw `/analyzer/astm`
  etc.)
- **QC detection** (`ASTMResultParser`): a sample is QC when its Order record's
  action code `O.12 == "Q"` (or via FR-15 registry rules). Rule codes are
  matched on **severity**, not code text.
- **QC metadata → FHIR** (`FhirBundleBuilder`): control observations get
  `meta.tag` `QC` (system `http://openelis-global.org/fhir/tags`) plus
  extensions `http://openelis-global.org/fhir/qc/lot-number` and
  `.../qc/control-level`.
- Analyzer identity: the bridge forwards even unknown sources; OE resolves the
  analyzer from the ASTM header via `analyzer.identifier_pattern` (or
  find/create a stub). Check the bridge log:
  `docker logs oe-bridge --since 60s | grep -i fhir` →
  `FHIR routing N results … /analyzer/fhir` and `FHIR Bundle accepted by OE`.

---

## 4. OpenELIS ingest & Westgard evaluation

- **`AnalyzerFhirImportController` (`POST /analyzer/fhir`)** —
  `mapObservationToAnalyzerResult` reads `meta.tag` `QC` → `setIsControl(true)`
  and the `qc/lot-number` + `qc/control-level` extensions; control results route
  to
  `qcResultProcessingService.processQCResult(analyzerId, testId, accession, lotNumber, controlLevel, value, units, timestamp)`.
- **`QCResultServiceImpl.createQCResult`** — resolves the control lot, computes
  the z-score, persists the `QCResult`, and (only when the lot is ACTIVE and the
  z-score is non-null) publishes a `QCResultCreatedEvent`.
- **`QCResultCreatedEventListener`** — `@Async` + `@TransactionalEventListener`
  (AFTER_COMMIT) + `@Transactional(REQUIRES_NEW)`: runs `evaluateAllRules`, and
  for each violated rule calls `QCRuleViolationServiceImpl.createViolation`. It
  also flips the `QCResult` status to `ACCEPTED` / `REJECTED`.
- **Westgard rules** (`qc/service/evaluator/`) — codes use Unicode subscripts:

  | Severity  | Rules                             |
  | --------- | --------------------------------- |
  | REJECTION | `1₃ₛ`, `2₂ₛ`, `4₁ₛ`, `R₄ₛ`, `10ₓ` |
  | WARNING   | `1₂ₛ`, `3₁ₛ`, `7ₜ`                |

  Multi-point rules (`2₂ₛ` needs 2 consecutive, `4₁ₛ` needs 4, `10ₓ` needs 10)
  only fire once enough consecutive QC history has accumulated — so repeated
  identical pushes trip progressively more rules.

---

## 5. NCE auto-creation (OGC-701 / OGC-728)

Hook: `QCRuleViolationServiceImpl.createViolation` — after inserting the
violation and its alert, if `severity == "REJECTION"` it calls
`QcViolationNceServiceImpl.createNceForViolation(violation)` (retry once;
failures are logged, never block violation creation).

`QcViolationNceServiceImpl` builds the NCE:

- **Fields:** `severity = CRITICAL`, `status = Pending`, `autoGenerated = true`,
  `westgardRule = <ruleCode>`, `nceNumber` from `NceNumberGeneratorService`
  (`NCE-YYYY-NNNNN`), reporter `"System (Westgard QC)"`, category **"Analysis"**
  / type **"QC failure"** (resolved by name; tolerates a missing seed),
  `immediateAction = "Sample run held pending review"`.
- **Idempotency:** trigger source is `("QC_VIOLATION", <violation UUID>)`,
  enforced by unique constraint `uq_nc_event_trigger_source`;
  `createNceForViolation` short-circuits if an NCE already exists for the
  violation.
- **NCE-number allocation** is serialized by a transaction-scoped
  `pg_advisory_xact_lock` in the generator (concurrent multi-analyte QC failures
  would otherwise race `MAX(seq)+1` and collide).
- **Affected samples (OGC-728):** window = `[last in-control QC, violation)`,
  floored at 24h and capped at the **50 newest distinct samples**. Samples are
  read via a **projection** (scalar ids, not managed entities) that dedupes
  result revisions to one link per sample. Recorded
  `affected_samples_cap_reason`: `none_applied` / `time_24h` / `count_50`.
- **History:** an `NceHistory` `AUTO_CREATED` entry (renders in the dashboard
  History tab).

**Granularity note (known limitation):** the hook creates **one NCE per
REJECTION violation**, so one QC run that fails several analytes across several
rules yields `(#analytes × #rejection-rules)` NCEs — e.g. a Pentra
`--qc-deviation 3.5` push after some QC history yields 5 analytes × 3 rules = 15
NCEs. A candidate change is to dedupe to one NCE per invalidated analyte (per
`triggering_result_id`). This is an open product decision.

### Surfaces

- **`/NceDashboard`** — auto-NCEs appear as CRITICAL/Pending; the expanded row
  shows the description (with z-score), Trigger = `QC_VIOLATION`, linked
  samples, and the cap chip.
- **QA Overview NCE Pulse tile** (`/qa/overview`) — counts
  `CRITICAL && Pending`.

---

## 6. Seeding prerequisites

For QC to resolve a control lot and evaluate Westgard rules, OE needs the
analyzer, tests, control lots (with statistics), and rule config. The mock
server generates this from its templates:

```bash
cd ~/Dev/analyzer-mock-server
python3 generate_analyzer_sql.py                 # writes seed_analyzers.sql (gitignored)
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < seed_analyzers.sql
```

It creates (idempotently): `analyzer` (with `identifier_pattern` matching the
template's ASTM header), `analyzer_test_map`, `qc_control_lot`
(`MANUFACTURER_FIXED` mean/SD), `qc_statistics` (so z-scores work immediately),
and `westgard_rule_config` (all 8 rules per test/instrument). It resolves OE
tests by **LOINC or name** so it is portable across catalogs, and
self-provisions the Generic ASTM `analyzer_type` when the plugin isn't loaded.

---

## 7. End-to-end runbook

```bash
# 1. Deploy OE (+ migrations run at boot)
bash ~/Desktop/dev-rebuild_2.sh
until docker logs openelisglobal-webapp 2>&1 | grep -q "Server startup in"; do sleep 5; done

# 2. Seed QC metadata (section 6)

# 3. Fire a rejection-level QC run
cd ~/Dev/analyzer-mock-server
python3 server.py --push tcp://localhost:12000 --template horiba_pentra60 --qc --qc-deviation 3.5

# 4. Confirm the bridge converted + forwarded
docker logs oe-bridge --since 60s 2>&1 | grep -i fhir

# 5. Verify in the DB (analyzer 9 = Pentra in the seed)
docker exec openelisglobal-database psql -U clinlims -d clinlims -tAc "
  SELECT v.severity, COUNT(*) violations, COUNT(n.id) nces
  FROM clinlims.qc_rule_violation v
  LEFT JOIN clinlims.nc_event n ON n.trigger_source_id=v.id AND n.auto_generated=true
  WHERE v.instrument_id=9 AND v.violation_date_time > NOW() - INTERVAL '2 minutes'
  GROUP BY v.severity;"        # REJECTION: violations==nces ; WARNING: nces=0

# 6. Verify in the UI  →  https://localhost  (accept cert: Advanced ▸ Proceed),  admin / adminADMIN!
#    /NceDashboard (expand a new NCE) and /qa/overview (NCE Pulse tile)
```

Automated coverage lives in
`src/test/java/org/openelisglobal/qaevent/QcViolationNceServiceTest.java`
(Spring integration, no mocks) — context fields, distinct-sample linking +
revision dedup, the three cap reasons, idempotency, WARNING-creates-nothing, and
an 8-thread concurrency test.

---

## 8. Gotchas

- **Don't `curl` the ingest endpoint through `https://localhost`** — the nginx
  proxy returns an empty reply to curl. Use the mock-server push, or
  `docker exec openelisglobal-webapp curl … http://localhost:8080/...`.
- **`/analyzer/astm` (legacy) does not run QC** — only `/analyzer/fhir` does.
  The bridge's default FHIR routing bridges the gap.
- **Local integration tests need `TESTCONTAINERS_RYUK_DISABLED=true`** on this
  machine (the Docker socket mount blocks the ryuk reaper).
- **NCE count grows with repeated identical pushes** — multi-point Westgard
  rules accumulate consecutive history (see §4). Not a bug; a test artifact.
- **Test datasets must be self-contained for FK targets** — e.g. an analysis
  row's `status_id` must reference a `status_of_sample` row the dataset itself
  seeds or that reliably survives, since other tests truncate reference tables.

---

## 9. Key files

| Concern                | Path                                                                                              |
| ---------------------- | ------------------------------------------------------------------------------------------------- |
| Mock QC generation     | `analyzer-mock-server/protocols/astm_handler.py::generate_qc`                                     |
| Mock seed generator    | `analyzer-mock-server/generate_analyzer_sql.py`                                                   |
| Bridge FHIR routing    | `openelis-analyzer-bridge/.../routing/HttpForwardingRouter.java`, `config/FhirRoutingConfig.java` |
| Bridge QC parse + FHIR | `openelis-analyzer-bridge/.../fhir/ASTMResultParser.java`, `fhir/FhirBundleBuilder.java`          |
| OE FHIR ingest         | `analyzerimport/action/AnalyzerFhirImportController.java`                                         |
| QC processing          | `analyzer/service/QCResultProcessingServiceImpl.java`, `qc/service/QCResultServiceImpl.java`      |
| Westgard event + rules | `qc/event/QCResultCreatedEventListener.java`, `qc/service/evaluator/`                             |
| Violation → NCE hook   | `qc/service/QCRuleViolationServiceImpl.java`                                                      |
| NCE auto-create        | `qaevent/service/QcViolationNceServiceImpl.java`, `NceNumberGeneratorServiceImpl.java`            |
| Schema                 | `liquibase/3.5.x.x/051-nce-trigger-source.xml`                                                    |
