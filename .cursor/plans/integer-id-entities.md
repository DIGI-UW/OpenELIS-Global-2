# Non-Storage Entities Using BaseObject<Integer>

**Total:** 31 non-storage entities use `BaseObject<Integer>` (simpler setup)  
**Storage entities:** 7 (StorageRoom, StorageDevice, StorageShelf, StorageRack,
StoragePosition, SampleStorageAssignment, SampleStorageMovement)

## Categories

### 1. **Pathology Module** (8 entities)

All pathology-related entities use Integer IDs:

- `PathologyReport` - Pathology reports
- `PathologySlide` - Pathology slides
- `PathologyBlock` - Pathology blocks
- `PathologyConclusion` - Pathology conclusions
- `PathologyTechnique` - Pathology techniques
- `PathologyRequest` - Pathology requests
- `ImmunohistochemistrySampleReport` - IHC reports
- `CytologySlide` - Cytology slides
- `CytologyDiagnosis` - Cytology diagnoses
- `CytologyDiagnosisCategoryResultsMap` - Cytology diagnosis mappings
- `CytologyReport` - Cytology reports
- `CytologySpecimenAdequacy` - Cytology specimen adequacy

### 2. **Notebook Module** (3 entities)

- `NoteBook` - Notebooks
- `NoteBookPage` - Notebook pages
- `NoteBookFile` - Notebook files

### 3. **Test Calculation/Reflex** (3 entities)

- `Calculation` - Test calculations
- `ResultCalculation` - Result calculations
- `ReflexRule` - Reflex rules

### 4. **External Connections** (3 entities)

- `ExternalConnection` - External system connections
- `ExternalConnectionAuthenticationData` (abstract) - Auth data base class
- `ExternalConnectionContact` - External connection contacts

### 5. **Notifications** (3 entities)

- `NotificationConfig` (abstract) - Notification configuration base
- `NotificationConfigOption` - Notification config options
- `NotificationPayloadTemplate` - Notification payload templates

### 6. **User Management** (2 entities)

- `LoginUser` - User login accounts
- `UserLabUnitRoles` - User lab unit role assignments

### 7. **Reference Data** (1 entity)

- `Gender` - Gender reference data

### 8. **Analysis/Results** (2 entities)

- `ResultFile` - Result files
- `ClientResultsViewBean` - Client results view (read-only view)

### 9. **Programs** (1 entity)

- `ProgramSample` - Program sample associations

### 10. **Analyzers** (1 entity)

- `AnalyzerExperiment` - Analyzer experiments

## Pattern Analysis

**Why these use Integer instead of String?**

1. **Newer modules** - Pathology, Notebook, Notifications, External Connections
   appear to be newer features (likely added after PostgreSQL migration)
2. **JPA annotations** - All use `@Entity` with JPA annotations (not Hibernate
   XML mappings)
3. **No legacy baggage** - These modules don't have legacy Hibernate XML
   mappings that would require `LIMSStringNumberUserType`
4. **Consistent within module** - Entire modules use Integer (e.g., all
   Pathology entities)

## Comparison: String vs Integer

| Category         | String IDs (Legacy)                       | Integer IDs (Newer)                              |
| ---------------- | ----------------------------------------- | ------------------------------------------------ |
| **Count**        | ~98 entities                              | 38 entities (31 non-storage)                     |
| **Mapping**      | Hibernate XML (`*.hbm.xml`)               | JPA annotations (`@Entity`)                      |
| **UserType**     | `LIMSStringNumberUserType`                | None (direct Integer)                            |
| **Examples**     | Sample, SampleItem, Patient, Organization | Storage entities, Pathology, Notebook, LoginUser |
| **When Created** | Original OpenELIS (Oracle era)            | Post-PostgreSQL migration                        |

## Key Insight

The codebase has **two parallel patterns**:

1. **Legacy pattern**: `BaseObject<String>` + `LIMSStringNumberUserType` +
   Hibernate XML
2. **Modern pattern**: `BaseObject<Integer>` + JPA annotations + no UserType

New features (Storage, Pathology, Notebook, etc.) use the modern pattern, while
core legacy entities (Sample, Patient, etc.) still use the String pattern for
backward compatibility.
