# Stage 5: Assay/Test Execution

**Branch**: `feat/053-tb-lab-workflow` | **Date**: 2025-12-24 **Parent Spec**:
`specs/053-tb-lab-workflow/spec.md`

## Overview

Stage 5 covers comprehensive diagnostic test execution for TB samples including
Culture final results, Smear Microscopy (AFB), GeneXpert, and Drug
Susceptibility Testing (DST). Each test type has distinct result structures and
documentation requirements including "Test done by" and "Reviewed by" fields.

**Key Workflow:** Culture Positive → Smear Microscopy → GeneXpert → Species
Identification → DST (if MTB)

## User Stories

### US5.A - Culture Final Results (Priority: P1)

Record final culture outcome after incubation monitoring.

**Acceptance Scenarios:**

1. **Given** culture positive at any week, **When** recording final result,
   **Then** result = Positive with positive_week, method, date
2. **Given** Week 8 no growth, **When** recording, **Then** result = Negative
   with final date
3. **Given** contamination detected, **When** recording, **Then** result =
   Contaminated with option to re-process
4. **Given** final result recorded, **Then** Result date, Test done by, Reviewed
   by captured

### US5.B - Smear Microscopy/AFB (Priority: P1)

Record smear microscopy results with AFB grading. _Existing TbSmearResult
entity._

**Acceptance Scenarios:**

1. **Given** smear requested, **When** recording, **Then** method
   (ZN/Concentrated/Fluorescence/Other) captured
2. **Given** smear done, **When** entering result, **Then** AFB grade
   (Negative/Scanty/1+/2+/3+) recorded
3. **Given** result complete, **Then** Result date, Test done by, Reviewed by
   captured
4. **Given** direct smear (before culture), **When** recording, **Then** sample
   can have smear without culture result

### US5.C - GeneXpert (Priority: P1)

Record GeneXpert rapid molecular test results. **NEW ENTITY.**

**Acceptance Scenarios:**

1. **Given** GeneXpert requested, **When** recording, **Then** method (GeneXpert
   MTB/RIF, Real-Time PCR, Other) captured
2. **Given** GeneXpert done, **Then** MTB result (Detected/Not Detected) + Rif
   status (Sensitive/Resistant/Indeterminate) recorded
3. **Given** Rif Resistant result, **Then** system displays clinical
   alert/warning
4. **Given** result complete, **Then** Result date, Test done by, Reviewed by
   captured
5. **Given** MTB not detected, **Then** sample marked as GeneXpert negative,
   culture may still proceed

### US5.D - Drug Susceptibility Testing (Priority: P2)

Record DST results for 1st and 2nd line anti-TB drugs. **NEW ENTITY.**

**Acceptance Scenarios:**

1. **Given** DST requested, **When** recording, **Then** method (Phenotypic 1st
   line, Phenotypic 2nd line, Molecular 1st line) captured
2. **Given** 1st line testing, **Then** INH/RMP/STM/EMB/PZA results
   (Sensitive/Resistant/Invalid) recorded individually
3. **Given** 2nd line testing, **Then** FLQ, KAN/AMK/CAP, KAN/CAP/VIO,
   KAN/AMK/CAP/VIO, Low level KAN results recorded
4. **Given** INH-R + RMP-R, **Then** MDR-TB flag auto-computed and displayed as
   alert
5. **Given** result complete, **Then** Result date, Test done by, Reviewed by
   captured

---

## Key Entities

### Existing: TbSmearResult (already implemented)

```java
// NOTE: TbSmearResult already exists and uses SampleItem, not NotebookPageSample
@Entity
@Table(name = "tb_smear_result")
public class TbSmearResult extends BaseObject<Integer> {
    private SampleItem sampleItem;        // Existing FK - matches codebase pattern
    private SmearMethod method;           // ZN, CONCENTRATED, FLUORESCENT, OTHER
    private AfbResult afbResult;          // NEGATIVE, SCANTY, PLUS1, PLUS2, PLUS3
    private Timestamp resultDate;
    private SystemUser testedBy;          // FK to SystemUser
    private SystemUser reviewedBy;        // FK to SystemUser
    private String notes;
    // No changes needed - entity already complete
}
```

### New: TbGeneXpertResult

