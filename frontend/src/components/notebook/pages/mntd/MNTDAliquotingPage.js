import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  NumberInput,
  TextInput,
  TextArea,
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Select,
  SelectItem,
  FileUploader,
  RadioButtonGroup,
  RadioButton,
  Checkbox,
} from "@carbon/react";
import {
  Add,
  ArrowRight,
  Renew,
  View,
  Chemistry,
  Upload,
  ChartBubble,
  Box as BoxIcon,
  WarningAlt,
  Checkmark,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import AssayPlateCreator from "../../workflow/AssayPlateCreator";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDAliquotingPage - Page 5 of the MNTD workflow.
 * Handles child sample creation (aliquoting) and routing to internal analysis (analyzer plates).
 *
 * Purpose: Create child samples and maintain parent-child relationships, then route to analyzer.
 *
 * Data Points:
 * - Aliquot Details: Aliquot type (Tube, Plate 96-well, Cryobox 9x9/10x10)
 * - Aliquot ID auto-generated (e.g., Ach-21-FY004-A1)
 * - Bulk Import: Upload plate map or box layout
 * - Volume/Quantity Tracking: Initial volume/DBS spots, remaining volume after aliquoting
 *
 * System Actions:
 * - Create child sample records
 * - Preserve lineage (Parent -> Aliquot)
 * - Update remaining volume on parent
 * - Route to Internal Analysis with analyzer plate assignment
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDAliquotingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [childSamples, setChildSamples] = useState([]);
  const [selectedParentIds, setSelectedParentIds] = useState([]);
  const [selectedChildIds, setSelectedChildIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Active tab state
  const [activeTab, setActiveTab] = useState(0);

  // Create aliquots modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [aliquotType, setAliquotType] = useState("tube");
  const [childCount, setChildCount] = useState(1);
  const [externalIdPrefix, setExternalIdPrefix] = useState("MNTD-ALQ");
  const [initialVolume, setInitialVolume] = useState("");
  const [initialDbsSpots, setInitialDbsSpots] = useState("");
  const [creating, setCreating] = useState(false);

  // QC During Processing modal state
  const [qcModalOpen, setQcModalOpen] = useState(false);
  const [qcProcessing, setQcProcessing] = useState(false);
  const [processingQcResult, setProcessingQcResult] = useState("");
  const [processingQcFailAction, setProcessingQcFailAction] = useState("");
  const [processingQcRemarks, setProcessingQcRemarks] = useState("");
  const [markForReExtraction, setMarkForReExtraction] = useState(false);
  const [markForReRun, setMarkForReRun] = useState(false);

  // View children modal state
  const [viewChildrenModalOpen, setViewChildrenModalOpen] = useState(false);
  const [selectedParentForView, setSelectedParentForView] = useState(null);
  const [parentChildren, setParentChildren] = useState([]);
  const [loadingChildren, setLoadingChildren] = useState(false);

  // Bulk import state
  const [bulkImportModalOpen, setBulkImportModalOpen] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importPreview, setImportPreview] = useState(null);
  const [importing, setImporting] = useState(false);

  // Route to analyzer modal state
  const [routeModalOpen, setRouteModalOpen] = useState(false);
  const [routing, setRouting] = useState(false);

  // Assay plate state (for Internal Analysis)
  const [assayPlates, setAssayPlates] = useState([]);
  const [selectedAssayPlateId, setSelectedAssayPlateId] = useState(null);

  // Aliquot type options
  const aliquotTypeOptions = [
    { id: "tube", text: "Tube", capacity: 1 },
    { id: "plate-96", text: "Plate (96-well)", capacity: 96 },
    { id: "cryobox-9x9", text: "Cryobox (9×9 = 81 positions)", capacity: 81 },
    {
      id: "cryobox-10x10",
      text: "Cryobox (10×10 = 100 positions)",
      capacity: 100,
    },
    { id: "dbs-card", text: "DBS Card (Dried Blood Spot)", capacity: 5 },
  ];

  // Sample type for DBS tracking
  const isDbs = aliquotType === "dbs-card";

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    // Skip loading for synthetic page IDs
    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Load samples with hierarchy info
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              volume: sample.volume || sample.data?.volume,
              remainingVolume: sample.data?.remainingVolume,
              // DBS tracking
              dbsSpots: sample.data?.dbsSpots,
              remainingDbsSpots: sample.data?.remainingDbsSpots,
              isDbs: sample.data?.aliquotType === "dbs-card",
              // Hierarchy information from backend
              hasChildren: sample.hasChildren || false,
              childAliquotCount: sample.childAliquotCount || 0,
              isAliquot: sample.isAliquot || false,
              nestingLevel: sample.nestingLevel || 0,
              parentSampleItemId: sample.parentSampleItemId,
              parentExternalId: sample.parentExternalId,
              // Routing information
              destinationType: sample.destinationType,
              wellCoordinate: sample.wellCoordinate,
              routingStatus: sample.destinationType ? "ROUTED" : "UNROUTED",
              // Aliquot-specific data
              aliquotType: sample.data?.aliquotType,
              initialVolume: sample.data?.initialVolume,
              // QC during processing
              processingQcResult: sample.data?.processingQcResult,
              processingQcFailAction: sample.data?.processingQcFailAction,
              markedForReExtraction: sample.data?.markedForReExtraction,
              markedForReRun: sample.data?.markedForReRun,
              // Project info for mixed plates
              projectId: sample.data?.projectId,
              projectName: sample.data?.projectName,
              isControl: sample.data?.isControl,
            }));

            // Separate parent samples (non-aliquots) from children (aliquots)
            const parents = transformedSamples.filter((s) => !s.isAliquot);
            const children = transformedSamples.filter((s) => s.isAliquot);

            setSamples(parents);
            setChildSamples(children);
          } else {
            setSamples([]);
            setChildSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const parentStats = useMemo(() => {
    const aliquoted = samples.filter((s) => s.hasChildren).length;
    const pending = samples.filter((s) => !s.hasChildren).length;
    return { total: samples.length, aliquoted, pending };
  }, [samples]);

  const childStats = useMemo(() => {
    const routed = childSamples.filter((s) => s.destinationType).length;
    const unrouted = childSamples.filter((s) => !s.destinationType).length;
    const qcPassed = childSamples.filter(
      (s) => s.processingQcResult === "Pass",
    ).length;
    const qcFailed = childSamples.filter(
      (s) => s.processingQcResult === "Fail",
    ).length;
    const forReExtraction = childSamples.filter(
      (s) => s.markedForReExtraction,
    ).length;
    const forReRun = childSamples.filter((s) => s.markedForReRun).length;
    const controls = childSamples.filter((s) => s.isControl).length;
    return {
      total: childSamples.length,
      routed,
      unrouted,
      qcPassed,
      qcFailed,
      forReExtraction,
      forReRun,
      controls,
    };
  }, [childSamples]);

  // Handle create aliquots modal open
  const handleOpenCreateModal = useCallback(() => {
    if (selectedParentIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.aliquoting.selectParent",
          defaultMessage: "Please select at least one parent sample.",
        }),
      );
      return;
    }
    setCreateModalOpen(true);
  }, [selectedParentIds, intl]);

  // Handle create aliquots
  const handleCreateAliquots = useCallback(() => {
    if (selectedParentIds.length === 0 || !hasRealPageId) return;

    setCreating(true);
    setError(null);

    // Build request with aliquot-specific data
    const requestData = {
      parentSampleIds: selectedParentIds.map((id) => parseInt(id, 10)),
      childCountPerParent: childCount,
      externalIdPrefix: externalIdPrefix,
      pageId: pageData?.id,
      aliquotData: {
        aliquotType: aliquotType,
        initialVolume: initialVolume ? parseFloat(initialVolume) : null,
        // DBS tracking - remainingDbsSpots is computed on backend (equals dbsSpots initially)
        dbsSpots:
          isDbs && initialDbsSpots ? parseInt(initialDbsSpots, 10) : null,
      },
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/create-children`,
      JSON.stringify(requestData),
      (response) => {
        setCreating(false);
        setCreateModalOpen(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.mntd.aliquoting.createSuccess",
                defaultMessage: "Successfully created {count} aliquot samples.",
              },
              { count: response.createdCount },
            ),
          );
          setSelectedParentIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to create aliquot samples.");
        }
      },
    );
  }, [
    selectedParentIds,
    hasRealPageId,
    notebookId,
    childCount,
    externalIdPrefix,
    aliquotType,
    initialVolume,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
    isDbs,
    initialDbsSpots,
  ]);

  // Handle view children for a parent
  const handleViewChildren = useCallback((parentSampleId) => {
    setSelectedParentForView(parentSampleId);
    setViewChildrenModalOpen(true);
    setLoadingChildren(true);

    getFromOpenElisServer(
      `/rest/notebook/samples/${parentSampleId}/children`,
      (response) => {
        setLoadingChildren(false);
        if (response && Array.isArray(response)) {
          setParentChildren(response);
        } else {
          setParentChildren([]);
        }
      },
    );
  }, []);

  // Handle bulk import modal open
  const handleOpenBulkImportModal = useCallback(() => {
    setBulkImportModalOpen(true);
    setImportFile(null);
    setImportPreview(null);
  }, []);

  // Handle file upload for bulk import
  const handleFileUpload = useCallback(
    (event) => {
      const files = event.target.files;
      if (files && files.length > 0) {
        setImportFile(files[0]);

        // Preview the file
        const formData = new FormData();
        formData.append("file", files[0]);

        postToOpenElisServerJsonResponse(
          `/rest/notebook/mntd/entry/${entryId}/aliquots/preview-import`,
          formData,
          (response) => {
            if (response && response.rows) {
              setImportPreview(response);
            }
          },
          true, // isFormData
        );
      }
    },
    [entryId],
  );

  // Handle bulk import execute
  const handleExecuteBulkImport = useCallback(() => {
    if (!importFile || !hasRealPageId) return;

    setImporting(true);
    setError(null);

    const formData = new FormData();
    formData.append("file", importFile);
    formData.append("pageId", pageData.id);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/mntd/entry/${entryId}/aliquots/import`,
      formData,
      (response) => {
        setImporting(false);
        setBulkImportModalOpen(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.mntd.aliquoting.importSuccess",
                defaultMessage:
                  "Successfully imported {count} aliquot samples.",
              },
              { count: response.importedCount },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to import aliquots.");
        }
      },
      true, // isFormData
    );
  }, [
    importFile,
    hasRealPageId,
    entryId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle route to analyzer modal open
  const handleOpenRouteModal = useCallback(() => {
    if (selectedChildIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.aliquoting.selectAliquot",
          defaultMessage: "Please select at least one aliquot sample to route.",
        }),
      );
      return;
    }
    setRouteModalOpen(true);
  }, [selectedChildIds, intl]);

  // Handle route to analyzer
  const handleRouteToAnalyzer = useCallback(() => {
    if (selectedChildIds.length === 0 || !hasRealPageId || !notebookId) return;

    // Validate plate selection
    const selectedPlate = assayPlates.find(
      (p) => p.id === selectedAssayPlateId,
    );
    if (!selectedPlate) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.aliquoting.selectPlate",
          defaultMessage:
            "Please create and select an analyzer plate for internal analysis.",
        }),
      );
      return;
    }

    setRouting(true);
    setError(null);

    const routeRequest = {
      sampleIds: selectedChildIds.map((id) => parseInt(id, 10)),
      destinationType: "INTERNAL_ANALYSIS",
      pageId: pageData?.id,
      assayPlate: {
        id: selectedPlate.id,
        name: selectedPlate.name,
        rows: selectedPlate.rows,
        columns: selectedPlate.columns,
      },
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/route`,
      JSON.stringify(routeRequest),
      (response) => {
        setRouting(false);
        setRouteModalOpen(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.mntd.aliquoting.routeSuccess",
                defaultMessage:
                  "Successfully routed {count} samples to analyzer plate {plate}.",
              },
              { count: response.routedCount, plate: selectedPlate.name },
            ),
          );
          setSelectedChildIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to route samples.");
        }
      },
    );
  }, [
    selectedChildIds,
    hasRealPageId,
    notebookId,
    assayPlates,
    selectedAssayPlateId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle status change for aliquoting completion
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "notebook.mntd.aliquoting.pageNotInitialized",
            defaultMessage:
              "Cannot update status: Page not properly initialized.",
          }),
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate, intl],
  );

  // Bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedParentIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.aliquoting.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedParentIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.mntd.aliquoting.markCompleted",
                defaultMessage:
                  "Marked {count} samples as aliquoting complete.",
              },
              { count: selectedParentIds.length },
            ),
          );
          loadPageSamples();
          setSelectedParentIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status. Please try again.");
        }
      },
    );
  }, [
    selectedParentIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle QC during processing modal open
  const handleOpenQcModal = useCallback(() => {
    if (selectedChildIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.aliquoting.selectForQc",
          defaultMessage: "Please select at least one aliquot sample for QC.",
        }),
      );
      return;
    }
    setProcessingQcResult("");
    setProcessingQcFailAction("");
    setProcessingQcRemarks("");
    setMarkForReExtraction(false);
    setMarkForReRun(false);
    setQcModalOpen(true);
  }, [selectedChildIds, intl]);

  // Handle QC during processing submit
  const handleProcessingQcSubmit = useCallback(() => {
    if (!processingQcResult || !hasRealPageId) return;

    // Validate fail action if failed
    if (processingQcResult === "Fail" && !processingQcFailAction) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.aliquoting.qc.selectFailAction",
          defaultMessage: "Please select an action for failed samples.",
        }),
      );
      return;
    }

    setQcProcessing(true);
    setError(null);

    const qcData = {
      processingQcResult: processingQcResult,
      processingQcFailAction:
        processingQcResult === "Fail" ? processingQcFailAction : null,
      processingQcRemarks: processingQcRemarks,
      markedForReExtraction:
        processingQcResult === "Fail" && markForReExtraction,
      markedForReRun: processingQcResult === "Fail" && markForReRun,
    };

    const applyData = {
      sampleIds: selectedChildIds.map((id) => parseInt(id, 10)),
      data: qcData,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(applyData),
      (status) => {
        setQcProcessing(false);
        if (status === 200) {
          setQcModalOpen(false);
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.mntd.aliquoting.qc.success",
                defaultMessage: "QC result applied to {count} sample(s).",
              },
              { count: selectedChildIds.length },
            ),
          );
          setSelectedChildIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to apply QC result. Please try again.");
        }
      },
    );
  }, [
    processingQcResult,
    processingQcFailAction,
    processingQcRemarks,
    markForReExtraction,
    markForReRun,
    selectedChildIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Render children action button
  const renderChildrenAction = (sample) => {
    return (
      <Button
        kind="ghost"
        size="sm"
        hasIconOnly
        iconDescription={intl.formatMessage({
          id: "notebook.mntd.aliquoting.viewAliquots",
          defaultMessage: "View Aliquots",
        })}
        renderIcon={View}
        onClick={() => handleViewChildren(sample.id)}
        disabled={!sample.hasChildren}
      />
    );
  };

  // Render volume column - supports both liquid volume and DBS spots
  const renderVolumeColumn = (sample) => {
    // DBS tracking
    if (sample.isDbs || sample.aliquotType === "dbs-card") {
      if (sample.remainingDbsSpots !== undefined) {
        return (
          <span>
            {sample.remainingDbsSpots} / {sample.dbsSpots} spots
          </span>
        );
      }
      return sample.dbsSpots ? `${sample.dbsSpots} spots` : "-";
    }
    // Liquid volume tracking
    if (sample.remainingVolume !== undefined) {
      return (
        <span>
          {sample.remainingVolume} / {sample.volume || sample.initialVolume} mL
        </span>
      );
    }
    return sample.volume || sample.initialVolume || "-";
  };

  // Render processing QC status tag
  const renderProcessingQcTag = (sample) => {
    if (!sample.processingQcResult) return <Tag type="gray">Pending</Tag>;
    if (sample.processingQcResult === "Pass")
      return <Tag type="green">Pass</Tag>;
    if (sample.processingQcResult === "Fail") {
      if (sample.markedForReExtraction) {
        return <Tag type="purple">Re-extraction</Tag>;
      }
      if (sample.markedForReRun) {
        return <Tag type="teal">Re-run</Tag>;
      }
      return <Tag type="red">Fail</Tag>;
    }
    return <Tag type="gray">{sample.processingQcResult}</Tag>;
  };

  // Render project/control info
  const renderProjectInfo = (sample) => {
    if (sample.isControl) {
      return <Tag type="cyan">Control</Tag>;
    }
    if (sample.projectName) {
      return <Tag type="blue">{sample.projectName}</Tag>;
    }
    return "-";
  };

  // Render aliquot count tag
  const renderAliquotCountTag = (value, sample) => {
    const s = sample || value;
    if (s?.hasChildren && s?.childAliquotCount > 0) {
      return <Tag type="green">{s.childAliquotCount} aliquots</Tag>;
    }
    return <Tag type="gray">No aliquots</Tag>;
  };

  // Render routing status tag for children
  const renderRoutingStatusTag = (value, sample) => {
    const s = sample || value;
    if (s?.destinationType) {
      return (
        <Tag type="green" title={s.wellCoordinate}>
          {s.wellCoordinate || "Routed"}
        </Tag>
      );
    }
    return <Tag type="gray">Unrouted</Tag>;
  };

  return (
    <div className="mntd-aliquoting-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.aliquoting.title"
            defaultMessage="Aliquoting / Bulk Sample Import"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.aliquoting.description"
            defaultMessage="Create child samples (aliquots) from parent samples and route them to analyzer plates for internal analysis. Upload plate maps or box layouts for bulk import."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.aliquoting.parentSamples"
                  defaultMessage="Parent Samples"
                />
              </span>
              <span className="progress-value">{parentStats.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.aliquoting.aliquoted"
                  defaultMessage="Aliquoted"
                />
              </span>
              <span className="progress-value">{parentStats.aliquoted}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.aliquoting.totalAliquots"
                  defaultMessage="Total Aliquots"
                />
              </span>
              <span className="progress-value">{childStats.total}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.aliquoting.routedToAnalyzer"
                  defaultMessage="Routed to Analyzer"
                />
              </span>
              <span className="progress-value">{childStats.routed}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Tabs for Parent Samples and Aliquots */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Aliquoting tabs">
          <Tab>
            <ChartBubble size={16} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="notebook.mntd.aliquoting.tab.parents"
              defaultMessage="Parent Samples ({count})"
              values={{ count: parentStats.total }}
            />
          </Tab>
          <Tab>
            <BoxIcon size={16} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="notebook.mntd.aliquoting.tab.aliquots"
              defaultMessage="Aliquots ({count})"
              values={{ count: childStats.total }}
            />
          </Tab>
        </TabList>
        <TabPanels>
          {/* Tab 1: Parent Samples - Aliquot Creation */}
          <TabPanel>
            {/* Action Buttons for Parent Samples */}
            <div className="page-actions-bar">
              <Button
                kind="primary"
                size="sm"
                renderIcon={Add}
                onClick={handleOpenCreateModal}
                disabled={selectedParentIds.length === 0}
              >
                <FormattedMessage
                  id="notebook.mntd.aliquoting.createAliquots"
                  defaultMessage="Create Aliquots ({count} selected)"
                  values={{ count: selectedParentIds.length }}
                />
              </Button>

              <Button
                kind="secondary"
                size="sm"
                renderIcon={Upload}
                onClick={handleOpenBulkImportModal}
              >
                <FormattedMessage
                  id="notebook.mntd.aliquoting.bulkImport"
                  defaultMessage="Bulk Import"
                />
              </Button>

              {selectedParentIds.length > 0 && (
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={ArrowRight}
                  onClick={handleBulkMarkCompleted}
                >
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.markComplete"
                    defaultMessage="Mark Aliquoting Complete ({count})"
                    values={{ count: selectedParentIds.length }}
                  />
                </Button>
              )}

              <Button
                kind="ghost"
                size="sm"
                renderIcon={Renew}
                onClick={loadPageSamples}
              >
                <FormattedMessage
                  id="notebook.mntd.aliquoting.refresh"
                  defaultMessage="Refresh"
                />
              </Button>
            </div>

            {/* Parent Samples Grid */}
            <div className="sample-grid-container">
              <SampleGrid
                gridId="mntd-aliquoting-parents"
                samples={samples}
                selectedIds={selectedParentIds}
                onSelectionChange={setSelectedParentIds}
                onStatusChange={handleStatusChange}
                statusFilter={statusFilter}
                onStatusFilterChange={setStatusFilter}
                showSelection={true}
                showHierarchy={false}
                loading={loading}
                additionalColumns={[
                  {
                    key: "volume",
                    header: intl.formatMessage({
                      id: "notebook.mntd.aliquoting.volume",
                      defaultMessage: "Volume",
                    }),
                    render: (_, sample) => renderVolumeColumn(sample),
                  },
                  {
                    key: "aliquots",
                    header: intl.formatMessage({
                      id: "notebook.mntd.aliquoting.aliquots",
                      defaultMessage: "Aliquots",
                    }),
                    render: renderAliquotCountTag,
                  },
                  {
                    key: "actions",
                    header: "",
                    render: (_, sample) => renderChildrenAction(sample),
                  },
                ]}
              />
            </div>

            {/* Empty state for parents */}
            {!loading && samples.length === 0 && (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.empty.parents"
                    defaultMessage="No samples available for aliquoting. Please complete the sample processing step first."
                  />
                </p>
              </div>
            )}
          </TabPanel>

          {/* Tab 2: Aliquots - Route to Analyzer */}
          <TabPanel>
            {/* Action Buttons for Aliquots */}
            <div className="page-actions-bar">
              <Button
                kind="primary"
                size="sm"
                renderIcon={Chemistry}
                onClick={handleOpenRouteModal}
                disabled={selectedChildIds.length === 0}
              >
                <FormattedMessage
                  id="notebook.mntd.aliquoting.routeToAnalyzer"
                  defaultMessage="Route to Analyzer ({count} selected)"
                  values={{ count: selectedChildIds.length }}
                />
              </Button>

              <Button
                kind="secondary"
                size="sm"
                renderIcon={Checkmark}
                onClick={handleOpenQcModal}
                disabled={selectedChildIds.length === 0}
              >
                <FormattedMessage
                  id="notebook.mntd.aliquoting.qcDuringProcessing"
                  defaultMessage="QC During Processing ({count})"
                  values={{ count: selectedChildIds.length }}
                />
              </Button>

              <Button
                kind="ghost"
                size="sm"
                renderIcon={Renew}
                onClick={loadPageSamples}
              >
                <FormattedMessage
                  id="notebook.mntd.aliquoting.refresh"
                  defaultMessage="Refresh"
                />
              </Button>
            </div>

            {/* QC Stats Row */}
            {(childStats.qcFailed > 0 ||
              childStats.forReExtraction > 0 ||
              childStats.forReRun > 0) && (
              <div className="qc-stats-row" style={{ marginBottom: "1rem" }}>
                <InlineNotification
                  kind="warning"
                  title={intl.formatMessage({
                    id: "notebook.mntd.aliquoting.qcIssues",
                    defaultMessage: "QC Issues",
                  })}
                  subtitle={intl.formatMessage(
                    {
                      id: "notebook.mntd.aliquoting.qcIssuesDetail",
                      defaultMessage:
                        "{failed} failed, {reExtraction} for re-extraction, {reRun} for re-run",
                    },
                    {
                      failed: childStats.qcFailed,
                      reExtraction: childStats.forReExtraction,
                      reRun: childStats.forReRun,
                    },
                  )}
                  lowContrast
                  hideCloseButton
                />
              </div>
            )}

            {/* Aliquots Grid */}
            <div className="sample-grid-container">
              <SampleGrid
                gridId="mntd-aliquoting-children"
                samples={childSamples}
                selectedIds={selectedChildIds}
                onSelectionChange={setSelectedChildIds}
                statusFilter={statusFilter}
                onStatusFilterChange={setStatusFilter}
                showSelection={true}
                showHierarchy={true}
                loading={loading}
                additionalColumns={[
                  {
                    key: "aliquotType",
                    header: intl.formatMessage({
                      id: "notebook.mntd.aliquoting.type",
                      defaultMessage: "Type",
                    }),
                    render: (value, sample) =>
                      value || sample?.aliquotType || "-",
                  },
                  {
                    key: "volumeOrSpots",
                    header: intl.formatMessage({
                      id: "notebook.mntd.aliquoting.volumeOrSpots",
                      defaultMessage: "Volume/Spots",
                    }),
                    render: (_, sample) => renderVolumeColumn(sample),
                  },
                  {
                    key: "processingQc",
                    header: intl.formatMessage({
                      id: "notebook.mntd.aliquoting.processingQc",
                      defaultMessage: "Processing QC",
                    }),
                    render: (_, sample) => renderProcessingQcTag(sample),
                  },
                  {
                    key: "project",
                    header: intl.formatMessage({
                      id: "notebook.mntd.aliquoting.project",
                      defaultMessage: "Project/Control",
                    }),
                    render: (_, sample) => renderProjectInfo(sample),
                  },
                  {
                    key: "routingStatus",
                    header: intl.formatMessage({
                      id: "notebook.mntd.aliquoting.analyzerPosition",
                      defaultMessage: "Analyzer Position",
                    }),
                    render: renderRoutingStatusTag,
                  },
                ]}
              />
            </div>

            {/* Empty state for aliquots */}
            {!loading && childSamples.length === 0 && (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.empty.aliquots"
                    defaultMessage="No aliquots created yet. Select parent samples in the Parent Samples tab and create aliquots."
                  />
                </p>
              </div>
            )}
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Create Aliquots Modal */}
      <Modal
        open={createModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.aliquoting.modal.createTitle",
          defaultMessage: "Create Aliquots",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.aliquoting.modal.create",
          defaultMessage: "Create Aliquots",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setCreateModalOpen(false)}
        onRequestSubmit={handleCreateAliquots}
        primaryButtonDisabled={creating}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.mntd.aliquoting.modal.description"
              defaultMessage="Create aliquot samples from {count} selected parent sample(s)."
              values={{ count: selectedParentIds.length }}
            />
          </p>
        </div>

        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="aliquotType"
              labelText={intl.formatMessage({
                id: "notebook.mntd.aliquoting.aliquotType",
                defaultMessage: "Aliquot Type",
              })}
              value={aliquotType}
              onChange={(e) => {
                const selectedType = e.target.value;
                setAliquotType(selectedType);
                // Auto-set childCount based on aliquot type capacity
                const selectedOption = aliquotTypeOptions.find(
                  (opt) => opt.id === selectedType,
                );
                if (selectedOption) {
                  // For tubes, default to 1 (user can adjust)
                  // For plates/cryoboxes, set to full capacity
                  if (selectedType === "tube") {
                    setChildCount(1);
                  } else {
                    setChildCount(selectedOption.capacity);
                  }
                }
              }}
            >
              {aliquotTypeOptions.map((option) => (
                <SelectItem
                  key={option.id}
                  value={option.id}
                  text={option.text}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            {aliquotType === "tube" ? (
              <NumberInput
                id="childCount"
                label={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.aliquotsPerParent",
                  defaultMessage: "Aliquots per Parent",
                })}
                value={childCount}
                onChange={(e, { value }) => setChildCount(value)}
                min={1}
                max={100}
                step={1}
                helperText={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.tubeCountHelp",
                  defaultMessage:
                    "Number of tube aliquots to create per parent",
                })}
              />
            ) : (
              <div style={{ marginTop: "1.5rem" }}>
                <Tag type="blue" size="lg">
                  {childCount}{" "}
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.positionsAuto"
                    defaultMessage="positions (auto-set from format)"
                  />
                </Tag>
                <p
                  style={{
                    fontSize: "0.75rem",
                    color: "#525252",
                    marginTop: "0.5rem",
                  }}
                >
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.capacityNote"
                    defaultMessage="Aliquot count is determined by the selected format capacity"
                  />
                </p>
              </div>
            )}
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="externalIdPrefix"
              labelText={intl.formatMessage({
                id: "notebook.mntd.aliquoting.idPrefix",
                defaultMessage: "Aliquot ID Prefix",
              })}
              value={externalIdPrefix}
              onChange={(e) => setExternalIdPrefix(e.target.value)}
              helperText={intl.formatMessage(
                {
                  id: "notebook.mntd.aliquoting.idPrefixHelp",
                  defaultMessage:
                    "Aliquots will be named: {prefix}-{year}-{sequence} (e.g., {example})",
                },
                {
                  prefix: externalIdPrefix || "PREFIX",
                  year: new Date().getFullYear(),
                  sequence: "001",
                  example: `${externalIdPrefix || "PREFIX"}-${new Date().getFullYear()}-001`,
                },
              )}
            />
          </Column>

          {/* Volume field - only for non-DBS types */}
          {!isDbs && (
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="initialVolume"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.initialVolume",
                  defaultMessage: "Initial Volume per Aliquot (mL)",
                })}
                value={initialVolume}
                onChange={(e) => setInitialVolume(e.target.value)}
                placeholder="e.g., 0.5"
              />
            </Column>
          )}

          {/* DBS Spots field - only for DBS cards */}
          {isDbs && (
            <Column lg={8} md={4} sm={4}>
              <NumberInput
                id="initialDbsSpots"
                label={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.dbsSpots",
                  defaultMessage: "DBS Spots per Card",
                })}
                value={initialDbsSpots}
                onChange={(e, { value }) => setInitialDbsSpots(value)}
                min={1}
                max={10}
                step={1}
                helperText={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.dbsSpotsHelp",
                  defaultMessage:
                    "Number of dried blood spots available for punching",
                })}
              />
            </Column>
          )}

          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                marginTop: "1rem",
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <strong>
                <FormattedMessage
                  id="notebook.mntd.aliquoting.summary"
                  defaultMessage="Summary:"
                />
              </strong>
              <p style={{ marginTop: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.mntd.aliquoting.totalToCreate"
                  defaultMessage="Total aliquots to create: {total}"
                  values={{ total: selectedParentIds.length * childCount }}
                />
              </p>
              {/* Format-specific summary info */}
              {aliquotType === "plate-96" && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.plateSummary"
                    defaultMessage="{count} parent(s) × 96 wells = {total} aliquots (8 rows × 12 columns per plate)"
                    values={{
                      count: selectedParentIds.length,
                      total: selectedParentIds.length * 96,
                    }}
                  />
                </p>
              )}
              {aliquotType === "cryobox-9x9" && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.cryobox9x9Summary"
                    defaultMessage="{count} parent(s) × 81 positions = {total} aliquots (9×9 cryobox)"
                    values={{
                      count: selectedParentIds.length,
                      total: selectedParentIds.length * 81,
                    }}
                  />
                </p>
              )}
              {aliquotType === "cryobox-10x10" && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.cryobox10x10Summary"
                    defaultMessage="{count} parent(s) × 100 positions = {total} aliquots (10×10 cryobox)"
                    values={{
                      count: selectedParentIds.length,
                      total: selectedParentIds.length * 100,
                    }}
                  />
                </p>
              )}
              {isDbs && initialDbsSpots && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.dbsSummary"
                    defaultMessage="Each DBS card will have {spots} spots available for punching"
                    values={{ spots: initialDbsSpots }}
                  />
                </p>
              )}
              {aliquotType === "tube" && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.tubeSummary"
                    defaultMessage="{count} parent(s) × {perParent} tube(s) = {total} aliquots"
                    values={{
                      count: selectedParentIds.length,
                      perParent: childCount,
                      total: selectedParentIds.length * childCount,
                    }}
                  />
                </p>
              )}
            </div>
          </Column>
        </Grid>
      </Modal>

      {/* View Children Modal */}
      <Modal
        open={viewChildrenModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.aliquoting.viewModal.title",
          defaultMessage: "Aliquot Samples",
        })}
        passiveModal
        onRequestClose={() => {
          setViewChildrenModalOpen(false);
          setParentChildren([]);
        }}
        size="md"
      >
        {loadingChildren ? (
          <p>
            <FormattedMessage
              id="notebook.mntd.aliquoting.loadingAliquots"
              defaultMessage="Loading aliquots..."
            />
          </p>
        ) : parentChildren.length === 0 ? (
          <p>
            <FormattedMessage
              id="notebook.mntd.aliquoting.noAliquots"
              defaultMessage="No aliquot samples found for this parent."
            />
          </p>
        ) : (
          <DataTable
            rows={parentChildren.map((child) => ({
              id: String(child.id),
              externalId: child.externalId || "-",
              sampleType: child.sampleType || "-",
              aliquotType: child.data?.aliquotType || "-",
              wellCoordinate: child.wellCoordinate || "-",
              status: child.status || "PENDING",
            }))}
            headers={[
              { key: "externalId", header: "Aliquot ID" },
              { key: "sampleType", header: "Sample Type" },
              { key: "aliquotType", header: "Aliquot Type" },
              { key: "wellCoordinate", header: "Well Position" },
              { key: "status", header: "Status" },
            ]}
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        {...getHeaderProps({ header })}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status" ? (
                            <Tag
                              type={
                                cell.value === "COMPLETED"
                                  ? "green"
                                  : cell.value === "IN_PROGRESS"
                                    ? "blue"
                                    : "gray"
                              }
                            >
                              {cell.value}
                            </Tag>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
        )}
      </Modal>

      {/* Bulk Import Modal */}
      <Modal
        open={bulkImportModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.aliquoting.bulkImport.title",
          defaultMessage: "Bulk Import Aliquots",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.aliquoting.bulkImport.import",
          defaultMessage: "Import",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setBulkImportModalOpen(false)}
        onRequestSubmit={handleExecuteBulkImport}
        primaryButtonDisabled={importing || !importFile}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.mntd.aliquoting.bulkImport.description"
              defaultMessage="Upload a plate map (CSV) or box layout file to bulk import aliquot samples with well positions and project associations."
            />
          </p>

          {/* Supported Formats Info */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.mntd.aliquoting.bulkImport.formats"
                defaultMessage="Supported Formats"
              />
            </h6>
            <ul
              style={{ fontSize: "0.875rem", paddingLeft: "1.5rem", margin: 0 }}
            >
              <li>
                <strong>
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.format.plate96"
                    defaultMessage="Plate (96-well)"
                  />
                </strong>
                {" - "}
                <FormattedMessage
                  id="notebook.mntd.aliquoting.format.plate96.desc"
                  defaultMessage="8 rows (A-H) × 12 columns"
                />
              </li>
              <li>
                <strong>
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.format.cryobox9x9"
                    defaultMessage="Cryobox (9×9)"
                  />
                </strong>
                {" - "}
                <FormattedMessage
                  id="notebook.mntd.aliquoting.format.cryobox9x9.desc"
                  defaultMessage="81 positions"
                />
              </li>
              <li>
                <strong>
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.format.cryobox10x10"
                    defaultMessage="Cryobox (10×10)"
                  />
                </strong>
                {" - "}
                <FormattedMessage
                  id="notebook.mntd.aliquoting.format.cryobox10x10.desc"
                  defaultMessage="100 positions"
                />
              </li>
            </ul>
          </div>

          {/* Mixed Plates & Controls Info */}
          <div
            style={{
              marginTop: "0.75rem",
              padding: "1rem",
              backgroundColor: "#e5f6ff",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.mntd.aliquoting.bulkImport.mixedPlates"
                defaultMessage="Mixed Project Plates & Controls"
              />
            </h6>
            <p style={{ fontSize: "0.875rem", margin: 0 }}>
              <FormattedMessage
                id="notebook.mntd.aliquoting.bulkImport.mixedPlatesDesc"
                defaultMessage="CSV may contain samples from multiple projects. Include a 'ProjectID' column to track project associations. Mark control samples with 'IsControl=true' column."
              />
            </p>
          </div>
        </div>

        <FileUploader
          labelTitle={intl.formatMessage({
            id: "notebook.mntd.aliquoting.bulkImport.uploadFile",
            defaultMessage: "Upload File",
          })}
          labelDescription={intl.formatMessage({
            id: "notebook.mntd.aliquoting.bulkImport.fileTypes",
            defaultMessage:
              "CSV format: SampleID, ParentID, WellPosition, ProjectID (optional), IsControl (optional), Volume/DBSSpots",
          })}
          buttonLabel={intl.formatMessage({
            id: "notebook.mntd.aliquoting.bulkImport.selectFile",
            defaultMessage: "Select file",
          })}
          filenameStatus="edit"
          accept={[".csv"]}
          onChange={handleFileUpload}
        />

        {importPreview && (
          <div style={{ marginTop: "1rem" }}>
            <h6>
              <FormattedMessage
                id="notebook.mntd.aliquoting.bulkImport.preview"
                defaultMessage="Import Preview"
              />
            </h6>

            {/* Summary Stats */}
            <div
              style={{
                display: "flex",
                gap: "1rem",
                marginTop: "0.5rem",
                flexWrap: "wrap",
              }}
            >
              <Tag type="blue">
                <FormattedMessage
                  id="notebook.mntd.aliquoting.bulkImport.totalSamples"
                  defaultMessage="{count} samples"
                  values={{ count: importPreview.totalCount }}
                />
              </Tag>
              {importPreview.projectCount && (
                <Tag type="purple">
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.bulkImport.projects"
                    defaultMessage="{count} projects"
                    values={{ count: importPreview.projectCount }}
                  />
                </Tag>
              )}
              {importPreview.controlCount > 0 && (
                <Tag type="cyan">
                  <FormattedMessage
                    id="notebook.mntd.aliquoting.bulkImport.controls"
                    defaultMessage="{count} controls"
                    values={{ count: importPreview.controlCount }}
                  />
                </Tag>
              )}
              {importPreview.formatType && (
                <Tag type="gray">{importPreview.formatType}</Tag>
              )}
            </div>

            {importPreview.errors && importPreview.errors.length > 0 && (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.bulkImport.warnings",
                  defaultMessage: "Import Warnings",
                })}
                subtitle={importPreview.errors.join("; ")}
                lowContrast
                hideCloseButton
                style={{ marginTop: "0.5rem" }}
              />
            )}
          </div>
        )}
      </Modal>

      {/* Route to Analyzer Modal */}
      <Modal
        open={routeModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.aliquoting.route.title",
          defaultMessage: "Route to Internal Analysis",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.aliquoting.route.submit",
          defaultMessage: "Route Samples",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setRouteModalOpen(false)}
        onRequestSubmit={handleRouteToAnalyzer}
        primaryButtonDisabled={routing || !selectedAssayPlateId}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.mntd.aliquoting.route.description"
              defaultMessage="Route {count} aliquot sample(s) to an analyzer plate for internal analysis. Wells will be auto-assigned in row-major order (A1, A2, ..., A12, B1, ...)."
              values={{ count: selectedChildIds.length }}
            />
          </p>
        </div>

        <AssayPlateCreator
          plates={assayPlates}
          onPlatesChange={setAssayPlates}
          selectedPlateId={selectedAssayPlateId}
          onPlateSelect={setSelectedAssayPlateId}
          sampleCount={selectedChildIds.length}
        />

        <p
          style={{
            marginTop: "1rem",
            fontSize: "0.875rem",
            color: "#525252",
          }}
        >
          <FormattedMessage
            id="notebook.mntd.aliquoting.route.help"
            defaultMessage="Create or select an analyzer plate above. Samples will be automatically assigned to available wells."
          />
        </p>
      </Modal>

      {/* QC During Processing Modal */}
      <Modal
        open={qcModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.aliquoting.qc.title",
          defaultMessage: "QC During Processing",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.aliquoting.qc.submit",
          defaultMessage: "Apply QC Result",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setQcModalOpen(false)}
        onRequestSubmit={handleProcessingQcSubmit}
        primaryButtonDisabled={qcProcessing || !processingQcResult}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.mntd.aliquoting.qc.description"
              defaultMessage="Apply QC result to {count} selected sample(s) during processing."
              values={{ count: selectedChildIds.length }}
            />
          </p>
        </div>

        <div className="qc-section">
          <Grid fullWidth>
            <Column lg={16} md={8} sm={4}>
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.qc.result",
                  defaultMessage: "QC Result",
                })}
                name="processingQcResult"
                valueSelected={processingQcResult}
                onChange={(value) => {
                  setProcessingQcResult(value);
                  if (value === "Pass") {
                    setProcessingQcFailAction("");
                    setMarkForReExtraction(false);
                    setMarkForReRun(false);
                  }
                }}
                orientation="horizontal"
              >
                <RadioButton
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.aliquoting.qc.pass",
                    defaultMessage: "Pass - Continue processing",
                  })}
                  value="Pass"
                  id="processingQc-pass"
                />
                <RadioButton
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.aliquoting.qc.fail",
                    defaultMessage: "Fail - Requires action",
                  })}
                  value="Fail"
                  id="processingQc-fail"
                />
              </RadioButtonGroup>
            </Column>
          </Grid>
        </div>

        {/* Fail Actions - Only show if QC result is Fail */}
        {processingQcResult === "Fail" && (
          <div className="qc-section" style={{ marginTop: "1rem" }}>
            <h6
              style={{
                marginBottom: "0.5rem",
                display: "flex",
                alignItems: "center",
              }}
            >
              <WarningAlt
                size={16}
                style={{ marginRight: "0.5rem", color: "#da1e28" }}
              />
              <FormattedMessage
                id="notebook.mntd.aliquoting.qc.failActions"
                defaultMessage="Fail Actions"
              />
            </h6>
            <Grid fullWidth>
              <Column lg={16} md={8} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.mntd.aliquoting.qc.selectAction",
                    defaultMessage: "Select action for failed samples",
                  })}
                  name="processingQcFailAction"
                  valueSelected={processingQcFailAction}
                  onChange={(value) => setProcessingQcFailAction(value)}
                  orientation="vertical"
                >
                  <RadioButton
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.aliquoting.qc.reExtraction",
                      defaultMessage:
                        "Re-extraction - Sample needs to be re-extracted",
                    })}
                    value="re_extraction"
                    id="failAction-reExtraction"
                  />
                  <RadioButton
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.aliquoting.qc.reRun",
                      defaultMessage:
                        "Re-run - Sample needs to be re-run on analyzer",
                    })}
                    value="re_run"
                    id="failAction-reRun"
                  />
                </RadioButtonGroup>
              </Column>

              <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
                <Checkbox
                  id="markForReExtraction"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.aliquoting.qc.markReExtraction",
                    defaultMessage: "Flag sample for re-extraction queue",
                  })}
                  checked={markForReExtraction}
                  onChange={(_, { checked }) => {
                    setMarkForReExtraction(checked);
                    if (checked) setMarkForReRun(false);
                  }}
                  disabled={processingQcFailAction !== "re_extraction"}
                />
              </Column>

              <Column lg={16} md={8} sm={4}>
                <Checkbox
                  id="markForReRun"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.aliquoting.qc.markReRun",
                    defaultMessage: "Flag sample for re-run queue",
                  })}
                  checked={markForReRun}
                  onChange={(_, { checked }) => {
                    setMarkForReRun(checked);
                    if (checked) setMarkForReExtraction(false);
                  }}
                  disabled={processingQcFailAction !== "re_run"}
                />
              </Column>
            </Grid>
          </div>
        )}

        {/* QC Remarks */}
        <div className="qc-section" style={{ marginTop: "1rem" }}>
          <Grid fullWidth>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="processingQcRemarks"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.qc.remarks",
                  defaultMessage: "QC Remarks",
                })}
                value={processingQcRemarks}
                onChange={(e) => setProcessingQcRemarks(e.target.value)}
                placeholder={intl.formatMessage({
                  id: "notebook.mntd.aliquoting.qc.remarksPlaceholder",
                  defaultMessage: "Document any observations or issues...",
                })}
                rows={3}
              />
            </Column>
          </Grid>
        </div>
      </Modal>
    </div>
  );
}

export default MNTDAliquotingPage;
