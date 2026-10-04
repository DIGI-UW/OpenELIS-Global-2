# Feature Specification: Declarative Notebook Workflow System

**Feature Branch**: `copilot/create-declarative-notebook-system`
**Created**: 2026-03-20
**Status**: Draft — Analysis & Planning Phase

---

## Executive Summary

The `demo/ethiopia` branch contains over **100 notebook page components** and
**12 workflow tab components** that are nearly identical in structure but
duplicated for each lab type (GBD, VirologyLab, Bacteriology, TB, Traditional
Medicine, etc.). Adding a new lab type currently requires thousands of lines of
hand-written, copy-pasted React code and Liquibase changesets. The only
variation between labs is the ordered list of workflow steps and which
domain-specific form fields appear in each step.

This specification proposes replacing that code-heavy approach with a
**declarative JSON-driven system** that lets labs be fully configured without
writing new React components. A new lab workflow would be described in a JSON
file (or database entry) and rendered automatically by a small set of
configurable, reusable components.

---

## 1. Problem Analysis

### 1.1 Current Architecture

```
NoteBookInstanceEntryForm.js
  └─ Selects WorkflowTab by notebook.title.includes("...") string matching
       ├─ GBDWorkflowTab          (~534 lines)
       ├─ VirologyLabWorkflowTab  (~545 lines)
       ├─ BacteriologyWorkflowTab (~658 lines)
       ├─ NotebookWorkflowTab     (~821 lines, Immunology/Virology)
       ├─ TBWorkflowTab
       ├─ MNTDWorkflowTab
       ├─ PharmaceuticalWorkflowTab
       ├─ TraditionalMedicineWorkflowTab
       ├─ BioanalyticalWorkflowTab
       ├─ BioequivalenceWorkflowTab
       ├─ MedLabWorkflowTab
       ├─ PathologyWorkflowTab
       └─ BiorepositoryWorkflowTab
            └─ Each WorkflowTab imports and switch-dispatches to
               5–14 per-lab Page components (300–1,617 lines each)
```

**Total mass:** ~12 workflow tabs × ~600 lines + ~100 page components ×
~800 lines = **≈87,000 lines of highly repetitive frontend code** for what
is conceptually configuration data.

### 1.2 Root Causes of Duplication

| Duplicated Pattern | Count | Notes |
|---|---|---|
| WorkflowTab container (loading, entry management, page dispatch) | 12 | Identical except for page list and component switch |
| Sample reception page | 12+ | Each is a near-copy of `SampleReceptionPage.js` |
| Processing step page (modal + sample grid) | ~60 | Identical structure; only form fields differ |
| Manifest import modal | 10+ | Identical rendering; differ only in data point descriptions and POST endpoint |
| `loadPageSamples()` callback | ~100 | Literally the same function in every page |
| ALCOA+ audit trail section | ~10 | Identical JSX block, copy-pasted |
| 3-tier review workflow section | ~8 | Identical JSX block, copy-pasted |
| `notify()` helper | ~100 | Same two-line function in every page |
| `hasRealPageId` check | ~100 | Same boolean expression in every page |

### 1.3 Workflow Selection Anti-Pattern

`NoteBookInstanceEntryForm.js` currently selects which workflow to render by
checking whether the notebook's `title` string contains certain substrings:

```jsx
{noteBookData?.title?.toLowerCase().includes("genomics & bioinformatics laboratory") && (
  <GBDWorkflowTab notebookId={noteBookData.id} />
)}
{noteBookData?.title?.toLowerCase().includes("virologylab") && (
  <VirologyLabWorkflowTab notebookId={noteBookData.id} />
)}
// ... 10 more conditions ...
// AND a final negative-match catch-all that checks !includes() for all 12 types
```

This is fragile (any title rename breaks the workflow), does not scale, and
forces the code to know about every lab type at compile time.

---

## 2. Proposed Solution: Declarative Workflow System

### 2.1 Core Concept

A notebook workflow is fully described by a **workflow definition object**. This
object specifies the ordered list of pages and, for each page, which page type
to render and what configuration that page type receives. The system provides a
small set of configurable page-type components that cover all observed patterns.
New lab types are added by writing a JSON definition — no new React components
required.

### 2.2 Workflow Definition Schema