```java
@Entity
@Table(name = "tb_genexpert_result")
public class TbGeneXpertResult extends BaseObject<Integer> {
    private SampleItem sampleItem;        // FK to SampleItem (matches codebase pattern)
    // Note: MolecularMethod enum already exists in TbEnums.java (GENEXPERT, REALTIME_PCR, OTHER)
    private MolecularMethod method;       // Use existing enum, not new GeneXpertMethod
    private MtbDetectionResult mtbResult; // NEW ENUM NEEDED: DETECTED, NOT_DETECTED
    private RifResult rifResult;          // NEW ENUM NEEDED: SENSITIVE, RESISTANT, INDETERMINATE, N_A
    private Date resultDate;
    private SystemUser testedBy;          // FK to SystemUser (not free text)
    private SystemUser reviewedBy;        // FK to SystemUser
    private Boolean rifResistanceAlert;   // Computed on save via @PrePersist
    private String notes;
    private Timestamp lastupdated;
}
```

**Enum Updates Required in TbEnums.java**:

- Refactor existing `GeneXpertResult` enum into:
  - `MtbDetectionResult` (DETECTED, NOT_DETECTED)
  - `RifResult` (SENSITIVE, RESISTANT, INDETERMINATE, N_A)
- Keep existing `MolecularMethod` as-is

### New: TbDstResult

```java
@Entity
@Table(name = "tb_dst_result")
public class TbDstResult extends BaseObject<Integer> {
    private SampleItem sampleItem;        // FK to SampleItem (matches codebase pattern)
    private DstMethod method;             // Already exists in TbEnums: PHENOTYPIC_1ST, PHENOTYPIC_2ND, MOLECULAR_1ST

    // 1st line drugs
    private DrugSusceptibility inhResult; // SENSITIVE, RESISTANT, INVALID
    private DrugSusceptibility rmpResult;
    private DrugSusceptibility stmResult;
    private DrugSusceptibility embResult;
    private DrugSusceptibility pzaResult;

    // 2nd line drugs (JSONB for flexibility)
    private String secondLineResults;     // JSON: {flq: "S", kanAmkCap: "R", ...}

    private Boolean mdrFlag;              // Computed: INH-R AND RMP-R
    private Boolean xdrFlag;              // Computed: MDR + FLQ-R + injectable-R
    private Date resultDate;
    private String testedBy;
    private String reviewedBy;
    private String notes;
    private Timestamp lastupdated;
}
```

---

## Database Schema

### tb_genexpert_result

```sql
CREATE TABLE tb_genexpert_result (
    id SERIAL PRIMARY KEY,
    notebook_page_sample_id INTEGER NOT NULL REFERENCES notebook_page_sample(id),
    method VARCHAR(30) NOT NULL CHECK (method IN ('GENEXPERT_MTB_RIF', 'REALTIME_PCR', 'OTHER')),
    mtb_result VARCHAR(20) NOT NULL CHECK (mtb_result IN ('DETECTED', 'NOT_DETECTED')),
    rif_result VARCHAR(20) CHECK (rif_result IN ('SENSITIVE', 'RESISTANT', 'INDETERMINATE', 'N_A')),
    result_date DATE NOT NULL,
    tested_by VARCHAR(100),
    reviewed_by VARCHAR(100),
    rif_resistance_alert BOOLEAN DEFAULT FALSE,
    notes TEXT,
    lastupdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_genexpert_sample UNIQUE (notebook_page_sample_id)
);

CREATE INDEX idx_genexpert_nps ON tb_genexpert_result(notebook_page_sample_id);
CREATE INDEX idx_genexpert_mtb ON tb_genexpert_result(mtb_result);
CREATE INDEX idx_genexpert_rif ON tb_genexpert_result(rif_result);
```

### tb_dst_result

```sql
CREATE TABLE tb_dst_result (
    id SERIAL PRIMARY KEY,
    notebook_page_sample_id INTEGER NOT NULL REFERENCES notebook_page_sample(id),
    method VARCHAR(20) NOT NULL CHECK (method IN ('PHENOTYPIC_1ST', 'PHENOTYPIC_2ND', 'MOLECULAR_1ST')),

    -- 1st line drugs
    inh_result VARCHAR(10) CHECK (inh_result IN ('SENSITIVE', 'RESISTANT', 'INVALID')),
    rmp_result VARCHAR(10) CHECK (rmp_result IN ('SENSITIVE', 'RESISTANT', 'INVALID')),
    stm_result VARCHAR(10) CHECK (stm_result IN ('SENSITIVE', 'RESISTANT', 'INVALID')),
    emb_result VARCHAR(10) CHECK (emb_result IN ('SENSITIVE', 'RESISTANT', 'INVALID')),
    pza_result VARCHAR(10) CHECK (pza_result IN ('SENSITIVE', 'RESISTANT', 'INVALID')),

    -- 2nd line drugs as JSONB
    second_line_results JSONB,

    mdr_flag BOOLEAN DEFAULT FALSE,
    xdr_flag BOOLEAN DEFAULT FALSE,
    result_date DATE NOT NULL,
    tested_by VARCHAR(100),
    reviewed_by VARCHAR(100),
    notes TEXT,
    lastupdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dst_nps ON tb_dst_result(notebook_page_sample_id);
CREATE INDEX idx_dst_mdr ON tb_dst_result(mdr_flag);
CREATE INDEX idx_dst_method ON tb_dst_result(method);
```

