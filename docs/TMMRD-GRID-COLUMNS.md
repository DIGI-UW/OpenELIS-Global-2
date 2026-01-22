# TMMRD Grid Column Mappings - Complete Reference

This document provides a comprehensive overview of the grid column definitions
for all 8 pages of the Traditional Medicine (TMMRD) Laboratory notebook
workflow.

## Document Purpose

This reference maps:

- All grid columns for each TMMRD page
- Data keys and display headers
- Custom rendering functions
- Alignment with SRS requirements
- Column ordering and purpose

---

## Page 1: Sample Intake, Registration & Authentication

**File:**
[TraditionalMedicineSampleCreationPage.js](../frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicineSampleCreationPage.js)

**SRS Stage:** Stage 1 - Sample Intake and Registration **Purpose:** Capture
full sample metadata and perform botanical authentication

**Two-Section Grid Layout:**

- **Section 1:** Pending/In Progress Samples (awaiting authentication)
- **Section 2:** Authenticated & Registered Samples (ready for next stage)

### Pending Samples Grid

| Order | Key                    | Header          | Type   | Purpose                                                      | Source         |
| ----- | ---------------------- | --------------- | ------ | ------------------------------------------------------------ | -------------- |
| 1     | `accessionNumber`      | Accession #     | Text   | LMS-generated unique identifier                              | Auto-generated |
| 2     | `externalId`           | Sample ID       | Text   | User-provided external sample ID                             | Sample record  |
| 3     | `sampleCategory`       | Category        | Text   | Classification (Whole Plant, Plant Part, Extract, etc.)      | Sample record  |
| 4     | `localName`            | Local Name      | Text   | Common/traditional name of specimen                          | Manifest input |
| 5     | `scientificName`       | Scientific Name | Text   | Binomial nomenclature                                        | Manifest input |
| 6     | `sourceType`           | Source Type     | Text   | Source classification (Plant material, Plant Extract, Other) | Manifest input |
| 7     | `originLocation`       | Origin          | Text   | Geographic origin/collection region                          | Manifest input |
| 8     | `collectedBy`          | Collector       | Text   | Name of field collector                                      | Manifest input |
| 9     | `collectionDate`       | Collection Date | Date   | Date of sample collection                                    | Manifest input |
| 10    | `plantPart`            | Plant Part      | Text   | Plant part used (Leaf, Root, Bark, etc.)                     | Manifest input |
| 11    | `sampleCondition`      | Condition       | Text   | Material state (Fresh, Dried, Preserved)                     | Manifest input |
| 12    | `intendedUse`          | Intended Use    | Text   | Purpose of sample (Research, Product development, etc.)      | Manifest input |
| 13    | `authenticationStatus` | Authentication  | Custom | Renders authentication status badge with method/result       | Computed       |

**Total Columns:** 13

**Custom Renderers:**

- `authenticationStatus` → `renderAuthenticationStatus(row)` - Displays
  color-coded status badge with method and result

### Registered Samples Grid

Identical to Pending Samples Grid (13 columns, same structure)

**Differences:**

- Read-only display (no selection checkboxes)
- Filters to show only COMPLETED samples
- Same column structure for data continuity

---

## Page 2: Storage & Herbarium Placement

**File:**
[TraditionalMedicineStoragePage.js](../frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicineStoragePage.js)

**SRS Stage:** Stage 2 - Sample Storage and Herbarium Assignment **Purpose:**
Assign storage locations or create herbarium specimens

### Main Grid

| Order | Key               | Header            | Type   | Purpose                                                     | Source        |
| ----- | ----------------- | ----------------- | ------ | ----------------------------------------------------------- | ------------- |
| 1     | `accessionNumber` | Accession Number  | Text   | Reference to sample from Page 1                             | Linked sample |
| 2     | `externalId`      | Sample ID         | Text   | External sample identifier                                  | Linked sample |
| 3     | `localName`       | Local Name        | Text   | Common name for reference                                   | Linked sample |
| 4     | `sampleCondition` | Condition         | Text   | Current material state                                      | Linked sample |
| 5     | `storage`         | Storage/Herbarium | Custom | Renders hierarchical storage path or herbarium details      | Computed      |
| 6     | `status`          | Status            | Text   | Workflow status (In Storage, Herbarium Reference, Complete) | Page data     |

**Total Columns:** 6

**Custom Renderers:**

- `storage` → `renderStorageInfo(sample)` - Displays hierarchical path (Room >
  Device > Shelf > Rack > Position) or herbarium ID with cataloging status

