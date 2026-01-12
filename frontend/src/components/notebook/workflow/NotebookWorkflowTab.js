import {
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
import "./NotebookWorkflow.css";

/**
 * Default workflow type when notebook doesn't specify one.
 * Falls back to immunology for backwards compatibility.
 */
const DEFAULT_WORKFLOW_TYPE = "immunology";

/**
 * NotebookWorkflowTab - Container component for notebook workflow pages.
 * Dynamically renders workflow pages based on the notebook's workflow type
 * using the page registry system.
 *
 * @param {Object} props
 * @param {number} props.notebookId - The notebook template ID (will auto-create entry if needed)
 * @param {number} props.entryId - The notebook entry ID (direct entry access)
 * @param {string} props.workflowType - Override workflow type (optional, defaults to notebook's type or 'immunology')
 */
function NotebookWorkflowTab({
  notebookId,
  entryId: propEntryId,
  workflowType: propWorkflowType,
}) {
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
  const [workflowType, setWorkflowType] = useState(
    propWorkflowType || DEFAULT_WORKFLOW_TYPE,
  );

  /**
   * Determine effective workflow type from props, notebook, or default.
   * Used for fallback to workflow definitions when notebook has no pages.
   */
  const effectiveWorkflowType = useMemo(() => {
    if (propWorkflowType) return propWorkflowType;

    // Check for typeName field (string like "Medical Lab", "Immunology")
    if (notebook?.typeName) {
      const typeMap = {
        IMMUNOLOGY: "immunology",
        MEDLAB: "medlab",
        "MEDICAL LAB": "medlab",
        HEMATOLOGY: "hematology",
        CHEMISTRY: "chemistry",
      };
      const normalizedType = notebook.typeName.toUpperCase();
      const result = typeMap[normalizedType];
      if (result) return result;
    }

    return workflowType || DEFAULT_WORKFLOW_TYPE;
  }, [propWorkflowType, notebook, workflowType]);

  /**
   * Build effective pages from notebook pages.
   * Pages come from the database (notebook_page table).
   */
  const effectivePages = useMemo(() => {
    if (pages && pages.length > 0) {
      const enhancedPages = pages.map((page, index) => ({
        ...page,
        order: page.pageOrder ?? page.order ?? index + 1,
      }));
      // Sort by order to maintain correct page sequence
      return enhancedPages.sort((a, b) => (a.order || 0) - (b.order || 0));
    }

    // Return empty array if no pages - this is a fallback workflow tab
    return [];
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
                // Update workflow type from notebook
                if (nbResponse.type && typeof nbResponse.type === "string") {
                  setWorkflowType(nbResponse.type.toLowerCase());
                }
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
        // Update workflow type from notebook
        if (nbResponse.type && typeof nbResponse.type === "string") {
          setWorkflowType(nbResponse.type.toLowerCase());
        }

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

  /**
   * Render page content.
   * This is a fallback for workflows without dedicated workflow tabs.
   * For dedicated workflow tabs (MedLab, Pharma, MNTD, TB), see their respective files.
   */
  const renderPageContent = (page) => {
    const pageOrder = page.pageOrder ?? page.order ?? 1;

    // Generic placeholder for workflows without dedicated tabs
    return (
      <div className="page-placeholder">
        <FormattedMessage
          id="notebook.workflow.pageDefault.description"
          defaultMessage="Page content for workflow step {step}"
          values={{ step: pageOrder }}
        />
        <p
          style={{ marginTop: "1rem", color: "#6f6f6f", fontSize: "0.875rem" }}
        >
          <FormattedMessage
            id="notebook.workflow.pageDefault.hint"
            defaultMessage="This workflow type does not have a dedicated workflow tab yet."
          />
        </p>
      </div>
    );
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
                  {effectivePages[activePage].instructions && (
                    <div
                      className="page-instructions"
                      dangerouslySetInnerHTML={{
                        __html: effectivePages[activePage].instructions,
                      }}
                    />
                  )}

                  {/* Page-specific content rendered via registry */}
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