```typescript
// A complete workflow definition
interface WorkflowDefinition {
  // Unique identifier for this workflow type (stored in DB, not derived from title)
  workflowKey: string;          // e.g. "gbd", "virology_lab", "bacteriology"
  
  // Human-readable name for admin UI
  displayName: string;          // e.g. "Genomics & Bioinformatics Laboratory"
  
  // Pages in order
  pages: PageDefinition[];
  
  // Optional: manifest import config for this workflow's reception page
  manifestConfig?: ManifestConfig;
}

// A single page in the workflow
interface PageDefinition {
  order: number;                 // 1-based display order
  title: string;                 // Navigation title shown in PageNavigation
  titleIntlId?: string;          // Optional i18n key for the title
  
  // Which built-in page type to render
  pageType: PageType;
  
  // Type-specific configuration (validated against the pageType)
  config: PageConfig;
  
  // Optional: roles required to view this page (empty = all roles)
  allowedRoles?: string[];
}

// All supported page types
type PageType =
  | "sampleReception"         // Step 1: Import samples, mark received
  | "processingStep"          // Record field data for selected samples (modal form)
  | "childSampleCreation"     // Create aliquots / child samples
  | "storageAssignment"       // Assign samples to physical storage locations
  | "resultEntry"             // Enter test results
  | "qualityControl"          // QC check with pass/fail
  | "reportingExport"         // Data export and reporting
  | "auditReview"             // Multi-tier review and sign-off
  | "custom"                  // Escape hatch: use a named registered component

// Config for "sampleReception" pages
interface SampleReceptionConfig {
  // Endpoint to POST CSV manifest to (if different from global default)
  manifestEndpoint?: string;
  // Which fields to show in the sample grid for received samples
  sampleDisplayFields?: string[];
  // Whether samples can be linked to existing OpenELIS orders
  enableOrderLinking?: boolean;
}

// Config for "processingStep" pages
interface ProcessingStepConfig {
  // Key under sample.data JSONB where this step's data is stored
  dataKey: string;             // e.g. "extraction", "pcr", "gel"
  // Endpoint to POST data to (defaults to /rest/notebook/page/{id}/samples/{dataKey})
  endpoint?: string;
  // Form sections shown in the modal when recording data for selected samples
  formSections: FormSection[];
  // Columns to show in sample grid for samples at this step
  gridColumns?: GridColumnDef[];
  // Optional sub-sections (e.g. ALCOA+ audit trail, 3-tier review)
  subSections?: SubSection[];
}

// Config for "storageAssignment" pages  
interface StorageAssignmentConfig {
  // Whether to show storage hierarchy selector
  enableHierarchySelector?: boolean;
  // Whether to show box layout viewer
  enableBoxLayout?: boolean;
  // Whether to allow retrieval tracking
  enableRetrieval?: boolean;
  // Whether to allow disposal recording
  enableDisposal?: boolean;
}

// Config for "reportingExport" pages
interface ReportingExportConfig {
  // Which report types are available
  reportTypes: string[];
  // Whether to show analytics charts
  enableCharts?: boolean;
}

// Config for "custom" pages — escape hatch for complex domain-specific pages
interface CustomPageConfig {
  // Name of a pre-registered component in the component registry
  componentKey: string;        // e.g. "TBTestExecutionPage"
}

// A single form field in a modal
interface FormSection {
  fieldKey: string;            // e.g. "methodKit", "lotNumbers"
  type: FieldType;
  labelIntlId: string;         // i18n key
  defaultMessage?: string;     // fallback English text
  required?: boolean;
  // Type-specific options
  options?: Array<{id: string; label: string}>;  // for "dropdown" / "select"
  min?: number; max?: number; step?: number;     // for "number"
  rows?: number;                                 // for "textarea"
  validation?: string;                           // regex or named validator
}

type FieldType =
  | "text" | "textarea" | "number" | "date" | "time" | "checkbox"
  | "dropdown" | "select" | "fileUpload";

// Optional sub-sections that can be attached to a processingStep
interface SubSection {
  type: "alcoaAuditTrail" | "threeTierReview" | "qcPassFail" | "imageUpload";
  config?: Record<string, unknown>;
}

// Manifest import configuration
interface ManifestConfig {
  // POST endpoint for manifest upload
  uploadEndpoint: string;
  // Required columns in the CSV
  requiredFields: ManifestField[];
  // Optional columns
  optionalFields: ManifestField[];
}

interface ManifestField {
  key: string;
  label: string;
  description: string;
  example: string;
}
```

### 2.3 Example: GBD Workflow Definition

