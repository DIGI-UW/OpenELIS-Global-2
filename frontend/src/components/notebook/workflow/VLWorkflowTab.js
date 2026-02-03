import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
} from "react";
import { Loading, Grid, Column, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { usePageAccessControl } from "../../../hooks/usePageAccessControl";
import config from "../../../config.json";
import { NotificationContext } from "../../layout/Layout";
import PageNavigation from "./PageNavigation";
import {
  VLSampleReceptionPage,
  VLDNARNAExtractionPage,
  VLQualityQuantityAssessmentPage,
  VLPCRAmplificationPage,
  VLGelElectrophoresesPage,
  VLLibraryPreparationPage,
  VLBioanalyzerQCPage,
  VLSequencingPage,
  VLBioinformaticsAnalysisPage,
  VLStorageEnvironmentalMonitoringPage,
} from "../pages/vl";
import "./NotebookWorkflow.css";

/**
 * VL (Genomics & Bioinformatics) Laboratory workflow pages.
 * Per spec: Sample Reception → DNA/RNA Extraction → Quality & Quantity Assessment → PCR Amplification →
 * Gel Electrophoresis → Library Preparation → Bioanalyzer QC → Sequencing → Bioinformatics Analysis →
 * Storage & Environmental Monitoring
 */
const VL_WORKFLOW_PAGES = [
  { id: "gbd-1", order: 1, title: "Sample Intake & Registration" },
  { id: "gbd-2", order: 2, title: "DNA/RNA Extraction" },
  { id: "gbd-3", order: 3, title: "Quality & Quantity Assessment" },
  { id: "gbd-4", order: 4, title: "PCR Amplification" },
  { id: "gbd-5", order: 5, title: "Gel Electrophoresis" },
  { id: "gbd-6", order: 6, title: "Library Preparation" },
  { id: "gbd-7", order: 7, title: "Bioanalyzer QC" },
  { id: "gbd-8", order: 8, title: "Sequencing" },
  { id: "gbd-9", order: 9, title: "Bioinformatics Analysis & Data Submission" },
  { id: "gbd-10", order: 10, title: "Storage & Environmental Monitoring" },
];

/**
 * VLWorkflowTab - Container component for VL workflow pages.
 * Displays the 9-page workflow with progress indicators and navigation.
 *
 * Supports:
 * - Sample reception and registration
 * - DNA/RNA extraction and quality assessment
 * - PCR amplification and gel electrophoresis analysis
 * - Library preparation and bioanalyzer QC
 * - Sequencing and bioinformatics analysis
 * - Role-based access control (5 VL laboratory roles)
 *
 * @param {Object} props
 * @param {number} props.notebookId - The notebook template ID (will auto-create entry if needed)
 * @param {number} props.entryId - The notebook entry ID (direct entry access)
 */
function VLWorkflowTab({ notebookId, entryId: propEntryId }) {
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
    usePageAccessControl(pages, VL_WORKFLOW_PAGES, 0, {
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

  // Render page-specific content based on page order
  // Per spec: Reception → Extraction → QA → PCR → Gel → Library Prep → Bioanalyzer → Sequencing → Bioinformatics
  const renderPageContent = (page) => {
    const pageOrder = page.order || 1;
    const progress = getProgressForPage(page.id);

    switch (pageOrder) {
      case 1:
        // Page 1: Sample Intake & Registration
        return (
          <VLSampleReceptionPage
            key={`gbd-reception-${page.id}`}
            samples={samples}
            pageData={page}
            entryId={entryId}
            onSampleUpdate={handleProgressUpdate}
            onSampleStatusChange={() => handleProgressUpdate()}
            isLoading={false}
          />
        );
      case 2:
        // Page 2: DNA/RNA Extraction
        return (
          <VLDNARNAExtractionPage
            key={`gbd-extraction-${page.id}`}
            samples={samples}
            pageData={page}
            onSampleUpdate={handleProgressUpdate}
            onSampleStatusChange={() => handleProgressUpdate()}
            isLoading={false}
          />
        );
      case 3:
        // Page 3: Quality & Quantity Assessment
        return (
          <VLQualityQuantityAssessmentPage
            key={`gbd-qa-${page.id}`}
            samples={samples}
            pageData={page}
            onSampleUpdate={handleProgressUpdate}
            onSampleStatusChange={() => handleProgressUpdate()}
            isLoading={false}
          />
        );
      case 4:
        // Page 4: PCR Amplification
        return (
          <VLPCRAmplificationPage
            key={`gbd-pcr-${page.id}`}
            samples={samples}
            pageData={page}
            onSampleUpdate={handleProgressUpdate}
            onSampleStatusChange={() => handleProgressUpdate()}
            isLoading={false}
          />
        );
      case 5:
        // Page 5: Gel Electrophoresis
        return (
          <VLGelElectrophoresesPage
            key={`gbd-gel-${page.id}`}
            samples={samples}
            pageData={page}
            onSampleUpdate={handleProgressUpdate}
            onSampleStatusChange={() => handleProgressUpdate()}
            isLoading={false}
          />
        );
      case 6:
        // Page 6: Library Preparation
        return (
          <VLLibraryPreparationPage
            key={`gbd-libprep-${page.id}`}
            samples={samples}
            pageData={page}
            onSampleUpdate={handleProgressUpdate}
            onSampleStatusChange={() => handleProgressUpdate()}
            isLoading={false}
          />
        );
      case 7:
        // Page 7: Bioanalyzer QC
        return (
          <VLBioanalyzerQCPage
            key={`gbd-bioanalyzer-${page.id}`}
            samples={samples}
            pageData={page}
            onSampleStatusChange={() => handleProgressUpdate()}
          />
        );
      case 8:
        // Page 8: Sequencing
        return (
          <VLSequencingPage
            key={`gbd-sequencing-${page.id}`}
            samples={samples}
            pageData={page}
            onSampleStatusChange={() => handleProgressUpdate()}
          />
        );
      case 9:
        // Page 9: Bioinformatics Analysis & Data Submission
        return (
          <VLBioinformaticsAnalysisPage
            key={`gbd-bioinformatics-${page.id}`}
            samples={samples}
            pageData={page}
            onSampleStatusChange={() => handleProgressUpdate()}
          />
        );
      case 10:
        // Page 10: Storage & Environmental Monitoring
        return (
          <VLStorageEnvironmentalMonitoringPage
            key={`gbd-storage-${page.id}`}
            samples={samples}
            pageData={page}
            entryId={entryId}
            notebookId={notebook?.id}
            onProgressUpdate={handleProgressUpdate}
            onSampleStatusChange={() => handleProgressUpdate()}
            isLoading={false}
          />
        );
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading VL Workflow..." />
      </div>
    );
  }

  if (!entry && !notebook) {
    return (
      <div
        className="workflow-error"
        style={{ padding: "2rem", color: "#525252" }}
      >
        Notebook not found. Please select a valid notebook.
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
        Failed to create notebook entry. Please refresh and try again.
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
              <span className="sample-count">{samples.length} samples</span>
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
                    <Tag type="red">Restricted</Tag>
                  </div>
                  <div className="page-content">
                    <p>
                      You do not have the required role to access this page.
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

export default VLWorkflowTab;