**Key Features:**

- Simplified grid (fewer columns) focusing on storage assignment
- Storage path preview support
- Herbarium specimen tracking
- Status tracking for storage transitions

---

## Page 3: Sample Preparation for Analysis

**File:**
[TraditionalMedicinePreparationPage.js](../frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicinePreparationPage.js)

**SRS Stage:** Stage 3 - Sample Preparation **Purpose:** Track physical
processing and yield calculations

### Main Grid

| Order | Key               | Header           | Type   | Purpose                                               | Source        |
| ----- | ----------------- | ---------------- | ------ | ----------------------------------------------------- | ------------- |
| 1     | `accessionNumber` | Accession Number | Text   | Reference to source sample                            | Linked sample |
| 2     | `externalId`      | Sample ID        | Text   | External identifier                                   | Linked sample |
| 3     | `localName`       | Local Name       | Text   | Common name                                           | Linked sample |
| 4     | `plantPart`       | Plant Part       | Text   | Part being processed                                  | Linked sample |
| 5     | `materialState`   | Material         | Custom | Color-coded tag showing state (Fresh/Dried/Preserved) | Computed      |
| 6     | `processingTypes` | Processing       | Custom | Tags listing all applied processing methods           | Computed      |
| 7     | `yieldPercentage` | Yield            | Custom | Calculated yield percentage with visual indicator     | Computed      |
| 8     | `moistureContent` | Moisture %       | Custom | QC status with pass/fail indicator                    | Computed      |
| 9     | `processedBy`     | Processed By     | Text   | Operator name                                         | Page data     |
| 10    | `status`          | Status           | Text   | Workflow status (Pending, In Progress, Complete)      | Page data     |

**Total Columns:** 10

**Custom Renderers:**

- `materialState` → `renderMaterialStateTag(sample)` - Color-coded tag (e.g.,
  "Fresh", "Dried")
- `processingTypes` → `renderProcessingTags(sample)` - Multiple tags for
  processing methods
- `yieldPercentage` → `renderYield(sample)` - Percentage display with visual
  bar/indicator
- `moistureContent` → `renderQCStatus(sample)` - Pass/fail badge based on
  threshold validation

**Key Features:**

- Processing method tracking (Grinding, Drying, Powdering, etc.)
- Yield calculation display
- QC status visualization
- Material state progression tracking

---

## Page 4: Extraction, Filtration & Concentration

**File:**
[TraditionalMedicineExtractionPage.js](../frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicineExtractionPage.js)

**SRS Stage:** Stage 4 - Solvent Extraction **Purpose:** Track extraction
processes and create aliquots

### Main Grid

| Order | Key               | Header           | Type   | Purpose                                            | Source        |
| ----- | ----------------- | ---------------- | ------ | -------------------------------------------------- | ------------- |
| 1     | `accessionNumber` | Accession Number | Text   | Reference to prepared material                     | Linked sample |
| 2     | `externalId`      | Sample ID        | Text   | External identifier                                | Linked sample |
| 3     | `localName`       | Local Name       | Text   | Common name                                        | Linked sample |
| 4     | `extraction`      | Extraction Info  | Custom | Tags showing solvent, technique, yield, filtration | Computed      |
| 5     | `aliquots`        | Aliquots         | Custom | Button to view child samples or tag showing parent | Computed      |
| 6     | `operator`        | Operator         | Text   | Extraction operator name                           | Page data     |
| 7     | `status`          | Status           | Text   | Workflow status (Pending, In Progress, Complete)   | Page data     |

**Total Columns:** 7

**Custom Renderers:**

- `extraction` → `renderExtractionTags(sample)` - Multiple tags for extraction
  parameters
- `aliquots` → Custom function with conditional rendering:
  - If parent with children: Button showing child count with `View` icon
  - If aliquot: Purple tag showing parent ID
  - Otherwise: Dash (no aliquots)

**Key Features:**

- Solvent and technique tracking
- Yield calculation for extracts
- Parent-child aliquot relationships
- Filtration and concentration details
- Child sample navigation

---

## Page 5: Analytical Pathway (Optional Advanced Profiling)

**File:**
[TraditionalMedicineAnalyticalPage.js](../frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicineAnalyticalPage.js)

**SRS Stage:** Stage 5 - Advanced Compound Profiling (Optional) **Purpose:**
Track fractionation, isolation, and structural analysis

### Main Grid

