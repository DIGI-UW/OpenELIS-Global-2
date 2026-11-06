# Research: Notebook Workflow Component Inventory

**Document Type**: Research / Inventory  
**Feature**: 015 — Declarative Notebook Workflow System  
**Source Branch**: `demo/ethiopia` (as of 2026-03-20)

---

## 1. Workflow Tab Inventory

All 12 workflow tab components share the same ~400-line shell logic (loading,
entry management, progress tracking, page dispatch). Only the page list and
per-page component selection differ.

| Component | Lines | Pages | Matched By |
|---|---|---|---|
| `NotebookWorkflowTab` | 821 | 10 (Immunology/Virology default) | catch-all: `!includes()` all 12 named types |
| `BacteriologyWorkflowTab` | 658 | 9 | `includes("bacteriology")` |
| `GBDWorkflowTab` | 534 | 10 | `includes("genomics & bioinformatics laboratory")` |
| `VirologyLabWorkflowTab` | 545 | 10 | `includes("virologylab")` or `includes("virology laboratory")` |
| `TBWorkflowTab` | ~500 | 7 | `includes("tuberculosis")` |
| `MNTDWorkflowTab` | ~500 | 10 | `includes("malaria and neglected tropical disease")` |
| `PharmaceuticalWorkflowTab` | 513 | ~8 | `includes("pharmaceutical")` |
| `TraditionalMedicineWorkflowTab` | ~500 | ~9 | `includes("traditional")` or `includes("modern medicine")` |
| `BioanalyticalWorkflowTab` | ~500 | 5 | `includes("bioanalytical")` |
| `BioequivalenceWorkflowTab` | ~500 | 5 | `includes("bioequivalence")` |
| `MedLabWorkflowTab` | ~450 | 4 | `includes("medical laboratory")` |
| `PathologyWorkflowTab` | ~500 | ~6 | `includes("pathology")` |
| `BiorepositoryWorkflowTab` | ~500 | ~8 | `includes("biorepository")` |

**Note**: The notebook title string matching is a fragile coupling between
database content and frontend code. A `workflowType` database column would
decouple these.

---

## 2. Page Component Inventory by Type

### 2.1 Sample Reception Pages

These pages all follow the identical pattern:
1. Load samples from `/rest/notebook/page/{id}/samples`
2. Display `SampleGrid` with selection
3. "Import from CSV" button → opens lab-specific `ManifestImportModal`
4. "Mark Received/Complete" button → bulk status update

| Component | Lab | Unique Features |
|---|---|---|
| `SampleReceptionPage` | Generic | Basic manifest import, minimal fields |
| `GBDSampleReceptionPageEnhanced` | GBD | Genomics-specific metadata, `GBDManifestImportModal` |
| `VirologyLabSampleReceptionPageEnhanced` | VirologyLab | Same as GBD with different strings |
| `BacteriologySampleReceptionPage` | Bacteriology | Rich metadata (project, participant, consent), `BacteriologyManifestImportModal` |
| `TBSampleCreationPage` | TB | Patient metadata, `TBManifestImportModal` |
| `MNTDProcessingQCPage` (Page 1) | MNTD | MNTD-specific fields |
| `BioanalyticalSampleReceptionPage` | Bioanalytical | Study-specific metadata, `BioanalyticalManifestImportModal` |
| `BioequivalenceSampleReceptionPage` | Bioequivalence | Similar to Bioanalytical |
| `TraditionalMedicineSampleCreationPage` | Traditional | Botanical metadata (species, taxonomy), extensive classification |
| `ImmunologySampleReceptionPage` | Immunology | Patient/participant metadata |
| `VirologySampleReceptionPage` | Virology | Cell culture source metadata |

**Declarative support**: All can be expressed as `"pageType": "sampleReception"`.

### 2.2 Processing Step Pages (Record Data for Selected Samples)

These pages follow the pattern: select samples → open modal → fill form → POST data.

