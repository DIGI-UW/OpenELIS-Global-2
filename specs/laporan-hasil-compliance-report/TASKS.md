# Laporan Hasil — Compliance Report: Build Tasks

## Overview

Build the "Laporan Hasil — Compliance Report" feature as seen in `lh_report.png` (root of repo).

This report lists environmental lab orders with compliance evaluation against regulatory standards
(e.g., PP No. 22/2021), with an expandable detail row per order showing site info, collection
conditions, per-parameter compliance summary, and analyst + lab manager e-signatures.

### What already exists (DO NOT rebuild)
- `compliance_standard`, `parameter_group`, `compliance_threshold`, `sample_compliance_standards` DB tables
- `ComplianceStandard`, `ComplianceThreshold`, `ParameterGroup`, `SampleComplianceStandard` entities/services
- `ElectronicSignature` entity + `ElectronicSignatureService` — use to fetch AUTHORED (analyst) and
  VALIDATED_AND_RELEASED (supervisor) signatures per sample
- iText5 PDF generation pattern — see `AuditTrailReportRestController.java` for the exact pattern
- Report routing pattern — `RoutineReport?type=environmental&report=complianceResults` maps to
  `RoutineReports` component switch in `frontend/src/components/reports/routine/Index.jsx`
- Carbon DataTable, expandable rows, Tag, Button — all available from `@carbon/react`

### Architecture pattern to follow
Follow the **AuditTrail report** end-to-end pattern:
- Backend: `src/main/java/org/openelisglobal/audittrail/controller/rest/AuditTrailReportRestController.java`
- Frontend: `frontend/src/components/reports/auditTrailReport/AuditTrailReport.jsx`
- Route entry: `frontend/src/components/reports/routine/Index.jsx` (RoutineReports switch)
- Nav entry: `frontend/src/components/reports/Routine.jsx` (RoutineReportsMenu)

---

## Screenshot Reference

See `lh_report.png` in repo root. Key UI sections:

**Top bar:** breadcrumb `Reports / Laporan Hasil`, title "Laporan Hasil — Compliance Report",
subtitle "Generate Sertifikat Hasil Uji (Test Results Certificates) for validated environmental orders"

**Stats tiles (3):**
- Left (grey): count of Ineligible orders (not yet validated / no compliance standard)
- Middle (green): count of Generated reports
- Right (yellow): count of Not Yet Generated (validated but PDF not yet produced)

**Filter bar:**
- Date From / Date To (date pickers)
- Sampling Site (dropdown — all sites)
- Compliance Standard (dropdown — all standards)
- Compliance Status (dropdown: All / Compliant / Non-Compliant / Marginal)
- Generation Status (dropdown: All / Generated / Not Yet Generated)

**Main table columns:**
- Checkbox | Lab Number | Site | Standard | Collection Date | Tests (count) |
  Compliance (badge) | Last Generated | Actions (Generate PDF button)

**Expanded row (inline detail panel — 3 sections side by side + signatures):**

*Site Information:*
- Site, GPS coords, Collection Date/Time, Collection Method, PP No., Standard name

*Collection Conditions:*
- Water Temp, Ambient Temp, Weather, Preservation method

*Compliance Summary table:*
- Parameter | Result | Threshold | Status (Compliant ✓ / Non-Compliant ✗ / Marginal ⚑)

*Signatures:*
- Left: Lab Analyst — name, date/time, role
- Right: Lab Manager — name, date/time, role

---

## Tasks

### Task 1 — Liquibase: Add `compliance_report_generation` tracking table

**File:** `src/main/resources/liquibase/3.5.x.x/030-compliance-report-generation.xml`

Create a table to track when a compliance report PDF was last generated per sample.

```xml
<createTable tableName="compliance_report_generation">
    <column name="id" type="bigint" autoIncrement="true"><constraints primaryKey="true"/></column>
    <column name="sample_id" type="bigint"><constraints nullable="false"/></column>
    <column name="generated_at" type="timestamp with time zone"><constraints nullable="false"/></column>
    <column name="generated_by_user_id" type="varchar(255)"/>
    <column name="last_updated" type="timestamp with time zone"/>
</createTable>
<addForeignKeyConstraint baseTableName="compliance_report_generation"
    baseColumnNames="sample_id"
    referencedTableName="sample"
    referencedColumnNames="id"
    constraintName="fk_crg_sample"/>
<createIndex tableName="compliance_report_generation" indexName="idx_crg_sample_id">
    <column name="sample_id"/>
</createIndex>
```

Register this file in `src/main/resources/liquibase/db.changelog-master.xml`.

**Acceptance:** `mvn clean install -DskipTests -Dmaven.test.skip=true` passes.