| Order | Key                            | Header           | Type | Purpose                                                        | Source        |
| ----- | ------------------------------ | ---------------- | ---- | -------------------------------------------------------------- | ------------- |
| 1     | `accessionNumber`              | Accession Number | Text | Reference to extract/sample                                    | Linked sample |
| 2     | `externalId`                   | Sample ID        | Text | External identifier                                            | Linked sample |
| 3     | `localName`                    | Local Name       | Text | Common name                                                    | Linked sample |
| 4     | `extractId`                    | Extract ID       | Text | Reference to parent extract                                    | Linked sample |
| 5     | `analyticalInfo`               | Analytical Data  | Text | Fractionation method, number of fractions                      | Page data     |
| 6     | `activeConstituentsIdentified` | Constituents     | Text | Named active compounds identified                              | Page data     |
| 7     | `status`                       | Status           | Text | Workflow status (Pending, In Progress, Characterized, Skipped) | Page data     |

**Total Columns:** 7

**Key Features:**

- Fractionation method tracking (Column, TLC, HPLC, Flash, etc.)
- Active constituent identification
- Purification level tracking
- Structural analysis data (NMR, MS, IR)
- Optional pathway support (can be skipped)

---

## Page 6: Product Development & Testing

**File:**
[TraditionalMedicineTestingPage.js](../frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicineTestingPage.js)

**SRS Stage:** Stage 6 - Safety & Efficacy Assessment **Purpose:** Track
phytochemical screening and biological assays

### Main Grid

| Order | Key               | Header              | Type | Purpose                                                     | Source        |
| ----- | ----------------- | ------------------- | ---- | ----------------------------------------------------------- | ------------- |
| 1     | `accessionNumber` | Accession Number    | Text | Reference to sample                                         | Linked sample |
| 2     | `externalId`      | Sample ID           | Text | External identifier                                         | Linked sample |
| 3     | `localName`       | Local Name          | Text | Common name                                                 | Linked sample |
| 4     | `testsPerformed`  | Phytochemical Tests | Text | List of phytochemical screens (Alkaloids, Flavonoids, etc.) | Page data     |
| 5     | `testingInfo`     | Results             | Text | Efficacy and toxicity results summary                       | Page data     |
| 6     | `testedBy`        | Tested By           | Text | Test operator name                                          | Page data     |
| 7     | `status`          | Status              | Text | Workflow status (Pending, In Progress, Complete)            | Page data     |

**Total Columns:** 7

**Key Features:**

- Phytochemical screening tracking
- Safety/Toxicity study types (In Vitro, In Vivo Acute/Subacute/Chronic, etc.)
- Biological assay types (Antimicrobial, Antioxidant, Anti-inflammatory, etc.)
- Efficacy outcome classification
- Three-way approval workflow (Approve/Further Testing/Reject)

---

## Page 7: Formulation of Medical Product

**File:**
[TraditionalMedicineFormulationPage.js](../frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicineFormulationPage.js)

**SRS Stage:** Stage 7 - Product Formulation **Purpose:** Create final
traditional/modern medicine products

### Main Grid

| Order | Key               | Header              | Type | Purpose                                            | Source        |
| ----- | ----------------- | ------------------- | ---- | -------------------------------------------------- | ------------- |
| 1     | `accessionNumber` | Accession Number    | Text | Reference to source material                       | Linked sample |
| 2     | `externalId`      | Sample ID           | Text | External identifier                                | Linked sample |
| 3     | `localName`       | Local Name          | Text | Common name                                        | Linked sample |
| 4     | `productName`     | Product Name        | Text | Name of formulated product                         | Page data     |
| 5     | `formulationInfo` | Formulation Details | Text | Product type, strength, dosage form                | Page data     |
| 6     | `preparedBy`      | Prepared By         | Text | Formulation operator name                          | Page data     |
| 7     | `status`          | Status              | Text | Workflow status (Pending, In Progress, Formulated) | Page data     |

**Total Columns:** 7

**Key Features:**

- Product type selection (Capsule, Tablet, Tincture, Ointment, Tea, Powder,
  Syrup, Oil, etc.)
- Batch ID generation and tracking
- Dosage form specification (Oral, Topical, Sublingual, etc.)
- Strength and quantity tracking
- Ingredient linkage to source extracts
- Shelf life and storage instructions
- QC pass/fail tracking

---

## Page 8: Reporting & Data Archival

**File:**
[TraditionalMedicineArchivalPage.js](../frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicineArchivalPage.js)