**GBD Lab (10 pages):**
| Component | Lines | Step | Key Fields |
|---|---|---|---|
| `GBDDNARNAExtractionPage` | 740 | DNA/RNA extraction | methodKit, lotNumbers, operator, date, notes |
| `GBDQualityQuantityAssessmentPage` | 1,091 | QC assessment | instrument (NanoDrop/Qubit/Bioanalyzer), concentration, ratios |
| `GBDPCRAmplificationPage` | 1,154 | PCR amplification | type, primers, protocol, cyclingConditions, expectedBandSize |
| `GBDGelElectrophoresesPage` | 1,603 | Gel electrophoresis | gelType, productSize, concentration, quality, integrityStatus + image upload + ALCOA+ + 3-tier review |
| `GBDLibraryPreparationPage` | 1,384 | Library preparation | platform, concentration, sizeDistribution, barcode, adapters + ALCOA+ + 3-tier review |
| `GBDBioanalyzerQCPage` | 1,339 | Bioanalyzer QC | rin, fragmentSize, concentration, qcOutcome |
| `GBDSequencingPage` | 1,523 | Sequencing | sequencer, runId, clusterDensity, q30Score |
| `GBDBioinformaticsAnalysisPage` | 1,473 | Bioinformatics | pipeline, software, referenceDb, alignment stats |
| `GBDStorageEnvironmentalMonitoringPage` | 1,617 | Storage + monitoring | storage hierarchy, retrieval, disposal, logbook |

**VirologyLab Lab (10 pages, nearly identical to GBD):**  
`VirologyLabDNARNAExtractionPage`, `VirologyLabQualityQuantityAssessmentPage`,
`VirologyLabPCRAmplificationPage`, `VirologyLabGelElectrophoresesPage`,
`VirologyLabLibraryPreparationPage`, `VirologyLabBioanalyzerQCPage`,
`VirologyLabSequencingPage`, `VirologyLabBioinformaticsAnalysisPage`,
`VirologyLabStorageEnvironmentalMonitoringPage`

These are essentially byte-for-byte copies of the GBD pages with "VirologyLab"
string substitutions. This confirms the immediate value of declarative configs:
the VirologyLab workflow would just reuse the same page type configs as GBD
with minor field-option label differences.

**Traditional Medicine Lab (9 pages):**
| Component | Lines | Step | Key Fields |
|---|---|---|---|
| `TraditionalMedicineSampleCreationPage` | ~800 | Sample creation | species, taxonomy, origin, collector, authentication |
| `TraditionalMedicineAuthenticationPage` | ~600 | Authentication | method, result, verifiedBy |
| `TraditionalMedicineAuthenticationStoragePage` | ~500 | Auth + storage | storage conditions |
| `TraditionalMedicinePreparationPage` | ~700 | Preparation | preparation method, equipment, operator |
| `TraditionalMedicineExtractionPage` | ~600 | Extraction | solvent, technique, yield |
| `TraditionalMedicineTestingPage` | ~700 | Testing | test type, equipment, results |
| `TraditionalMedicineAnalyticalPage` | ~600 | Analytical testing | analytical method, standards |
| `TraditionalMedicineFormulationPage` | ~600 | Formulation | formulation type, excipients |
| `TraditionalMedicineArchivalPage` | ~400 | Archival | archive location, retention |

**Bacteriology Lab (9 pages, 5 are standard processing step type):**
| Component | Lines | Step | Declarative? |
|---|---|---|---|
| `BacteriologySampleReceptionPage` | ~400 | Reception | Yes |
| `BacteriologyReceptionVerificationPage` | ~400 | Verification | Yes |
| `BacteriologyTemporaryStoragePage` | ~350 | Storage | Yes |
| `BacteriologyIsolateCreationPage` | ~700 | Isolate creation | **Partially** — complex parent-child UI |
| `BacteriologyProcessingQCPage` | ~500 | Processing QC | Yes |
| `BacteriologyAssayTestExecutionPage` | ~600 | Assay execution | Yes |
| `BacteriologyPostAnalysisPage` | ~400 | Post-analysis | Yes |
| `BacteriologySampleRetrievalDisposalPage` | ~500 | Retrieval/disposal | Yes |
| `BacteriologyReportingDataExportPage` | ~400 | Reporting | Yes (`reportingExport` type) |