---

### Task 2 — Java: `ComplianceReportGeneration` entity + DAO + Service

**Package:** `org.openelisglobal.compliance`

#### 2a. Valueholder
`src/main/java/org/openelisglobal/compliance/valueholder/ComplianceReportGeneration.java`

```java
@Entity
@Table(name = "compliance_report_generation")
public class ComplianceReportGeneration extends BaseObject<Long> {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "generated_by_user_id")
    private String generatedByUserId;
    // getters/setters
}
```

#### 2b. DAO interface + impl
- `ComplianceReportGenerationDAO.java` — interface with:
  - `Optional<ComplianceReportGeneration> findLatestBySampleId(Long sampleId)`
  - `ComplianceReportGeneration save(ComplianceReportGeneration entity)`
- `ComplianceReportGenerationDAOImpl.java` — JPA impl, follows pattern of `ComplianceStandardDAOImpl`

#### 2c. Service interface + impl
- `ComplianceReportGenerationService.java`
- `ComplianceReportGenerationServiceImpl.java`
  - `recordGeneration(Long sampleId, String userId)`
  - `getLastGenerated(Long sampleId): Optional<LocalDateTime>`

---

### Task 3 — Java: Compliance evaluation logic

**File:** `src/main/java/org/openelisglobal/compliance/service/ComplianceEvaluationService.java`

This service compares actual test results against thresholds and returns a per-parameter status
and a rolled-up order-level status.

```java
public interface ComplianceEvaluationService {
    /**
     * Evaluate all results for a sample against its linked compliance standard.
     * Returns null if no standard is linked or no results exist.
     */
    ComplianceEvaluationResult evaluate(Sample sample);
}
```

**`ComplianceEvaluationResult` DTO** (inner class or separate file):
```java
public class ComplianceEvaluationResult {
    private ComplianceStatus overallStatus;          // COMPLIANT, NON_COMPLIANT, MARGINAL
    private List<ParameterResult> parameterResults;  // one per test result

    public static class ParameterResult {
        private String parameterCode;
        private String displayName;
        private String resultValue;
        private String thresholdDisplay;  // e.g. "4.5–8.5" or "≤0.03"
        private String units;
        private ComplianceStatus status;
    }
}
```

