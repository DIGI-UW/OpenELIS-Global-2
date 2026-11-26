# Implementation Plan: Declarative Notebook Workflow System

**Feature Branch**: `copilot/create-declarative-notebook-system`  
**Created**: 2026-03-20  
**Status**: Draft — Architecture Design  

---

## Architecture Overview

The declarative notebook system has three layers:

```
┌──────────────────────────────────────────────────────────────────┐
│  Layer 1: Workflow Definitions (JSON)                            │
│  frontend/src/components/notebook/workflows/definitions/         │
│  gbd.json, virology_lab.json, bacteriology.json, ...             │
│  Describes: ordered pages, page types, form fields               │
└──────────────────────────────┬───────────────────────────────────┘
                               │ imported by
┌──────────────────────────────▼───────────────────────────────────┐
│  Layer 2: Workflow Registry                                      │
│  frontend/src/components/notebook/workflows/registry.js          │
│  Maps workflowKey → WorkflowDefinition                          │
│  Registers custom page component overrides                       │
└──────────────────────────────┬───────────────────────────────────┘
                               │ used by
┌──────────────────────────────▼───────────────────────────────────┐
│  Layer 3: Configurable Components (React)                        │
│  GenericWorkflowTab — the single workflow tab container          │
│  ConfigurableSampleReceptionPage — handles step 1               │
│  ConfigurableProcessingStepPage — handles steps 2–N             │
│  ConfigurableStorageAssignmentPage — handles storage steps       │
│  ConfigurableReportingPage — handles final reporting steps       │
│  ConfigurableManifestImportModal — handles all CSV imports       │
│  ConfigurableFormModal — handles all data-recording modals       │
└──────────────────────────────────────────────────────────────────┘
```

---

## New Files to Create

### Workflow Definitions

```
frontend/src/components/notebook/workflows/
├── definitions/
│   ├── gbd.json                    # GBD lab (10 pages)
│   ├── virology_lab.json           # VirologyLab (10 pages, near-copy of GBD)
│   ├── bacteriology.json           # Bacteriology (9 pages)
│   ├── tb.json                     # TB (7 pages)
│   ├── mntd.json                   # MNTD (10 pages)
│   ├── pharmaceutical.json         # Pharmaceutical (~8 pages)
│   ├── traditional_medicine.json   # Traditional Medicine (9 pages)
│   ├── immunology.json             # Immunology (10 pages)
│   ├── virology.json               # Original Virology (14 pages)
│   ├── bioanalytical.json          # Bioanalytical (5 pages)
│   ├── bioequivalence.json         # Bioequivalence (5 pages)
│   ├── medlab.json                 # Medical Lab (4 pages)
│   ├── biorepository.json          # Biorepository (8 pages)
│   └── pathology.json              # Pathology (6 pages) — mostly custom pages
│
├── registry.js                     # WorkflowKey → Definition mapping + custom renderers
├── schema.js                       # TypeScript-style JSDoc type definitions
└── index.js                        # Public exports
```

### Configurable Components

```
frontend/src/components/notebook/
├── GenericWorkflowTab.js           # Replaces all 12 WorkflowTab components
├── pages/
│   ├── ConfigurableSampleReceptionPage.js
│   ├── ConfigurableProcessingStepPage.js
│   ├── ConfigurableStorageAssignmentPage.js
│   └── ConfigurableReportingPage.js
├── common/
│   ├── ConfigurableManifestImportModal.js  # Replaces 10+ manifest modals
│   ├── ConfigurableFormModal.js            # Replaces per-step modal forms
│   ├── SampleActionBar.js                  # Action buttons row
│   ├── SampleStatusTag.js                  # Status tag rendering
│   ├── AlcoaPlusAuditTrailSection.js       # ALCOA+ sub-section
│   └── ThreeTierReviewSection.js           # 3-tier review sub-section
└── hooks/
    └── useNotebookPage.js                  # Common page state/logic hook
```

### Backend Changes