**TB Lab (7 pages, 2 are complex):**
| Component | Lines | Step | Declarative? |
|---|---|---|---|
| `TBSampleCreationPage` | ~500 | Reception | Yes |
| `TBInitialProcessingPage` | ~400 | Initial processing | Yes |
| `TBTestExecutionPage` | ~800 | Test execution | **No** — 4-panel complex UI (GeneXpert/Smear/Culture/DST) |
| `TBStorageAssignmentPage` | ~350 | Storage | Yes |
| `TBQualityCheckPage` | ~400 | QC | Yes |
| `TBReportingPage` | ~400 | Reporting | Yes |

**MNTD Lab (10 pages):**
| Component | Lines | Step | Declarative? |
|---|---|---|---|
| `MNTDProcessingQCPage` (reception) | ~500 | Reception/QC | Yes |
| `MNTDAliquotingPage` | ~800 | Aliquoting | **Partially** — plate layout, assay plate creator |
| `MNTDDataAnalysisPage` | ~500 | Data analysis/export | Yes (`reportingExport` type) |
| + 7 other pages | ~400 each | Various | Yes |

**Immunology Lab (10 pages):**
All follow standard patterns except `ImmunologyReportingREDCapPage` (REDCap-specific).

**Virology (original, not VirologyLab) Lab (14+ pages):**
Similar patterns, some complex (cell culture, dark room imaging).

**Pharmaceutical Lab:**  
Standard processing steps, pharmaceutical-specific fields.

**Bioanalytical / Bioequivalence Labs (5 pages each, nearly identical):**
Reception → Test Assignment → Analytical Execution → Storage/Archiving → Reporting

**MedLab (4 pages):**
Reception → QC → Processing → Routing

**Pathology Lab:**
Unique hierarchical tissue processing workflow; mostly custom.

**Biorepository (8+ pages):**
Complex sample request approval workflow; `BiorepositorySampleRequestPage` is
particularly complex with document verification and multi-tab approval flow.

### 2.3 Storage / Environmental Monitoring Pages

Several labs share a storage page with the same pattern:
- Storage hierarchy selector
- Box/position assignment
- Retrieval tracking
- Disposal recording

| Component | Lab |
|---|---|
| `GBDStorageEnvironmentalMonitoringPage` | GBD |
| `VirologyLabStorageEnvironmentalMonitoringPage` | VirologyLab |
| `BacteriologyTemporaryStoragePage` | Bacteriology |
| `TBStorageAssignmentPage` | TB |
| `BioanalyticalStorageArchivingPage` | Bioanalytical |
| `BioequivalenceStorageArchivingPage` | Bioequivalence |
| `BiorepositoryStorageAssignmentPage` | Biorepository |
| `BiorepositoryEnvironmentalMonitoringPage` | Biorepository |

All can be expressed as `"pageType": "storageAssignment"`.

### 2.4 Reporting / Export Pages

Several labs have a final page for data export and reporting:

| Component | Lab |
|---|---|
| `BacteriologyReportingDataExportPage` | Bacteriology |
| `TBReportingPage` | TB |
| `MNTDDataAnalysisPage` | MNTD |
| `BioanalyticalReportingPage` | Bioanalytical |
| `BioequivalenceReportingPage` | Bioequivalence |
| `BiorepositoryReportingPage` | Biorepository |
| `ImmunologyReportingREDCapPage` | Immunology |

All can be expressed as `"pageType": "reportingExport"` with different
`reportTypes` config.

---

## 3. Manifest Import Modal Inventory

| Component | Lines | Differences from base |
|---|---|---|
| `ManifestImportModal` (base) | 604 | Generic; maps columns manually |
| `GBDManifestImportModal` | 1,123 | Genomics data points; auto-infers columns |
| `VirologyLabManifestImportModal` | 1,131 | Identical to GBD; "virology" strings |
| `BacteriologyManifestImportModal` | 1,263 | Rich clinical/environmental fields |
| `TBManifestImportModal` | 1,366 | Patient + specimen metadata |
| `MNTDManifestImportModal` | 1,428 | Epidemiological fields |
| `TraditionalMedicineManifestImportModal` | 1,070 | Botanical taxonomy fields |
| `PharmaManifestImportModal` | ~900 | Pharmaceutical study fields |
| `BioanalyticalManifestImportModal` | ~1,000 | Bioanalytical study fields |
| `BioequivalenceManifestImportModal` | ~1,000 | Similar to Bioanalytical |
| `ImmunologyManifestImportModal` | ~900 | Immunology study fields |
| `PathologyManifestImportModal` | ~900 | Pathology specimen fields |

