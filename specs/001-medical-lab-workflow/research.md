# Research: Medical Laboratory Workflow

**Feature**: 001-medical-lab-workflow **Date**: 2024-12-14 **Purpose**: Document
research findings for implementation decisions

## 1. Existing OpenELIS Services Analysis

### Decision: Reuse Patient and Sample Services

**Rationale**: OpenELIS already has mature patient registration, sample
management, and storage services. Building on these reduces development time and
ensures consistency with existing functionality.

**Existing Services to Leverage**:

| Service                | Package                               | Purpose                  |
| ---------------------- | ------------------------------------- | ------------------------ |
| PatientService         | org.openelisglobal.patient.service    | Patient CRUD             |
| SampleService          | org.openelisglobal.sample.service     | Sample management        |
| SampleItemService      | org.openelisglobal.sampleitem.service | Sample item tracking     |
| StorageLocationService | org.openelisglobal.storage.service    | Hierarchical storage     |
| AnalyzerService        | org.openelisglobal.analyzer.service   | Instrument configuration |
| ResultService          | org.openelisglobal.result.service     | Test result management   |

**Alternatives Considered**:

- Build new services from scratch: Rejected (duplication, consistency issues)
- Fork existing services: Rejected (maintenance burden)

### Decision: Create New medlab Module

**Rationale**: Medical lab-specific functionality (QC, transport packaging,
environmental monitoring, disposal) is distinct enough to warrant a separate
module rather than extending existing modules.

**New Module Structure**: `org.openelisglobal.medlab`

**Alternatives Considered**:

- Add to existing sample module: Rejected (too many responsibilities)
- Add to existing notebook module: Rejected (different use case)

## 2. Quality Control Standards

### Decision: Implement Westgard Rules for QC

**Rationale**: Westgard rules are the industry standard for laboratory QC
evaluation. ISO 15189 and CLIA require documented QC with rule-based acceptance
criteria.

**Rules to Implement**:

1. 1:2s - Single control >2SD from mean (warning)
2. 1:3s - Single control >3SD from mean (reject)
3. 2:2s - Two consecutive controls >2SD in same direction (reject)
4. R:4s - One control >+2SD and one >-2SD (reject)
5. 4:1s - Four consecutive controls >1SD in same direction (reject)
6. 10x - Ten consecutive controls on same side of mean (reject)

**Implementation Approach**:

- Store QC results in `qc_result` table with value, target, SD
- Calculate rule violations in service layer
- Display Levey-Jennings chart using Carbon Charts

**Alternatives Considered**:

- Simple pass/fail only: Rejected (insufficient for accreditation)
- Third-party QC software: Rejected (integration complexity)

### Decision: Dual-Level QC (Normal and Pathologic)

**Rationale**: Running both normal and pathologic controls ensures accuracy
across the measurement range. Required by CLIA.

**Implementation**: QCResult entity with `level` enum (NORMAL, PATHOLOGIC)

## 3. IATA PI650 Compliance

### Decision: Three-Level Packaging Validation

**Rationale**: IATA PI650 (Packing Instruction 650) specifies three-layer
packaging for Category B biological substances. The system must document
compliance at all three levels.

**Packaging Levels**:

1. **Primary** (Specimen Container): Container type, seal status, barcode,
   absorbent material
2. **Secondary**: Packaging type, integrity, watertight compliance, container
   count
3. **Tertiary** (Outer): Box type, labeling, temperature logger, courier details

**Compliance Calculation**: All mandatory fields must pass for IATA PI650
compliance status = "Yes"

**Alternatives Considered**:

- Single-level validation: Rejected (non-compliant with IATA)
- Manual compliance entry: Rejected (error-prone)

## 4. Environmental Monitoring

### Decision: Twice-Daily Temperature Recording

**Rationale**: ISO 15189 and good laboratory practice require regular
temperature monitoring of storage equipment with documented excursion
management.

**Implementation**:

- `environmental_reading` table with device_id, temperature, reading_time,
  reading_type (AM/PM)
- Excursion alerts when temperature exceeds configurable thresholds
- Optional integration with external data loggers via API

