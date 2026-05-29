# Analyzer Integration — Live E2E Proof Record

**Date:** 2026-05-29. **Stack:** live 4-branch dev stack (OE2
`feat/analyzer-order-dispatch`, bridge `feat/outbound-mllp-hl7`, mock
`feat/order-in-result-out`, distro). **Success metric:** a result is **fully
imported** — staged on the correct accession AND resolved to the correct OE2
test (`analyzer_results.test_id` populated), acceptable on AccessionResults.

## Evidence — three protocols, real round-trips (no stubs)

| # (`analyzer_results.id`) | Protocol / analyzer            | Accession            | Wire testCode     | `test_name`            | `test_id`   | Fully imported?      |
| ------------------------- | ------------------------------ | -------------------- | ----------------- | ---------------------- | ----------- | -------------------- |
| 18                        | **ASTM** — Cepheid GeneXpert   | DEV01260000000000004 | `21613-5` (LOINC) | Chlamydia trachomatis  | **400**     | ✅                   |
| 19                        | **HL7/MLLP** — Mindray BC-5380 | DEV01260000000000016 | `6690-2` (LOINC)  | White Blood Cell Count | **13**      | ✅                   |
| 20                        | **FILE** — QuantStudio 5       | DEV01260000000000008 | `DENV` (raw code) | DENV                   | **(blank)** | ⚠️ partial — see gap |

### ASTM (outbound order → result round-trip)

`POST /api/orders {host:10.42.20.10,port:9600,protocol:ASTM,accession:DEV…004,loincCodes:[21613-5]}`
→ bridge reverse-maps 21613-5→`CT`, ASTM ENQ handshake (contention-aware after
fix `#62`), mock receives order + pushes result → bridge inbound (12001) →
`FHIR routing 1 results for accession DEV…004` → OE2
`Inserted 1 results … Cepheid GeneXpert`. Staged: CT "NOT DETECTED",
test_id 400. **Fully imported.**

### HL7/MLLP (outbound order → result round-trip)

`POST /api/orders {…port:5380,protocol:HL7,accession:DEV…016,loincCodes:[6690-2]}`
→ bridge maps 6690-2→`WBC`, MLLP ORM^O01 → mock ACK + ORU^R01 push → bridge
inbound (2575), identity matched `Mindray BC-5380` → OE2
`Inserted 1 results … Mindray BC-5380`. Staged: WBC 7.5, test_id 13. **Fully
imported.**

### FILE (drop → watch → parse → import)

Faithful loadable fixture (`Results` sheet, `Block Type` anchor,
`Sample Name=DEV…008, Target Name=DENV`) dropped into the QuantStudio-5 watch
dir → `FileWatcher Processing … for analyzer: 43` →
`Parsing Excel file … for analyzer 43` → `FHIR file import: 1 results` → OE2
`accession=DEV…008 testCode=DENV analyzerId=43 … Inserted 1 results … QuantStudio 5`.
Staged: row 20 on DEV…008. **Reaches the accession, but `test_id` is blank**
(see gap).

## Gaps found by the E2E proof (green unit/guard tests missed these)

### G1 — FILE path does not apply code→LOINC (consistency gap) — OPEN

M2 ("bridge emits LOINC, not raw analyzer code") was wired into the ASTM/HL7
inbound path (`HttpForwardingRouter` passes a `codeToLoinc` function to
`FhirBundleBuilder.buildBundle`), but **not** the FILE inbound path:
`FileMessageHandler.java:223` calls the 3-arg `buildBundle` with
`codeToLoinc=null`, so the FILE FHIR Observation carries the **raw** analyzer
testCode (`DENV`, no LOINC system). OE2 resolves results by LOINC, so `DENV`
doesn't resolve → row 20 has a blank `test_id` (vs ASTM/HL7 which carry LOINC
and resolve to test 400/13). This contradicts the project goal "the bridge owns
analyzer-code↔LOINC for **all three** paths." **Fix (focused):** in
`FileMessageHandler`, resolve the analyzer registry entry's `getLoincForCode`
and pass it to `buildBundle` (mirror `HttpForwardingRouter`); then `DENV`→
`7855-0`→test 309 (DENGUE) and the FILE result fully imports onto DEV…008.
Re-prove after.

### G2 — stale FileWatcher dir→analyzer binding after re-registration — worked around

The QuantStudio-5 watch dir was bound to a **stale** analyzer id (34, no column
mappings) from an earlier registration cycle, while the current registry had id
43 (with mappings) — so the first drop failed
`No column mappings registered for analyzer 34 — refusing FILE fallback`. A
bridge restart re-bootstrapped from the (now-healthy) OE2 and re-bound the dir
to id 43; the re-drop then parsed. Indicates the FileWatcher dir→analyzer
binding isn't refreshed when an analyzer is re-registered with a new id.
**Follow-up:** refresh/replace the watch binding on re-registration.

## Status

- ASTM + HL7: **fully imported, proven live** (rows 18, 19).
- FILE: pipeline proven (parse → FHIR → import on accession), **blocked on G1**
  for full test resolution. Fixing G1 (one bridge change) completes a consistent
  3/3.
- Fixtures: 5 faithful loadable fixtures + the contract guard (50 assertions)
  are green; the FILE fixture (`quantstudio5-results.xlsx`) is what drove this
  proof.
