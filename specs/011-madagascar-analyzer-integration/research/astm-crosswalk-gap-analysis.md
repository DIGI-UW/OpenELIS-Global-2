# ASTM Generic Plugin: Crosswalk Gap Analysis

**Date:** 2025-02-25
**Compares:** ASTM Analyzer Mapping Specification v1.0 (Nov 2025) ↔ Generic ASTM Plugin Feasibility Analysis (Feb 2025)

---

## 1. Summary

The v1.0 Mapping Spec covers the **GUI and API for configuring field mappings between analyzers and OpenELIS**. The Feasibility Analysis covers the **backend architecture and configuration engine needed to make a single generic ASTM plugin work.** They are complementary documents, but there are significant gaps in both directions — the Mapping Spec is missing backend concepts that the Feasibility Analysis identifies as essential, and the Feasibility Analysis is missing GUI/API detail that the Mapping Spec has already worked out.

| Category | Mapping Spec v1.0 | Feasibility Analysis | Gap Owner |
|----------|-------------------|---------------------|-----------|
| Analyzer registration & list view | ✅ Detailed | ⬜ Not covered (assumes it exists) | — |
| Connection settings (port, timeout, role) | 🟡 Partial (IP/port only) | ✅ Detailed (role, bidirectional, retry) | Mapping Spec |
| Test code mapping table | ✅ Detailed (FR-8, FR-9, FR-10) | ✅ Detailed (Section 4.3) | Aligned |
| Field extraction configuration | ⬜ Not covered | ✅ Detailed (Section 4.2) | Mapping Spec |
| QC sample identification | ⬜ Not covered | ✅ Detailed (Section 4.4) | Mapping Spec |
| Value transformation rules | 🟡 Partial (qualitative mapping only) | ✅ Detailed (Section 5.1) | Mapping Spec |
| Result aggregation modes | ⬜ Not covered | ✅ Detailed (Section 5.2) | Mapping Spec |
| Message simulator / test parser | ⬜ Not covered | ✅ Identified as critical | Mapping Spec |
| Bidirectional configuration | ⬜ Not covered | 🟡 Phase 2 identified | Both |
| Copy/clone configuration | ✅ Covered (FR-11) | ✅ Phase 2 | Aligned |
| Error dashboard | ✅ Detailed (API endpoints) | ⬜ Not covered | Feasibility |
| Dual-panel drag-and-drop UI | ✅ Detailed (FR-7, FR-8) | ⬜ Not covered | — |
| Field type detection / query | ✅ Detailed (FR-5, FR-6) | ⬜ Not covered | Feasibility |
| Permissions/RBAC | ✅ Detailed (Section 8) | ⬜ Not covered | Feasibility |
| API contract (REST endpoints) | ✅ Detailed (Section 6) | ⬜ Not covered | Feasibility |
| Audit trail | ✅ Mentioned | ✅ Mentioned | Aligned |
| Database schema | ⬜ Not covered | ✅ Detailed (Section 7.2) | Mapping Spec |
| Localization tags | ⬜ Not covered | ✅ Detailed (Section 8.2) | Mapping Spec |
| ASTM field reference accuracy | ⚠️ Off-by-one errors | ✅ Correct | Mapping Spec |

---

## 2. Critical Gaps in the Mapping Spec v1.0

These are things the Feasibility Analysis identifies as necessary for a generic ASTM plugin that the Mapping Spec does not address at all.

### Gap 1: QC Sample Identification Rules — MISSING

**Severity: HIGH**

