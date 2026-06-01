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
import { usePageAccessControl } from "../../../hooks/usePageAccessControl";
import { normalizeWorkflowType } from "../../../constants/ahriWorkflowRegistry";
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

const PATHOLOGY_WORKFLOW_STAGE_MAP = {
  histopathology_biopsy_tissue: new Set([
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
  ]),
  peripheral_smear_bone_marrow_morphology: new Set([
    1, 2, 7, 8, 9, 10, 11, 12, 13,
  ]),
  fnac: new Set([1, 2, 7, 8, 9, 10, 11, 12, 13]),
  cytology_liquid_based_pap_smear: new Set([1, 2, 5, 7, 8, 9, 10, 11, 12, 13]),
};

const CANONICAL_PATHOLOGY_ORDER_BY_TITLE = {
  "sample creation and full metadata capture": 1,
  "sample creation and metadata capture": 1,
  "sample creation metadata capture": 1,
  "sample quality control": 2,
  "gross examination": 3,
  "cassette setup": 4,
  "sample processing": 5,
  "block creation": 6,
  "slide preparation": 7,
  "slide staining": 8,
  "microscopy and diagnosis": 9,
  "microscopy diagnosis": 9,
  "microscopy and diagnosis and reporting": 9,
  "individual patient report preview and print": 10,
  "storage and inventory management": 11,
  "storage inventory management": 11,
  "reporting and performance monitoring": 12,
  "disposal and archiving": 13,
};