```
src/main/java/org/openelisglobal/notebook/
└── valueholder/
    └── NoteBook.java    # Add workflowType field

src/main/resources/liquibase/3.4.x.x/
└── XXX-add-workflow-type-to-notebook.xml   # New column + data migration
```

---

## Component Specifications

### `useNotebookPage(pageData, entryId)` Hook

Encapsulates the boilerplate present in every page component.

**Returns:**
```javascript
{
  samples,                 // Array of samples for this page
  setSamples,              // Setter
  loading,                 // Boolean
  hasRealPageId,           // Boolean: pageData.id exists and is not a "default-" synthetic ID
  loadPageSamples,         // useCallback: fetches samples from API
  notify,                  // Helper: ({ kind, title, message }) => void
  selectedSampleIds,       // Array of selected sample IDs
  setSelectedSampleIds,    // Setter
  componentMounted,        // useRef — for async callback safety
}
```

**Example usage in a custom page:**
```javascript
function MyCustomPage({ pageData, entryId, onProgressUpdate }) {
  const {
    samples, loading, hasRealPageId, loadPageSamples, notify, selectedSampleIds
  } = useNotebookPage(pageData, entryId);
  
  // Only need custom logic from here
}
```

---

### `ConfigurableManifestImportModal` Props

```javascript
{
  open: boolean,
  onClose: () => void,
  entryId: number,
  onImportSuccess: (result) => void,
  
  // Content configuration
  uploadEndpoint: string,           // e.g. "/rest/notebook/manifest/gbd"
  title?: string,                   // Modal heading
  requiredFields: ManifestField[],  // CSV columns that must be present
  optionalFields: ManifestField[],  // CSV columns that may be present
}
```

**`ManifestField` shape:**
```javascript
{
  key: string,          // CSV column key
  label: string,        // Display label
  description: string,  // Help text
  example: string,      // Example value
}
```

---

### `ConfigurableFormModal` Props

```javascript
{
  open: boolean,
  onRequestClose: () => void,
  onRequestSubmit: (formValues: object) => void,
  modalHeading: string,
  primaryButtonText?: string,
  isSubmitting?: boolean,
  
  // Form field definitions
  formSections: FormSection[],
  
  // Sub-sections to append after main fields
  subSections?: SubSection[],
}
```

**`FormSection` shape (field definition):**
```javascript
{
  fieldKey: string,
  type: 'text' | 'textarea' | 'number' | 'date' | 'time' | 'checkbox' | 'dropdown' | 'select' | 'fileUpload',
  labelIntlId: string,
  defaultMessage?: string,
  required?: boolean,
  options?: Array<{ id: string, label: string }>,  // for dropdown/select
  min?: number, max?: number, step?: number,        // for number
  rows?: number,                                    // for textarea
}
```

The component renders each field using the appropriate Carbon component:
- `text` → `<TextInput>`
- `textarea` → `<TextArea>`
- `number` → `<NumberInput>` 
- `date` → `<DatePicker>` + `<DatePickerInput>`
- `dropdown` → `<Dropdown>`
- `select` → `<Select>` + `<SelectItem>`
- `checkbox` → `<Checkbox>`
- `fileUpload` → `<FileUploaderDropContainer>`

---

### `ConfigurableProcessingStepPage` Props

```javascript
{
  pageData: object,               // Notebook page data (includes page.id)
  entryId: number,                // Notebook entry ID
  onProgressUpdate?: () => void,  // Called after successful data submission
  
  // Configuration
  config: {
    dataKey: string,              // e.g. "extraction", "pcr", "gel"
    formSections: FormSection[],  // Fields in the data-recording modal
    endpoint?: string,            // Override default API endpoint
    gridColumns?: GridColumnDef[], // Override default sample grid columns
    actionButtonLabel?: string,   // e.g. "Record Extraction" (derived from dataKey if not set)
    subSections?: SubSection[],   // Optional sub-sections (ALCOA+, 3-tier review, image upload)
  }
}
```

