# TMMRD Notebook Refactoring Plan - Phases 1 & 2

## Overview

This document outlines the refactoring of the TMMRD notebook to align with SRS
requirements and implement Phases 1 & 2 of the implementation roadmap. All work
focuses on the first two pages only, with comprehensive implementation of Stages
1-3 of the SRS.

**Status:** Planning Phase **Scope:** Pages 1-2 only (Stages 1-3 of TMMRD SRS)
**Timeline:** Phases 1 & 2 (4-6 weeks estimated)

---

## Current State Analysis

### What Exists Now

- **8 frontend page files** (all following Immunology pattern)
- **1 Liquibase migration** creating all 8 pages in database
- **Page 1** currently implements: Sample creation + manifest import +
  authentication
- **Pages 2-8** are empty scaffolds following page_type patterns

### What's Wrong

- Pages 2-8 don't match SRS requirements (herbarium, storage, extraction, etc.)
- Page 1 mixes "Sample Arrival" and "Registration & Labeling" but lacks proper
  labeling workflow
- No individual sample creation support (only bulk manifest import)
- Database has 8 pages that don't align with actual SRS workflow

### What We're Doing

1. **Delete Pages 3-8** from database and frontend (keep Page 2 skeleton)
2. **Redesign Page 1** to properly implement Stage 1 (Sample Arrival +
   Registration & Labeling)
3. **Create Page 2** for Stages 2-3 (Authentication + Storage & Herbarium)
4. **Update Liquibase** to only create 2 pages with correct specifications

---

## PHASE 1 IMPLEMENTATION (Weeks 1-2)

### Stage 1: Sample Intake & Registration (Page 1)

**SRS Requirements:**

```
Sample Arrival:
  ● Plant materials or traditional medicine samples received
  ● May arrive fresh or dried

Registration & Labeling:
  ● Assign unique sample ID
  ● Record metadata (origin, species, collector, date/time)
```

**Current Page 1 Implementation Gaps:**

- Focuses on authentication as part of the same page
- Lacks explicit "labeling" workflow after sample creation
- Doesn't separate "sample arrival" from "registration & labeling"

**Proposed Page 1 (Two-Phase Workflow):**

#### Phase 1a: Sample Arrival & Initial Receipt

1. **Create Samples** (via manifest or individual form)

   - Manifest CSV import (bulk creation)
   - Individual sample creation form (single sample)
   - Both → Creates samples in "RECEIVED" status

2. **Display: Received Samples Grid**
   - Shows samples in "RECEIVED" status
   - Progress tile: "Received" count
   - Columns: Accession #, Sample ID, Category, Scientific Name, Collector,
     Date, Origin
   - No edit/action buttons (read-only at this stage)

#### Phase 1b: Registration & Labeling

1. **Select Received Samples**

   - Checkbox selection from received grid
   - "Register & Label" button when samples selected

2. **Registration & Labeling Form**

   - Modal form with two sections:
     - **Sample Identity** - Verify/update basic info
     - **Metadata Recording** - Collect full taxonomy and usage info
   - Fields:
     - Sample Category (from SRS: Whole Plant, Plant Part, Extract, etc.)
     - Local/Common Name
     - Scientific Name
     - Species
     - Plant Part (Root, Leaf, Stem, Bark, etc.)
     - Sample Condition (Fresh, Dried, Preserved)
     - Intended Use
     - Collection Site (GPS/region)
     - Notes/Remarks
   - Follow pattern from Bioanalytical "Edit Metadata" modal
   - Bulk apply to all selected samples

3. **Display: Registered Samples Grid**
   - Shows samples in "REGISTERED" status
   - Progress tile: "Registered" count
   - Same columns as Received grid
   - Allows authentication workflow to begin
   - Button: "Authenticate Selected" (starts Stage 2)

#### Page 1 Progress Tiles

```
┌─────────────────────────────────┐
│  Received: 5  │  Registered: 12  │
└─────────────────────────────────┘
```

#### Page 1 Grid Layout

```
RECEIVED SAMPLES (Awaiting Registration)
[Checkbox | Accession # | Sample ID | Category | Scientific Name | Collector | ...]
[  ☑    | ACC-001   | TM-001   | Whole Pl  | Ocimum ...    | John      | ...]
[  ☑    | ACC-002   | TM-002   | Plant Pa  | Cinnam ...    | Jane      | ...]
[  ☐    | ACC-003   | TM-003   | Extract   | Zingiber...   | Bob       | ...]

[Register & Label Selected] button

REGISTERED SAMPLES (Ready for Authentication)
[Accession # | Sample ID | Category | Scientific Name | Collector | Auth Status]
[ACC-101     | TM-101   | Whole Pl | Passiflora ed.  | Mary      | Pending    ]
[ACC-102     | TM-102   | Extract  | Glyc...         | David     | Confirmed  ]

[Authenticate Selected] button
```