---

## Implementation Tasks

### T5-01: Liquibase for tb_genexpert_result

**File**: `src/main/resources/liquibase/3.4.x.x/035-tb-genexpert-result.xml`
_Note: Stage 4 uses 032-034, Stage 5 starts at 035_

### T5-02: Liquibase for tb_dst_result

**File**: `src/main/resources/liquibase/3.4.x.x/036-tb-dst-result.xml`

### T5-03: Update base.xml

**File**: `src/main/resources/liquibase/3.4.x.x/base.xml`

### T5-04: TbEnums enhancement

**File**: `src/main/java/org/openelisglobal/tb/valueholder/TbEnums.java`

Add/update enums:

- `GeneXpertMethod` (GENEXPERT_MTB_RIF, REALTIME_PCR, OTHER)
- `MtbDetectionResult` (DETECTED, NOT_DETECTED)
- `RifResult` (SENSITIVE, RESISTANT, INDETERMINATE, N_A)
- `DstMethod` (PHENOTYPIC_1ST, PHENOTYPIC_2ND, MOLECULAR_1ST)
- `DrugSusceptibility` (SENSITIVE, RESISTANT, INVALID)

### T5-05: TbGeneXpertResult valueholder

**File**:
`src/main/java/org/openelisglobal/tb/valueholder/TbGeneXpertResult.java`

### T5-06: TbDstResult valueholder

**File**: `src/main/java/org/openelisglobal/tb/valueholder/TbDstResult.java`

Include:

- `@PrePersist` / `@PreUpdate` to compute `mdrFlag` (INH-R AND RMP-R)
- Helper method `isMdr()`, `isXdr()`

### T5-07: TbGeneXpertResultDAO

**Files**:

- `src/main/java/org/openelisglobal/tb/dao/TbGeneXpertResultDAO.java`
- `src/main/java/org/openelisglobal/tb/daoimpl/TbGeneXpertResultDAOImpl.java`

### T5-08: TbDstResultDAO

**Files**:

- `src/main/java/org/openelisglobal/tb/dao/TbDstResultDAO.java`
- `src/main/java/org/openelisglobal/tb/daoimpl/TbDstResultDAOImpl.java`

### T5-09: TbGeneXpertResultService

**Files**:

- `src/main/java/org/openelisglobal/tb/service/TbGeneXpertResultService.java`
- `src/main/java/org/openelisglobal/tb/service/TbGeneXpertResultServiceImpl.java`

Methods:

- `saveResult(TbGeneXpertResult result)` - auto-sets rifResistanceAlert
- `getByNotebookPageSampleId(Integer npsId)`
- `getRifResistantSamples(Integer entryId)` - for alerts

### T5-10: TbDstResultService

**Files**:

- `src/main/java/org/openelisglobal/tb/service/TbDstResultService.java`
- `src/main/java/org/openelisglobal/tb/service/TbDstResultServiceImpl.java`

Methods:

- `saveResult(TbDstResult result)` - auto-computes mdrFlag, xdrFlag
- `getMdrSamples(Integer entryId)` - for MDR alerts
- `getDstSummary(Integer npsId)` - formatted drug results

### T5-11: TbAssayController

**File**:
`src/main/java/org/openelisglobal/tb/controller/rest/TbAssayController.java`

Endpoints:

- `POST /rest/tb/assays/genexpert` - save GeneXpert result
- `GET /rest/tb/assays/genexpert/{npsId}` - get result
- `POST /rest/tb/assays/dst` - save DST result
- `GET /rest/tb/assays/dst/{npsId}` - get result
- `GET /rest/tb/assays/alerts/{entryId}` - get Rif/MDR alerts