The Mapping Spec has zero coverage of how QC samples are distinguished from patient samples. This is critical because the Analyzer Import workflow (already spec'd in `analyzer-import-requirements.md`) uses a QC-first evaluation model — QC must pass before patient results can be accepted.

The Feasibility Analysis identifies four QC rule types that need GUI configuration:

| Rule Type | Example | Used By |
|-----------|---------|---------|
| `FIELD_EQUALS` | `O.16 = "QC"` | MALDI Biotyper |
| `SPECIMEN_ID_PREFIX` | Specimen ID starts with `QC-` | Chemistry analyzers |
| `SPECIMEN_ID_REGEX` | `^(QC\|CTRL\|CAL).*` | General pattern |
| `FIELD_CONTAINS` | `P.5 contains "CONTROL"` | Various |

**Recommendation:** Add a "QC Identification" section to the Mapping Spec with a rule builder UI (similar to the existing filter builder pattern) and corresponding API endpoints.

---

### Gap 2: Field Extraction Configuration — MISSING

**Severity: MEDIUM-HIGH**

The Mapping Spec assumes a dual-panel drag-and-drop interface where the "source" is a list of ASTM fields discovered via query. But it doesn't address the fundamental question: **which ASTM record fields does the generic parser look at for key data?**

The Feasibility Analysis identifies these as configurable extraction parameters:

| Parameter | Default | Why Configurable |
|-----------|---------|-----------------|
| Specimen ID field | `O.3` | Some instruments use O.2 |
| Test ID field | `R.3` | Standard |
| Test ID component | `4` (4th component of `^^^CODE`) | Some use different component positions |
| Result value field | `R.4` | Standard |
| Result units field | `R.5` | Standard |
| Abnormal flag field | `R.7` | Standard |
| Result timestamp field | `R.13` | Standard |

Most of these will use defaults and never need changing, but the MALDI Biotyper already proves that non-standard field usage exists (P.3 for run UUID, O.13 for plate position, O.16 for sample type).

**Recommendation:** Add an "Advanced Field Extraction" accordion section (collapsed by default) to the analyzer configuration form. Only power users would touch this.

---

### Gap 3: Value Transformation Rules — ONLY PARTIAL

**Severity: MEDIUM-HIGH**

The Mapping Spec covers qualitative result mapping (FR-10: map analyzer values like "POS"/"NEG" to OpenELIS codes) but misses several other transformation types identified in the Feasibility Analysis:

| Transformation | Mapping Spec v1.0 | Feasibility Analysis |
|---------------|-------------------|---------------------|
| Pass-through (numeric/text as-is) | ✅ Implied | ✅ Explicit |
| Qualitative value mapping (S/I/R, POS/NEG) | ✅ FR-10 | ✅ `VALUE_MAP` |
| Greater/less-than flag detection (`>500`, `<0.1`) | ⬜ Missing | ✅ `GREATER_LESS_FLAG` |
| Threshold classification (score ranges → labels) | ⬜ Missing | ✅ `THRESHOLD_CLASSIFY` |
| Coded lookup (text → master table code) | ⬜ Missing | ✅ `CODED_LOOKUP` |

The `GREATER_LESS_FLAG` gap is particularly important for chemistry analyzers (Indiko Plus) where results exceeding linearity are sent as `>500` or `<0.1` — these need to be stored with both the numeric portion and the operator flag.

**Recommendation:** Extend FR-10 into a broader "Value Transformation" feature with a transformation type dropdown per test code mapping row.

---

### Gap 4: Result Aggregation Mode — MISSING

**Severity: MEDIUM**

The Mapping Spec doesn't address how results from the same specimen are grouped when they arrive across multiple ASTM messages. The Feasibility Analysis identifies three modes:

| Mode | Use Case |
|------|----------|
| `PER_MESSAGE` | Default — one ASTM message = one batch |
| `AGGREGATE_BY_SPECIMEN` | Collect messages with same O.3 within a time window |
| `AGGREGATE_BY_SESSION` | Collect all between ENQ/EOT boundaries |

This matters for instruments that send one result per message (some configurations) vs. all results in one message.

**Recommendation:** Add an "Aggregation" setting to the analyzer configuration form with a mode dropdown and optional time window parameter.

---

### Gap 5: Message Simulator — MISSING

**Severity: HIGH (for usability)**

The Mapping Spec has "Test Connection" (FR-4, BR-8) which validates TCP handshake, but has no facility for testing the **parsing and mapping configuration** against a real ASTM message. The Feasibility Analysis identifies this as the single most important tool for making a generic plugin viable without developer support.

**Proposed functionality:**
1. Paste a raw ASTM message (or upload a capture file)
2. Click "Parse" → system runs the message through the generic parser using the current configuration
3. Display extracted fields, matched test codes, QC identification results, and transformed values
4. Highlight any unmatched test codes or parse errors

**Recommendation:** Add a "Message Simulator" tab or section to the analyzer configuration interface. This is what turns a "developer configures it" workflow into a "lab IT configures it" workflow.

---

### Gap 6: Connection Role — MISSING

**Severity: MEDIUM**

The Mapping Spec assumes OpenELIS connects TO the analyzer (client mode: IP + port). But the Feasibility Analysis and the actual instrument specs show that **OpenELIS usually acts as a server (listener)** — the analyzer connects to OpenELIS, not the other way around.

The Mapping Spec FR-1 collects "IP address" and "port" as required fields, which implies client mode. The MALDI Biotyper, Indiko Plus, and both Mindray instruments all connect to OpenELIS as the server.

**Recommendation:** Replace the single IP/port model with:
- Connection Role: Server (listen) or Client (connect)
- For Server mode: Listen Port (OpenELIS port)
- For Client mode: Analyzer IP + Port

---

### Gap 7: Database Schema — MISSING

**Severity: MEDIUM**

The Mapping Spec defines API contracts but no database schema. The Feasibility Analysis provides a complete schema (`astm_analyzer_config`, `astm_test_mapping`, `astm_qc_rule`, `astm_flag_mapping`) that should be incorporated.

**Recommendation:** Add database schema to the Mapping Spec or create a separate data model document.

---

### Gap 8: Localization Tags — MISSING

**Severity: MEDIUM**

The Mapping Spec uses hardcoded English labels throughout (e.g., "Add Analyzer", "Test Connection"). Per OpenELIS standards, all UI text must use localization tags. The Feasibility Analysis includes a comprehensive localization tag table.

**Recommendation:** Add localization tags appendix to the Mapping Spec, following the `label.analyzer.*` namespace convention.

---

## 3. Gaps in the Feasibility Analysis

These are things the Mapping Spec covers well that the Feasibility Analysis doesn't address.

### Gap A: Dual-Panel Mapping UI Pattern

The Mapping Spec's dual-panel drag-and-drop interface (Section 5.3) with source fields on the left and target OpenELIS fields on the right is a well-designed UI pattern. The Feasibility Analysis proposes a simpler table-based mapping approach (analyzer code → OpenELIS test). Both are valid but target different complexity levels:

- **Dual-panel:** Better for initial setup when you don't know the analyzer's fields
- **Table-based:** Better for ongoing management and quick edits

**Recommendation:** Support both views — dual-panel for initial "discovery" mapping, table view for ongoing management.

### Gap B: Field Discovery / "Query Analyzer" Feature

The Mapping Spec's FR-5 (Query Analyzer) sends an ASTM query message to discover available fields. The Feasibility Analysis doesn't address auto-discovery — it proposes "Auto-detect codes from live messages" as Phase 2.

These are complementary approaches:
- FR-5 actively queries the analyzer for its test menu
- Auto-detect passively learns from real result messages

**Recommendation:** Include both. FR-5 is the proactive approach; auto-detect from live messages is the passive approach. The Message Simulator serves as the manual middle ground.

### Gap C: Error Dashboard

The Mapping Spec has API endpoints for error management (GET/POST `/api/analyzer-errors`), acknowledgment workflow, and error filtering. The Feasibility Analysis doesn't address error handling beyond basic parse error logging.

**Recommendation:** Incorporate the Mapping Spec's error dashboard design into the generic plugin architecture.

### Gap D: Permissions / RBAC

The Mapping Spec defines role-based access (RESULTS viewer, SYSTEM_ADMIN full access). The Feasibility Analysis doesn't address permissions.

**Recommendation:** Adopt the Mapping Spec's permission model, potentially extending it with a LAB_SUPERVISOR role for the Message Simulator.

### Gap E: API Contract

The Mapping Spec has a complete REST API design. The Feasibility Analysis doesn't define endpoints. The Mapping Spec's API design should be adopted and extended for the new configuration entities (QC rules, extraction config, transformation rules, aggregation settings).

---

## 4. ASTM Field Reference Errors in v1.0

The Mapping Spec's Appendix A has **off-by-one field position errors** compared to the actual ASTM LIS2-A2 standard and the field mapping specs we've built for real instruments:

| Record | Field | Mapping Spec v1.0 | Correct Position | Impact |
|--------|-------|-------------------|-----------------|--------|
| **O** | Sample ID | O\|2 | **O\|3** (O.3) | ⚠️ Would extract wrong field |
| **O** | Universal Test ID | O\|4 | **O\|5** (O.5) | ⚠️ Would extract wrong field |
| **O** | Ordered Date/Time | O\|6 | **O\|7** (O.7) | Minor |
| **O** | Sample Type | O\|15 | **O\|16** (O.16) | Would miss sample type |
| **R** | Universal Test ID | R\|2 | **R\|3** (R.3) | ⚠️ Would extract wrong field |
| **R** | Data/Measurement Value | R\|3 | **R\|4** (R.4) | ⚠️ Would extract wrong field |
| **R** | Units | R\|4 | **R\|5** (R.5) | Would extract wrong field |
| **R** | Abnormal Flag | R\|6 | **R\|7** (R.7) | Would extract wrong field |
| **R** | Result Status | R\|7 | **R\|8** (R.8) | Would extract wrong field |

**Root cause:** The v1.0 spec appears to have used 0-indexed field positions (skipping the record type identifier), while ASTM LIS2-A2 convention counts the record type as field 1. In ASTM, `R|1|^^^WBC|8.5|mg/dL|...` means:
- R.1 = Sequence Number (`1`)
- R.2 = (Instrument Specimen ID, often empty)
- R.3 = Universal Test ID (`^^^WBC`)
- R.4 = Data/Measurement Value (`8.5`)
- R.5 = Units (`mg/dL`)

The actual instrument specs we've built (Indiko Plus, MALDI Biotyper, BC-5380, BS-series) all use the correct 1-indexed positions.

**Recommendation:** Correct Appendix A to use standard ASTM field numbering. This is important because the generic plugin's field extraction configuration will reference these positions.

---

## 5. Reconciled Feature List for v2.0 Spec

Merging the best of both documents, here's what a unified Generic ASTM Analyzer Configuration spec should contain:

### From Mapping Spec v1.0 (keep as-is)
- ✅ FR-1: Analyzer registration (enhanced with connection role)
- ✅ FR-2: Analyzer list view
- ✅ FR-3: Search and filter
- ✅ FR-4: Analyzer actions (test connection)
- ✅ FR-5: Field discovery / query analyzer
- ✅ FR-7: Dual-panel mapping interface
- ✅ FR-8: Mapping creation (drag-and-drop + table view)
- ✅ FR-9: Unit of measure mapping
- ✅ FR-11: Copy mappings
- ✅ Section 5: Carbon Design System guidance
- ✅ Section 6: API endpoints (extended)
- ✅ Section 7: Validation rules
- ✅ Section 8: Security & permissions
- ✅ Error dashboard design

### From Feasibility Analysis (add new)
- 🆕 Connection role (server/client) configuration
- 🆕 Field extraction configuration (advanced accordion)
- 🆕 QC identification rule builder
- 🆕 Value transformation rules (extending FR-10)
- 🆕 Result aggregation mode selection
- 🆕 Message simulator
- 🆕 Database schema
- 🆕 Localization tags
- 🆕 Escape valve: extension class hooks

### Modified from both
- 🔄 FR-10: Qualitative mapping → broader "Value Transformation" feature
- 🔄 Appendix A: Fix ASTM field position numbering
- 🔄 FR-1: Add connection role, remove required IP for server mode
- 🔄 Phase 2 items: bidirectional template engine, auto-detect from live messages

---

## 6. Recommended Next Steps

1. **Fix the ASTM field reference errors** in the Mapping Spec — these would cause real bugs if implemented as written
2. **Merge the two documents** into a unified "Generic ASTM Analyzer Configuration FRS v2.0"
3. **Build the Message Simulator** first — it's the key enabler for the generic approach and the best way to validate the configuration engine
4. **Prioritize QC identification rules** — without this, the Analyzer Import workflow can't differentiate QC from patient results
5. **Create a React mockup** showing the enhanced analyzer configuration form with the new sections (QC rules, transformations, simulator)

---

## 7. Effort Delta

| If we just implement Mapping Spec v1.0 | If we implement the merged v2.0 |
|----------------------------------------|-------------------------------|
| GUI + API for field mapping only | GUI + API + backend configuration engine |
| Still need per-instrument Java plugins for QC identification, transformation, aggregation | No per-instrument plugins needed |
| Each new analyzer: ~1 week dev work | Each new analyzer: ~2 hours config work |
| Covers ~50% of the "generic" goal | Covers ~90% of the "generic" goal |

The additional effort to go from v1.0 → v2.0 is approximately 2–3 weeks of development, but it eliminates the need for per-instrument plugin development permanently.