**SRS Stage:** Stage 8 - Reporting & Archival **Purpose:** Close lifecycle with
full traceability and regulatory compliance

### Main Grid

| Order | Key               | Header            | Type | Purpose                                                       | Source        |
| ----- | ----------------- | ----------------- | ---- | ------------------------------------------------------------- | ------------- |
| 1     | `accessionNumber` | Accession Number  | Text | Reference to final product                                    | Linked sample |
| 2     | `externalId`      | Sample ID         | Text | External identifier                                           | Linked sample |
| 3     | `localName`       | Local Name        | Text | Common name                                                   | Linked sample |
| 4     | `productName`     | Product           | Text | Final product name                                            | Linked sample |
| 5     | `reportsInfo`     | Reports Generated | Text | Types of reports generated                                    | Page data     |
| 6     | `archivalInfo`    | Archive Status    | Text | Archive location and metadata lock status                     | Page data     |
| 7     | `status`          | Status            | Text | Workflow status (Pending Archival, Archived, Metadata Locked) | Page data     |

**Total Columns:** 7

**Key Features:**

- Multiple report type generation (Sample History, Processing Summary, Product
  Lineage, Quality Certificate)
- Data archival tracking (Raw Lab Data, Spectral Data, Image Data)
- Archive location specification
- Metadata locking for regulatory compliance
- Immutable record preservation

---

## Summary by Page

| Page | File                                     | Purpose                        | Total Columns | Key Focus                                |
| ---- | ---------------------------------------- | ------------------------------ | ------------- | ---------------------------------------- |
| 1    | TraditionalMedicineSampleCreationPage.js | Sample intake & authentication | 13 (×2 grids) | Botanical verification, metadata capture |
| 2    | TraditionalMedicineStoragePage.js        | Storage & herbarium assignment | 6             | Storage location hierarchy, preservation |
| 3    | TraditionalMedicinePreparationPage.js    | Physical processing            | 10            | Yield tracking, QC validation            |
| 4    | TraditionalMedicineExtractionPage.js     | Solvent extraction             | 7             | Aliquot creation, extraction parameters  |
| 5    | TraditionalMedicineAnalyticalPage.js     | Advanced profiling (optional)  | 7             | Fractionation, structural analysis       |
| 6    | TraditionalMedicineTestingPage.js        | Safety & efficacy              | 7             | Assay results, approval workflow         |
| 7    | TraditionalMedicineFormulationPage.js    | Product creation               | 7             | Batch creation, dosage specification     |
| 8    | TraditionalMedicineArchivalPage.js       | Reporting & archival           | 7             | Report generation, compliance tracking   |

---

## Data Flow Across Pages

```
Page 1: Sample Input
  ↓ (Sample with accessionNumber + authentication)
Page 2: Storage Assignment
  ↓ (Preserved sample)
Page 3: Preparation
  ↓ (Processed material + yield)
Page 4: Extraction
  ↓ (Extract + aliquots)
Page 5: Analytics (Optional)
  ↓ (Characterized compound)
Page 6: Testing
  ↓ (Approved extract)
Page 7: Formulation
  ↓ (Product batch)
Page 8: Archival
  (Final product with complete history)
```

---

## Manifest Alignment

The comprehensive manifest file
([tradmed-sample-manifest-example.csv](../test-data/tradmed-sample-manifest-example.csv))
includes all columns required for Page 1 grid import:

**Required Columns (SRS Stage 1):**

- `sample_id` → Maps to `externalId`
- `source_type` → Maps to `sourceType`
- `scientific_name` → Maps to `scientificName`
- `collection_date` → Maps to `collectionDate`
- `collection_site` → Maps to `originLocation`

**Optional Columns:**

- `sample_category` → `sampleCategory`
- `local_name` → `localName`
- `collected_by` → `collectedBy`
- `plant_part` → `plantPart`
- `sample_condition` → `sampleCondition`
- `intended_use` → `intendedUse`
- And 15+ more optional fields

---

## References

- **SRS Document:** `/trmmd.pdf` - Traditional & Modern Medicine Research
  Development Laboratory
- **Manifest Import:**
  [TraditionalMedicineManifestImportModal.js](../frontend/src/components/workflow/TraditionalMedicineManifestImportModal.js)
- **Test Data:**
  [tradmed-sample-manifest-example.csv](../test-data/tradmed-sample-manifest-example.csv)
- **Carbon Design System:** Column rendering patterns
- **React Intl:** Internationalization for headers and messages

---

**Last Updated:** January 2025 **Status:** Complete - All 8 pages documented
