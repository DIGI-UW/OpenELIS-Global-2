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
  const [creating, setCreating] = useState(false);

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
    { id: "tube", text: "Tube" },
    { id: "plate-96", text: "Plate (96-well)" },
    { id: "cryobox-9x9", text: "Cryobox (9×9)" },
    { id: "cryobox-10x10", text: "Cryobox (10×10)" },
  ];

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
    return { total: childSamples.length, routed, unrouted };
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
      },
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/samples/create-children`,
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
    entryId,
    childCount,
    externalIdPrefix,
    aliquotType,
    initialVolume,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
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

  // Render volume column
  const renderVolumeColumn = (sample) => {
    if (sample.remainingVolume !== undefined) {
      return (
        <span>
          {sample.remainingVolume} / {sample.volume || sample.initialVolume} mL
        </span>
      );
    }
    return sample.volume || sample.initialVolume || "-";
  };

  // Render aliquot count tag
  const renderAliquotCountTag = (sample) => {
    if (sample.hasChildren && sample.childAliquotCount > 0) {
      return <Tag type="green">{sample.childAliquotCount} aliquots</Tag>;
    }
    return <Tag type="gray">No aliquots</Tag>;
  };

  // Render routing status tag for children
  const renderRoutingStatusTag = (sample) => {
    if (sample.destinationType) {
      return (
        <Tag type="green" title={sample.wellCoordinate}>
          {sample.wellCoordinate || "Routed"}
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
                    render: renderVolumeColumn,
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
                    render: renderChildrenAction,
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
                    render: (sample) => sample.aliquotType || "-",
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
              onChange={(e) => setAliquotType(e.target.value)}
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
            <NumberInput
              id="childCount"
              label={intl.formatMessage({
                id: "notebook.mntd.aliquoting.aliquotsPerParent",
                defaultMessage: "Aliquots per Parent",
              })}
              value={childCount}
              onChange={(e, { value }) => setChildCount(value)}
              min={1}
              max={20}
              step={1}
            />
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
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="notebook.mntd.aliquoting.bulkImport.description"
            defaultMessage="Upload a plate map (CSV) or box layout file to bulk import aliquot samples with well positions and project associations."
          />
        </p>

        <FileUploader
          labelTitle={intl.formatMessage({
            id: "notebook.mntd.aliquoting.bulkImport.uploadFile",
            defaultMessage: "Upload File",
          })}
          labelDescription={intl.formatMessage({
            id: "notebook.mntd.aliquoting.bulkImport.fileTypes",
            defaultMessage: "Only CSV files are supported",
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
            <p>
              <FormattedMessage
                id="notebook.mntd.aliquoting.bulkImport.previewCount"
                defaultMessage="{count} aliquots will be imported"
                values={{ count: importPreview.totalCount }}
              />
            </p>
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
    </div>
  );
}

export default MNTDAliquotingPage;
