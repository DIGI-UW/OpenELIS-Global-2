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
  VirologySampleReceptionPage,
  VirologyMediaPreparationPage,
  VirologyCellCulturePage,
  VirologyQualityControlPage,
  VirologyVirusCulturePage,
  VirologyDarkRoomImagingPage,
  VirologyFormulationPage,
  VirologyFeedingPage,
  VirologyPackagingPage,
  VirologyVirusIsolationPage,
  VirologyTiterMeasurementPage,
  VirologyGenomeSequencingPage,
  VirologySeedVirusProductionPage,
  VirologyTrialsPage,
} from "../pages/virology";
import "./NotebookWorkflow.css";

/**
 * Viral vaccine notebooks in production still use the original 14-page
 * virology workflow. Keep the active workflow tab aligned with that shape so
 * stage access, page rendering, and manifest imports all point at the same
 * notebook pages.
 */
const VIROLOGY_LAB_WORKFLOW_PAGES = [
  { id: "viral-vaccine-1", order: 1, title: "Sample Intake and Registration" },
  { id: "viral-vaccine-2", order: 2, title: "Media Preparation" },
  { id: "viral-vaccine-3", order: 3, title: "Cell Culture" },
  { id: "viral-vaccine-4", order: 4, title: "Quality Control" },
  { id: "viral-vaccine-5", order: 5, title: "Virus Culture" },
  { id: "viral-vaccine-6", order: 6, title: "Dark Room Imaging" },
  { id: "viral-vaccine-7", order: 7, title: "Formulation" },
  { id: "viral-vaccine-8", order: 8, title: "Feeding" },
  { id: "viral-vaccine-9", order: 9, title: "Packaging" },
  { id: "viral-vaccine-10", order: 10, title: "Virus Isolation" },
  { id: "viral-vaccine-11", order: 11, title: "Titer Measurement" },
  { id: "viral-vaccine-12", order: 12, title: "Genome Sequencing" },
  { id: "viral-vaccine-13", order: 13, title: "Seed Virus Production" },
  {
    id: "viral-vaccine-14",
    order: 14,
    title: "Preclinical & Clinical Trials",
  },
];

/**
 * ViralVaccineWorkflowTab - container component for viral_vaccine notebooks.
 * The name is historical; these entries still use the 14-page vaccine flow.
 *
 * @param {Object} props
 * @param {number} props.notebookId - The notebook template ID (will auto-create entry if needed)
 * @param {number} props.entryId - The notebook entry ID (direct entry access)
 */
function ViralVaccineWorkflowTab({ notebookId, entryId: propEntryId }) {
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
    usePageAccessControl(pages, VIROLOGY_LAB_WORKFLOW_PAGES, 0, {
      isCreating: isCreatingEntry,
      workflowType: "viral_vaccine",
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

  // Render page-specific content based on page order.
  // Keep this aligned with the 14-page viral vaccine notebooks already used in
  // production so page data, imports, and stage access stay consistent.
  const renderPageContent = (page) => {
    const pageOrder = page.order || 1;
    const progress = getProgressForPage(page.id);

    switch (pageOrder) {
      case 1:
        return (
          <VirologySampleReceptionPage
            key={`virology-intake-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 2:
        return (
          <VirologyMediaPreparationPage
            key={`virology-media-prep-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
            templateInstruments={notebook?.analyzers}
          />
        );
      case 3:
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
        return (
          <VirologyFeedingPage
            key={`virology-feeding-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 9:
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
        return null;
    }
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading
          withOverlay={false}
          description="Loading Viral Vaccine Workflow..."
        />
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

export default ViralVaccineWorkflowTab;