**Internal behavior:**
1. Uses `useNotebookPage(pageData, entryId)` for all common state
2. Renders `SampleActionBar` with "Apply [step]" and "Mark Complete" buttons
3. "Apply" opens `ConfigurableFormModal` with `config.formSections`
4. On submit, POSTs to `/rest/notebook/bulk/page/${pageData.id}/samples/apply`
   with `{ sampleIds: [...], data: { [dataKey]: formValues } }`
5. Renders `SampleGrid` with `config.gridColumns` (or sensible defaults)

---

### `GenericWorkflowTab` Props

```javascript
{
  notebookId: number,                    // Notebook template ID
  entryId?: number,                      // Direct entry access (optional)
  
  // Workflow definition (from registry)
  workflowDefinition: WorkflowDefinition,
  
  // Optional: override rendering for specific page types or page keys
  customPageRenderers?: {
    [pageTypeOrKey: string]: React.ComponentType<PageProps>
  }
}
```

**Internal behavior** (extracted from all 12 existing WorkflowTab components):
1. `loadNotebookData()` → determines if creating or viewing an entry
2. `usePageAccessControl()` with definition's `defaultPages`
3. `renderPageContent(page)`:
   - Looks up `page.pageType` in the page type registry
   - Passes `page.config` as props to the appropriate configurable component
   - Falls back to `customPageRenderers[page.pageType]` if registered

---

### `WorkflowRegistry`

```javascript
// frontend/src/components/notebook/workflows/registry.js

import gbdDefinition from './definitions/gbd.json';
import virologyLabDefinition from './definitions/virology_lab.json';
// ... etc

const WORKFLOW_DEFINITIONS = new Map([
  ['gbd',                  gbdDefinition],
  ['virology_lab',         virologyLabDefinition],
  ['bacteriology',         bacteriologyDefinition],
  // ...
]);

// Custom renderers for pages that can't be expressed declaratively
const CUSTOM_PAGE_RENDERERS = {
  'TBTestExecutionPage':         lazy(() => import('../pages/tb/TBTestExecutionPage')),
  'BacteriologyIsolateCreation': lazy(() => import('../pages/bacteriology/BacteriologyIsolateCreationPage')),
  'MNTDAliquoting':              lazy(() => import('../pages/mntd/MNTDAliquotingPage')),
  // ...
};

export function getWorkflowDefinition(workflowKey) {
  return WORKFLOW_DEFINITIONS.get(workflowKey);
}

export function getCustomRenderer(key) {
  return CUSTOM_PAGE_RENDERERS[key];
}
```

---

### `NoteBookInstanceEntryForm.js` Refactor

**Current (fragile):**
```javascript
{noteBookData?.title?.toLowerCase().includes("genomics & bioinformatics laboratory") && (
  <GBDWorkflowTab notebookId={noteBookData.id} />
)}
// ... 11 more conditions
```

**Target:**
```javascript
import { getWorkflowDefinition } from './workflows/registry';

// In the WORKFLOW tab:
const workflowDefinition = getWorkflowDefinition(noteBookData.workflowType);
return workflowDefinition
  ? <GenericWorkflowTab notebookId={noteBookData.id} workflowDefinition={workflowDefinition} />
  : <NotebookWorkflowTab notebookId={noteBookData.id} />;  // Fallback for unknown types
```

This requires `notebook.workflowType` to be populated (see backend changes).

---

## Migration Strategy

### Backward Compatibility

During migration, preserve all existing WorkflowTab and Page components. Only
delete them after the GenericWorkflowTab is proven equivalent via:
1. Automated E2E tests covering each workflow
2. Manual QA sign-off on one workflow before migrating the rest

### Migration Order (least-risky first)

1. **GBD + VirologyLab first** — They are nearly identical; migrating both in
   one pass proves the system handles "same config, different workflowKey"
   and validates `ConfigurableProcessingStepPage` against 20 real pages.

2. **Bioanalytical + Bioequivalence** — Similar to each other; 5-page workflows
   are simpler to validate.

