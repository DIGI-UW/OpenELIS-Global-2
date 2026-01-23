# Research: Pathology Laboratory Workflow

**Date**: 2025-12-14 **Feature**: 052-pathology-lab-workflow **Purpose**:
Resolve technical unknowns and identify reusable patterns from OGC-51 Immunology
Workflow

## Executive Summary

This research document consolidates findings from analyzing the existing OGC-51
Immunology Workflow implementation to inform the Pathology Laboratory Workflow
design. All technical unknowns have been resolved, and clear patterns have been
identified for reuse.

---

## Research Area 1: Notebook Architecture Patterns

### Decision: Reuse existing Notebook/NoteBookPage/NotebookEntry architecture

**Rationale**:

- Proven 5-layer architecture already implemented
- Template-based workflow support (NoteBook as template, NotebookEntry as
  instance)
- Page-level progress tracking via NotebookPageSample junction entity
- FHIR questionnaire UUID mapping for standards compliance

**Key Patterns Identified**:

| Component                 | Location                            | Purpose                                                   |
| ------------------------- | ----------------------------------- | --------------------------------------------------------- |
| `NoteBook.java`           | `notebook/valueholder/`             | Workflow container with template support, status tracking |
| `NoteBookPage.java`       | `notebook/valueholder/`             | Individual workflow steps with page_type enum             |
| `NotebookPageSample.java` | `notebook/valueholder/`             | Per-sample-per-page tracking with JSONB data              |
| `NoteBookService`         | `notebook/service/`                 | Template/instance management                              |
| `NoteBookDashBoard.js`    | `frontend/src/components/notebook/` | Workflow navigation UI                                    |

**Alternatives Considered**:

- Custom workflow engine → Rejected: Over-engineering, existing system
  sufficient
- Separate pathology module → Rejected: Would duplicate notebook infrastructure

---

## Research Area 2: Entity Design Patterns

### Decision: Follow BaseObject<PK> pattern with JSONB for flexible data

**Rationale**:

- All notebook entities extend `BaseObject<PK>` for consistent audit trail
- `@Version` column provides optimistic locking for concurrent access
- JSONB data fields enable page-specific data without schema changes

**Entity Pattern Template**:

```java
@Entity
@Table(name = "pathology_sample_registration")
public class PathologySampleRegistration extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
        generator = "pathology_sample_registration_generator")
    @SequenceGenerator(name = "pathology_sample_registration_generator",
        sequenceName = "pathology_sample_registration_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "fhir_uuid", columnDefinition = "uuid")
    private UUID fhirUuid;  // FHIR Specimen mapping

    @Type(type = "jsonb-map")
    @Column(name = "criteria_results", columnDefinition = "jsonb")
    private Map<String, Object> criteriaResults;  // Flexible QC criteria

    @Version
    private Timestamp lastupdated;  // Optimistic locking
}
```

**Alternatives Considered**:

- Separate columns per QC criterion → Rejected: Not flexible for different
  sample types
- XML-based mapping → Rejected: JPA annotations preferred for new entities

---

## Research Area 3: FHIR Mapping for Pathology Specimens

### Decision: Map pathology samples to FHIR Specimen, test results to DiagnosticReport/Observation

**Rationale**:

- Existing `StorageLocationFhirTransform.java` provides transform pattern
- IHE mCSD profile already used for Location resources
- FHIR Specimen resource supports parent-child relationships via `parent`
  reference

**FHIR Resource Mappings**:

| Entity                      | FHIR Resource                  | Profile          |
| --------------------------- | ------------------------------ | ---------------- |
| PathologySampleRegistration | Specimen                       | IHE LAB Specimen |
| TestResultRecord            | DiagnosticReport + Observation | IHE LAB Results  |
| QualityControlRecord        | Observation (quality)          | Custom extension |
| ReferenceDocument           | DocumentReference              | FHIR Core        |

**FHIR Transform Pattern**:

```java
@Service
public class PathologySpecimenFhirTransform implements FhirTransformService {

    public Specimen transformToFhir(PathologySampleRegistration registration) {
        Specimen specimen = new Specimen();

        // Identifier
        specimen.addIdentifier()
            .setSystem("http://openelis.org/pathology-accession")
            .setValue(registration.getAccessionNumber());

        // Type (sample category)
        specimen.getType().addCoding()
            .setSystem("http://snomed.info/sct")
            .setCode(registration.getSampleTypeCode());

        // Parent-child (for blocks/slides)
        if (registration.getParentSampleId() != null) {
            specimen.addParent()
                .setReference("Specimen/" + registration.getParentFhirUuid());
        }

        return specimen;
    }
}
```