**Implementation Approach:**

- Keep existing TraditionalMedicineSampleCreationPage.js
- Enhance to use "RECEIVED" and "REGISTERED" status instead of "PENDING" and
  "AUTHENTICATED"
- Add "Registration & Labeling" modal (similar to Bioanalytical's BulkApplyForm)
- Support both manifest import AND individual sample creation
- Backend creates samples with initial status "RECEIVED"

---

## PHASE 2 IMPLEMENTATION (Weeks 3-4)

### Stages 2-3: Authentication + Storage & Herbarium (Page 2)

**SRS Requirements:**

#### Stage 2: Authentication

```
Process:
  ● Botanical verification or expert identification
  ● Methods:
    ○ Morphological examination
    ○ Microscopic analysis
    ○ Molecular identification (DNA barcoding: ITS, rbcL, matK genes)

Documentation:
  ● Authentication method used
  ● Result (species confirmed, uncertain, misidentified)
  ● Authenticator name and date
  ● If herbarium specimen prepared, link to herbarium catalog number
```

#### Stage 3: Sample Storage & Herbarium Placement

```
Storage Options:
  ● Fresh samples: Refrigerated (2-8°C) for short-term
  ● Dried samples: Room temperature in sealed containers (desiccant if needed)
  ● Preserved samples: In fixative or alcohol

Herbarium Placement (for reference specimens):
  ● Mount, label, and catalog specimen
  ● Record herbarium entry:
    ○ Specimen ID
    ○ Species, collector, location, date
    ○ Herbarium cabinet location
  ● Link to LMIS sample record
  ● Link to Projects: Herbarium entries linked to ongoing projects
```

**Proposed Page 2 (Two-Phase Workflow):**

#### Phase 2a: Authentication Workflow

1. **Select Registered Samples**

   - Fetch registered samples from Page 1
   - Checkbox selection
   - "Authenticate" button opens authentication modal

2. **Authentication Modal**

   - Section 1: Basic Info (read-only)
     - Sample ID, Scientific Name, Collector, Date
   - Section 2: Authentication Details
     - Authentication Method (choice: Morphological, Microscopic, Molecular,
       Expert ID)
     - Result (choice: Confirmed, Uncertain, Misidentified)
     - Authenticator Name (text)
     - Authentication Date (date picker)
     - Notes (text area)
   - Section 3: Evidence (if applicable)
     - File upload for evidence (photos, DNA sequences, etc.)
     - Max 3 files per sample

3. **Display: Authenticated Samples Grid**
   - Shows samples with authentication status
   - Columns: Accession #, Scientific Name, Auth Method, Result, Authenticator,
     Date
   - Status indicator: Confirmed (green), Uncertain (yellow), Misidentified
     (red)
   - Button: "Move to Herbarium/Storage" (proceeds to Phase 2b)

#### Phase 2b: Storage & Herbarium Workflow

1. **Select Authenticated Samples**

   - Can select confirmed OR uncertain samples
   - Misidentified samples cannot proceed (requires rework)

2. **Storage & Herbarium Modal**

   - Section 1: Storage Assignment

     - Storage Type (choice: Fresh, Dried, Preserved)
     - Storage Location (hierarchical selector: Cabinet → Shelf → Bin)
     - Storage Condition Notes (text area)

   - Section 2: Herbarium Specimen Decision
     - Herbarium Specimen? (yes/no)
     - If YES:
       - Herbarium Specimen ID (system-generated or custom)
       - Specimen Status (Mounted, Labeled, Cataloged)
       - Herbarium Cabinet Location (hierarchical)
       - Linked Projects (multi-select: associates to ongoing projects)
       - Specimen Notes (text area)

3. **Display: Storage & Herbarium Grid**
   - Shows samples with storage assignments
   - Columns: Accession #, Scientific Name, Storage Type, Location, Herbarium
     ID, Project Link
   - Status indicator: Stored (gray), In Herbarium (blue)
   - Mark as "COMPLETED" when done

#### Page 2 Progress Tiles

```
┌──────────────────────────────────────────────────┐
│  Authenticated: 8  │  In Storage: 12  │  In Herbarium: 5  │
└──────────────────────────────────────────────────┘
```

#### Page 2 Grid Layout

```
PENDING AUTHENTICATION (From Page 1)
[Checkbox | Accession # | Scientific Name | Collector | Condition | Date]
[  ☑     | ACC-101   | Passiflora ed.   | Mary      | Fresh    | 1/15]
[  ☑     | ACC-102   | Zingiber off.    | David     | Dried    | 1/16]

[Authenticate Selected] button

AUTHENTICATED SAMPLES (Ready for Storage)
[Accession # | Scientific Name | Method | Result | Herbarium? | Location]
[ACC-101     | Passiflora ed.  | Morph  | OK     | Yes       | Cab A1   ]

[Move to Storage] button

STORED/HERBARIUM SAMPLES (Completed)
[Accession # | Scientific Name | Storage Type | Herbarium # | Project]
[ACC-101     | Passiflora ed.  | Fresh (2°C) | HRB-001    | Hyper...  ]
```

**Implementation Approach:**

- Create new TraditionalMedicineAuthenticationPage.js
- Two-section layout: (Authenticated + Stored/Herbarium)
- Authenticate modal with evidence upload
- Storage hierarchical selector (cabinet structure)
- Herbarium linking to projects
- Link samples to ongoing projects via herbarium

---

## DATABASE & LIQUIBASE CHANGES

### Current State

- **Migration File:** `019-traditional-medicine-notebook-template.xml`
- **Creates:** 1 notebook template + 8 pages

### Proposed Changes

#### Step 1: Remove Pages 3-8 from Database

**Create new migration:** `020-tradmed-cleanup-pages.xml`

```xml
<delete tableName="notebook_page">
  <where>
    id IN (SELECT np.id FROM notebook_page np
           JOIN notebook n ON np.notebook_id = n.id
           WHERE n.title = 'Traditional &amp; Modern Medicine Research Lab'
           AND np.page_order > 2)
  </where>
</delete>
```

#### Step 2: Update Page 1 Content

**Modify:** `019-traditional-medicine-notebook-template.xml`

```sql
UPDATE notebook_page
SET title = 'Sample Arrival, Reception & Registration',
    instructions = 'Receive plant material samples and register full metadata including taxonomy, collection details, and intended use. Two-phase workflow: Sample Receipt → Registration & Labeling.',
    content = {...SRS Stage 1 content...}
WHERE page_order = 1 AND ...
```

**Update Page 1 JSON Content:**

- Remove authentication methods (move to Page 2)
- Add registration & labeling fields
- Add sample status states (RECEIVED, REGISTERED)
- Keep authentication as next-page action only

#### Step 3: Update Page 2 Content

**Modify:** `019-traditional-medicine-notebook-template.xml`

```sql
UPDATE notebook_page
SET title = 'Authentication, Storage & Herbarium Placement',
    instructions = 'Authenticate plant specimens through morphological, microscopic, or molecular analysis. Assign storage locations and manage herbarium specimens with project linking.',
    content = {...SRS Stages 2-3 content...}
WHERE page_order = 2 AND ...
```

**Update Page 2 JSON Content:**

- Authentication section (methods, results, evidence upload)
- Storage section (hierarchical cabinet structure)
- Herbarium section (specimen ID, project linking)
- Sample status states (AUTHENTICATED, STORED, IN_HERBARIUM)

#### Step 4: Cascade Delete Pages 3-8

**Ensure cleanup:** `020-tradmed-cleanup-pages.xml`

```xml
<!-- Delete associated data before deleting pages -->
<delete tableName="notebook_page_sample">
  <where>
    page_id IN (SELECT id FROM notebook_page
                WHERE notebook_id = ? AND page_order > 2)
  </where>
</delete>

<delete tableName="notebook_page">
  <where>
    notebook_id = ? AND page_order > 2
  </where>
</delete>
```

---

## FRONTEND PAGE FILES

### Files to Keep

1. **TraditionalMedicineSampleCreationPage.js** - Enhanced for Stage 1 only
2. **TraditionalMedicineStoragePage.js** → Rename to
   **TraditionalMedicineAuthenticationStoragePage.js** - Stages 2-3

### Files to Delete

1. TraditionalMedicinePreparationPage.js (Page 3)
2. TraditionalMedicineExtractionPage.js (Page 4)
3. TraditionalMedicineAnalyticalPage.js (Page 5)
4. TraditionalMedicineTestingPage.js (Page 6)
5. TraditionalMedicineFormulationPage.js (Page 7)
6. TraditionalMedicineArchivalPage.js (Page 8)

### Update index.js

Remove imports for deleted pages, export only Pages 1-2:

```javascript
export { default as TraditionalMedicineSampleCreationPage } from "./TraditionalMedicineSampleCreationPage";
export { default as TraditionalMedicineAuthenticationStoragePage } from "./TraditionalMedicineAuthenticationStoragePage";
```

---

## BACKEND CHANGES

### Service Layer Updates

**TraditionalMedicineManifestImportService.java**

- Already has `getValidSampleCategories()` ✓
- Add: `updateSampleMetadata(sampleIds, metadata)` - For registration & labeling
- Add: `authenticateSamples(sampleIds, authDetails)` - For Stage 2
- Add: `assignStorage(sampleIds, storageDetails)` - For Stage 3

**TraditionalMedicineManifestImportServiceImpl.java**

- Implement new methods
- Add storage location hierarchy management
- Add herbarium specimen tracking
- Support evidence file upload linking

### Controller Layer Updates

**TraditionalMedicineManifestImportController.java**

- Existing: `GET /sample-categories` ✓
- Existing: `POST /preview-manifest` ✓
- Existing: `POST /create-from-manifest` ✓
- Add: `POST /update-metadata` - Register & label samples
- Add: `POST /authenticate` - Record authentication data
- Add: `POST /assign-storage` - Record storage & herbarium info

### New DAO/Entity Updates

- Add methods for sample status transitions
- Add evidence attachment storage
- Add herbarium specimen entity/table (if needed)
- Add storage location hierarchy (cabinet structure)

---

## TESTING STRATEGY

### Unit Tests

- [ ] Service methods for metadata updates
- [ ] Authentication data validation
- [ ] Storage location hierarchy
- [ ] Herbarium linking logic

### Integration Tests

- [ ] Full Page 1 workflow (create → receive → register)
- [ ] Full Page 2 workflow (authenticate → store → herbarium)
- [ ] Status transitions (RECEIVED → REGISTERED → AUTHENTICATED → STORED)

### E2E Tests

- [ ] Complete sample lifecycle from arrival through herbarium placement
- [ ] Manifest import + registration in single flow
- [ ] Evidence file upload with authentication
- [ ] Project linking to herbarium specimens

### Manual Testing Checklist

- [ ] Create individual sample on Page 1
- [ ] Import samples via manifest on Page 1
- [ ] Register & label multiple samples
- [ ] Authenticate samples with different methods
- [ ] Upload evidence (photos/DNA sequences)
- [ ] Assign samples to storage hierarchies
- [ ] Create herbarium specimens with project linking

---

## TRANSLATION KEYS (i18n)

### New Keys Needed

**Page 1 (Registration & Labeling):**

```
notebook.tradmed.reception.title
notebook.tradmed.reception.received
notebook.tradmed.reception.registered
notebook.tradmed.registration.modal.title
notebook.tradmed.registration.modal.sampleIdentity
notebook.tradmed.registration.modal.metadata
notebook.tradmed.registration.button.registerLabel
```

**Page 2 (Authentication & Storage):**

```
notebook.tradmed.authentication.title
notebook.tradmed.authentication.authenticated
notebook.tradmed.authentication.stored
notebook.tradmed.authentication.modal.method
notebook.tradmed.authentication.modal.result
notebook.tradmed.authentication.modal.evidence
notebook.tradmed.storage.modal.storageType
notebook.tradmed.storage.modal.location
notebook.tradmed.herbarium.modal.specimenId
notebook.tradmed.herbarium.modal.projects
```

---

## DEPENDENCIES & BLOCKERS

### No Blockers

- All reference patterns exist (Bioanalytical, Immunology labs)
- Database schema supports needed features
- Frontend component library has all needed widgets

### Dependencies

- Liquibase migrations must run in order (cleanup after main template)
- Frontend index.js must be updated after file structure changes
- Translation keys must be added before deployment

---

## EFFORT ESTIMATE

### Phase 1 (Page 1): 1-2 weeks

- Enhance Page 1 component (2-3 days)
- Add registration & labeling modal (2-3 days)
- Update service methods (1-2 days)
- Add controller endpoints (1 day)
- Create tests (2-3 days)
- Update Liquibase migration (1 day)

### Phase 2 (Page 2): 2-3 weeks

- Create Page 2 component (3-4 days)
- Add authentication modal (2-3 days)
- Add storage/herbarium modal (2-3 days)
- Update service methods (2-3 days)
- Add controller endpoints (1-2 days)
- Create tests (3-4 days)
- Update Liquibase (1 day)

### Total: 4-6 weeks for Phases 1 & 2

---

## NEXT STEPS AFTER PHASES 1-2

Once Pages 1-2 are complete and tested:

1. **Collect User Feedback** - Test with lab users to validate workflow
2. **Phase 3 Planning** - Stages 4-5 (Sample Preparation + Extraction)
3. **Phase 4 Planning** - Stages 6-8 (Analysis, Testing, Formulation, Archival)
4. **User Roles & QC** - Implement role-based access + QC workflows

---

**Document Status:** Ready for Implementation **Last Updated:** January 2025
**Next Review:** After Phase 1 implementation
