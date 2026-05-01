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
import config from "../../../config.json";
import { NotificationContext } from "../../layout/Layout";
import PageNavigation from "./PageNavigation";
import {
  PathologySampleCreationPage,
  PathologyQualityControlPage,
  PathologySampleProcessingPage,
  PathologyGrossExaminationPage,
  PathologyCassettesPage,
  PathologyBlocksPage,
  PathologySlidesPage,
  PathologyStainingPage,
  PathologyTestingMicroscopyPage,
  PathologyStorageInventoryPage,
  PathologyReportingPage,
  PathologyDisposalArchivingPage,
} from "../pages/pathology";
import "./NotebookWorkflow.css";

/**
 * Default workflow pages for Pathology workflow.
 * Page 1: Sample Creation and Metadata Capture
 * Page 2: Sample Quality Control
 * Page 3: Gross Examination
 * Page 4: Cassette Setup
 * Page 5: Sample Processing
 * Page 6: Block Creation
 * Page 7: Slide Preparation
 * Page 8: Slide Staining
 * Page 9: Microscopy and Diagnosis
 * Page 10: Individual Patient Report Preview and Print
 * Page 11: Storage and Inventory Management
 * Page 12: Reporting and Performance Monitoring
 * Page 13: Disposal and Archiving
 */
const DEFAULT_PATHOLOGY_WORKFLOW_PAGES = [
  {
    id: "default-1",
    order: 1,
    title: "Sample Creation and Metadata Capture",
  },
  { id: "default-2", order: 2, title: "Sample Quality Control" },
  { id: "default-3", order: 3, title: "Gross Examination" },
  { id: "default-4", order: 4, title: "Cassette Setup" },
  { id: "default-5", order: 5, title: "Sample Processing" },
  { id: "default-6", order: 6, title: "Block Creation" },
  { id: "default-7", order: 7, title: "Slide Preparation" },
  { id: "default-8", order: 8, title: "Slide Staining" },
  { id: "default-9", order: 9, title: "Microscopy and Diagnosis" },
  {
    id: "default-10",
    order: 10,
    title: "Individual Patient Report Preview and Print",
  },
  {
    id: "default-11",
    order: 11,
    title: "Storage and Inventory Management",
  },
  {
    id: "default-12",
    order: 12,
    title: "Reporting and Performance Monitoring",
  },
  { id: "default-13", order: 13, title: "Disposal and Archiving" },
];

/**
 * PathologyWorkflowTab - Container component for Pathology Laboratory workflow pages.
 * Displays the Pathology-specific workflow with progress indicators and navigation.
 *
 * @param {Object} props
 * @param {number} props.notebookId - The notebook template ID (will auto-create entry if needed)
 * @param {number} props.entryId - The notebook entry ID (direct entry access)
 */