**Alternatives Considered**:

- Custom proprietary format → Rejected: Constitution requires FHIR/IHE
  compliance
- HL7 v2 messages → Rejected: FHIR R4 is the established standard

---

## Research Area 4: Quality Control (QC) Criteria Storage

### Decision: Use JSONB for specimen-type-specific QC criteria with enum-based QC types

**Rationale**:

- Different specimen types have different QC criteria (histology vs cytology vs
  blood)
- JSONB allows adding new criteria without schema migration
- Enum for QC type ensures consistent categorization

**QC Criteria Structure**:

```json
// Histology QC criteria
{
  "fixationType": "10% NBF",
  "fixationRatio": "10:1",
  "fixationDuration": "24h",
  "tissueIntegrity": "adequate",
  "requisitionMatch": true
}

// Cytology QC criteria
{
  "containerIntegrity": true,
  "preservativeType": "Cytolyt",
  "volume": "50mL",
  "clotPresence": false
}

// Block QC criteria
{
  "surfaceSmoothness": true,
  "correctDepth": true,
  "correctOrientation": true,
  "paraffinOverflow": false
}
```

**QC Type Enum**:

```java
public enum QCType {
    INITIAL_INSPECTION,    // Reception QC
    BLOCK_QC,              // Post-embedding tissue block QC
    SLIDE_QC,              // Slide quality assessment
    STAIN_QC               // Stain quality assessment
}
```

**Alternatives Considered**:

- Separate tables per specimen type → Rejected: Over-normalization, complex
  queries
- Fixed column schema → Rejected: Not flexible for new specimen types

---

## Research Area 5: Test Result Storage

### Decision: Generic TestResultRecord with JSONB result_data, type-specific validation in service layer

**Rationale**:

- Different tests (H&E, IHC, special stains) have different result structures
- JSONB result_data accommodates varied result formats
- Service layer validation ensures data integrity per test type

**Result Data Structures**:

```json
// H&E Result
{
  "stainQuality": "acceptable",
  "morphology": "free-text description",
  "diagnosis": "pending pathologist review"
}

// IHC Result
{
  "stainingPattern": "membranous",
  "intensity": "3+",
  "percentPositive": 80,
  "interpretation": "Positive for CD20",
  "positiveControl": "PASS",
  "negativeControl": "PASS"
}

// Special Stain Result
{
  "stainType": "AFB",
  "result": "Positive",
  "organisms": "acid-fast bacilli present",
  "quantitation": "few"
}
```

**Alternatives Considered**:

- Separate entities per test type → Rejected: Would require 10+ entities
- Flat structure with nullable columns → Rejected: Too many columns, sparse data

---

## Research Area 6: Parent-Child Sample Relationships

### Decision: Reuse existing SampleItem parent-child pattern with SampleItemAliquotRelationship

**Rationale**:

- SampleItem already has `parentSampleItem` and `childAliquots` fields
- SampleItemAliquotRelationship provides rich metadata (quantity, sequence,
  notes)
- Proven pattern from 001-sample-storage feature

**Existing Pattern**:

```java
// SampleItem.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_sample_item_id")
private SampleItem parentSampleItem;

@OneToMany(mappedBy = "parentSampleItem", fetch = FetchType.LAZY)
private List<SampleItem> childAliquots = new ArrayList<>();

// SampleItemAliquotRelationship.java (for rich metadata)
private Integer parentSampleItemId;
private Integer childSampleItemId;
private BigDecimal quantityTransferred;
private Integer sequenceNumber;
private String notes;
```

**Pathology Hierarchy**:

```
Parent Specimen (A24-0001)
├── Block A (A24-0001-A)
│   ├── Slide 1 (A24-0001-A-S1)
│   └── Slide 2 (A24-0001-A-S2)
└── Block B (A24-0001-B)
    └── Slide 1 (A24-0001-B-S1)
```

**Alternatives Considered**:

- New pathology-specific hierarchy entity → Rejected: Would duplicate existing
  functionality
- Flat sample list with external_id pattern → Rejected: Loses relationship
  integrity

---

## Research Area 7: Reference Document & SOP Management

### Decision: New ReferenceDocument entity with version chain, reuse NoteBookFile pattern for storage

**Rationale**:

