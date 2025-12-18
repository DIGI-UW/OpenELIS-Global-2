import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Loading,
  Grid,
  Column,
  Button,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import { usePermissions } from "../../../hooks/usePermissions";
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
];

/**
 * NotebookWorkflowTab - Container component for immunology workflow pages.
 * Displays the 9-page workflow with progress indicators and navigation.
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
  const { hasRoleForCurrentLabUnit } = usePermissions();

  const [loading, setLoading] = useState(true);
  const [notebook, setNotebook] = useState(null);
  const [entry, setEntry] = useState(null);
  const [entryId, setEntryId] = useState(propEntryId);
  const [pages, setPages] = useState([]);
  const [pageProgress, setPageProgress] = useState({});
  const [activePage, setActivePage] = useState(0);
  const [samples, setSamples] = useState([]);
  const [errorMessage, setErrorMessage] = useState(null);

  // Use actual pages if available, otherwise use default workflow pages
  // Filter pages based on user's role access (page-level RBAC)
  const effectivePages = useMemo(() => {
    let pagesToUse = DEFAULT_WORKFLOW_PAGES;
    if (pages && pages.length > 0) {
      pagesToUse = pages;
    }

    // Filter pages based on allowedRoles (page-level access control)
    return pagesToUse.filter((page) => {
      // Get allowedRoles, handling null/undefined and Set/Array types
      const pageRoles = page.allowedRoles
        ? Array.isArray(page.allowedRoles)
          ? page.allowedRoles
          : Array.from(page.allowedRoles)
        : [];

      // No roles defined = no restriction = show page to everyone
      if (pageRoles.length === 0) {
        return true;
      }

      // Check if user has any of the page's required roles
      return hasRoleForCurrentLabUnit(pageRoles);
    });
  }, [pages, hasRoleForCurrentLabUnit]);

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
                // Use the first/most recent entry
                const existingEntry = entriesResponse[0];
                setEntry(existingEntry);
                setEntryId(existingEntry.id);

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
                console.log(
                  "No existing entries found for notebook",
                  nbId,
                  "- creating new entry",
                );
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
    console.log("Creating new notebook entry for notebook:", nbId);
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
        console.log(
          "Entry creation HTTP status:",
          response.status,
          response.statusText,
        );
        const text = await response.text();
        console.log("Entry creation raw response:", text);
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
        console.log("Entry creation response:", data);
        if (componentMounted.current) {
          if (data && data.id) {
            setEntry(data);
            setEntryId(data.id);
            setSamples([]);
            console.log("Entry created successfully with ID:", data.id);
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

  const handlePageChange = (pageIndex) => {
    setActivePage(pageIndex);
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
  // Per spec: Reception → Processing → Assays → Child Samples → Prep → Analysis → Storage → Results → Archive
  const renderPageContent = (page) => {
    const pageOrder = page.order || 1;
    const progress = getProgressForPage(page.id);

    // Debug logging
    console.log("renderPageContent called:", { page, pageOrder, entryId });

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
            {effectivePages.length > 0 && effectivePages[activePage] && (
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
          </div>
        </Column>
      </Grid>
    </div>
  );
}

export default NotebookWorkflowTab;
