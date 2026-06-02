# Biorepository QC Data Source Trace

## Problem (before fix)

| Surface | API | Backend source | Eligibility |
|---------|-----|----------------|-------------|
| **Storage Management** (`/Storage/samples`) | `GET /rest/storage/sample-items` | `SampleStorageServiceImpl.getAllSamplesWithAssignments()` | Active = not disposed; optional department/device filters |
| **QC Inspection** (legacy) | `GET /rest/biorepository/qc-inspection/samples` | `BioSampleService.getAll()` filtered by `WorkflowStatus.STORED` | Only rows in `biosample` with STORED status |

Imported or assigned sample-items often had storage assignments but **no** `BioSample` row, so QC showed **Total Stored = 0** while Storage showed thousands of active items.

## Fix (current)

`BiorepositoryQcSamplePoolService` / `BiorepositoryQcSamplePoolServiceImpl`:

1. **Source of truth**: `sampleStorageService.getAllSamplesWithAssignments()` (same as Storage Dashboard).
2. **Active filter**: `IStatusService` — exclude `SampleStatus.Disposed` (matches `StorageDashboardServiceImpl` `status=active`).
3. **Must have assignment**: non-blank `location` path.
4. **Biorepository scope** (notebook-aware):
   - Notebook `departmentId` vs row `departmentTestSectionId` / `departmentName`
   - Device under resolved biorepository hierarchy (`biorepositoryStorage` flag or department fallback on rooms)
   - Location path contains department name when IDs are missing
5. **BioSample link**: `getBySampleItemId` or `ensureBioSampleForStoredSampleItem` (creates STORED extension only when a real assignment exists).

## API wiring

| Endpoint | Uses pool service |
|----------|-------------------|
| `GET /rest/biorepository/qc-inspection/samples?notebookId=` | `listSamplesForQcTable` |
| `GET /rest/biorepository/qc-inspection/storage-overview` | `buildStorageOverview` (includes `scopeStats`, `diagnostics`) |
| `POST /rest/biorepository/qc-inspection/generate-round` | Unchanged; reads `eligibleSamples` from overview built via pool |

## Diagnostics (`storage-overview` response)

| Field | Meaning |
|-------|---------|
| `storageManagementActiveInScope` | Active, assigned rows matching biorepository department scope (before BioSample ensure) |
| `qcPoolTotal` | Rows in QC pool after ensure |
| `qcEligibleCount` | Same as pool total at build time (quarter filter applied separately for generation list) |
| `excludedNotInScope` | Active assigned rows outside biorepository scope |
| `excludedNoBioSample` | In scope but `ensureBioSampleForStoredSampleItem` returned null |
| `bioSamplesLazyLinked` | BioSample rows created during this overview build via `ensureBioSampleForStoredSampleItem` |

## AHRI / dev Docker verification (2026-06-01)

Stack: `https://localhost`, admin session, WAR mounted via `docker-compose.qc-deploy.yml`.

| Check | URL / params | Result |
|-------|----------------|--------|
| Storage active (all departments) | `GET /rest/storage/sample-items?countOnly=true` | **active: 20**, totalSampleItems: 20 |
| QC storage overview | `GET .../storage-overview?notebookId=27&includeInspected=true` | **totalStored: 19**, qcPoolTotal: 19, excludedNotInScope: 1, bioSamplesLazyLinked: 0 |
| QC samples table | `GET .../samples?notebookId=27` | **19** rows |
| Round generation | `POST .../generate-round` with `boxesPerRound=1`, `samplesPerBox=2` | 200, 2 samples (dev has one eligible box) |

**Notebook URL note:** `/NoteBookInstanceEditForm/{id}` uses the **notebook template id** (`notebook.id`), not `notebook_entry.id`. For biorepository QC UI use template **27** (`workflowType=biorepository`, department **196** Biorepository Laboratory). Template **16** maps to Traditional & Modern Medicine (dept **187**) and does not show the biorepository QC workflow.

**Scope fix:** `BiorepositoryQcSamplePoolServiceImpl.resolveQcDepartmentScope()` always unions the Biorepository Laboratory test section (id **196** in dev) with notebook-linked departments so QC pool matches Storage assignments under dept 196.

## Key classes

- `org.openelisglobal.biorepository.service.BiorepositoryQcSamplePoolService`
- `org.openelisglobal.biorepository.service.BiorepositoryQcSamplePoolServiceImpl`
- `org.openelisglobal.biorepository.controller.rest.BiorepositoryQCInspectionRestController` (delegates sample/overview loading)
- `org.openelisglobal.biorepository.controller.rest.BiorepositoryQcHierarchyParser` (path parsing; now public)

## Tests

- `BiorepositoryQcSamplePoolServiceTest` — disposed exclusion, ensure BioSample, sampleItemId in list API
- `BiorepositoryQcSamplePoolVsStorageDashboardTest` — active-in-scope count alignment with Storage-style filtering
- `BiorepositoryQcRoundGenerationServiceTest` — round generation unchanged