- Existing NoteBookFile shows file storage pattern (LONGBLOB or file path)
- Version chain via `replaced_by_id` self-reference enables version history
- Separate from notebook attachments (SOPs are lab-wide, not notebook-specific)

**Entity Design**:

```java
@Entity
@Table(name = "reference_document")
public class ReferenceDocument extends BaseObject<Integer> {

    private String title;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;  // SOP, PROTOCOL, REFERENCE

    private String filePath;  // Or LONGBLOB for inline storage
    private String version;   // "2.1"
    private LocalDate effectiveDate;

    @ManyToOne
    private SystemUser uploadedBy;

    private Timestamp uploadedAt;

    @ManyToOne
    @JoinColumn(name = "replaced_by_id")
    private ReferenceDocument replacedBy;  // Version chain
}
```

**Version Query Pattern**:

```java
// Get all versions of a document (ordered newest first)
@Query("SELECT r FROM ReferenceDocument r WHERE r.title = :title ORDER BY r.uploadedAt DESC")
List<ReferenceDocument> getVersionHistory(String title);

// Get current version (no replacedBy means current)
@Query("SELECT r FROM ReferenceDocument r WHERE r.title = :title AND r.replacedBy IS NULL")
ReferenceDocument getCurrentVersion(String title);
```

**Alternatives Considered**:

- Reuse NoteBookFile directly → Rejected: SOPs need version control, notebook
  files don't
- External document management system → Rejected: Out of scope, adds complexity

---

## Research Area 8: Project-Based Access Control

### Decision: New ProjectAccess junction entity with role-based filtering in service layer

**Rationale**:

- Research samples require access restrictions per project
- Junction table enables many-to-many user-project relationships
- Service layer filtering ensures consistent access control

**Entity Design**:

```java
@Entity
@Table(name = "project_access",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
public class ProjectAccess extends BaseObject<Integer> {

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private SystemUser user;

    @Enumerated(EnumType.STRING)
    private AccessRole accessRole;  // TECHNICIAN, COORDINATOR, PATHOLOGIST

    @ManyToOne
    private SystemUser grantedBy;

    private Timestamp grantedAt;
}
```

**Access Control Pattern**:

```java
@Service
public class PathologySampleServiceImpl {

    @Transactional(readOnly = true)
    public List<PathologySampleRegistration> searchSamples(String query, String userId) {
        List<PathologySampleRegistration> allResults = sampleDAO.search(query);

        // Filter by access control
        return allResults.stream()
            .filter(sample -> {
                // Clinical samples - no restriction
                if (sample.getCategory() == SampleCategory.CLINICAL) {
                    return true;
                }
                // Research samples - check project access
                return projectAccessService.hasAccess(
                    sample.getProjectId(), userId);
            })
            .collect(Collectors.toList());
    }
}
```

**Alternatives Considered**:

- Spring Security method-level annotations → Rejected: Too coarse-grained
- Database-level row security → Rejected: PostgreSQL RLS complex to maintain

---

## Research Area 9: Environmental Monitoring Storage

### Decision: New StorageEnvironmentLog entity for twice-daily temperature recordings

**Rationale**:

- Manual temperature monitoring (twice daily per spec)
- Separate from sample storage to avoid bloating SampleStorageAssignment
- Simple insert-only log pattern

**Entity Design**:

```java
@Entity
@Table(name = "storage_environment_log")
@Immutable  // Audit log - never updated
public class StorageEnvironmentLog extends BaseObject<Integer> {

    @ManyToOne
    @JoinColumn(name = "storage_unit_id", nullable = false)
    private StorageDevice storageUnit;  // Freezer, refrigerator, etc.

    @Column(name = "recorded_temperature")
    private BigDecimal recordedTemperature;  // In Celsius

    @Column(name = "recorded_at")
    private Timestamp recordedAt;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private SystemUser recordedBy;

    @Column(name = "is_excursion")
    private Boolean isExcursion;  // Temperature outside acceptable range

    private String notes;  // Excursion explanation, corrective action
}
```

**Excursion Detection Logic**:

```java
@Service
public class StorageEnvironmentServiceImpl {

    @Transactional
    public StorageEnvironmentLog recordTemperature(Integer storageUnitId,
            BigDecimal temperature, String userId) {

        StorageDevice unit = storageDeviceDAO.get(storageUnitId);

        // Determine if excursion
        boolean isExcursion = temperature.compareTo(unit.getMinTemp()) < 0
            || temperature.compareTo(unit.getMaxTemp()) > 0;

        StorageEnvironmentLog log = new StorageEnvironmentLog();
        log.setStorageUnit(unit);
        log.setRecordedTemperature(temperature);
        log.setIsExcursion(isExcursion);
        log.setRecordedBy(userService.get(userId));
        log.setRecordedAt(Timestamp.from(Instant.now()));

        insert(log);
        return log;
    }
}
```