```json
{
  "workflowKey": "gbd",
  "displayName": "Genomics & Bioinformatics Laboratory",
  "manifestConfig": {
    "uploadEndpoint": "/rest/notebook/manifest/gbd",
    "requiredFields": [
      { "key": "sampleId",          "label": "Sample ID",            "description": "Unique sample identifier", "example": "GBD-2024-001-A" },
      { "key": "sampleType",        "label": "Sample Type",          "description": "DNA, RNA, tissue, swabs, isolates, PCR products, libraries", "example": "DNA" },
      { "key": "source",            "label": "Source/Origin",        "description": "Facility or study site",  "example": "Research Hospital Addis Ababa" },
      { "key": "collectionDate",    "label": "Collection Date",      "description": "Date of collection (YYYY-MM-DD)", "example": "2024-01-15" },
      { "key": "receptionDateTime", "label": "Reception Date & Time","description": "Date and time of reception", "example": "2024-01-16 14:00" }
    ],
    "optionalFields": [
      { "key": "projectStudyAssociation", "label": "Project/Study Association", "description": "Research project identifier", "example": "PROJ-GENOMICS-2024" },
      { "key": "volumeConcentration",     "label": "Volume/Concentration",      "description": "Volume (mL) or concentration (ng/µL)", "example": "50 ng/µL" },
      { "key": "a260_280",               "label": "A260/280 Ratio",             "description": "Purity metric for nucleic acid", "example": "1.8-2.0" },
      { "key": "a260_230",               "label": "A260/230 Ratio",             "description": "Salt contamination indicator",  "example": "2.0-2.2" },
      { "key": "rin",                    "label": "RIN",                        "description": "RNA Integrity Number",          "example": "8.5" }
    ]
  },
  "pages": [
    {
      "order": 1,
      "title": "Sample Intake & Registration",
      "pageType": "sampleReception",
      "config": {
        "sampleDisplayFields": ["accessionNumber", "externalId", "sampleType", "collectionDate", "source", "volumeConcentration", "projectStudyAssociation"]
      }
    },
    {
      "order": 2,
      "title": "DNA/RNA Extraction",
      "pageType": "processingStep",
      "config": {
        "dataKey": "extraction",
        "formSections": [
          { "fieldKey": "methodKit",    "type": "dropdown", "labelIntlId": "notebook.gbd.extraction.methodKit", "defaultMessage": "Extraction Method/Kit *", "required": true,
            "options": [{"id":"Qiagen DNeasy","label":"Qiagen DNeasy"},{"id":"Trizol","label":"Trizol"},{"id":"Phenol/Chloroform","label":"Phenol/Chloroform"},{"id":"Other","label":"Other"}] },
          { "fieldKey": "lotNumbers",   "type": "text",     "labelIntlId": "notebook.common.lotNumbers",         "defaultMessage": "Lot Numbers *",           "required": true },
          { "fieldKey": "operator",     "type": "text",     "labelIntlId": "notebook.common.operator",           "defaultMessage": "Operator *",              "required": true },
          { "fieldKey": "dateTime",     "type": "date",     "labelIntlId": "notebook.common.date",               "defaultMessage": "Extraction Date" },
          { "fieldKey": "notes",        "type": "textarea", "labelIntlId": "notebook.common.notes",              "defaultMessage": "Notes" }
        ],
        "gridColumns": [
          {"key": "accessionNumber",  "header": "Accession #"},
          {"key": "externalId",       "header": "Sample ID"},
          {"key": "sampleType",       "header": "Sample Type"},
          {"key": "methodKit",        "header": "Method/Kit"},
          {"key": "operator",         "header": "Operator"},
          {"key": "status",           "header": "Status", "render": "statusTag"}
        ]
      }
    },
    {
      "order": 3,
      "title": "Quality & Quantity Assessment",
      "pageType": "processingStep",
      "config": {
        "dataKey": "qc",
        "formSections": [
          { "fieldKey": "instrument", "type": "dropdown", "labelIntlId": "notebook.gbd.qc.instrument", "defaultMessage": "Instrument",
            "options": [{"id":"nanodrop","label":"NanoDrop"},{"id":"qubit","label":"Qubit"},{"id":"bioanalyzer","label":"Bioanalyzer"}] },
          { "fieldKey": "concentration", "type": "number", "labelIntlId": "notebook.gbd.qc.concentration", "defaultMessage": "Concentration (ng/µL)", "min": 0, "max": 5000, "step": 0.1 },
          { "fieldKey": "a260_280",      "type": "number", "labelIntlId": "notebook.gbd.qc.a260_280",     "defaultMessage": "A260/280 Ratio",         "min": 0, "max": 3,    "step": 0.01 },
          { "fieldKey": "operator",      "type": "text",   "labelIntlId": "notebook.common.operator",     "defaultMessage": "Operator" },
          { "fieldKey": "dateTime",      "type": "date",   "labelIntlId": "notebook.common.date",         "defaultMessage": "QC Date" },
          { "fieldKey": "notes",         "type": "textarea","labelIntlId": "notebook.common.notes",        "defaultMessage": "Notes" }
        ]
      }
    },
    {
      "order": 4,
      "title": "PCR Amplification",
      "pageType": "processingStep",
      "config": {
        "dataKey": "pcr",
        "formSections": [
          { "fieldKey": "type",             "type": "dropdown", "labelIntlId": "notebook.gbd.pcr.type", "defaultMessage": "PCR Type",
            "options": [{"id":"conventional","label":"Conventional PCR"},{"id":"qpcr","label":"qPCR"}] },
          { "fieldKey": "primers",          "type": "text",     "labelIntlId": "notebook.gbd.pcr.primers",          "defaultMessage": "Primers" },
          { "fieldKey": "expectedBandSize", "type": "number",   "labelIntlId": "notebook.gbd.pcr.expectedBandSize", "defaultMessage": "Expected Band Size (bp)", "min": 50, "max": 15000 },
          { "fieldKey": "operator",         "type": "text",     "labelIntlId": "notebook.common.operator",          "defaultMessage": "Operator" },
          { "fieldKey": "dateTime",         "type": "date",     "labelIntlId": "notebook.common.date",              "defaultMessage": "PCR Date" },
          { "fieldKey": "notes",            "type": "textarea", "labelIntlId": "notebook.common.notes",             "defaultMessage": "Notes" }
        ]
      }
    },
    {
      "order": 5,
      "title": "Gel Electrophoresis",
      "pageType": "processingStep",
      "config": {
        "dataKey": "gel",
        "formSections": [
          { "fieldKey": "gelType",        "type": "dropdown", "labelIntlId": "notebook.gbd.gel.type", "defaultMessage": "Gel Type *", "required": true,
            "options": [{"id":"agarose","label":"Agarose Gel"},{"id":"polyacrylamide","label":"Polyacrylamide (PAGE)"}] },
          { "fieldKey": "productSize",    "type": "number",   "labelIntlId": "notebook.gbd.gel.productSize",    "defaultMessage": "Observed Product Size (bp)", "min": 50, "max": 15000 },
          { "fieldKey": "concentration",  "type": "number",   "labelIntlId": "notebook.gbd.gel.concentration",  "defaultMessage": "Concentration (ng/µL)",      "min": 0,  "max": 5000, "step": 0.1 },
          { "fieldKey": "quality",        "type": "dropdown", "labelIntlId": "notebook.gbd.gel.quality",        "defaultMessage": "Quality Assessment",
            "options": [{"id":"excellent","label":"Excellent"},{"id":"good","label":"Good"},{"id":"acceptable","label":"Acceptable"},{"id":"poor","label":"Poor"}] },
          { "fieldKey": "integrityStatus","type": "dropdown", "labelIntlId": "notebook.gbd.gel.integrityStatus","defaultMessage": "Integrity Status *", "required": true,
            "options": [{"id":"pass","label":"Pass"},{"id":"fail","label":"Fail"}] },
          { "fieldKey": "operator",       "type": "text",     "labelIntlId": "notebook.common.operator",        "defaultMessage": "Operator" },
          { "fieldKey": "dateTime",       "type": "date",     "labelIntlId": "notebook.common.date",            "defaultMessage": "Gel Date" },
          { "fieldKey": "notes",          "type": "textarea", "labelIntlId": "notebook.common.notes",           "defaultMessage": "Notes" }
        ],
        "subSections": [
          { "type": "imageUpload", "config": { "accept": ["png", "jpg", "jpeg", "tiff", "tif"], "endpoint": "/rest/notebook/upload/gel-image" } },
          { "type": "alcoaAuditTrail" },
          { "type": "threeTierReview" }
        ]
      }
    },
    {
      "order": 6,
      "title": "Library Preparation",
      "pageType": "processingStep",
      "config": {
        "dataKey": "libraryPrep",
        "formSections": [
          { "fieldKey": "platform",         "type": "dropdown", "labelIntlId": "notebook.gbd.libprep.platform", "defaultMessage": "Sequencing Platform",
            "options": [{"id":"illumina","label":"Illumina"},{"id":"dnbseq","label":"DNBSEQ"}] },
          { "fieldKey": "concentration",    "type": "number",   "labelIntlId": "notebook.gbd.libprep.concentration",   "defaultMessage": "Library Concentration (nM)" },
          { "fieldKey": "sizeDistribution", "type": "text",     "labelIntlId": "notebook.gbd.libprep.sizeDistribution", "defaultMessage": "Size Distribution" },
          { "fieldKey": "barcode",          "type": "text",     "labelIntlId": "notebook.gbd.libprep.barcode",          "defaultMessage": "Barcode" },
          { "fieldKey": "operator",         "type": "text",     "labelIntlId": "notebook.common.operator",              "defaultMessage": "Operator" },
          { "fieldKey": "dateTime",         "type": "date",     "labelIntlId": "notebook.common.date",                  "defaultMessage": "Prep Date" },
          { "fieldKey": "notes",            "type": "textarea", "labelIntlId": "notebook.common.notes",                 "defaultMessage": "Notes" }
        ],
        "subSections": [
          { "type": "alcoaAuditTrail" },
          { "type": "threeTierReview" }
        ]
      }
    },
    {
      "order": 7,
      "title": "Bioanalyzer QC",
      "pageType": "processingStep",
      "config": {
        "dataKey": "bioanalyzer",
        "formSections": [
          { "fieldKey": "rin",              "type": "number", "labelIntlId": "notebook.gbd.bioanalyzer.rin",              "defaultMessage": "RIN Score",              "min": 0, "max": 10, "step": 0.1 },
          { "fieldKey": "fragmentSize",     "type": "number", "labelIntlId": "notebook.gbd.bioanalyzer.fragmentSize",     "defaultMessage": "Fragment Size (bp)",     "min": 0 },
          { "fieldKey": "concentration",    "type": "number", "labelIntlId": "notebook.gbd.bioanalyzer.concentration",    "defaultMessage": "Library Concentration (nM)", "min": 0 },
          { "fieldKey": "qcOutcome",        "type": "dropdown","labelIntlId": "notebook.gbd.bioanalyzer.qcOutcome",       "defaultMessage": "QC Outcome",
            "options": [{"id":"pass","label":"Pass"},{"id":"fail","label":"Fail"},{"id":"borderline","label":"Borderline"}] },
          { "fieldKey": "operator",         "type": "text",   "labelIntlId": "notebook.common.operator",                  "defaultMessage": "Operator" },
          { "fieldKey": "dateTime",         "type": "date",   "labelIntlId": "notebook.common.date",                      "defaultMessage": "QC Date" },
          { "fieldKey": "notes",            "type": "textarea","labelIntlId": "notebook.common.notes",                     "defaultMessage": "Notes" }
        ]
      }
    },
    {
      "order": 8,
      "title": "Sequencing",
      "pageType": "processingStep",
      "config": {
        "dataKey": "sequencing",
        "formSections": [
          { "fieldKey": "sequencer",    "type": "dropdown", "labelIntlId": "notebook.gbd.sequencing.sequencer", "defaultMessage": "Sequencer",
            "options": [{"id":"Illumina","label":"Illumina"},{"id":"DNBSEQ","label":"DNBSEQ"}] },
          { "fieldKey": "runId",        "type": "text",     "labelIntlId": "notebook.gbd.sequencing.runId",     "defaultMessage": "Run ID" },
          { "fieldKey": "q30Score",     "type": "number",   "labelIntlId": "notebook.gbd.sequencing.q30Score",  "defaultMessage": "Q30 Score (%)", "min": 0, "max": 100, "step": 0.1 },
          { "fieldKey": "operator",     "type": "text",     "labelIntlId": "notebook.common.operator",          "defaultMessage": "Operator" },
          { "fieldKey": "dateTime",     "type": "date",     "labelIntlId": "notebook.common.date",              "defaultMessage": "Run Date" },
          { "fieldKey": "notes",        "type": "textarea", "labelIntlId": "notebook.common.notes",             "defaultMessage": "Notes" }
        ]
      }
    },
    {
      "order": 9,
      "title": "Bioinformatics Analysis & Data Submission",
      "pageType": "processingStep",
      "config": {
        "dataKey": "bioinformatics",
        "formSections": [
          { "fieldKey": "pipeline",      "type": "text",     "labelIntlId": "notebook.gbd.bio.pipeline",       "defaultMessage": "Analysis Pipeline" },
          { "fieldKey": "softwareVersion","type": "text",    "labelIntlId": "notebook.gbd.bio.softwareVersion", "defaultMessage": "Software Version" },
          { "fieldKey": "referenceDb",   "type": "text",     "labelIntlId": "notebook.gbd.bio.referenceDb",    "defaultMessage": "Reference Database" },
          { "fieldKey": "operator",      "type": "text",     "labelIntlId": "notebook.common.operator",        "defaultMessage": "Analyst" },
          { "fieldKey": "dateTime",      "type": "date",     "labelIntlId": "notebook.common.date",            "defaultMessage": "Analysis Date" },
          { "fieldKey": "notes",         "type": "textarea", "labelIntlId": "notebook.common.notes",           "defaultMessage": "Notes" }
        ]
      }
    },
    {
      "order": 10,
      "title": "Storage & Environmental Monitoring",
      "pageType": "storageAssignment",
      "config": {
        "enableHierarchySelector": true,
        "enableBoxLayout": true,
        "enableRetrieval": true,
        "enableDisposal": true
      }
    }
  ]
}
```