**GBD vs VirologyLab modal comparison** (primary duplication case):
- Imports: Identical
- `EXPECTED_DATA_POINTS.sampleType.example`: Only difference is `"bacterial/viral isolates"` vs `"viral isolates"`
- Upload endpoint: Only difference between the two

---

## 4. Shared Utility Components (Already Correct Pattern)

These are already shared correctly and should serve as the model:

| Component | Location | Used By |
|---|---|---|
| `SampleGrid` | `workflow/SampleGrid.js` | All ~100 page components |
| `PageNavigation` | `workflow/PageNavigation.js` | All 13 workflow tabs |
| `BulkApplyForm` | `workflow/BulkApplyForm.js` | Multiple pages |
| `BulkLinkOrderModal` | `workflow/BulkLinkOrderModal.js` | Multiple pages |
| `BulkOrderModal` | `workflow/BulkOrderModal.js` | Multiple pages |
| `LinkOrderModal` | `workflow/LinkOrderModal.js` | Multiple pages |
| `LinkPatientModal` | `workflow/LinkPatientModal.js` | Multiple pages |
| `FlagSampleModal` | `workflow/FlagSampleModal.js` | Multiple pages |
| `StorageHierarchySelector` | `workflow/StorageHierarchySelector.js` | Storage pages |
| `BoxLayoutViewer` | `workflow/BoxLayoutViewer.js` | Storage pages |
| `AssayPlateCreator` | `workflow/AssayPlateCreator.js` | MNTD aliquoting |
| `TraceabilityChecklist` | `workflow/TraceabilityChecklist.js` | Selected pages |
| `usePageAccessControl` | `hooks/usePageAccessControl.js` | All workflow tabs |
| `useComponentMounted` | `notebook/hooks/useComponentMounted.js` | Some pages |

---

## 5. Duplicated Code Patterns (Verbatim)

The following code blocks appear in **every single page component** without
variation. These are prime candidates for extraction into a shared hook.

### 5.1 Component Mount Guard (100+ occurrences)
```javascript
const componentMounted = useRef(false);
// ...in useEffect:
componentMounted.current = true;
// ...in callback:
if (componentMounted.current) { /* ... */ }
// ...in cleanup:
return () => { componentMounted.current = false; };
```

### 5.2 Real Page ID Check (100+ occurrences)
```javascript
const hasRealPageId = pageData?.id && !String(pageData.id).startsWith("default-");
```

### 5.3 Notification Helper (100+ occurrences)
```javascript
const { setNotificationVisible, addNotification } = useContext(NotificationContext);
const notify = useCallback(({ kind = NotificationKinds.info, title, message }) => {
  setNotificationVisible(true);
  addNotification({ kind, title, message });
}, [addNotification, setNotificationVisible]);
```

### 5.4 Load Page Samples (100+ occurrences, verbatim)
```javascript
const loadPageSamples = useCallback(() => {
  if (!pageData?.id || String(pageData.id).startsWith("default-")) {
    setLoading(false);
    return;
  }
  setLoading(true);
  getFromOpenElisServer(
    `/rest/notebook/page/${pageData.id}/samples`,
    (response) => {
      if (componentMounted.current) {
        // transform and set samples
        setLoading(false);
      }
    },
  );
}, [pageData?.id]);

useEffect(() => {
  componentMounted.current = true;
  loadPageSamples();
  return () => { componentMounted.current = false; };
}, [entryId, pageData?.id, loadPageSamples]);
```

### 5.5 ALCOA+ Audit Trail State (10+ occurrences)
```javascript
const [recordedBy, setRecordedBy] = useState("");
const [recordedDate, setRecordedDate] = useState(new Date().toISOString().split("T")[0]);
const [recordedTime, setRecordedTime] = useState("09:00");
const [lastModifiedBy, setLastModifiedBy] = useState("");
const [lastModifiedDate, setLastModifiedDate] = useState(new Date().toISOString().split("T")[0]);
const [lastModifiedTime, setLastModifiedTime] = useState("09:00");
```