3. **MedLab** — Only 4 pages; quick win.

4. **TB, Bacteriology, Traditional Medicine, MNTD** — Medium complexity;
   each has 1–2 custom pages requiring the escape hatch.

5. **Immunology, Virology (original)** — More complex; 10–14 pages.

6. **Pathology, Biorepository** — Most complex; defer if needed.

### Feature Flag During Migration

Use the `workflowType` column as the feature flag:
- Notebooks with `workflowType` set → use `GenericWorkflowTab`
- Notebooks with no `workflowType` → fall back to old WorkflowTab (title matching)

This allows gradual rollout without breaking anything.

---

## Liquibase Changeset

```xml
<!-- src/main/resources/liquibase/3.4.x.x/XXX-add-workflow-type-to-notebook.xml -->
<changeSet id="XXX-add-workflow-type-to-notebook" author="copilot">
    <addColumn tableName="notebook" schemaName="clinlims">
        <column name="workflow_type" type="VARCHAR(50)" />
    </addColumn>
    
    <!-- Data migration: derive workflowType from existing notebook titles -->
    <!-- Note: ampersands in XML must be written as &amp; to prevent parse errors -->
    <!-- Only for existing templates (isTemplate = true) -->
    <update tableName="notebook" schemaName="clinlims">
        <column name="workflow_type" value="gbd"/>
        <where>is_template = true AND LOWER(title) LIKE '%genomics &amp; bioinformatics%'</where>
    </update>
    <update tableName="notebook" schemaName="clinlims">
        <column name="workflow_type" value="virology_lab"/>
        <where>is_template = true AND (LOWER(title) LIKE '%virologylab%' OR LOWER(title) LIKE '%virology laboratory%')</where>
    </update>
    <!-- ... etc for all workflow types -->
    
    <rollback>
        <dropColumn tableName="notebook" schemaName="clinlims" columnName="workflow_type"/>
    </rollback>
</changeSet>
```

---

## Testing Strategy

### Unit Tests (Jest)

- `useNotebookPage` hook: Test sample loading, hasRealPageId, notify
- `ConfigurableFormModal`: Test each field type renders correctly; test required validation; test submit
- `ConfigurableManifestImportModal`: Test with different `requiredFields`/`optionalFields` configs
- `WorkflowRegistry`: Test `getWorkflowDefinition` returns correct definition

### Integration Tests

- `ConfigurableProcessingStepPage` with mock API: Test the full cycle (load → select → modal → submit → reload)
- `GenericWorkflowTab` with mock notebook: Test page navigation, progress tracking

### E2E Tests (Playwright, `core-app` project)

Per workflow migrated to the declarative system, add a Playwright test covering:
1. Navigate to the workflow
2. Import samples via manifest
3. Advance at least one sample through two steps
4. Verify progress indicators update

---

## Open Questions for Implementation

1. **Server-side workflow definitions**: Should workflow definitions be stored in the database (more flexible, can update without deploy) or bundled in the frontend JS (simpler, faster)? **Recommendation**: Start with frontend JSON; add DB storage as a follow-up.

2. **i18n for field labels**: Should field labels be i18n keys (`labelIntlId`) or plain English strings with an optional i18n override? **Recommendation**: Always require `labelIntlId`; provide `defaultMessage` as fallback. This ensures all fields are translatable from the start.

3. **Column mapping for BulkApplyForm**: The existing `BulkApplyForm` already accepts a `fields` array. Should `ConfigurableProcessingStepPage` reuse `BulkApplyForm` or create its own modal? **Recommendation**: `ConfigurableFormModal` should be used by both `ConfigurableProcessingStepPage` and replace `BulkApplyForm`'s ad-hoc field rendering, unifying both approaches.

4. **Custom page registration**: Should custom pages be registered globally in the registry, or passed via props? **Recommendation**: Props, so each workflow's custom pages are explicit in that workflow's configuration and don't pollute the global registry.