---

## 3. Reusable Component Candidates

### 3.1 Priority 1 — High Impact, Immediate Extraction

These components either already exist as semi-shared but have multiple
near-identical copies, or they represent patterns that appear in literally
every page and workflow in the system.

#### `GenericWorkflowTab` (replaces all 12 WorkflowTab components)

All 12 workflow tab components share the following ~400-line shell:

```
loadNotebookData() → loadEntryData() or loadNotebookAndEntry()
createEntryForNotebook()
handleProgressUpdate()
usePageAccessControl()
PageNavigation + page content dispatch
```

The only differences are: (1) which `defaultPages` array is used, and
(2) which component is rendered for each page order number.

A single `GenericWorkflowTab` component could accept:
- `workflowDefinition: WorkflowDefinition` — the JSON config
- `customPageRenderers?: Record<string, ReactComponent>` — escape hatch for
  pages that need custom rendering (maps `pageType` or `pageKey` to a component)

This eliminates ~7,000 lines of duplicated container logic.

#### `ConfigurableManifestImportModal` (replaces 10+ manifest modals)

`GBDManifestImportModal.js` (1,123 lines) and `VirologyLabManifestImportModal.js`
(1,131 lines) are byte-for-byte identical except for:
- The sample ID example prefix (`GBD-2024-001-A` vs `VLAB-2024-001-A`)
- Two sentences of description text
- The server endpoint they POST to