**Alternatives Considered**:

- Store in StorageDevice entity → Rejected: Would lose historical readings
- External monitoring system integration → Rejected: Out of scope (manual
  monitoring per spec)

---

## Research Area 10: Performance Reporting Metrics

### Decision: Calculated metrics via service layer queries, no pre-aggregation

**Rationale**:

- Monthly reports infrequent (once per month)
- Real-time calculation simpler than maintaining aggregation tables
- Existing timestamp fields provide TAT calculation data

**Metric Calculations**:

```java
@Service
public class PathologyReportingServiceImpl {

    @Transactional(readOnly = true)
    public PathologyMetrics calculateMonthlyMetrics(LocalDate startDate, LocalDate endDate) {
        PathologyMetrics metrics = new PathologyMetrics();

        // Specimen volume
        metrics.setTotalSpecimens(sampleDAO.countByDateRange(startDate, endDate));

        // Rejection rate
        long totalReceived = qcDAO.countByDateRange(startDate, endDate);
        long failed = qcDAO.countFailedByDateRange(startDate, endDate);
        metrics.setRejectionRate(failed * 100.0 / totalReceived);

        // Turnaround time (reception to result delivery)
        List<TATRecord> tatRecords = sampleDAO.getTATByDateRange(startDate, endDate);
        metrics.setAverageTAT(tatRecords.stream()
            .mapToLong(TATRecord::getDurationHours)
            .average()
            .orElse(0));

        // Assay success rate
        long totalAssays = testResultDAO.countByDateRange(startDate, endDate);
        long failedControls = testResultDAO.countFailedControlsByDateRange(startDate, endDate);
        metrics.setAssaySuccessRate((totalAssays - failedControls) * 100.0 / totalAssays);

        return metrics;
    }
}
```

**Alternatives Considered**:

- Pre-aggregated metrics tables → Rejected: Adds complexity, data staleness
  issues
- External analytics system → Rejected: Out of scope for this feature

---

## Summary of Technical Decisions

| Area                  | Decision                       | Key Pattern                              |
| --------------------- | ------------------------------ | ---------------------------------------- |
| Notebook Architecture | Reuse OGC-51 patterns          | NoteBook/NoteBookPage/NotebookPageSample |
| Entity Design         | BaseObject<PK> with JSONB      | Flexible data, optimistic locking        |
| FHIR Mapping          | Specimen/DiagnosticReport      | IHE LAB profiles                         |
| QC Criteria           | JSONB with QCType enum         | Specimen-type flexibility                |
| Test Results          | Generic with JSONB result_data | Type-specific service validation         |
| Parent-Child          | Existing SampleItem pattern    | SampleItemAliquotRelationship            |
| SOP Management        | New ReferenceDocument          | Version chain via replaced_by_id         |
| Access Control        | ProjectAccess junction         | Service-layer filtering                  |
| Temp Monitoring       | StorageEnvironmentLog          | Immutable insert-only log                |
| Metrics               | Calculated on-demand           | Service layer queries                    |

---

## Dependencies Identified

### Existing Components to Reuse

- `org.openelisglobal.notebook.*` - Notebook infrastructure
- `org.openelisglobal.storage.*` - Storage location hierarchy
- `org.openelisglobal.sample.valueholder.SampleItem` - Parent-child samples
- `org.openelisglobal.common.valueholder.BaseObject` - Entity base class
- `org.openelisglobal.systemuser.valueholder.SystemUser` - User references
- `org.openelisglobal.project.valueholder.Project` - Project entity (if exists,
  else create)

### New Components to Create

- `org.openelisglobal.pathology.*` - All pathology-specific entities/services
- Liquibase changesets in `liquibase/pathology/`
- Frontend components in `frontend/src/components/pathology/`
- i18n keys in `en.json`, `fr.json`

---

## Next Steps

1. Proceed to Phase 1: Generate `data-model.md` with complete entity definitions
2. Generate `contracts/` with OpenAPI specifications
3. Generate `quickstart.md` developer guide
4. Execute `/speckit.tasks` for implementation task breakdown
