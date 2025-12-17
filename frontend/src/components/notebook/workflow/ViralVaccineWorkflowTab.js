import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Loading,
  Grid,
  Column,
  Tag,
  Button,
  InlineNotification,
} from "@carbon/react";
import { Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";
import { NotificationContext } from "../../layout/Layout";
import PageNavigation from "./PageNavigation";
import {
  ViralVaccineSampleRegistrationPage,
  ViralVaccineMediaPreparationPage,
  ViralVaccineGenericPage,
} from "../pages/viral-vaccine";
import "./NotebookWorkflow.css";

/**
 * Default workflow pages for Viral & Vaccine Unit workflow.
 * Pages 1-13 covering the complete viral and vaccine development workflow
 */
const DEFAULT_VIRAL_VACCINE_WORKFLOW_PAGES = [
  { id: "default-1", order: 1, title: "Sample Creation & Registration" },
  { id: "default-2", order: 2, title: "Viral Media Preparation" },
  { id: "default-3", order: 3, title: "Sterilization" },
  { id: "default-4", order: 4, title: "Cell Culture (Host Cell Preparation)" },
  { id: "default-5", order: 5, title: "Cell Culture Quality Control" },
  { id: "default-6", order: 6, title: "Virus Culture Inoculation" },
  { id: "default-7", order: 7, title: "Imaging / Analysis" },
  { id: "default-8", order: 8, title: "Culture Feeding & Maintenance" },
  { id: "default-9", order: 9, title: "Formulation & Packaging" },
  { id: "default-10", order: 10, title: "Virus Isolation & Titer Measurement" },
  { id: "default-11", order: 11, title: "Genome Sequencing" },
  { id: "default-12", order: 12, title: "Seed Virus Production" },
  { id: "default-13", order: 13, title: "Preclinical & Clinical Trial Reference" },
];

/**
 * ViralVaccineWorkflowTab - Container component for Viral & Vaccine Unit workflow pages.
 * Displays the Viral & Vaccine-specific workflow with progress indicators and navigation.
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
  const [activePage, setActivePage] = useState(0);
  const [samples, setSamples] = useState([]);
  const [errorMessage, setErrorMessage] = useState(null);
  const [syncing, setSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState(null);

  // Use actual pages if available, otherwise use default workflow pages
  const effectivePages = useMemo(() => {
    if (pages && pages.length > 0) {
      return pages;
    }
    return DEFAULT_VIRAL_VACCINE_WORKFLOW_PAGES;
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

  // Sync pages from template to instance (adds missing pages)
  const handleSyncPages = useCallback(() => {
    if (!entryId) return;

    setSyncing(true);
    setSyncMessage(null);

    fetch(`${config.serverBaseUrl}/rest/notebook-entry/${entryId}/sync-pages`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
    })
      .then((response) => response.json())
      .then((data) => {
        if (componentMounted.current) {
          if (data.success) {
            if (data.pagesAdded > 0) {
              setSyncMessage({
                kind: "success",
                text: intl.formatMessage(
                  {
                    id: "notebook.workflow.syncSuccess",
                    defaultMessage:
                      "{count} new page(s) added from template. Refreshing...",
                  },
                  { count: data.pagesAdded },
                ),
              });
              // Reload the notebook data to get new pages
              setTimeout(() => {
                loadNotebookData();
                setSyncMessage(null);
              }, 1500);
            } else {
              setSyncMessage({
                kind: "info",
                text: intl.formatMessage({
                  id: "notebook.workflow.syncNoChanges",
                  defaultMessage:
                    "All pages are already in sync with the template.",
                }),
              });
              setTimeout(() => setSyncMessage(null), 3000);
            }
          } else {
            setSyncMessage({
              kind: "error",
              text: data.error || "Sync failed",
            });
          }
        }
      })
      .catch((error) => {
        console.error("Sync pages error:", error);
        if (componentMounted.current) {
          setSyncMessage({
            kind: "error",
            text: error.message || "Failed to sync pages",
          });
        }
      })
      .finally(() => {
        if (componentMounted.current) {
          setSyncing(false);
        }
      });
  }, [entryId, intl]);

  // Render page-specific content based on page order
  const renderPageContent = (page) => {
    const pageOrder = page.order || 1;
    const progress = getProgressForPage(page.id);

    switch (pageOrder) {
      case 1:
        // Page 1: Sample Creation & Registration
        return (
          <ViralVaccineSampleRegistrationPage
            key={`registration-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 2:
        // Page 2: Viral Media Preparation
        return (
          <ViralVaccineMediaPreparationPage
            key={`media-prep-${page.id}`}
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={handleProgressUpdate}
            notebookId={notebook?.id}
          />
        );
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
        // Pages 3-13: Use generic page component
        return (
          <ViralVaccineGenericPage
            key={`generic-${page.id}-${pageOrder}`}
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
          description="Loading Viral & Vaccine workflow..."
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
    <div className="notebook-workflow-container viral-vaccine-workflow">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="workflow-header">
            <h2>{displayTitle}</h2>
            <div className="workflow-meta">
              <Tag type="teal">Viral &amp; Vaccine</Tag>
              <Tag type="blue">{displayStatus}</Tag>
              <span className="sample-count">
                <FormattedMessage
                  id="notebook.workflow.sampleCount"
                  values={{ count: samples.length }}
                />
              </span>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Renew}
                onClick={handleSyncPages}
                disabled={syncing || !entryId}
                style={{ marginLeft: "1rem" }}
              >
                {syncing ? (
                  <FormattedMessage
                    id="notebook.workflow.syncing"
                    defaultMessage="Syncing..."
                  />
                ) : (
                  <FormattedMessage
                    id="notebook.workflow.syncPages"
                    defaultMessage="Sync Pages"
                  />
                )}
              </Button>
            </div>
          </div>
          {syncMessage && (
            <InlineNotification
              kind={syncMessage.kind}
              title=""
              subtitle={syncMessage.text}
              lowContrast
              onCloseButtonClick={() => setSyncMessage(null)}
              style={{ marginTop: "0.5rem" }}
            />
          )}
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

export default ViralVaccineWorkflowTab;