`BacteriologyManifestImportModal.js` (1,263 lines), `TBManifestImportModal.js`
(1,366 lines), and `MNTDManifestImportModal.js` (1,428 lines) all follow the
same multi-step CSV upload flow (Upload → Map Columns → Preview → Import)
with different field lists.

A single `ConfigurableManifestImportModal` component accepting:
- `uploadEndpoint: string`
- `requiredFields: ManifestField[]`
- `optionalFields: ManifestField[]`
- `title: string`
- standard `open`, `onClose`, `entryId`, `onImportSuccess` props

Would replace **~10,000 lines** of duplicated modal code.

#### `ConfigurableProcessingStepPage` (replaces ~60 processing step pages)

Pages like `GBDDNARNAExtractionPage`, `GBDPCRAmplificationPage`,
`GBDGelElectrophoresesPage`, `VirologyLabDNARNAExtractionPage`,
`TraditionalMedicinePreparationPage`, etc., all follow the exact same
structural pattern:

1. `loadPageSamples()` from `/rest/notebook/page/${pageData.id}/samples`
2. Show `SampleGrid` for samples at this step
3. "Apply [Step Name]" button that opens a modal form
4. Modal form with step-specific fields
5. POST to `/rest/notebook/bulk/page/${pageId}/samples/apply`
6. "Mark Complete" button for bulk status update