**Logic in `ComplianceEvaluationServiceImpl`:**
1. Load `SampleComplianceStandard` for the sample (priority order, take first)
2. For each `Analysis` on the sample with a `Result`:
   - Find matching `ComplianceThreshold` by `parameterCode` = test name/code
   - Evaluate based on `ThresholdType`:
     - `RANGE`: COMPLIANT if `minValue ≤ result ≤ maxValue`, MARGINAL if within 10% of boundary, else NON_COMPLIANT
     - `MAXIMUM`: COMPLIANT if `result ≤ maxValue`, MARGINAL if `result ≤ maxValue * 1.1`, else NON_COMPLIANT
     - `MINIMUM`: COMPLIANT if `result ≥ minValue`, MARGINAL if `result ≥ minValue * 0.9`, else NON_COMPLIANT
     - `EXACT`: COMPLIANT if result matches `targetValue`, else NON_COMPLIANT
   - If no matching threshold found: skip (don't include in evaluation)
3. Roll up: any NON_COMPLIANT → overall NON_COMPLIANT; any MARGINAL (no NON_COMPLIANT) → MARGINAL; all COMPLIANT → COMPLIANT

**Dependencies to inject:** `SampleComplianceStandardDAO`, `ComplianceThresholdDAO`,
`AnalysisService`, `ResultService`

---

### Task 4 — Java: `ComplianceReportDTO` and `ComplianceReportOrderDTO`

**File:** `src/main/java/org/openelisglobal/compliance/controller/rest/ComplianceReportDTO.java`

Top-level response DTO for the report list endpoint:
```java
public class ComplianceReportDTO {
    private int ineligibleCount;
    private int generatedCount;
    private int notYetGeneratedCount;
    private List<ComplianceReportOrderDTO> orders;
}
```

**File:** `src/main/java/org/openelisglobal/compliance/controller/rest/ComplianceReportOrderDTO.java`

Per-order DTO matching the table row + expandable detail:
```java
public class ComplianceReportOrderDTO {
    // Table row fields
    private String labNumber;           // accession number
    private String siteCode;            // sample item location code
    private String siteName;            // sampling site display name
    private String standardName;        // ComplianceStandard display name
    private String regulationNumber;    // e.g. "PP No. 22/2021"
    private LocalDate collectionDate;
    private int testCount;
    private String complianceStatus;    // COMPLIANT / NON_COMPLIANT / MARGINAL / NONE
    private LocalDateTime lastGenerated; // null if never generated

    // Expandable — Site Information
    private String gpsCoordinates;      // "lat, lon" from sample_item
    private String collectionDateTime;
    private String collectionMethod;

    // Expandable — Collection Conditions (from environmentalFields map)
    private String waterTemp;
    private String ambientTemp;
    private String weather;
    private String preservation;

    // Expandable — Compliance Summary
    private List<ComplianceEvaluationResult.ParameterResult> parameterResults;

    // Expandable — Signatures
    private SignatureDTO analystSignature;
    private SignatureDTO managerSignature;

    public static class SignatureDTO {
        private String signerName;
        private String signerRole;  // "Lab Analyst" or "Lab Manager"
        private LocalDateTime signedAt;
    }
}
```

---

### Task 5 — Java: `ComplianceReportRestController`

**File:**
`src/main/java/org/openelisglobal/compliance/controller/rest/ComplianceReportRestController.java`

```java
@RestController
@RequestMapping("/rest/complianceReport")
public class ComplianceReportRestController {

    // GET /rest/complianceReport — filtered list + stats
    @GetMapping
    @PreAuthorize("hasRole('ROLE_RESULTS') or hasRole('ROLE_SUPERVISOR')")
    public ResponseEntity<ComplianceReportDTO> getReport(
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        @RequestParam(required = false) String siteId,
        @RequestParam(required = false) Long standardId,
        @RequestParam(required = false) String complianceStatus,
        @RequestParam(required = false) String generationStatus
    )

    // GET /rest/complianceReport/exportPdf?sampleId=123
    @GetMapping("/exportPdf")
    @PreAuthorize("hasRole('ROLE_RESULTS') or hasRole('ROLE_SUPERVISOR')")
    public void exportPdf(@RequestParam Long sampleId, HttpServletResponse response)
}
```

**`getReport` logic:**
1. Query all `Sample` records with `workflowType = ENVIRONMENTAL` and status = RELEASED
   in the date range, filtered by site/standard/status as requested
2. For each sample:
   - Load compliance standard via `SampleComplianceStandard`
   - Call `ComplianceEvaluationService.evaluate(sample)` for status + parameter results
   - Load latest generation record via `ComplianceReportGenerationService.getLastGenerated`
   - Load e-signatures: call `ElectronicSignatureService.getSignaturesForRecord(recordType="RESULT", recordId=sampleId)`
     - Filter for `SignatureMeaning.AUTHORED` → analyst signature
     - Filter for `SignatureMeaning.VALIDATED_AND_RELEASED` → manager signature
   - Extract collection conditions from `sample.getSampleOrderItem().getEnvironmentalFields()` map
   - Map to `ComplianceReportOrderDTO`
3. Compute stats tiles: count by eligibility/generation status
4. Apply `generationStatus` and `complianceStatus` post-filters

**`exportPdf` logic (iText5, follow AuditTrailReportRestController pattern):**
1. Load single order DTO
2. Set response: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="LH-{labNumber}.pdf"`
3. Build PDF with iText5:
   - Header: logo placeholder + "LAPORAN HASIL — CERTIFICATE OF TEST RESULTS"
   - Lab Number, Standard, Collection Date
   - Site Information table (2-col)
   - Collection Conditions table (2-col)
   - Compliance Summary table (Parameter | Result | Threshold | Status)
   - Signatures block: two columns (Analyst left, Lab Manager right), name + date + role
4. Call `ComplianceReportGenerationService.recordGeneration(sampleId, currentUser)`

---

### Task 6 — Java: Unit test for `ComplianceEvaluationServiceImpl`

**File:**
`src/test/java/org/openelisglobal/compliance/service/ComplianceEvaluationServiceImplTest.java`

Follow TDD (write test first). Test cases:
- RANGE threshold: value in range → COMPLIANT
- RANGE threshold: value at boundary → COMPLIANT
- RANGE threshold: value 5% outside → MARGINAL
- RANGE threshold: value far outside → NON_COMPLIANT
- MAXIMUM threshold: value below max → COMPLIANT
- No linked standard → returns null
- No matching threshold for test → that test excluded from results
- Mixed results: one NON_COMPLIANT, others COMPLIANT → overall NON_COMPLIANT
- Mixed results: one MARGINAL, others COMPLIANT → overall MARGINAL

Use `@ExtendWith(MockitoExtension.class)`. Mock: `SampleComplianceStandardDAO`,
`ComplianceThresholdDAO`, `AnalysisService`, `ResultService`.

---

### Task 7 — Frontend: `LaporanHasilReport.jsx` component

**File:** `frontend/src/components/reports/compliance/LaporanHasilReport.jsx`

Build the full report UI. Follow `AuditTrailReport.jsx` structure and Carbon Design System
patterns throughout. Use Carbon components only — no Bootstrap, no Tailwind.

#### Filter bar
Use Carbon `DatePicker`, `Dropdown`, `Button` (Search / Reset):
- Date From / Date To
- Sampling Site dropdown (fetch from `/rest/samplelocations` or existing site endpoint)
- Compliance Standard dropdown (fetch from `/rest/compliance/standards/active`)
- Compliance Status dropdown: All | Compliant | Non-Compliant | Marginal
- Generation Status dropdown: All | Generated | Not Yet Generated

#### Stats tiles
Three Carbon `Tile` components in a `Grid`:
- Ineligible (grey) — count from `response.ineligibleCount`
- Generated (green) — `response.generatedCount`
- Not Yet Generated (yellow/orange) — `response.notYetGeneratedCount`

#### Main table
Use Carbon `DataTable` with `TableExpandRow` / `TableExpandedRow` for inline detail.

Columns: Lab Number | Site | Standard | Collection Date | Tests | Compliance | Last Generated | Actions

**Compliance badge:** Carbon `Tag` component:
- `type="green"` + "✓ Compliant" for COMPLIANT
- `type="red"` + "✗ Non-Compliant" for NON_COMPLIANT
- `type="warm-gray"` + "⚑ Marginal" for MARGINAL
- `type="gray"` + "–" for NONE

**Actions column:** Carbon `Button` size="sm" kind="primary" label="Generate PDF"
→ calls `GET /rest/complianceReport/exportPdf?sampleId={id}` via `window.open()`,
then refreshes the row's `lastGenerated` timestamp.

#### Expanded row panel
Three-column layout using Carbon `Grid` + `Column`:

**Left — Site Information** (`Column lg={4}`):
- `StructuredListWrapper` with rows for: Site, GPS, Collection Date/Time,
  Collection Method, PP No., Standard

**Middle — Collection Conditions** (`Column lg={4}`):
- `StructuredListWrapper` with rows for: Water Temp, Ambient Temp, Weather, Preservation

**Right — Compliance Summary** (`Column lg={8}`):
- Nested `DataTable` (no toolbar): Parameter | Result | Threshold | Status
- Status column uses inline `Tag` (green/red/warm-gray)

**Bottom — Signatures** (full width, two `Tile` side by side):
- Left tile: "Lab Analyst" — name, role, formatted date/time
- Right tile: "Lab Manager (Supervisor)" — name, role, formatted date/time
- If signature not present: show "–" placeholder

#### API call
```js
getFromOpenElisServer(
  `/rest/complianceReport?dateFrom=${dateFrom}&dateTo=${dateTo}&siteId=${siteId}&standardId=${standardId}&complianceStatus=${complianceStatus}&generationStatus=${generationStatus}`,
  (data) => { setReportData(data); setIsLoading(false); }
);
```

---

### Task 8 — Frontend: Wire route + nav

#### 8a. Add route to `RoutineReports` switch
**File:** `frontend/src/components/reports/routine/Index.jsx`

Add inside the `RoutineReports` component:
```jsx
import LaporanHasilReport from "../compliance/LaporanHasilReport";

// inside return:
{type === "environmental" && report === "laporanHasil" && (
  <LaporanHasilReport />
)}
```

Also add route entry in `frontend/src/App.jsx` if a standalone page is needed:
```jsx
import LaporanHasilIndex from "./components/reports/compliance/Index";
// <Route path="/EnvironmentalReport" component={() => <LaporanHasilIndex />} />
```

#### 8b. Add sidebar nav entry
**File:** `frontend/src/components/reports/Routine.jsx`

Add a new section to `RoutineReportsMenu.sideNavMenuItems`:
```jsx
{
  title: <FormattedMessage id="sideNav.title.environmentalReports" />,
  icon: IbmWatsonNaturalLanguageUnderstanding,
  SideNavMenuItem: [
    {
      link: "/RoutineReport?type=environmental&report=laporanHasil",
      label: <FormattedMessage id="sideNav.label.laporanHasil" />,
    },
  ],
},
```

---

### Task 9 — i18n: Add `en.json` keys

**File:** `frontend/src/languages/en.json`

Add the following keys (all new keys go in `en.json` ONLY — Transifex handles other languages):

```json
"sideNav.title.environmentalReports": "Environmental Reports",
"sideNav.label.laporanHasil": "Laporan Hasil (Compliance)",

"laporanHasil.title": "Laporan Hasil — Compliance Report",
"laporanHasil.subtitle": "Generate Sertifikat Hasil Uji (Test Results Certificates) for validated environmental orders",
"laporanHasil.tile.ineligible": "Ineligible",
"laporanHasil.tile.ineligible.desc": "Not validated or no standard",
"laporanHasil.tile.generated": "Generated",
"laporanHasil.tile.notGenerated": "Not Yet Generated",

"laporanHasil.filter.dateFrom": "Date From",
"laporanHasil.filter.dateTo": "Date To",
"laporanHasil.filter.samplingSite": "Sampling Site",
"laporanHasil.filter.allSites": "All Sites",
"laporanHasil.filter.standard": "Compliance Standard",
"laporanHasil.filter.allStandards": "All Standards",
"laporanHasil.filter.complianceStatus": "Compliance Status",
"laporanHasil.filter.generationStatus": "Generation Status",
"laporanHasil.filter.all": "All",
"laporanHasil.filter.generated": "Generated",
"laporanHasil.filter.notGenerated": "Not Yet Generated",

"laporanHasil.col.labNumber": "Lab Number",
"laporanHasil.col.site": "Site",
"laporanHasil.col.standard": "Standard",
"laporanHasil.col.collectionDate": "Collection Date",
"laporanHasil.col.tests": "Tests",
"laporanHasil.col.compliance": "Compliance",
"laporanHasil.col.lastGenerated": "Last Generated",
"laporanHasil.col.actions": "Actions",

"laporanHasil.action.generatePdf": "Generate PDF",
"laporanHasil.action.regeneratePdf": "Re-generate PDF",

"laporanHasil.detail.siteInfo": "Site Information",
"laporanHasil.detail.site": "Site",
"laporanHasil.detail.gps": "GPS",
"laporanHasil.detail.collectionDateTime": "Collection Date/Time",
"laporanHasil.detail.collectionMethod": "Collection Method",
"laporanHasil.detail.ppNo": "PP No.",
"laporanHasil.detail.standard": "Standard",

"laporanHasil.detail.conditions": "Collection Conditions",
"laporanHasil.detail.waterTemp": "Water Temp",
"laporanHasil.detail.ambientTemp": "Ambient Temp",
"laporanHasil.detail.weather": "Weather",
"laporanHasil.detail.preservation": "Preservation",

"laporanHasil.detail.complianceSummary": "Compliance Summary",
"laporanHasil.detail.parameter": "Parameter",
"laporanHasil.detail.result": "Result",
"laporanHasil.detail.threshold": "Threshold",
"laporanHasil.detail.status": "Status",

"laporanHasil.detail.signatures": "Signatures",
"laporanHasil.detail.labAnalyst": "Lab Analyst",
"laporanHasil.detail.labManager": "Lab Manager",

"laporanHasil.status.compliant": "Compliant",
"laporanHasil.status.nonCompliant": "Non-Compliant",
"laporanHasil.status.marginal": "Marginal",
"laporanHasil.status.none": "–",

"laporanHasil.empty": "No orders found for the selected filters.",
"laporanHasil.loading": "Loading compliance report...",
"laporanHasil.error": "Failed to load report. Please try again."
```

---

### Task 10 — Frontend: Unit tests

**File:** `frontend/src/components/reports/compliance/__tests__/LaporanHasilReport.test.jsx`

Follow pattern of `frontend/src/components/reports/tat/__tests__/TATFilterBar.test.jsx`.

Test cases:
- Renders title and subtitle
- Stats tiles show correct counts from mock API response
- Filter bar renders all 5 filter controls
- Table renders rows with correct lab numbers
- Compliance badge renders correct Carbon Tag type for each status
- Expanded row shows Site Information, Collection Conditions, Compliance Summary, Signatures sections
- "Generate PDF" button calls correct URL
- Empty state shown when `orders` array is empty
- Loading state shown while fetching

Use `jest` + `@testing-library/react`. Mock `getFromOpenElisServer`.

---

## Build Order

```
Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 (parallel with 7)
                                                Task 7 → Task 8 → Task 9 → Task 10
```

Tasks 6 and 7 can proceed in parallel (backend test + frontend build are independent).

## Formatting (MANDATORY before commit)

```bash
# Backend
mvn spotless:apply

# Frontend
cd frontend && npm run format && cd ..
```

## Build verification

```bash
# Skip all tests
mvn clean install -DskipTests -Dmaven.test.skip=true

# Run backend test for Task 6
mvn test -pl . -Dtest=ComplianceEvaluationServiceImplTest

# Run frontend tests for Task 10
cd frontend && npm test -- --testPathPattern=LaporanHasilReport
```