### T5-12: ORM Validation Tests

**Files**:

- `src/test/java/org/openelisglobal/tb/TbGeneXpertResultOrmTest.java`
- `src/test/java/org/openelisglobal/tb/TbDstResultOrmTest.java`

### T5-13: MDR Flag Computation Tests

**File**:
`src/test/java/org/openelisglobal/tb/service/TbDstResultServiceTest.java`

Test scenarios:

- INH-S + RMP-S → mdrFlag = false
- INH-R + RMP-S → mdrFlag = false
- INH-S + RMP-R → mdrFlag = false
- INH-R + RMP-R → mdrFlag = true
- Invalid results handling

### T5-13b: GeneXpert Service Tests (GAP FIX)

**File**:
`src/test/java/org/openelisglobal/tb/service/TbGeneXpertResultServiceTest.java`

Test scenarios:

- Save GeneXpert result with MTB_DETECTED + RIF_RESISTANT → rifResistanceAlert =
  true
- Save GeneXpert result with MTB_DETECTED + RIF_SENSITIVE → rifResistanceAlert =
  false
- Save GeneXpert result with MTB_NOT_DETECTED → rifResult should be N_A
- Get by SampleItem ID
- Get Rif resistant samples for entry

### T5-14: Controller Integration Tests

**File**:
`src/test/java/org/openelisglobal/tb/controller/TbAssayControllerTest.java`

---

## Frontend Tasks

### T5-15: Create TBGeneXpertPage

**File**: `frontend/src/components/notebook/pages/tb/TBGeneXpertPage.js`

Carbon form with:

- Method dropdown (GeneXpert MTB/RIF, Real-Time PCR, Other)
- MTB Result (Detected / Not Detected)
- Rif Result (conditional on MTB Detected)
- Result date picker
- Test done by / Reviewed by fields
- Rif Resistance alert banner (when applicable)

### T5-16: Create TBDSTPage

**File**: `frontend/src/components/notebook/pages/tb/TBDSTPage.js`

Carbon form with:

- Method selection tabs (Phenotypic 1st, Phenotypic 2nd, Molecular 1st)
- 1st line drug panel: INH, RMP, STM, EMB, PZA (radio buttons: S/R/Invalid)
- 2nd line drug panel (conditional)
- MDR-TB alert banner (auto-computed)
- XDR-TB alert banner (if applicable)
- Result date / Test done by / Reviewed by

### T5-17: Create MdrAlertBanner component

**File**:
`frontend/src/components/notebook/pages/tb/components/MdrAlertBanner.js`

Carbon InlineNotification with:

- MDR-TB warning (INH-R + RMP-R)
- Rif resistance warning
- XDR-TB warning
- Link to supervisor review

### T5-18: Create TestedReviewedBy component

**File**:
`frontend/src/components/notebook/pages/tb/components/TestedReviewedBy.js`

Reusable component for:

- Result date picker
- Test done by text input
- Reviewed by text input

### T5-19: Verify TBSmearResultsPage

**File**: `frontend/src/components/notebook/pages/tb/TBSmearResultsPage.js`

Verify existing page has:

- All required fields
- TestedReviewedBy component integrated
- Proper validation

### T5-20: Update TBWorkflowTab

**File**: `frontend/src/components/notebook/workflow/TBWorkflowTab.js`

Add Stage 5 pages to workflow navigation.

### T5-21: i18n keys

**Files**:

- `frontend/src/languages/en.json`
- `frontend/src/languages/fr.json`

Add keys for:

- GeneXpert page labels
- DST page labels (all drugs)
- MDR/XDR alert messages
- TestedReviewedBy labels

### T5-22: SWR hooks

**File**: `frontend/src/components/notebook/pages/tb/hooks/useStage5.js`

Hooks:

- `useGeneXpertResult(npsId)`
- `useDstResult(npsId)`
- `useSaveGeneXpert()`
- `useSaveDst()`
- `useAssayAlerts(entryId)`

### T5-23: Jest tests

**Files**:

- `frontend/src/components/notebook/pages/tb/TBGeneXpertPage.test.js`
- `frontend/src/components/notebook/pages/tb/TBDSTPage.test.js`
- `frontend/src/components/notebook/pages/tb/components/MdrAlertBanner.test.js`

---

## API Endpoints

### GeneXpert