A `ConfigurableProcessingStepPage` accepting a `ProcessingStepConfig`
(with `dataKey` and `formSections`) would replace these components.

#### `ConfigurableFormModal` (drives the modal forms in processing steps)

Every processing step page has a modal form with:
- A `<Modal>` wrapper
- A `<Grid>` of `<Column>` elements
- Fields matching `FormSection` definitions
- Submit/cancel button handling

A `ConfigurableFormModal` accepting `formSections: FormSection[]` and
rendering each field type (text, dropdown, date, number, textarea, checkbox,
fileUpload) would provide a consistent, declarative form experience.

### 3.2 Priority 2 — High Impact, Moderate Complexity

#### `WorkflowRegistry` (replaces title-string matching in NoteBookInstanceEntryForm)

Currently, `NoteBookInstanceEntryForm.js` uses 12 `includes()` checks on the
notebook title to decide which workflow tab to render. This should be replaced
with a registry that maps `workflowKey` (stored in the database) to a workflow
definition.

Two approaches:
- **Frontend registry**: A `Map<string, WorkflowDefinition>` imported from
  `frontend/src/components/notebook/workflows/registry.js`
- **Backend-driven**: The workflow definition is served via REST
  (`/rest/notebook/workflow-definition/{key}`) and cached on the client

The registry approach also requires adding a `workflowType` column to the
`notebook` table in Liquibase, or at minimum a `workflowKey` field in the
notebook type dictionary.

#### `SampleActionBar` (replaces ad-hoc button rows in every page)

Every page has a row of buttons at the top: some combination of:
- Import from CSV (opens ManifestImportModal)
- Add single sample
- Mark selected complete
- Mark selected skipped
- Apply data to selected
- Refresh / reload
- Export

A `SampleActionBar` component accepting an `actions: ActionDef[]` array
would eliminate ~30 lines of repeated button JSX per page.

#### `useNotebookPage` hook (encapsulates common page logic)

Every page component has:

```javascript
const componentMounted = useRef(false);
const hasRealPageId = pageData?.id && !String(pageData.id).startsWith("default-");
const loadPageSamples = useCallback(() => { /* same pattern */ }, [pageData?.id]);
useEffect(() => { componentMounted.current = true; loadPageSamples(); return () => { componentMounted.current = false; }; }, [entryId, pageData?.id]);
const notify = useCallback(({ kind, title, message }) => { setNotificationVisible(true); addNotification({ kind, title, message }); }, [...]);
```

A `useNotebookPage(pageData, entryId)` hook returning
`{ samples, setSamples, loading, hasRealPageId, loadPageSamples, notify, selectedSampleIds, setSelectedSampleIds }`
would eliminate ~50 lines of boilerplate from every page component.

### 3.3 Priority 3 — Domain-Specific Sub-Components

These are reusable sub-sections that appear across multiple pages within the
same sub-domain group:

#### `AlcoaPlusAuditTrailSection`

The ALCOA+ audit trail section (recordedBy, recordedDate, recordedTime,
lastModifiedBy, lastModifiedDate, lastModifiedTime) appears identically in
`GBDGelElectrophoresesPage`, `GBDLibraryPreparationPage`, and several other
pages. Currently copy-pasted as ~60 lines of JSX + ~12 state variables.

A `AlcoaPlusAuditTrailSection` component with its own internal state (or
accepting a controlled value) and a `SubSection` config type `"alcoaAuditTrail"`
would centralize this.

#### `ThreeTierReviewSection`

The three-tier review workflow (primary review → bioinformatics review →
final approval) appears identically in `GBDGelElectrophoresesPage` and
`GBDLibraryPreparationPage`. Currently ~80 lines of JSX + ~18 state variables
per page.

#### `SampleStatusTag` (already near-extracted but inconsistently used)

Every page renders status tags using switch statements on
`PENDING / IN_PROGRESS / COMPLETED / SKIPPED`. A shared `SampleStatusTag`
component accepting a `status` string would provide consistent rendering.

#### `StorageHierarchySelector` (already exists as a shared component)

Already exists in `workflow/StorageHierarchySelector.js` and
`workflow/BoxLayoutViewer.js` — good examples of the desired pattern. The
`ConfigurableStorageAssignmentPage` would compose these.

