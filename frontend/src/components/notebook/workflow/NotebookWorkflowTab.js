import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import { Loading, Grid, Column, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { usePageAccessControl } from "../../../hooks/usePageAccessControl";
import config from "../../../config.json";
import { NotificationContext } from "../../layout/Layout";
import PageNavigation from "./PageNavigation";
import SampleReceptionPage from "../pages/SampleReceptionPage";
import ImmunologySampleReceptionPage from "../pages/immunology/ImmunologySampleReceptionPage";
import ImmunologyInitialProcessingPage from "../pages/immunology/ImmunologyInitialProcessingPage";
import ImmunologyAdditionalAssaysPage from "../pages/immunology/ImmunologyAdditionalAssaysPage";
import ImmunologyChildSampleCreationPage from "../pages/immunology/ImmunologyChildSampleCreationPage";
import ImmunologyPostAnalysisPage from "../pages/immunology/ImmunologyPostAnalysisPage";
import ImmunologyResultCompilationPage from "../pages/immunology/ImmunologyResultCompilationPage";
import ImmunologyArchivingPage from "../pages/immunology/ImmunologyArchivingPage";
import ImmunologyDataAnalysisPage from "../pages/immunology/ImmunologyDataAnalysisPage";
import VirologySampleReceptionPage from "../pages/virology/VirologySampleReceptionPage";
import VirologyMediaPreparationPage from "../pages/virology/VirologyMediaPreparationPage";
import VirologyCellCulturePage from "../pages/virology/VirologyCellCulturePage";
import VirologyQualityControlPage from "../pages/virology/VirologyQualityControlPage";
import VirologyVirusCulturePage from "../pages/virology/VirologyVirusCulturePage";
import VirologyDarkRoomImagingPage from "../pages/virology/VirologyDarkRoomImagingPage";
import VirologyFormulationPage from "../pages/virology/VirologyFormulationPage";
import VirologyFeedingPage from "../pages/virology/VirologyFeedingPage";
import VirologyPackagingPage from "../pages/virology/VirologyPackagingPage";
import VirologyVirusIsolationPage from "../pages/virology/VirologyVirusIsolationPage";
import VirologyTiterMeasurementPage from "../pages/virology/VirologyTiterMeasurementPage";
import VirologyGenomeSequencingPage from "../pages/virology/VirologyGenomeSequencingPage";
import VirologySeedVirusProductionPage from "../pages/virology/VirologySeedVirusProductionPage";
import VirologyTrialsPage from "../pages/virology/VirologyTrialsPage";
import InitialProcessingPage from "../pages/InitialProcessingPage";
import AssaysPage from "../pages/AssaysPage";
import ChildSampleCreationPage from "../pages/ChildSampleCreationPage";
import SampleRoutingPage from "../pages/SampleRoutingPage";
import PrepPage from "../pages/PrepPage";
import AnalysisPage from "../pages/AnalysisPage";
import StoragePage from "../pages/StoragePage";
import ResultCompilationPage from "../pages/ResultCompilationPage";
import EndOfProjectArchivingPage from "../pages/EndOfProjectArchivingPage";
import "./NotebookWorkflow.css";

/**
 * Default workflow pages for immunology workflow.
 * Per spec: Reception → Processing → Assays → Child Samples → Prep → Analysis → Storage → Results → Archive
 * Note: Page 4 "Child Samples" includes BOTH child sample creation AND destination routing (per User Story 4)
 */
const DEFAULT_WORKFLOW_PAGES = [
  { id: "default-1", order: 1, title: "Sample Reception" },
  { id: "default-2", order: 2, title: "Initial Processing" },
  { id: "default-3", order: 3, title: "Assays" },
  { id: "default-4", order: 4, title: "Child Samples" },
  { id: "default-5", order: 5, title: "Prep" },
  { id: "default-6", order: 6, title: "Analysis" },
  { id: "default-7", order: 7, title: "Storage" },
  { id: "default-8", order: 8, title: "Results" },
  { id: "default-9", order: 9, title: "Archive" },
  { id: "default-10", order: 10, title: "Reporting & REDCap" },
];

/**
 * NotebookWorkflowTab - Container component for immunology workflow pages.
 * Displays the 10-page workflow with progress indicators and navigation.
 *
 * @param {Object} props
 * @param {number} props.notebookId - The notebook template ID (will auto-create entry if needed)
 * @param {number} props.entryId - The notebook entry ID (direct entry access)
 */
function NotebookWorkflowTab({ notebookId, entryId: propEntryId }) {
  const componentMounted = useRef(false);
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [notebook, setNotebook] = useState(null);
  const [entry, setEntry] = useState(null);
  const [entryId, setEntryId] = useState(propEntryId);
  const [pages, setPages] = useState([]);
  const [pageProgress, setPageProgress] = useState({});
  const [samples, setSamples] = useState([]);
  const [errorMessage, setErrorMessage] = useState(null);
  // Track whether we're creating a new entry vs viewing/editing existing
  // This is determined after checking if an entry exists for the notebook
  const [isCreatingEntry, setIsCreatingEntry] = useState(!propEntryId);

  // Use shared hook for page access control
  // isCreating: true when creating a new entry (bypasses page-level role restrictions)
  // isCreating: false when viewing/editing existing entry (applies role restrictions)
  const { effectivePages, activePage, setActivePage, handlePageChange } =
    usePageAccessControl(pages, DEFAULT_WORKFLOW_PAGES, 0, {
      isCreating: isCreatingEntry,
    });

  useEffect(() => {
    componentMounted.current = true;
    loadNotebookData();

    return () => {
      componentMounted.current = false;
    };
  }, [notebookId, propEntryId]);

  const loadNotebookData = () => {
    if (!notebookId && !propEntryId) {
      setLoading(false);
      return;
    }

    setLoading(true);

    if (propEntryId) {
      // Direct entry access - load entry and its notebook
      loadEntryData(propEntryId);
    } else if (notebookId) {
      // Notebook ID provided - first load notebook, then get/create entry
      loadNotebookAndEntry(notebookId);
    }
  };

  const loadEntryData = (eId) => {
    let loadCount = 0;
    const checkDone = () => {
      loadCount++;
      if (loadCount >= 2) {
        setLoading(false);
      }
    };

    // Load entry and its template notebook
    getFromOpenElisServer(`/rest/notebook-entry/${eId}`, (response) => {
      if (componentMounted.current && response) {
        setEntry(response);
        setEntryId(eId);
        // If entry has a notebook reference, use its pages
        if (response.notebook) {
          setNotebook(response.notebook);
          // Load pages from template notebook
          getFromOpenElisServer(
            `/rest/notebook/view/${response.notebook.id}`,
            (nbResponse) => {
              if (componentMounted.current && nbResponse) {
                setPages(nbResponse.pages || []);
              }
            },
          );
        }
      }
      checkDone();
    });

    // Load samples for entry
    getFromOpenElisServer(`/rest/notebook-entry/${eId}/samples`, (response) => {
      if (componentMounted.current && response) {
        setSamples(response || []);
      }
      checkDone();
    });
  };

  const loadNotebookAndEntry = (nbId) => {
    // First load the notebook data
    getFromOpenElisServer(`/rest/notebook/view/${nbId}`, (nbResponse) => {
      if (componentMounted.current && nbResponse) {
        setNotebook(nbResponse);
        setPages(nbResponse.pages || []);

        // Check if there's an existing entry for this notebook
        getFromOpenElisServer(
          `/rest/notebook-entry/by-notebook/${nbId}`,
          (entriesResponse) => {
            if (componentMounted.current) {
              // Handle both array responses and error responses
              if (
                entriesResponse &&
                Array.isArray(entriesResponse) &&
                entriesResponse.length > 0
              ) {
                // Use the first/most recent entry - this is an EXISTING entry
                // so page-level role restrictions should apply
                const existingEntry = entriesResponse[0];
                setEntry(existingEntry);
                setEntryId(existingEntry.id);
                setIsCreatingEntry(false); // Viewing/editing existing entry

                // Load samples for this entry
                getFromOpenElisServer(
                  `/rest/notebook-entry/${existingEntry.id}/samples`,
                  (samplesResponse) => {
                    if (componentMounted.current) {
                      setSamples(
                        Array.isArray(samplesResponse) ? samplesResponse : [],
                      );
                    }
                    setLoading(false);
                  },
                );
              } else {
                // No entry exists or got error - create one automatically
                // This is a NEW entry, so page-level restrictions should NOT apply
                setIsCreatingEntry(true); // Creating new entry
                createEntryForNotebook(nbId);
              }
            }
          },
        );
      } else {
        console.error("Failed to load notebook:", nbId);
        setLoading(false);
      }
    });
  };

  const createEntryForNotebook = (nbId) => {
    // Create a new entry for this notebook
    fetch(
      `${config.serverBaseUrl}/rest/notebook-entry/create?notebookId=${nbId}`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      },
    )
      .then(async (response) => {
        const text = await response.text();
        let data = {};
        try {
          data = text ? JSON.parse(text) : {};
        } catch (e) {
          console.error("Failed to parse response as JSON:", e);
        }
        if (!response.ok) {
          const errorMsg =
            data.error || `HTTP ${response.status}: ${response.statusText}`;
          console.error("Entry creation failed:", errorMsg);
          throw new Error(errorMsg);
        }
        return data;
      })
      .then((data) => {
        if (componentMounted.current) {
          if (data && data.id) {
            setEntry(data);
            setEntryId(data.id);
            setSamples([]);
            setIsCreatingEntry(false); // Entry created - apply page restrictions
          } else if (data && data.error) {
            console.error("Entry creation error:", data.error);
          } else {
            console.error("Entry creation returned invalid data:", data);
          }
          setLoading(false);
        }
      })
      .catch((error) => {
        console.error("Failed to create notebook entry:", error.message);
        if (componentMounted.current) {
          setErrorMessage(error.message);
          setLoading(false);
        }
      });
  };

  const getProgressForPage = (pageId) => {
    const progress = pageProgress[pageId];
    if (!progress) {
      return { total: 0, completed: 0, percentage: 0 };
    }
    return progress;
  };

  // Callback to refresh progress after page operations
  const handleProgressUpdate = useCallback(() => {
    if (entryId) {
      getFromOpenElisServer(
        `/rest/notebook-entry/${entryId}/samples`,
        (response) => {
          if (componentMounted.current && response) {
            setSamples(response || []);
          }
        },
      );
    }
  }, [entryId]);

  // Determine the workflow type based on notebook title
  const workflowType = useMemo(() => {
    const title = (notebook?.title || entry?.title || "").toLowerCase();
    if (title.includes("virology") || title.includes("vaccine")) {
      return "virology";
    }
    if (title.includes("immunology")) {
      return "immunology";
    }
    // Default to immunology for backwards compatibility
    return "immunology";
  }, [notebook?.title, entry?.title]);

  // Render page-specific content based on page order and workflow type
  // Per spec: Reception → Processing → Assays → Child Samples → Prep → Analysis → Storage → Results → Archive
  const renderPageContent = (page) => {
    const pageOrder = page.order || 1;
    const progress = getProgressForPage(page.id);

    // Virology & Vaccine Unit workflow (2 pages)
    // Page 1: Sample Intake & Registration
    // Page 2: Media Preparation & Quality Control
    if (workflowType === "virology") {
      switch (pageOrder) {
        case 1:
          // Page 1: Sample Intake & Registration
          // Sample arrival (pre-labeled or labeled), metadata entry, test type assignment
          return (
            <VirologySampleReceptionPage
              key={`virology-intake-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 2:
          // Page 2: Media Preparation & Quality Control
          // Ensure full traceability of materials and equipment for media preparation
          // Captures: media type, reagents (supplier, lot, expiry), equipment, QC parameters
          return (
            <VirologyMediaPreparationPage
              key={`virology-media-prep-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 3:
          // Page 3: Cell Culture - Grow host cells
          // Track cell line, passage number, growth conditions (temperature, CO₂, humidity)
          return (
            <VirologyCellCulturePage
              key={`virology-cell-culture-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 4:
          // Page 4: Quality Control - Validate cell viability and sterility
          // Log QC results (viability %, sterility pass/fail)
          return (
            <VirologyQualityControlPage
              key={`virology-quality-control-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 5:
          // Page 5: Virus Culture - Inoculate cells with virus
          // Record virus strain, culture conditions (temp, CO₂, duration)
          return (
            <VirologyVirusCulturePage
              key={`virology-virus-culture-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 6:
          // Page 6: Dark Room Imaging - Imaging or fluorescence analysis
          // Capture image data, CPE observations, fluorescence intensity
          return (
            <VirologyDarkRoomImagingPage
              key={`virology-dark-room-imaging-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 7:
          // Page 7: Formulation - Prepare viral product
          // Document formulation details (stabilizers, preservatives, concentrations)
          return (
            <VirologyFormulationPage
              key={`virology-formulation-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 8:
          // Page 8: Feeding - Culture maintenance and feeding schedule
          // Log feeding schedule and reagents used from inventory with full traceability
          return (
            <VirologyFeedingPage
              key={`virology-feeding-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 9:
          // Page 9: Packaging - Final product packaging
          // Track batch ID, vial type, fill volume, labeling information
          return (
            <VirologyPackagingPage
              key={`virology-packaging-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 10:
          // Page 10: Virus Isolation - Isolate virus from culture
          // Link to culture batch ID, record isolation method and virus strain
          return (
            <VirologyVirusIsolationPage
              key={`virology-virus-isolation-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 11:
          // Page 11: Titer Measurement - Quantify viral load
          // Record titer values (TCID50, PFU/ml, etc.) with assay methods
          return (
            <VirologyTiterMeasurementPage
              key={`virology-titer-measurement-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 12:
          // Page 12: Genome Sequencing - Viral genome analysis
          // Store sequence data (FASTA files, GenBank accession)
          return (
            <VirologyGenomeSequencingPage
              key={`virology-genome-sequencing-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 13:
          // Page 13: Seed Virus Production - Select strain for vaccine
          // Document selection criteria and seed virus batch ID
          return (
            <VirologySeedVirusProductionPage
              key={`virology-seed-virus-production-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        case 14:
          // Page 14: Preclinical & Clinical Trials
          // Preclinical: Animal testing - track trial initiation, species, immunogenicity, safety
          // Clinical: Human testing - link trial phases (I/II/III), outcomes, regulatory submissions
          return (
            <VirologyTrialsPage
              key={`virology-trials-${page.id}`}
              entryId={entryId}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
            />
          );
        default:
          // Fall through to default handling for any unexpected pages
          break;
      }
    }

    // Immunology workflow (default)
    switch (pageOrder) {
      case 1:
        // Page 1: Sample Reception - Use enhanced ImmunologySampleReceptionPage
        // for full reception metadata capture (inspired by Pharma Sample Creation)
        return (
          <ImmunologySampleReceptionPage
            key={`reception-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
          />
        );
      case 2:
        // Page 2: Initial Processing - Volume determination, cell count & isolation,
        // parameter logging, and quality checks (inspired by Pharma QC page)
        return (
          <ImmunologyInitialProcessingPage
            key={`processing-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            templateInstruments={notebook?.analyzers}
          />
        );
      case 3:
        // Page 3: Additional Assays - Perform supplementary tests prior to extraction
        // Includes: cell phenotyping, viability assays, functional assays, contamination checks
        // Documents: test type, operator, reagents (with lot numbers), results, pass/fail, deviations
        return (
          <ImmunologyAdditionalAssaysPage
            key={`assays-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            templateInstruments={notebook?.analyzers}
          />
        );
      case 4:
        // Page 4: Child Samples - Create child samples AND route to destinations
        // Per User Story 4: "Child Sample Creation with Destination Routing"
        // This page combines ImmunologyChildSampleCreationPage AND SampleRoutingPage
        // ImmunologyChildSampleCreationPage includes: extraction from isolated material,
        // unique child sample IDs, parent-child linking, extraction volume, remaining parent volume
        return (
          <React.Fragment key={`child-samples-${page.id}`}>
            <ImmunologyChildSampleCreationPage
              key={`child-creation-${page.id}`}
              entryId={entryId}
              notebookId={notebook?.id}
              pageData={page}
              progress={progress}
              onProgressUpdate={handleProgressUpdate}
              templateInstruments={notebook?.analyzers}
            />
            <div className="routing-section" style={{ marginTop: "2rem" }}>
              <h4 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.workflow.page4.routing"
                  defaultMessage="Destination Routing"
                />
              </h4>
              <SampleRoutingPage
                key={`routing-${page.id}`}
                entryId={entryId}
                notebookId={notebook?.id}
                pageData={page}
                progress={progress}
                onProgressUpdate={handleProgressUpdate}
              />
            </div>
          </React.Fragment>
        );
      case 5:
        // Page 5: Prep - Analysis preparation (fresh, thawed, or incubated)
        return (
          <PrepPage
            key={`prep-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
          />
        );
      case 6:
        // Page 6: Analysis - Import analyzer results from ELISA/Flow Cytometry
        return (
          <AnalysisPage
            key={`analysis-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
          />
        );
      case 7:
        // Page 7: Post-Analysis Handling - Store processed samples under defined conditions,
        // track remaining volume and sample status (analyzed/partially used/exhausted),
        // flag samples with quality issues (insufficient volume, quality issues, unexpected results)
        return (
          <ImmunologyPostAnalysisPage
            key={`post-analysis-${page.id}`}
            entryId={entryId}
            notebookId={notebook?.id}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
          />
        );
      case 8:
        // Page 8: Result Compilation & Dissemination - Compile analysis outputs into structured files,
        // generate reports, flag invalid/inconclusive results (failed controls, instrument errors,
        // borderline values, poor cell viability), determine repeat testing needs,
        // export deliverables (raw data, analyzed data, QC summary, visualizations),
        // and deliver to Data Management Team, sponsors, or project databases (REDCap)
        return (
          <ImmunologyResultCompilationPage
            key={`results-${page.id}`}
            entryId={entryId}
            notebookId={notebook?.id}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
          />
        );
      case 9:
        // Page 9: Archiving - Project conclusion with comprehensive traceability:
        // - Identify all remaining samples (parent and child)
        // - Transfer to Biorepository Laboratory with complete documentation
        // - Ensure traceability (Parent→Child relationships, processing events, analysis events, storage history)
        // - Archive all associated data (raw assay data, analysis files, QC records, protocols)
        return (
          <ImmunologyArchivingPage
            key={`archive-${page.id}`}
            entryId={entryId}
            notebookId={notebook?.id}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
          />
        );
      case 10:
        // Page 10: Data Analysis & Export - Final reporting and data export:
        // - View validation summary statistics
        // - Export comprehensive results to Excel/CSV formats
        // - Record result delivery to recipients
        // - View delivery history
        return (
          <ImmunologyDataAnalysisPage
            key={`dataanalysis-${page.id}`}
            entryId={entryId}
            notebookId={notebook?.id}
            pageData={page}
          />
        );
      default:
        return (
          <div className="page-placeholder">
            <FormattedMessage
              id="notebook.workflow.pageDefault.description"
              defaultMessage="Page content for workflow step {step}"
              values={{ step: pageOrder }}
            />
          </div>
        );
    }
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading workflow..." />
      </div>
    );
  }

  if (!entry && !notebook) {
    return (
      <div
        className="workflow-error"
        style={{ padding: "2rem", color: "#525252" }}
      >
        <FormattedMessage
          id="notebook.workflow.notFound"
          defaultMessage="Notebook not found. Please select a valid notebook."
        />
      </div>
    );
  }

  // Show error if notebook is loaded but entry creation failed
  if (notebook && !entryId) {
    return (
      <div
        className="workflow-error"
        style={{ padding: "2rem", color: "#525252" }}
      >
        <FormattedMessage
          id="notebook.workflow.entryCreationFailed"
          defaultMessage="Failed to create notebook entry. Please refresh and try again."
        />
        {errorMessage && (
          <div
            style={{
              marginTop: "1rem",
              fontSize: "0.875rem",
              color: "#da1e28",
            }}
          >
            Error: {errorMessage}
          </div>
        )}
      </div>
    );
  }

  // Get display title - prefer entry title, fall back to notebook title
  const displayTitle = entry?.title || notebook?.title;
  const displayStatus = entry?.status || notebook?.status;

  return (
    <div className="notebook-workflow-container">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="workflow-header">
            <h2>{displayTitle}</h2>
            <div className="workflow-meta">
              <Tag type="blue">{displayStatus}</Tag>
              <span className="sample-count">
                <FormattedMessage
                  id="notebook.workflow.sampleCount"
                  values={{ count: samples.length }}
                />
              </span>
            </div>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="workflow-content">
        <Column lg={4} md={2} sm={4}>
          <PageNavigation
            pages={effectivePages}
            activePage={activePage}
            onPageChange={handlePageChange}
            pageProgress={pageProgress}
          />
        </Column>

        <Column lg={12} md={6} sm={4}>
          <div className="workflow-page-content">
            {effectivePages.length > 0 &&
              effectivePages[activePage] &&
              effectivePages[activePage].hasAccess && (
                <div className="page-panel">
                  <div className="page-header">
                    <h3>{effectivePages[activePage].title}</h3>
                    <div className="page-progress">
                      {(() => {
                        const progress = getProgressForPage(
                          effectivePages[activePage].id,
                        );
                        return (
                          <span>
                            {progress.completed}/{progress.total}{" "}
                            <FormattedMessage id="notebook.workflow.samplesCompleted" />
                          </span>
                        );
                      })()}
                    </div>
                  </div>

                  <div className="page-content">
                    {effectivePages[activePage].content && (
                      <div
                        className="page-instructions"
                        dangerouslySetInnerHTML={{
                          __html: effectivePages[activePage].content,
                        }}
                      />
                    )}

                    {/* Page-specific content rendered based on page order */}
                    {/* Key forces React to unmount/remount when switching pages to reset state */}
                    <div key={`page-content-${effectivePages[activePage].id}`}>
                      {renderPageContent(effectivePages[activePage])}
                    </div>
                  </div>
                </div>
              )}
            {/* Show access denied message if current page is restricted */}
            {effectivePages.length > 0 &&
              effectivePages[activePage] &&
              !effectivePages[activePage].hasAccess && (
                <div className="page-panel access-denied">
                  <div className="page-header">
                    <h3>{effectivePages[activePage].title}</h3>
                    <Tag type="red">
                      <FormattedMessage
                        id="notebook.page.restricted"
                        defaultMessage="Restricted"
                      />
                    </Tag>
                  </div>
                  <div className="page-content">
                    <p>
                      <FormattedMessage
                        id="notebook.page.accessDeniedMessage"
                        defaultMessage="You do not have the required role to access this page."
                      />
                    </p>
                  </div>
                </div>
              )}
          </div>
        </Column>
      </Grid>
    </div>
  );
}

export default NotebookWorkflowTab;