### 5.6 Three-Tier Review State (8+ occurrences)
```javascript
const [primaryReviewCompleted, setPrimaryReviewCompleted] = useState(false);
const [primaryReviewedBy, setPrimaryReviewedBy] = useState("");
const [primaryReviewedDate, setPrimaryReviewedDate] = useState(new Date().toISOString().split("T")[0]);
const [primaryReviewedTime, setPrimaryReviewedTime] = useState("09:00");
const [bioReviewCompleted, setBioReviewCompleted] = useState(false);
// ... 6 more state variables
const [finalApprovalCompleted, setFinalApprovalCompleted] = useState(false);
// ... 3 more state variables
```

### 5.7 Status Tag Render (100+ occurrences)
```javascript
const renderStatus = (sample) => {
  const status = sample.status || "PENDING";
  switch (status.toUpperCase()) {
    case "COMPLETED": return <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>Completed</Tag>;
    case "IN_PROGRESS": return <Tag type="blue" size="sm">In Progress</Tag>;
    default: return <Tag type="gray" size="sm" renderIcon={Pending}>Pending</Tag>;
  }
};
```

---

## 6. Workflow-to-Lab Mapping

The following table maps each lab workflow to its key characteristics,
which informs the JSON config design:

| Lab | workflowKey | Steps | Primary Data Pattern | Unique Aspects |
|---|---|---|---|---|
| Immunology | `immunology` | 10 | Patient samples, child samples, REDCap | REDCap export |
| Virology (vaccine) | `virology` | 14 | Cell culture progression | Cell viability, titer |
| MNTD | `mntd` | 10 | Epidemiology, plate-based | Aliquot plates, box layout |
| Pharmaceutical | `pharmaceutical` | ~8 | Pharmaceutical study | ALCOA+ throughout |
| Traditional Medicine | `traditional_medicine` | 9 | Botanical | Botanical taxonomy, species |
| TB | `tb` | 7 | Clinical TB | GeneXpert, DST panels |
| Bacteriology | `bacteriology` | 9 | Clinical/environmental | Multi-origin (human/animal/env/food) |
| Pathology | `pathology` | ~6 | Tissue/histology | Tissue hierarchy |
| Bioanalytical | `bioanalytical` | 5 | Pharma study | Study-linked samples |
| Bioequivalence | `bioequivalence` | 5 | Pharma study | Nearly identical to bioanalytical |
| Medical Lab | `medlab` | 4 | Routine clinical | Standard clinical workflow |
| Biorepository | `biorepository` | 8 | Biobank | Complex request/approval workflow |
| GBD | `gbd` | 10 | Genomics | Sequencing, bioinformatics |
| VirologyLab | `virology_lab` | 10 | Virology genomics | ~Identical to GBD |

---

## 7. Backend API Surface (Relevant to Frontend)

All notebook page APIs are already generic. No new backend APIs are required
for the declarative system.

| Endpoint | Method | Purpose |
|---|---|---|
| `/rest/notebook/page/{id}/samples` | GET | Load samples for a page |
| `/rest/notebook/bulk/page/{id}/samples/apply` | POST | Apply data to selected samples |
| `/rest/notebook/bulk/page/{id}/samples/status` | POST | Bulk status update |
| `/rest/notebook/manifest/{lab}` | POST | Import samples from CSV (lab-specific variants) |
| `/rest/notebook-entry/{id}` | GET | Load entry details |
| `/rest/notebook-entry/by-notebook/{nbId}` | GET | Find entries for a notebook |
| `/rest/notebook/view/{id}` | GET | Load notebook with pages |
| `/rest/notebook/create` | POST | Create notebook entry |
| `/rest/notebook/update/{id}` | POST | Update notebook entry |

**Consolidation opportunity**: The `/rest/notebook/manifest/{lab}` endpoints
(one per lab type, each in its own controller) could be consolidated to a
single `/rest/notebook/manifest` endpoint that accepts a `workflowType`
parameter, reducing backend duplication to match the frontend simplification.