**Thresholds**: | Device Type | Min | Max | | ----------------- | ------ |
------ | | Room Temperature | 18°C | 25°C | | Refrigerator | 2°C | 8°C | |
Freezer (-20°C) | -25°C | -15°C | | Deep Freezer | -86°C | -76°C |

**Alternatives Considered**:

- Once-daily recording: Rejected (insufficient per ISO 15189)
- Continuous monitoring only: Rejected (requires expensive equipment)

## 5. Sample Quality Criteria by Type

### Decision: Configurable Sample-Type-Specific Criteria

**Rationale**: Different sample types have different quality requirements.
Chemistry samples have different delay tolerances than Stool samples.

**Criteria by Sample Type**:

| Sample Type  | Max Delay | Min Volume | Specific Criteria                   |
| ------------ | --------- | ---------- | ----------------------------------- |
| Chemistry    | 1 hour    | 3 mL       | Hemolysis, lipemia, icterus         |
| Hematology   | 4 hours   | 2 mL       | Anticoagulant type, clotting        |
| Stool        | 30 min    | pea grain  | Urine contamination, container type |
| Urine        | 30 min    | 10 mL      | Stool contamination                 |
| Microbiology | -         | -          | Visible contamination               |

**Implementation**: `quality_check` table with sample_type_id, criteria_json for
flexible configuration

**Alternatives Considered**:

- Hardcoded criteria: Rejected (inflexible)
- Single criteria for all: Rejected (clinically incorrect)

## 6. Result Validation Workflow

### Decision: Department-Specific Approval Queues

**Rationale**: Laboratory supervisors should only see and approve results from
their department. This follows separation of duties and departmental
accountability.

**Departments**:

- Chemistry
- Hematology
- Parasitology
- Urinalysis
- Serology
- Microbiology

**Workflow**:

1. Technician enters/reviews results
2. Results appear in department validation queue
3. Supervisor approves, rejects (with reason), or requests retest
4. Approved results become available for reporting
5. Results cannot be modified after approval without audit trail

**Implementation**: `validation_record` table with department_id, action enum
(APPROVED, REJECTED, RETEST), approver_id, timestamp

**Alternatives Considered**:

- Single approval queue: Rejected (overwhelming, no specialization)
- Auto-approval: Rejected (quality/compliance concern)

## 7. Disposal Compliance

### Decision: Disposal Method Enforcement by Sample Type

**Rationale**: International biological waste guidelines specify disposal
methods. Blood/stool require incineration; urine requires chemical treatment.

**Methods**: | Sample Type | Required Method | Facility Requirement | |
---------------- | ------------------- | ----------------------- | | Blood |
Incineration | Licensed facility | | Stool | Incineration | Licensed facility |
| Urine | Chemical treatment | Accredited facility | | Analyzer waste | Chemical
treatment | Accredited facility |

**Implementation**: `disposal_record` with method enum, facility_name,
facility_license_number, disposal_date, personnel_id

**Alternatives Considered**:

- User-selected method: Rejected (compliance risk)
- No method tracking: Rejected (regulatory requirement)

## 8. Turnaround Time (TAT) Calculation

### Decision: Collection-to-Result TAT by Test Type

**Rationale**: TAT is a key laboratory performance indicator. Different tests
have different target TATs (STAT vs routine).

**TAT Metrics**: | Test Category | Target TAT (STAT) | Target TAT (Routine) | |
------------- | ----------------- | -------------------- | | Chemistry | 1 hour
| 4 hours | | Hematology | 30 minutes | 2 hours | | Urinalysis | 1 hour | 4
hours | | Coagulation | 1 hour | 4 hours |

**Calculation Points**:

- Start: Sample collection timestamp
- End: Result validation timestamp
- Reported: Collection → Report delivery

**Implementation**: Calculate in LabDashboardService using existing timestamps

**Alternatives Considered**:

- Fixed TAT for all: Rejected (not clinically appropriate)
- Receipt-to-result only: Rejected (incomplete picture)

## 9. Analyzer Integration Patterns

### Decision: Leverage Existing Analyzer Framework

**Rationale**: OpenELIS already has analyzer integration (ASTM protocol, file
upload). This feature extends existing patterns rather than creating new ones.

**Existing Patterns to Use**:

- Worklist generation via AnalyzerService
- Result upload via analyzer-specific plugins
- Reference range configuration in test definitions

**Enhancements**:

- Add equipment usage logging
- Add automatic flagging (H/L/C)

**Alternatives Considered**:

- New analyzer framework: Rejected (duplication)
- Third-party middleware: Rejected (cost, complexity)

## 10. Frontend Component Strategy

### Decision: Carbon Design System Components

**Rationale**: Constitution mandates Carbon Design System. The following
components will be used for consistent UI.

**Primary Components**: | Feature | Carbon Components | | -------------------- |
-------------------------------------------- | | Data grids | DataTable,
TableToolbar, Pagination | | Forms | Form, TextInput, Select, DatePicker | |
Modals | Modal, ComposedModal | | Charts | @carbon/charts-react (Line, Bar,
Gauge) | | Navigation | Tabs, SideNav, Breadcrumb | | Feedback |
InlineNotification, ToastNotification |

**Levey-Jennings Chart**: Use Carbon Charts Line chart with:

- Mean line (solid)
- +/-1SD lines (dotted)
- +/-2SD lines (dashed)
- +/-3SD lines (dashed, different color)
- Point markers for QC values

**Alternatives Considered**:

- Chart.js: Rejected (not Carbon ecosystem)
- D3 directly: Rejected (complexity, maintenance)

## 11. Internationalization Keys

### Decision: Hierarchical Key Structure

**Rationale**: With ~500 new keys, a consistent naming convention is essential.

**Key Pattern**: `{module}.{page}.{element}.{description}`

**Examples**:

```json
{
  "medlab.reception.title": "Sample Reception",
  "medlab.reception.qc.hemolysis": "Hemolysis",
  "medlab.reception.qc.reject": "Reject Sample",
  "medlab.qc.chart.title": "Levey-Jennings Chart",
  "medlab.validation.approve": "Approve Result",
  "medlab.dashboard.tat.title": "Turnaround Time"
}
```

**Alternatives Considered**:

- Flat key structure: Rejected (organization issues at scale)
- Auto-generated keys: Rejected (poor readability)

## 12. Database Indexing Strategy

### Decision: Targeted Indexes for Common Queries

**Rationale**: Performance requirements (3s single operations, 30s bulk) require
proper indexing.

**Key Indexes**:

```sql
-- Quality check lookups by sample
CREATE INDEX idx_quality_check_sample ON quality_check(sample_item_id);

-- Environmental readings by device and date
CREATE INDEX idx_env_reading_device_date ON environmental_reading(device_id, reading_date);

-- Validation queue by department
CREATE INDEX idx_validation_department_status ON validation_record(department_id, status);

-- TAT calculations
CREATE INDEX idx_sample_collection_date ON sample_item(collection_date);

-- QC trending
CREATE INDEX idx_qc_result_test_date ON qc_result(test_id, result_date);
```

**Alternatives Considered**:

- No explicit indexes: Rejected (performance)
- Index everything: Rejected (write overhead)

## Summary of Research Decisions

| Area                     | Decision                                 | Status   |
| ------------------------ | ---------------------------------------- | -------- |
| Existing services        | Reuse Patient, Sample, Storage           | RESOLVED |
| New module               | Create medlab module                     | RESOLVED |
| QC rules                 | Westgard rules + Levey-Jennings          | RESOLVED |
| Transport packaging      | Three-level IATA PI650 validation        | RESOLVED |
| Environmental monitoring | Twice-daily with configurable thresholds | RESOLVED |
| Quality criteria         | Sample-type-specific, configurable       | RESOLVED |
| Validation workflow      | Department-specific queues               | RESOLVED |
| Disposal compliance      | Method enforcement by sample type        | RESOLVED |
| TAT calculation          | Collection-to-result by test type        | RESOLVED |
| Analyzer integration     | Extend existing framework                | RESOLVED |
| Frontend components      | Carbon Design System + Charts            | RESOLVED |
| Internationalization     | Hierarchical key structure               | RESOLVED |
| Database indexing        | Targeted indexes for common queries      | RESOLVED |

All NEEDS CLARIFICATION items have been resolved through research.