function PathologyWorkflowTab({ notebookId, entryId: propEntryId }) {
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
  const [activePage, setActivePage] = useState(0);
  const [samples, setSamples] = useState([]);
  const [errorMessage, setErrorMessage] = useState(null);

  // Use actual pages if available, otherwise use default Pathology workflow pages.
  const effectivePages = useMemo(() => {
    const canonicalTitleByOrder = {
      1: "Sample Creation and Metadata Capture",
      2: "Sample Quality Control",
      3: "Gross Examination",
      4: "Cassette Setup",
      5: "Sample Processing",
      6: "Block Creation",
      7: "Slide Preparation",
      8: "Slide Staining",
      9: "Microscopy and Diagnosis",
      10: "Individual Patient Report Preview and Print",
      11: "Storage and Inventory Management",
      12: "Reporting and Performance Monitoring",
      13: "Disposal and Archiving",
    };

    const normalizeTitles = (sourcePages) =>
      sourcePages.map((page) => {
        const pageOrder = page.pageOrder ?? page.order;
        const canonicalTitle = canonicalTitleByOrder[pageOrder];
        return canonicalTitle ? { ...page, title: canonicalTitle } : page;
      });

    if (pages && pages.length > 0) {
      const sortedPages = [...pages].sort((a, b) => {
        const orderA = a.pageOrder ?? a.order ?? 0;
        const orderB = b.pageOrder ?? b.order ?? 0;
        return orderA - orderB;
      });
      return normalizeTitles(sortedPages);
    }
    return normalizeTitles(DEFAULT_PATHOLOGY_WORKFLOW_PAGES);
  }, [pages]);

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
      loadEntryData(propEntryId);
    } else if (notebookId) {
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

    getFromOpenElisServer(`/rest/notebook-entry/${eId}`, (response) => {
      if (componentMounted.current && response) {
        setEntry(response);
        setEntryId(eId);
        if (response.notebook) {
          setNotebook(response.notebook);
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

    getFromOpenElisServer(`/rest/notebook-entry/${eId}/samples`, (response) => {
      if (componentMounted.current && response) {
        setSamples(response || []);
      }
      checkDone();
    });
  };

  const loadNotebookAndEntry = (nbId) => {
    getFromOpenElisServer(`/rest/notebook/view/${nbId}`, (nbResponse) => {
      if (componentMounted.current && nbResponse) {
        setNotebook(nbResponse);
        setPages(nbResponse.pages || []);

        getFromOpenElisServer(
          `/rest/notebook-entry/by-notebook/${nbId}`,
          (entriesResponse) => {
            if (componentMounted.current) {
              if (
                entriesResponse &&
                Array.isArray(entriesResponse) &&
                entriesResponse.length > 0
              ) {
                const existingEntry = entriesResponse[0];
                setEntry(existingEntry);
                setEntryId(existingEntry.id);

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
                createEntryForNotebook(nbId);
              }
            }
          },
        );
      } else {
        setLoading(false);
      }
    });
  };

  const createEntryForNotebook = (nbId) => {
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
          } else if (data && data.error) {
            console.error("Entry creation error:", data.error);
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

  // Render Pathology page-specific content based on page order
  const renderPageContent = (page, pageIndex) => {
    const pageOrder = page.order ?? 1;
    const progress = getProgressForPage(page.id);
    const previousPage = pageIndex > 0 ? effectivePages[pageIndex - 1] : null;

    switch (pageOrder) {
      case 1:
        // Page 1: Sample Creation & Full Metadata Capture
        return (
          <PathologySampleCreationPage
            key={`creation-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 2:
        // Page 2: Sample Quality Control
        return (
          <PathologyQualityControlPage
            key={`qc-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 3:
        // Page 3: Gross Examination
        return (
          <PathologyGrossExaminationPage
            key={`gross-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 4:
        // Page 4: Cassette Setup
        return (
          <PathologyCassettesPage
            key={`cassettes-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 5:
        // Page 5: Sample Processing
        return (
          <PathologySampleProcessingPage
            key={`processing-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 6:
        // Page 6: Block Creation
        return (
          <PathologyBlocksPage
            key={`blocks-${page.id}`}
            entryId={entryId}
            pageData={page}
            previousPageId={previousPage?.id}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 7:
        // Page 7: Slide Preparation
        return (
          <PathologySlidesPage
            key={`slides-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 8:
        // Page 8: Slide Staining
        return (
          <PathologyStainingPage
            key={`staining-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 9:
        // Page 9: Microscopy and Diagnosis
        return (
          <PathologyTestingMicroscopyPage
            key={`microscopy-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 10:
        // Page 10: Individual Patient Report Preview and Print
        return (
          <PathologyReportingPage
            key={`individual-report-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 11:
        // Page 11: Storage and Inventory Management
        return (
          <PathologyStorageInventoryPage
            key={`storage-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 12:
        // Page 12: Reporting and Performance Monitoring
        return (
          <PathologyReportingPage
            key={`reporting-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 13:
        // Page 13: Disposal and Archiving
        return (
          <PathologyDisposalArchivingPage
            key={`disposal-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
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
        <Loading
          withOverlay={false}
          description="Loading Pathology workflow..."
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
        <FormattedMessage
          id="notebook.workflow.notFound"
          defaultMessage="Notebook not found. Please select a valid notebook."
        />
      </div>
    );
  }

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

  const displayTitle = entry?.title || notebook?.title;
  const displayStatus = entry?.status || notebook?.status;

  return (
    <div className="notebook-workflow-container pathology-workflow">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="workflow-header">
            <h2>{displayTitle}</h2>
            <div className="workflow-meta">
              <Tag type="teal">Pathology</Tag>
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
                  {effectivePages[activePage].instructions && (
                    <div className="page-instructions">
                      {effectivePages[activePage].instructions}
                    </div>
                  )}

                  <div key={`page-content-${effectivePages[activePage].id}`}>
                    {renderPageContent(effectivePages[activePage], activePage)}
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

export default PathologyWorkflowTab;
