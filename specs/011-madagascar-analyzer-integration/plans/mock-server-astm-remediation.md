# Plan: Mock Server ASTM Remediation — GeneXpert + General Compliance

## Context

The analyzer mock server at `tools/analyzer-mock-server/` is a multi-protocol
simulator that generates test messages for OpenELIS integration testing. It
currently has **4 ASTM templates** (Horiba Pentra 60, Horiba Micros 60, Stago
STart 4, Mindray BA-88A) and **1 HL7 GeneXpert template** — but **no ASTM
GeneXpert template**.

The Cepheid GeneXpert supports both ASTM and HL7 (per the official GeneXpert LIS
Protocol Specification Rev E, Dec 2014). Since the 011-Madagascar project
targets ASTM-based integration via the Generic ASTM plugin, we need the mock to
accurately simulate GeneXpert ASTM communication.

**Two targets:**

1. **GeneXpert ASTM** — Create a template that matches the Cepheid PDF spec
   (Sections 4-6), including QC marking, multi-level results, ISID, and
   GeneXpert-specific H-segment format
2. **General ASTM compliance** — Fix gaps in `astm_handler.py` where the
   generated messages deviate from ASTM E-1394/LIS2-A2

**Key finding from gap analysis crosswalk (Section 4):** The Mapping Spec v1.0
had off-by-one field reference errors, but the mock server and GenericASTM
plugin are **already aligned** — both use consistent 0-based array indexing
after `split("|")` which correctly maps to 1-indexed ASTM positions (e.g.,
split[2] = R.3 = Universal Test ID). No field position changes are needed.

**Plan archive:** This plan will be saved to
`specs/011-madagascar-analyzer-integration/plans/mock-server-astm-remediation.md`
before implementation begins.

---

## Identified Gaps (Mock Server vs. Specifications)

### Gap Summary Table