const CANONICAL_PATHOLOGY_TITLE_BY_ORDER = {
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

const normalizePathologyStageTitle = (title) =>
  String(title || "")
    .toLowerCase()
    .replaceAll("&", " and ")
    .replaceAll(/[^a-z0-9]+/g, " ")
    .trim();

const getCanonicalPathologyStageOrder = (page) => {
  const normalizedTitle = normalizePathologyStageTitle(page?.title);
  return (
    CANONICAL_PATHOLOGY_ORDER_BY_TITLE[normalizedTitle] ??
    page?.pageOrder ??
    page?.order ??
    0
  );
};

const getCanonicalPathologyStageTitle = (page) =>
  CANONICAL_PATHOLOGY_TITLE_BY_ORDER[getCanonicalPathologyStageOrder(page)] ||
  page?.title;

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
 * @param {string} [props.draftWorkflowType] - Unsaved workflow type from the parent edit form (must win over last-fetched notebook so the tab matches the dropdown)
 */
function PathologyWorkflowTab({
  notebookId,
  entryId: propEntryId,
  draftWorkflowType,
}) {
  const componentMounted = useRef(false);
  /** Last explicit stage (canonical order 1–13) so we can remap active index when workflow_type changes. */
  const preservedStageOrderRef = useRef(1);
  const prevWorkflowTypeForRemapRef = useRef(null);
  const prevNotebookIdForResetRef = useRef(notebookId);
  const [pendingEditIntent, setPendingEditIntent] = useState(null);
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [notebook, setNotebook] = useState(null);
  const [entry, setEntry] = useState(null);
  const [entryId, setEntryId] = useState(propEntryId);
  const [pages, setPages] = useState([]);
  const [pageProgress, setPageProgress] = useState({});
  const [isCreatingEntry, setIsCreatingEntry] = useState(!propEntryId);
  const [samples, setSamples] = useState([]);
  const [errorMessage, setErrorMessage] = useState(null);

  const workflowType = useMemo(() => {
    const draft =
      typeof draftWorkflowType === "string" && draftWorkflowType.trim() !== ""
        ? draftWorkflowType.trim()
        : null;
    const rawType =
      draft ||
      entry?.notebook?.workflowType ||
      notebook?.workflowType ||
      entry?.workflowType ||
      "";
    const normalized = String(rawType).trim().toLowerCase();

    if (!normalized) {
      return "histopathology_biopsy_tissue";
    }

    if (PATHOLOGY_WORKFLOW_STAGE_MAP[normalized]) {
      return normalized;
    }

    switch (normalized) {
      case "histopathology":
      case "biopsy":
      case "histopathology/biopsy":
      case "histopathology_biopsy":
      case "pathology":
        return "histopathology_biopsy_tissue";
      case "peripheral_smear":
      case "bone_marrow":
      case "peripheral_smear_bone_marrow":
        return "peripheral_smear_bone_marrow_morphology";
      case "cytology":
      case "liquid_based_pap_smear":
      case "pap_smear":
        return "cytology_liquid_based_pap_smear";
      default:
        return "histopathology_biopsy_tissue";
    }
  }, [
    draftWorkflowType,
    entry?.notebook?.workflowType,
    notebook?.workflowType,
    entry?.workflowType,
  ]);

  const pathologyStagePages = useMemo(() => {
    const enabledStages =
      PATHOLOGY_WORKFLOW_STAGE_MAP[workflowType] ||
      PATHOLOGY_WORKFLOW_STAGE_MAP.histopathology_biopsy_tissue;
    const sourcePages =
      pages && pages.length > 0 ? pages : DEFAULT_PATHOLOGY_WORKFLOW_PAGES;

    return [...sourcePages]
      .map((page) => {
        const workflowStageOrder = getCanonicalPathologyStageOrder(page);
        return {
          ...page,
          title: getCanonicalPathologyStageTitle(page),
          workflowStageOrder,
          order: workflowStageOrder,
          pageOrder: workflowStageOrder,
        };
      })
      .filter((page) => enabledStages.has(page.workflowStageOrder))
      .sort((a, b) => a.workflowStageOrder - b.workflowStageOrder);
  }, [pages, workflowType]);

  const registryWorkflowType = useMemo(
    () => normalizeWorkflowType(workflowType) || "pathology",
    [workflowType],
  );

  const {
    effectivePages,
    activePage,
    setActivePage,
    handlePageChange: navigateAccessiblePage,
  } = usePageAccessControl(pathologyStagePages, DEFAULT_PATHOLOGY_WORKFLOW_PAGES, 0, {
    isCreating: isCreatingEntry,
    workflowType: registryWorkflowType,
  });

  /** Comma-separated real DB page IDs (excludes default-* placeholders) for progress API. */
  const pathologyRealPageIdsKey = useMemo(
    () =>
      effectivePages
        .map((p) => p.id)
        .filter((id) => id != null && !String(id).startsWith("default-"))
        .join(","),
    [effectivePages],
  );

  const loadPathologyPageProgressForIds = useCallback((pageIds) => {
    if (!pageIds || pageIds.length === 0) {
      return;
    }
    const next = {};
    let finished = 0;
    const total = pageIds.length;
    pageIds.forEach((pageId) => {
      getFromOpenElisServer(
        `/rest/notebook/bulk/page/${pageId}/progress`,
        (resp) => {
          if (
            componentMounted.current &&
            resp &&
            typeof resp.total === "number"
          ) {
            next[pageId] = {
              total: resp.total,
              completed: resp.completed,
              pending: resp.pending ?? 0,
              inProgress: resp.inProgress ?? 0,
              percentage: resp.percentage ?? 0,
            };
          }
          finished += 1;
          if (finished === total) {
            setPageProgress((prev) => ({ ...prev, ...next }));
          }
        },
      );
    });
  }, []);

  useEffect(() => {
    if (!entryId || !pathologyRealPageIdsKey) {
      return;
    }
    const ids = pathologyRealPageIdsKey.split(",").filter(Boolean);
    if (ids.length === 0) {
      return;
    }
    loadPathologyPageProgressForIds(ids);
  }, [entryId, pathologyRealPageIdsKey, loadPathologyPageProgressForIds]);

  useEffect(() => {
    if (prevNotebookIdForResetRef.current !== notebookId) {
      prevNotebookIdForResetRef.current = notebookId;
      prevWorkflowTypeForRemapRef.current = null;
      preservedStageOrderRef.current = 1;
      setActivePage(0);
    }
  }, [notebookId]);

  useEffect(() => {
    if (activePage >= effectivePages.length && effectivePages.length > 0) {
      setActivePage(0);
    }
  }, [activePage, effectivePages]);

  /**
   * When pathology workflow_type changes, effectivePages is filtered/reordered but
   * activePage is still an array index — the same index can point at the wrong stage
   * (e.g. histo index 6 = Slide Prep, FNAC index 6 = Microscopy). That breaks
   * Slide Preparation → Slide Staining (wrong pageId on save / mark complete).
   * Remap by canonical workflowStageOrder instead.
   */
  useEffect(() => {
    if (effectivePages.length === 0) {
      return;
    }
    if (prevWorkflowTypeForRemapRef.current === null) {
      prevWorkflowTypeForRemapRef.current = workflowType;
      const p = effectivePages[activePage];
      if (p?.workflowStageOrder != null) {
        preservedStageOrderRef.current = p.workflowStageOrder;
      }
      return;
    }
    if (prevWorkflowTypeForRemapRef.current !== workflowType) {
      const orderToKeep = preservedStageOrderRef.current;
      prevWorkflowTypeForRemapRef.current = workflowType;
      setPendingEditIntent(null);
      const idx = effectivePages.findIndex(
        (p) => p.workflowStageOrder === orderToKeep,
      );
      setActivePage(idx >= 0 ? idx : 0);
    }
  }, [workflowType, effectivePages, setActivePage]);

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
                setIsCreatingEntry(false);

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
            setIsCreatingEntry(false);
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
    const p = effectivePages[pageIndex];
    if (p?.workflowStageOrder != null) {
      preservedStageOrderRef.current = p.workflowStageOrder;
    }
    navigateAccessiblePage(pageIndex);
  };

  const navigateToWorkflowStage = useCallback(
    (stageOrder, options = {}) => {
      if (options?.openEdit && options?.patientKey) {
        setPendingEditIntent({
          patientKey: options.patientKey,
          sampleIds: options.sampleIds || [],
          openEdit: true,
        });
      }
      const targetIndex = effectivePages.findIndex(
        (page) => page.workflowStageOrder === stageOrder,
      );
      if (targetIndex >= 0) {
        preservedStageOrderRef.current = stageOrder;
        setActivePage(targetIndex);
      }
    },
    [effectivePages, setActivePage],
  );

  const handleEditIntentConsumed = useCallback(() => {
    setPendingEditIntent(null);
  }, []);

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
    if (pathologyRealPageIdsKey) {
      const ids = pathologyRealPageIdsKey.split(",").filter(Boolean);
      loadPathologyPageProgressForIds(ids);
    }
  }, [entryId, pathologyRealPageIdsKey, loadPathologyPageProgressForIds]);

  // Render Pathology page-specific content based on page order
  const renderPageContent = (page, pageIndex) => {
    const pageOrder = page.workflowStageOrder ?? page.order ?? 1;
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
            pendingEditIntent={pendingEditIntent}
            onEditIntentConsumed={handleEditIntentConsumed}
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
            individualPatientReportOnly
            onNavigateToStage={navigateToWorkflowStage}
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
            onNavigateToStage={navigateToWorkflowStage}
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
            {effectivePages.length > 0 &&
              effectivePages[activePage] &&
              !effectivePages[activePage].hasAccess && (
                <div className="page-panel access-denied">
                  <h3>{effectivePages[activePage].title}</h3>
                  <p>
                    <FormattedMessage
                      id="notebook.page.restricted"
                      defaultMessage="Restricted"
                    />
                  </p>
                  <p>
                    <FormattedMessage
                      id="notebook.page.accessDenied"
                      defaultMessage="You don't have the required role to access this page"
                    />
                  </p>
                </div>
              )}
          </div>
        </Column>
      </Grid>
    </div>
  );
}

export default PathologyWorkflowTab;
