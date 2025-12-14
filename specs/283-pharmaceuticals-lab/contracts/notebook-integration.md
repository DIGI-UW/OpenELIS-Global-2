# Notebook Integration Contract: OGC-51 Reuse

**Feature:** Pharmaceuticals Laboratory Workflow **Date:** 2025-12-14
**Purpose:** Document the API contract for reusing OGC-51 Notebook/Page
architecture

## Overview

The Pharmaceuticals Laboratory Workflow reuses the existing Notebook/Page
architecture implemented in OGC-51 (Immunology Notebook). This document defines
the integration points, expected interfaces, and extension patterns.

## Backend Services to Reuse

### NotebookService

**Location:** `org.openelisglobal.notebook.service.NotebookService`

**Key Methods:**

- `createNotebook(NotebookForm form)` - Creates a new notebook instance
- `getNotebookById(String id)` - Retrieves notebook by ID
- `getNotebooksByType(String notebookType)` - Filter notebooks by type
- `updateNotebookStatus(String id, NotebookStatus status)` - Update workflow
  status

**Extension for Pharma:**

- Add `notebookType = "PHARMACEUTICAL"` to distinguish from Immunology notebooks
- Reuse existing approval workflow (Draft → Submitted → Approved/Rejected)

### NotebookPageService

**Location:** `org.openelisglobal.notebook.service.NotebookPageService`

**Key Methods:**

- `createPage(NotebookPageForm form)` - Creates a page within a notebook
- `getPagesByNotebookId(String notebookId)` - Get all pages for a notebook
- `updatePageData(String pageId, Map<String, Object> data)` - Update page fields
- `calculateResults(String pageId)` - Trigger calculation engine (e.g., %RSD,
  CV)

**Extension for Pharma:**

- Create pharma-specific page templates (HPLC Potency, TLC ID, Dissolution,
  etc.)
- Implement custom calculation methods for pharmaceutical assays

## Frontend Components to Reuse

### NotebookContainer

**Location:** `frontend/src/components/notebook/NotebookContainer.jsx`

**Props Interface:**

```javascript
{
  notebookId: string,
  notebookType: 'IMMUNOLOGY' | 'PHARMACEUTICAL',
  onStatusChange: (status) => void,
  readOnly: boolean
}
```

### NotebookPage

**Location:** `frontend/src/components/notebook/NotebookPage.jsx`

**Props Interface:**

```javascript
{
  pageId: string,
  templateId: string,
  data: object,
  onSave: (data) => void,
  onCalculate: () => void
}
```

## Pharma-Specific Templates

The following templates must be created for pharmaceutical assays:

| Template ID           | Assay Type         | Calculations             | Acceptance Criteria           |
| --------------------- | ------------------ | ------------------------ | ----------------------------- |
| `PHARMA-HPLC-POTENCY` | HPLC Potency       | %RSD, Mean, Recovery     | %RSD <= 2.0, Recovery 98-102% |
| `PHARMA-TLC-ID`       | TLC Identification | Rf comparison            | Rf within 0.05 of standard    |
| `PHARMA-DISSOLUTION`  | Dissolution        | % Released at timepoints | Per USP monograph             |
| `PHARMA-MICRO-LIMIT`  | Microbial Limit    | CFU count                | Below specified limits        |
| `PHARMA-STERILITY`    | Sterility Test     | Growth/No Growth         | No growth at 14 days          |

## Integration Points

### 1. AssayRun ↔ NotebookPage Link

```java
// AssayRun entity (new)
@Entity
public class AssayRun {
    @Column(name = "notebook_page_id")
    private String notebookPageId;  // Links to existing NotebookPage

    @Column(name = "sample_id")
    private String sampleId;

    @Column(name = "aliquot_id")
    private String aliquotId;
}
```

### 2. OOS Flag ↔ DeviationCAPA Link

When `NotebookPage.oosFlag = true`:

1. System creates pending `DeviationCAPA` record
2. Blocks finalization until CAPA linked
3. Multi-tier review enforced

### 3. Review Workflow

Reuse existing `NotebookApproval` entity:

- Primary Reviewer (Analyst)
- Secondary Reviewer (Supervisor)
- Investigator Sign-off (for OOS cases)

## REST API Endpoints

### Existing Endpoints (Reuse)

- `GET /rest/notebook/{id}` - Get notebook
- `POST /rest/notebook` - Create notebook
- `GET /rest/notebook/{id}/pages` - Get pages
- `POST /rest/notebook/{id}/pages` - Add page
- `PUT /rest/notebook/page/{id}` - Update page

### New Endpoints (Pharma-Specific)

- `GET /rest/pharmaceutical/assay-templates` - List pharma templates
- `POST /rest/pharmaceutical/assay-run` - Create assay run with notebook page
- `GET /rest/pharmaceutical/assay-run/{id}/results` - Get calculated results
- `POST /rest/pharmaceutical/assay-run/{id}/deviation` - Link deviation/CAPA

## Configuration

Add to `common.properties`:

```properties
# Pharmaceutical Notebook Configuration
notebook.pharma.templates.enabled=true
notebook.pharma.default.replicates=3
notebook.pharma.rsd.limit=2.0
notebook.pharma.review.tiers=2
```

## Testing Requirements

1. **Unit Tests:** Mock NotebookService, verify pharma-specific calculations
2. **Integration Tests:** Create pharma notebook, add page, verify OOS workflow
3. **E2E Tests:** Full assay execution from template selection to approval

## Dependencies

- OGC-51 must be merged to `develop` before pharma notebook work begins
- Verify `NotebookService` and `NotebookPageService` APIs are stable