```
POST /rest/tb/assays/genexpert
Request: {
    notebookPageSampleId: 1,
    method: "GENEXPERT_MTB_RIF",
    mtbResult: "DETECTED",
    rifResult: "RESISTANT",
    resultDate: "2025-01-20",
    testedBy: "Dr. Smith",
    reviewedBy: "Dr. Jones"
}
Response: {
    id: 1,
    rifResistanceAlert: true,
    message: "Rifampicin resistance detected - clinical attention required"
}
```

### DST

```
POST /rest/tb/assays/dst
Request: {
    notebookPageSampleId: 1,
    method: "PHENOTYPIC_1ST",
    inhResult: "RESISTANT",
    rmpResult: "RESISTANT",
    stmResult: "SENSITIVE",
    embResult: "SENSITIVE",
    pzaResult: "SENSITIVE",
    resultDate: "2025-02-15",
    testedBy: "Lab Tech A",
    reviewedBy: "Dr. Jones"
}
Response: {
    id: 1,
    mdrFlag: true,
    xdrFlag: false,
    message: "MDR-TB pattern detected (INH-R + RMP-R)"
}

POST /rest/tb/assays/dst (2nd line)
Request: {
    notebookPageSampleId: 1,
    method: "PHENOTYPIC_2ND",
    secondLineResults: {
        flq: "RESISTANT",
        kanAmkCap: "SENSITIVE",
        kanCapVio: "SENSITIVE"
    },
    resultDate: "2025-03-01",
    testedBy: "Lab Tech A",
    reviewedBy: "Dr. Jones"
}
```

### Alerts

```
GET /rest/tb/assays/alerts/{entryId}
Response: {
    rifResistant: [
        { npsId: 1, sampleId: "TB-001/25", resultDate: "2025-01-20" }
    ],
    mdr: [
        { npsId: 1, sampleId: "TB-001/25", resultDate: "2025-02-15" }
    ],
    xdr: []
}
```

---

## Success Criteria

- [ ] SC5-01: GeneXpert results saved with <30 second submission
- [ ] SC5-02: DST 1st line results show MDR alert within 1 second of INH-R +
      RMP-R
- [ ] SC5-03: Rif resistance triggers visual alert banner
- [ ] SC5-04: All test types capture Test done by / Reviewed by / Result date
- [ ] SC5-05: DST 2nd line drugs stored as JSONB with proper validation
- [ ] SC5-06: Backend MDR/XDR flag computation covered by unit tests
- [ ] SC5-07: Frontend MDR alert banner displays correctly

---

## Files Summary

| Category  | File                                            | Action |
| --------- | ----------------------------------------------- | ------ |
| Liquibase | `liquibase/3.4.x.x/035-tb-genexpert-result.xml` | CREATE |
| Liquibase | `liquibase/3.4.x.x/036-tb-dst-result.xml`       | CREATE |
| Backend   | `tb/valueholder/TbEnums.java`                   | MODIFY |
| Backend   | `tb/valueholder/TbGeneXpertResult.java`         | CREATE |
| Backend   | `tb/valueholder/TbDstResult.java`               | CREATE |
| Backend   | `tb/dao/TbGeneXpertResultDAO.java`              | CREATE |
| Backend   | `tb/dao/TbDstResultDAO.java`                    | CREATE |
| Backend   | `tb/service/TbGeneXpertResultService.java`      | CREATE |
| Backend   | `tb/service/TbDstResultService.java`            | CREATE |
| Backend   | `tb/controller/rest/TbAssayController.java`     | CREATE |
| Test      | `tb/TbGeneXpertResultOrmTest.java`              | CREATE |
| Test      | `tb/TbDstResultOrmTest.java`                    | CREATE |
| Test      | `tb/service/TbDstResultServiceTest.java`        | CREATE |
| Frontend  | `pages/tb/TBGeneXpertPage.js`                   | CREATE |
| Frontend  | `pages/tb/TBDSTPage.js`                         | CREATE |
| Frontend  | `pages/tb/components/MdrAlertBanner.js`         | CREATE |
| Frontend  | `pages/tb/components/TestedReviewedBy.js`       | CREATE |
| Frontend  | `pages/tb/hooks/useStage5.js`                   | CREATE |
| Frontend  | `languages/en.json`                             | MODIFY |
| Frontend  | `languages/fr.json`                             | MODIFY |

---

## Dependencies

- Requires Stage 4 (culture positive samples for DST)
- Existing TbSmearResult entity and TBSmearResultsPage
- Culture tracking for positive/negative status