### 3.4 Priority 4 — Already Shared (Reference Patterns)

These are already done right and should be the model for the new work:

| Component | Location | Notes |
|---|---|---|
| `SampleGrid` | `workflow/SampleGrid.js` | Accepts `columns`, `additionalColumns`, `showSelection`, etc. |
| `PageNavigation` | `workflow/PageNavigation.js` | Already role-aware; no changes needed |
| `BulkApplyForm` | `workflow/BulkApplyForm.js` | Already field-driven; extend its `fields` schema |
| `usePageAccessControl` | `hooks/usePageAccessControl.js` | Clean hook; no changes needed |
| `ManifestImportModal` | `workflow/ManifestImportModal.js` | Good baseline; extend to accept `dataPoints` |

---

## 4. Backend Changes Required

### 4.1 Add `workflowType` to Notebook

The title-string matching must be replaced with an explicit workflow type stored
in the database. Two options:

**Option A (Preferred): New column on `notebook` table**
```sql
ALTER TABLE clinlims.notebook ADD COLUMN workflow_type VARCHAR(50);
```

This stores a `workflowKey` like `"gbd"`, `"virology_lab"`, `"bacteriology"`.
Requires a Liquibase changeset.

**Option B: Use the existing `type` dictionary field**

The `notebook.type` column already maps to the `dictionary` table. New
dictionary entries for each workflow type could be added via Liquibase without
schema changes. However, this conflates "notebook type" (research vs
diagnostic) with "workflow template" (which steps to run), which has different
semantics.

Option A is recommended for clarity.

### 4.2 Serve Workflow Definitions via REST (optional)

If workflow definitions are stored on the backend (vs bundled in frontend JS),
a new REST endpoint can serve them:

```
GET /rest/notebook/workflow-definition/{workflowKey}
→ Returns WorkflowDefinition JSON
```

This enables workflow configs to be updated without a frontend deploy.
For phase 1, frontend-bundled JSON files are simpler and sufficient.

### 4.3 Existing APIs Are Already Sufficient

The sample data APIs are already generic:
- `GET /rest/notebook/page/{pageId}/samples` — works for all page types
- `POST /rest/notebook/bulk/page/{pageId}/samples/apply` — works for all step types (takes arbitrary `data` JSONB)
- `POST /rest/notebook/bulk/page/{pageId}/samples/status` — works for status transitions

No new backend endpoints are required for the initial implementation.

---

## 5. Phased Implementation Plan

### Phase 0: Foundation (No UI changes)

**Goal**: Lay groundwork without breaking anything.

- [ ] Add `workflowType` column to `notebook` table via Liquibase
- [ ] Update `NoteBook` JPA entity and DAO
- [ ] Populate `workflow_type` for existing notebooks via migration data
- [ ] Extract `useNotebookPage` hook and replace boilerplate in 2-3 pages as proof of concept
- [ ] Write unit tests for the hook

### Phase 1: Configurable Manifest Import Modal

**Goal**: Replace 10+ near-identical manifest import modals with one.

- [ ] Create `ConfigurableManifestImportModal` accepting `ManifestConfig`
- [ ] Migrate `GBDManifestImportModal` and `VirologyLabManifestImportModal` to use it (they are 99% identical — ideal proof)
- [ ] Migrate remaining 8 modal variants one by one
- [ ] Keep old modal files as thin wrappers during migration; delete after all usages updated

**LOC reduction**: ~9,000 lines

### Phase 2: Generic Workflow Tab Container

**Goal**: Replace 12 workflow tab components with one.

- [ ] Create `GenericWorkflowTab` accepting `workflowDefinition` and optional `customPageRenderers`
- [ ] Create `WorkflowRegistry` mapping `workflowKey → WorkflowDefinition`
- [ ] Update `NoteBookInstanceEntryForm` to read `notebook.workflowType` and dispatch to `GenericWorkflowTab` with the registered definition
- [ ] Migrate `GBDWorkflowTab` (proof of concept) — verify all 10 pages still work
- [ ] Migrate all 12 workflow tabs

**LOC reduction**: ~7,000 lines

### Phase 3: Configurable Processing Step Pages

**Goal**: Replace ~60 processing step page components with `ConfigurableProcessingStepPage`.

- [ ] Create `ConfigurableFormModal` accepting `formSections: FormSection[]`
- [ ] Create `ConfigurableProcessingStepPage` accepting `ProcessingStepConfig`
- [ ] Migrate the 10 GBD pages (already expressed as JSON in this spec)
- [ ] Migrate VirologyLab pages (nearly identical to GBD — trivial migration)
- [ ] Identify pages requiring `custom` page type (escape hatch) and list them explicitly
- [ ] Migrate remaining processing step pages across all labs

**LOC reduction**: ~35,000 lines

### Phase 4: Configurable Sample Reception Page

**Goal**: Replace 12+ sample reception page components.

- [ ] Create `ConfigurableSampleReceptionPage` accepting `SampleReceptionConfig`
- [ ] Migrate all reception pages