| #   | Gap                                                       | Source                                                                                                                | Severity     | Mock Server Impact                                                                                                                                                 | This Plan Addresses?                                                       |
| --- | --------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------- |
| G1  | No GeneXpert ASTM template                                | Cepheid PDF spec Section 6                                                                                            | **CRITICAL** | Cannot test GeneXpert ASTM integration at all                                                                                                                      | **YES** — Section 1                                                        |
| G2  | H-record missing Message ID (H.3)                         | ASTM E-1394, Cepheid PDF Section 5.1                                                                                  | HIGH         | Non-compliant messages; some LIS may reject                                                                                                                        | **YES** — Section 3a                                                       |
| G3  | H-record missing Receiver ID (H.10)                       | ASTM E-1394, Cepheid PDF Section 5.1                                                                                  | MEDIUM       | Bidirectional routing may fail                                                                                                                                     | **YES** — Section 3a                                                       |
| G4  | H-record missing Processing ID (H.12)                     | Cepheid PDF Section 5.1                                                                                               | MEDIUM       | GeneXpert always sends "P" (Production)                                                                                                                            | **YES** — Section 3a                                                       |
| G5  | H-record hardcoded "LIS2-A2" version (H.13)               | Cepheid PDF: "1394-97"                                                                                                | MEDIUM       | GeneXpert uses "1394-97", not "LIS2-A2"                                                                                                                            | **YES** — Section 3a                                                       |
| G6  | O-record non-standard format                              | ASTM E-1394, Cepheid PDF Section 5.4                                                                                  | HIGH         | `O\|1\|sample^LAB\|panel^panel Panel\|\|ts` differs from standard `O\|1\|specimen\|\|^^^testID\|priority\|ts\|...\|actionCode\|...\|specimenDesc\|...\|reportType` | **YES** — Section 3b                                                       |
| G7  | O-record missing Action Code (O.12)                       | Cepheid PDF Section 5.4, Gap Analysis Gap 1                                                                           | **CRITICAL** | Cannot generate QC samples — GeneXpert uses O.12="Q" for QC                                                                                                        | **YES** — Section 3b, 3d                                                   |
| G8  | O-record missing Specimen Descriptor (O.16)               | Cepheid PDF Section 5.4                                                                                               | LOW          | GeneXpert sends "ORH" (POCT1-A Other)                                                                                                                              | **YES** — Section 3b                                                       |
| G9  | O-record missing Report Type (O.26)                       | Cepheid PDF Section 5.4                                                                                               | LOW          | GeneXpert sends "F" (Final), "Q" (Query response)                                                                                                                  | **YES** — Section 3b                                                       |
| G10 | R-record lacks multi-level result support                 | Cepheid PDF Section 6.3.4.1.6                                                                                         | HIGH         | Can't simulate GeneXpert's main→analyte→complementary hierarchy (Ct, Conc/LOG)                                                                                     | **YES** — Section 3c                                                       |
| G11 | R-record lacks extended component format                  | Cepheid PDF Section 6.3.4.1.6                                                                                         | HIGH         | R.3 should be `^^^code^name^version^analyte^complementary` (8 components), mock generates `^^^code` or `^^^code^display` (2-4 components)                          | **YES** — Section 3c                                                       |
| G12 | No Comment record (C) support                             | Cepheid PDF Section 5.6, ASTM E-1394                                                                                  | MEDIUM       | Can't simulate error results with accompanying error details                                                                                                       | **YES** — Section 3e                                                       |
| G13 | No QC message generation                                  | Gap Analysis Gap 1 (QC Identification)                                                                                | **CRITICAL** | Can't test QC sample identification in OpenELIS pipeline                                                                                                           | **YES** — Section 3d                                                       |
| G14 | No ISID (Instrument Specimen ID) support                  | Cepheid PDF Section 5.4 (O.4)                                                                                         | MEDIUM       | Can't test dual-ID specimen tracking                                                                                                                               | Deferred — flag defined (`enable_isid`) but not implemented                |
| G15 | No bidirectional host query response generation           | Cepheid PDF Section 6.3.1, Gap Analysis Gap B                                                                         | LOW          | Mock already has basic field query response in `server.py`                                                                                                         | Out of scope                                                               |
| G16 | R-record timestamp at wrong position                      | ASTM E-1394 (R.12/R.13 vs mock's R.10)                                                                                | LOW          | GenericASTM plugin reads from same offset (both consistently non-standard), so pipeline works. Real analyzers would differ.                                        | Deferred — changing would break existing alignment with GenericASTM plugin |
| G17 | Template schema lacks `astm_config`                       | New requirement for GeneXpert-specific config                                                                         | HIGH         | No way to specify per-analyzer ASTM config overrides                                                                                                               | **YES** — Section 2a                                                       |
| G18 | Template schema lacks `qcSample`                          | New requirement for QC generation                                                                                     | HIGH         | No way to define QC sample parameters in templates                                                                                                                 | **YES** — Section 2c                                                       |
| G19 | No `seedQualitative` for deterministic qualitative values | Testing requirement                                                                                                   | MEDIUM       | Can't write deterministic E2E assertions for qualitative results                                                                                                   | **YES** — Section 2b                                                       |
| G20 | `astm_header` doesn't match `identifier_pattern`          | Cross-reference: `genexpert-astm.json` has pattern `GENEXPERT\|CEPHEID` but mock sends `GeneXpert PC^GeneXpert^4.6.0` | HIGH         | GenericASTM plugin won't match the mock's H-segment to the GeneXpert analyzer config                                                                               | **YES** — Section 7c (use `GENEXPERT^GeneXpert^4.6.0`)                     |

### Gap Categories

**Critical (3):** G1 (no template), G7 (no QC action code), G13 (no QC
generation) **High (6):** G2, G6, G10, G11, G17, G18, G20 **Medium (5):** G3,
G4, G5, G12, G14, G16, G19 **Low (3):** G8, G9, G15

### Gaps NOT Addressed (Deferred / Out of Scope)

These gaps from the crosswalk gap analysis are **not mock server issues** — they
are OpenELIS backend/frontend concerns:

| Gap Analysis Ref | Description                                 | Why Deferred                                                                                                           |
| ---------------- | ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Gap 1 (partial)  | QC Identification Rules UI/backend          | Mock generates QC messages (G7/G13 addressed), but OpenELIS needs rule builder to consume them — separate backend work |
| Gap 2            | Field Extraction Configuration              | OpenELIS backend feature — mock generates correct field positions already                                              |
| Gap 3            | Value Transformation Rules (`>500`, `<0.1`) | OpenELIS backend feature — mock could add this to value generation later                                               |
| Gap 4            | Result Aggregation Modes                    | OpenELIS backend feature — mock sends individual messages                                                              |
| Gap 5            | Message Simulator UI                        | OpenELIS frontend feature — entirely separate from mock                                                                |
| Gap 6            | Connection Role (Server/Client)             | Bridge/OpenELIS configuration — mock already supports both server and push modes                                       |
| Gap 7            | Database Schema                             | OpenELIS backend feature                                                                                               |
| Gap 8            | Localization Tags                           | OpenELIS frontend feature                                                                                              |

---

## 1. Create GeneXpert ASTM Template

**New file:** `tools/analyzer-mock-server/templates/genexpert_astm.json`

Based on the Cepheid PDF spec Section 6.3.4 ("Instrument System Uploads Test
Results"):

```json
{
  "analyzer": {
    "name": "Cepheid GeneXpert",
    "model": "GeneXpert",
    "manufacturer": "Cepheid",
    "category": "MOLECULAR"
  },
  "protocol": {
    "type": "ASTM",
    "version": "E-1394-97",
    "transport": "TCP"
  },
  "identification": {
    "astm_header": "GeneXpert PC^GeneXpert^4.6.0"
  },
  "astm_config": {
    "processing_id": "P",
    "version_number": "1394-97",
    "receiver_id": "LIS",
    "specimen_descriptor": "ORH",
    "enable_isid": false,
    "enable_qc": true
  },
  "fields": [
    {
      "code": "MTB-RIF",
      "name": "Xpert MTB/RIF",
      "loinc": "23826-1",
      "type": "QUALITATIVE",
      "possibleValues": ["POSITIVE", "NEGATIVE", "INDETERMINATE", "ERROR"],
      "seedValue": null,
      "seedQualitative": "NEGATIVE",
      "version": "2.1"
    },
    {
      "code": "HIV-VL",
      "name": "Xpert HIV-1 Viral Load",
      "loinc": "20447-9",
      "type": "NUMERIC",
      "unit": "copies/mL",
      "normalRange": "20-10000000",
      "seedValue": 1250,
      "version": "1.0",
      "complementaryResults": [
        { "name": "Conc/LOG", "unit": "log(copies/mL)", "seedValue": 3.1 }
      ]
    },
    {
      "code": "COVID19",
      "name": "Xpert Xpress SARS-CoV-2",
      "loinc": "94500-6",
      "type": "QUALITATIVE",
      "possibleValues": ["POSITIVE", "NEGATIVE", "INDETERMINATE", "ERROR"],
      "seedQualitative": "NEGATIVE",
      "version": "1.0"
    }
  ],
  "qcSample": {
    "id": "QC-MTB-CTRL-001",
    "actionCode": "Q",
    "fields": [{ "code": "MTB-RIF", "seedQualitative": "NEGATIVE" }]
  },
  "testPatient": {
    "id": "PAT-GX-001",
    "name": "Rakoto^Jean^A",
    "sex": "M",
    "dob": "19850315"
  },
  "testSample": {
    "id": "SPECIMEN-GX-001",
    "type": "ORH^Other"
  }
}
```

**Key GeneXpert-specific features in this template:**

- `astm_config.processing_id`: "P" (Production) — goes in H.12
- `astm_config.version_number`: "1394-97" — goes in H.13 (per GeneXpert spec)
- `astm_config.receiver_id`: "LIS" — goes in H.10
- `astm_config.specimen_descriptor`: "ORH" — goes in O.16 (POCT1-A "Other")
- `fields[].version`: Assay version (e.g., "2.1") — goes in R.3 component 6
- `fields[].complementaryResults`: Multi-level results (Ct, Conc/LOG) — per
  Section 6.3.4.1.6
- `fields[].seedQualitative`: Deterministic qualitative value for testing
- `qcSample`: QC sample config with Action Code "Q" in O.12

---

## 2. Extend Schema for GeneXpert Features

**File:** `tools/analyzer-mock-server/templates/schema.json`

Add these new optional properties:

### 2a. `astm_config` object (new top-level property)

```json
"astm_config": {
  "type": "object",
  "description": "ASTM-specific message configuration overrides",
  "properties": {
    "processing_id": { "type": "string", "default": "P", "description": "H.12 Processing ID" },
    "version_number": { "type": "string", "description": "H.13 Version Number (e.g., '1394-97')" },
    "receiver_id": { "type": "string", "description": "H.10 Receiver ID" },
    "specimen_descriptor": { "type": "string", "description": "O.16 Specimen Descriptor" },
    "enable_isid": { "type": "boolean", "default": false, "description": "Generate O.4 Instrument Specimen ID" },
    "enable_qc": { "type": "boolean", "default": false, "description": "Generate QC sample messages" }
  }
}
```

### 2b. Field-level additions

Add to `fields.items.properties`:

- `version` (string): Assay version for R.3 component 6
- `seedQualitative` (string): Deterministic qualitative value
- `complementaryResults` (array): Sub-results with `name`, `unit`, `seedValue`

### 2c. `qcSample` object (new top-level property)

```json
"qcSample": {
  "type": "object",
  "description": "QC sample configuration for generating QC messages",
  "properties": {
    "id": { "type": "string", "description": "QC specimen ID" },
    "actionCode": { "type": "string", "default": "Q", "description": "O.12 Action Code for QC" },
    "fields": { "type": "array", "description": "QC-specific field overrides" }
  }
}
```

---

## 3. Extend ASTM Handler for GeneXpert Features

**File:** `tools/analyzer-mock-server/protocols/astm_handler.py`

### 3a. Fix H-record format (General ASTM)

**Current** (line 89):

```python
f"H|\\^&|||{analyzer_name}|||||||LIS2-A2|{timestamp}"
```

**Problem:** Missing H.3 (Message ID), H.10 (Receiver ID), H.12 (Processing ID),
H.13 (Version Number). Uses hardcoded "LIS2-A2" for version.

**Fixed:**

```python
msg_id = kwargs.get("message_id") or f"MSG-{timestamp}-{random.randint(100, 999)}"
receiver = astm_config.get("receiver_id", "")
processing = astm_config.get("processing_id", "P")
version = astm_config.get("version_number", "LIS2-A2")
f"H|\\^&|{msg_id}||{analyzer_name}|||||{receiver}||{processing}|{version}|{timestamp}"
```

This generates the GeneXpert format:
`H|\^&|MSG-UUID||GeneXpert PC^GeneXpert^4.6.0|||||LIS||P|1394-97|20240515143022`

**Backward compatibility:** Existing templates without `astm_config` get
defaults: empty receiver, "P" processing, "LIS2-A2" version — functionally
equivalent to current behavior.

### 3b. Fix O-record format (General ASTM)

**Current** (line 91):

```python
f"O|1|{sample_id}^LAB|{panel_name}^{panel_name} Panel||{order_ts}"
```

**Problem:** Non-standard format. Missing O.5 (Universal Test ID in correct
position), O.6 (Priority), O.12 (Action Code), O.16 (Specimen Descriptor), O.26
(Report Type).

**Fixed:** Generate one O-record per sample (not per panel). For each sample's
test(s), put the test ID at O.5:

```python
specimen_desc = astm_config.get("specimen_descriptor", "")
action_code = ""  # Empty for patient, "Q" for QC
report_type = "F"  # Final
f"O|1|{sample_id}||^^^{first_test_code}|R|{order_ts}|||||{action_code}||||{specimen_desc}|||^^|||||||{report_type}"
```

For GeneXpert:
`O|1|SPECIMEN-GX-001||^^^MTB-RIF|R|20240515140000|||||||||ORH|||^^|||||||F`

**Backward compatibility:** Non-GeneXpert templates continue working — they get
empty action code, empty specimen descriptor.

### 3c. Extend R-record for multi-level results

**Current R-record** (line 122):

```python
f"R|{seq}|{test_id}|{value}|{unit}|{normal_range}|N||F|{result_ts}"
```

**Add support for GeneXpert result hierarchy:**

1. **Main result** (qualitative):
   `R|1|^^^MTB-RIF^Xpert MTB/RIF^2.1^^|POSITIVE^|||||F||OperatorName|startTs|endTs|^serialNum^^^^`
2. **Complementary result** (numeric, e.g., Conc/LOG):
   `R|2|^^^MTB-RIF^Xpert MTB/RIF^2.1^^Conc/LOG|^3.10|log(copies/mL)|...|F||...`

The R.3 field format per GeneXpert spec (components separated by `^`):

- Component 1-3: Empty (authority codes)
- Component 4: Test Code (e.g., "MTB-RIF")
- Component 5: Test Name (e.g., "Xpert MTB/RIF")
- Component 6: Version (e.g., "2.1")
- Component 7: Analyte Name (empty for main result)
- Component 8: Complementary Name (e.g., "Conc/LOG", "Ct")

**Implementation:** Add a `_build_genexpert_result()` function that generates
the extended R-record format when `astm_config` is present. Falls back to simple
format for non-GeneXpert templates.

### 3d. Add QC message generation

When `astm_config.enable_qc` is true and `qcSample` is defined in template:

Generate a separate message (after the patient message) with:

- Same H/L records
- P-record with QC patient info (or empty)
- O-record with `action_code = "Q"` in O.12
- R-records with QC seed values

This is how the GeneXpert reports QC results — Action Code "Q" in the Order
record marks it as QC (per PDF spec Section 6.1 & 6.3.4.1.4).

### 3e. Add Comment record (C) for error simulation

Per GeneXpert spec, when a test fails, an error Comment record follows the
R-record:

```
R|1|^^^MTB-RIF^Xpert MTB/RIF^2.1^^|ERROR^|||||F||...
C|1|I|Error^^Error^^20240515140630|N
```

Add optional error simulation: if a field's generated value is "ERROR", also
emit a C-record.

---

## 4. Update `_build_astm_message()` Signature

**File:** `tools/analyzer-mock-server/protocols/astm_handler.py`

Extend `_build_astm_message()` to accept the new parameters:

```python
def _build_astm_message(
    analyzer_name: str,
    fields: List[Dict[str, Any]],
    panel_name: str = "CBC",
    patient_id: Optional[str] = None,
    sample_id: Optional[str] = None,
    patient_name: Optional[str] = None,
    patient_dob: Optional[str] = None,
    patient_sex: Optional[str] = None,
    # New parameters:
    astm_config: Optional[Dict[str, Any]] = None,
    message_id: Optional[str] = None,
    action_code: str = "",
    operator_id: Optional[str] = None,
) -> str:
```

And update `ASTMHandler.generate()` (line 141) to pass `astm_config` from the
template:

```python
def generate(self, template: Dict[str, Any], **kwargs) -> str:
    ...
    astm_config = template.get("astm_config", {})
    return _build_astm_message(
        ...,
        astm_config=astm_config,
        action_code=kwargs.get("action_code", ""),
        operator_id=kwargs.get("operator_id"),
    )
```

**Backward compatibility:** All new parameters have defaults that preserve
current behavior. Templates without `astm_config` generate identical output to
before.

---

## 5. Normalize Field Extraction for GeneXpert

**File:** `tools/analyzer-mock-server/protocols/astm_handler.py`
(`_normalize_fields_from_template`)

Extend to carry GeneXpert-specific metadata through to the R-record builder:

```python
def _normalize_fields_from_template(template):
    out = []
    for f in template.get("fields", []):
        code = f.get("code", f.get("name", "Unknown"))
        out.append({
            "name": f.get("name", "Unknown"),
            "displayName": f.get("displayName", f.get("name", "Unknown")),
            "astmRef": f.get("astmRef", f"^^^{code}"),
            "type": f.get("type", "NUMERIC"),
            "unit": f.get("unit") or "",
            "normalRange": f.get("normalRange", ""),
            "possibleValues": f.get("possibleValues"),
            "seedValue": f.get("seedValue"),
            "seedQualitative": f.get("seedQualitative"),
            # NEW: GeneXpert-specific
            "version": f.get("version"),
            "complementaryResults": f.get("complementaryResults", []),
        })
    return out
```

---

## 6. Update Existing ASTM Templates (Optional Enhancement)

The 4 existing ASTM templates (Horiba Pentra 60, Horiba Micros 60, Stago STart
4, Mindray BA-88A) work correctly today. However, for consistency, we can
optionally add `astm_config` to them:

```json
"astm_config": {
  "processing_id": "P",
  "version_number": "LIS2-A2"
}
```

This is **low priority** — the defaults handle this correctly. Only add if we
want explicit documentation of their protocol version.

---

## 7. Verification

### 7a. Unit test: GeneXpert ASTM template generates valid messages

```bash
cd tools/analyzer-mock-server
python -c "
from protocols.astm_handler import ASTMHandler
import json
with open('templates/genexpert_astm.json') as f:
    template = json.load(f)
handler = ASTMHandler()
msg = handler.generate(template)
print(msg)
# Verify:
# - H-record has 'GeneXpert PC^GeneXpert^4.6.0' in field 5
# - H-record has '1394-97' in field 13
# - O-record has ^^^MTB-RIF in field 5
# - O-record has 'ORH' in field 16
# - R-record has ^^^MTB-RIF^Xpert MTB/RIF^2.1^^ in field 3
# - R-record has qualitative value in field 4
# - L-record terminates with 'N'
"
```

### 7b. Integration test: Push GeneXpert ASTM to bridge

```bash
# Start harness
cd projects/analyzer-harness
docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml up -d

# Push GeneXpert ASTM message through bridge
docker exec analyzer-harness-astm-simulator-1 python server.py \
  --template genexpert_astm \
  --push https://oe:8443/api/OpenELIS-Global/analyzer/astm

# Verify in OpenELIS:
# 1. Navigate to https://localhost/analyzers
# 2. Check that GeneXpert results appear in the analyzer import queue
# 3. Verify test codes (MTB-RIF, HIV-VL, COVID19) are matched
```

### 7c. E2E: GenericASTM plugin parses GeneXpert message correctly

The key validation is that the GenericASTM plugin at
[GenericASTMAnalyzer.java](plugins/analyzers/GenericASTM/src/main/java/org/openelisglobal/plugins/analyzer/genericastm/GenericASTMAnalyzer.java)
correctly:

1. Matches H-segment field 4 against `identifier_pattern = "GENEXPERT|CEPHEID"`
   - The mock sends `GeneXpert PC^GeneXpert^4.6.0` which doesn't match
     "GENEXPERT" directly
   - **Action needed:** Either update the mock's `astm_header` to include
     "GENEXPERT" (e.g., `GENEXPERT^GeneXpert^4.6.0`) OR update the identifier
     pattern to match "GeneXpert"
   - Recommend: Use `astm_header: "GENEXPERT^GeneXpert^4.6.0"` in the template
     to match what a real GeneXpert configured with system ID "GENEXPERT" would
     send
2. Extracts R.3 test code (split[2] component 4 after `^` split) = "MTB-RIF"
3. Looks up `AnalyzerTestMapping` for analyzer + "MTB-RIF" → finds OpenELIS test
4. Extracts R.4 value (split[3]) = "POSITIVE" or "NEGATIVE"

### 7d. QC message validation

Verify that the QC message has Action Code "Q" in O.12 and that the GenericASTM
plugin (or future QC identification rules per the gap analysis) can distinguish
it from patient results.

---

## Critical Files

| File                                                             | Action                                                                                                     |
| ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `tools/analyzer-mock-server/templates/genexpert_astm.json`       | **New** — GeneXpert ASTM template                                                                          |
| `tools/analyzer-mock-server/templates/schema.json`               | **Modify** — Add `astm_config`, `qcSample`, field-level `version`/`complementaryResults`/`seedQualitative` |
| `tools/analyzer-mock-server/protocols/astm_handler.py`           | **Modify** — Fix H/O record format, add multi-level R-records, QC generation, C-records                    |
| `tools/analyzer-mock-server/templates/genexpert.json`            | **Read-only** — Existing HL7 template (keep as-is for HL7 mode testing)                                    |
| `tools/analyzer-mock-server/templates/horiba_pentra60.json`      | **Read-only** — Reference for existing ASTM template pattern                                               |
| `projects/analyzer-defaults/astm/genexpert-astm.json`            | **Read-only** — OpenELIS default config for GeneXpert (identifier_pattern, test mappings)                  |
| `plugins/analyzers/GenericASTM/.../GenericASTMLineInserter.java` | **Read-only** — Verify field position alignment                                                            |
| `plugins/analyzers/GenericASTM/.../GenericASTMAnalyzer.java`     | **Read-only** — Verify H-segment identifier matching                                                       |

## Git Workflow (Submodule)

The mock server lives in a separate git repository
(`DIGI-UW/analyzer-mock-server`) referenced as a submodule at
`tools/analyzer-mock-server/`. Changes require **two PRs** — one in the
submodule repo, one in the parent repo to update the submodule pointer.

### Submodule PR (analyzer-mock-server)

**Remote:** `https://github.com/DIGI-UW/analyzer-mock-server.git` **Current
branch:** `main` (at commit `7cd4a9c`)

```bash
# 1. Switch to the submodule directory
cd tools/analyzer-mock-server

# 2. Create feature branch
git checkout -b feat/genexpert-astm-template

# 3. Make all changes (schema, template, handler)
# ... (implementation steps below)

# 4. Commit and push
git add templates/genexpert_astm.json templates/schema.json protocols/astm_handler.py
git commit -m "feat: add GeneXpert ASTM template and improve ASTM handler compliance"
git push -u origin feat/genexpert-astm-template

# 5. Create PR in DIGI-UW/analyzer-mock-server
gh pr create --repo DIGI-UW/analyzer-mock-server \
  --base main \
  --title "feat: GeneXpert ASTM template + ASTM handler compliance" \
  --body "## Summary
- Add genexpert_astm.json template per Cepheid LIS Protocol Spec (Rev E)
- Extend schema.json with astm_config, qcSample, multi-level results
- Fix H-record (missing Message ID, Receiver, Processing ID, Version)
- Fix O-record (correct field positions, Action Code, Specimen Descriptor)
- Add multi-level R-record support (main + complementary results)
- Add QC message generation (Action Code Q in O.12)
- Add Comment record (C) for error simulation

## Related
- Parent repo PR: OpenELIS-Global-2#XXXX (submodule pointer update)
- Spec: specs/011-madagascar-analyzer-integration/research/astm-crosswalk-gap-analysis.md
- Reference: Cepheid GeneXpert LIS Protocol Specification Rev E (Sections 4-6)
"
```

### Parent Repo PR (OpenELIS-Global-2)

After the submodule PR is merged (or at least created for review):

```bash
# 1. Back in parent repo root
cd /home/ubuntu/OpenELIS-Global-2

# 2. Create branch in parent repo
git checkout -b feat/011-genexpert-astm-mock

# 3. Update submodule pointer to the new commit
cd tools/analyzer-mock-server
git checkout feat/genexpert-astm-template  # or main after merge
cd ../..
git add tools/analyzer-mock-server

# 4. Commit and push
git commit -m "chore: update analyzer-mock-server submodule (GeneXpert ASTM template)"
git push -u origin feat/011-genexpert-astm-mock

# 5. Create PR targeting develop
gh pr create --base develop \
  --title "chore: update analyzer-mock-server for GeneXpert ASTM" \
  --body "## Summary
Updates analyzer-mock-server submodule to include GeneXpert ASTM template and ASTM handler improvements.

## Related
- Submodule PR: DIGI-UW/analyzer-mock-server#XX
- Parent config form PR: #2941
"
```

### Cross-References

| PR                                | Repo      | Description                             | Depends On              |
| --------------------------------- | --------- | --------------------------------------- | ----------------------- |
| `DIGI-UW/analyzer-mock-server#XX` | Submodule | GeneXpert ASTM template + handler fixes | —                       |
| `OpenELIS-Global-2#YYYY`          | Parent    | Submodule pointer update                | Submodule PR            |
| `OpenELIS-Global-2#2941`          | Parent    | Analyzer config form fix (already open) | Independent but related |

---

## Implementation Order

1. **Archive plan**: Save this plan to
   `specs/011-madagascar-analyzer-integration/plans/mock-server-astm-remediation.md`
2. **Create submodule branch**:
   `cd tools/analyzer-mock-server && git checkout -b feat/genexpert-astm-template`
3. Extend `schema.json` with new properties (astm_config, qcSample, field-level
   extensions)
4. Create `genexpert_astm.json` template
5. Refactor `_build_astm_message()` to accept `astm_config` parameter
6. Fix H-record: add Message ID, Receiver, Processing ID, Version
7. Fix O-record: correct field positions, add Action Code, Specimen Descriptor,
   Report Type
8. Add multi-level R-record support (main + complementary results)
9. Add QC message generation
10. Add C-record (Comment) for error simulation
11. Run unit test (7a) to verify message format
12. **Commit, push, and create submodule PR** in `DIGI-UW/analyzer-mock-server`
13. **Create parent repo branch**:
    `git checkout -b feat/011-genexpert-astm-mock` from `develop`
14. **Update submodule pointer**: `git add tools/analyzer-mock-server` +
    commit + push
15. **Create parent repo PR** targeting `develop` with cross-references
16. Run integration test (7b) against harness

## Scope Boundaries

**In scope:**

- New GeneXpert ASTM template
- ASTM handler improvements for standards compliance
- QC sample generation
- Multi-level result support
- Comment record support

**Out of scope (future work per gap analysis):**

- Bidirectional host query simulation (gap analysis Section 6.3.1) — mock server
  already has basic support
- ISID generation (enable_isid flag defined but not implemented) — Phase 2
- Value transformation rules in OpenELIS (gap analysis Gap 3) — separate backend
  work
- Result aggregation modes (gap analysis Gap 4) — separate backend work
- Message Simulator UI (gap analysis Gap 5) — separate frontend work