**LOC reduction**: ~6,000 lines

### Phase 5: Declarative JSON Workflow Definitions

**Goal**: Move all workflow definitions to JSON files.

- [ ] Create `frontend/src/components/notebook/workflows/definitions/` directory
- [ ] Create one JSON file per workflow type (gbd.json, virologylab.json, bacteriology.json, etc.)
- [ ] Validate JSON files against TypeScript schema at build time
- [ ] Load definitions via static import (Phase 5a) or REST (Phase 5b)
- [ ] Add Cypress/Playwright E2E test for each migrated workflow to verify no regression

---

## 6. Pages Requiring Custom (Non-Declarative) Rendering

Not every page can be expressed as a simple processing step. These pages have
complex domain-specific UI that would require either custom sub-components or
parameterization that is impractical to express in JSON. They will use the
`"pageType": "custom"` escape hatch with a named registered component.

| Page | Lab | Reason |
|---|---|---|
| `TBTestExecutionPage` | TB | Complex multi-panel UI (GeneXpert, Smear, Culture, DST panels) with nested forms |
| `BacteriologyIsolateCreationPage` | Bacteriology | Parent-child sample tree management with visual representation |
| `MNTDAliquotingPage` | MNTD | Plate-layout viewer, assay plate creator, box layout management |
| `BiorepositoryIntakePage` | Biorepository | Multi-tab form with complex shipment reception UI |
| `BiorepositorySampleRequestPage` | Biorepository | Complex approval workflow with document verification |
| `BacteriologyQCBackend` | Bacteriology | Custom CSS-heavy visualization, separate component |
| `BiorepositoryReportingPage` | Biorepository | Multi-tab dashboard with charts, export, and audit trail tabs |
| `ImmunologyReportingREDCapPage` | Immunology | REDCap integration-specific UI |
| `PathologyWorkflowTab` (entire) | Pathology | Pathology-specific hierarchy table, deeply domain-specific |

These represent a minority of pages (~10-15%). The declarative system should
embrace this explicitly: most pages are declarative, complex pages use the
custom escape hatch.

---

## 7. Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| Regression in existing workflows | Medium | Exhaustive E2E tests per phase; phased migration with feature flags |
| JSON schema expressiveness gaps | Medium | Start with GBD (best-documented); expand schema iteratively |
| Performance (JSON config parsing) | Low | Configs are tiny; bundle-time import, no runtime overhead |
| i18n coverage gaps | Low | Provide `defaultMessage` in every field; add missing keys incrementally |
| Backend migration data accuracy | Low | Use `COALESCE(workflow_type, derived_from_title)` fallback during transition |
| Custom pages not fitting the escape hatch | Low | The `custom` page type gives full flexibility |

---

## 8. Success Metrics

| Metric | Current | Target |
|---|---|---|
| Lines of code in `frontend/src/components/notebook/` | ~87,000 | <15,000 |
| Number of React component files for notebooks | ~130 | <25 |
| Steps to add a new lab workflow | Write ~5,000 lines of code | Write ~100 lines of JSON |
| Time to add a new lab workflow | 1–2 weeks | < 1 day |
| Test coverage of notebook workflow logic | Fragmented | Centralized; testable via configurable component unit tests |

---

## 9. Non-Goals

This spec explicitly does NOT propose:
- Building a visual drag-and-drop workflow editor UI (that is a future feature)
- Moving notebook templates to the database-defined format (templates are already in the DB; this is about frontend rendering)
- Changing the backend API surface (existing APIs are sufficient)
- Supporting workflow branching or conditional pages (linear workflows only, for now)
- Migrating the complex custom pages (Pathology, TB test execution, Biorepository request, MNTD aliquoting)

---

## 10. Open Questions

1. **Where to store workflow definitions long-term?** Frontend JSON files
   (simple, no deploy needed for new workflows) vs. backend REST (workflow
   configs can be updated without a frontend deploy, but adds backend complexity).
   Recommendation: Start with frontend JSON files; add backend storage as a
   follow-on feature.

2. **How to handle labs that have a unique manifest backend endpoint?** Each
   lab's manifest import currently hits a lab-specific controller
   (`VirologyLabManifestImportController`, etc.). Should we consolidate to a
   single generic manifest import endpoint that takes a `workflowKey` parameter,
   or keep lab-specific endpoints and just configure which one to call?
   Recommendation: Consolidate to a single `/rest/notebook/manifest/{workflowKey}`
   endpoint on the backend as part of Phase 1.

3. **How to handle the `workflowType` transition for existing notebooks?**
   Existing notebooks in the Ethiopia demo branch have no `workflow_type` column.
   A Liquibase migration script should derive the value from the title string
   (one-time migration), after which the title-string matching in the frontend
   can be removed.

4. **Field validation registry**: The schema already includes a `validation`
   string field (see `FormSection` interface) that maps to named validators.
   The open question is what named validators to provide out of the box
   (e.g., `"lotNumber"`, `"dateISO"`, `"accessionNumber"`) and whether the
   registry should also accept inline regex strings for ad-hoc validation.
