import React, { useState, useEffect, useRef, useCallback } from "react";
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
  Select,
  SelectItem,
  MultiSelect,
} from "@carbon/react";
import {
  Add,
  ArrowRight,
  Renew,
  View,
  Edit,
  Chemistry,
  InventoryManagement,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PharmaceuticalProcessingPage - Page 3 of the Pharmaceuticals workflow.
 * Handles sample processing and aliquoting with parent-child traceability.
 * Similar to ChildSampleCreationPage + MNTDSampleProcessingPage patterns.
 *
 * Processing activities:
 * - Pharmaceutical: Weighing, grinding, homogenization, extraction/dissolution
 * - Biological: Centrifugation, aliquoting, tissue homogenization
 * - Microbiological: Swab streaking, inoculation, filtration
 */
function PharmaceuticalProcessingPage({
  entryId,
  notebookId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [selectedParentIds, setSelectedParentIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Create aliquot modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [childCount, setChildCount] = useState(1);
  const [externalIdPrefix, setExternalIdPrefix] = useState("PH-ALQ");
  const [creating, setCreating] = useState(false);

  // Bulk prepare modal state (like MNTD)
  const [bulkPrepareModalOpen, setBulkPrepareModalOpen] = useState(false);
  const [isBulkApplying, setIsBulkApplying] = useState(false);
  const [bulkPrepareValues, setBulkPrepareValues] = useState({
    processingMethod: "",
    technicianName: "",
    processingDate: new Date().toISOString().slice(0, 10),
    selectedReagents: [],
    selectedInstruments: [],
    batchNumber: "",
    lotNumber: "",
    notes: "",
  });

  // View children modal
  const [viewChildrenModalOpen, setViewChildrenModalOpen] = useState(false);
  const [selectedParentForView, setSelectedParentForView] = useState(null);
  const [parentChildren, setParentChildren] = useState([]);
  const [loadingChildren, setLoadingChildren] = useState(false);

  // Reagents and instruments from inventory
  const [reagents, setReagents] = useState([]);
  const [instruments, setInstruments] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);
  const [loadingInstruments, setLoadingInstruments] = useState(false);
  const [sampleTypes, setSampleTypes] = useState([]);

  // Processing method options (pharma-specific)
  const processingMethods = [
    { value: "weighing", label: "Weighing" },
    { value: "grinding", label: "Grinding / Milling" },
    { value: "homogenization", label: "Homogenization" },
    { value: "extraction", label: "Extraction / Dissolution" },
    { value: "centrifugation", label: "Centrifugation" },
    { value: "filtration", label: "Filtration" },
    { value: "aliquoting", label: "Aliquoting" },
    { value: "dilution", label: "Dilution / Sample Preparation" },
    { value: "derivatization", label: "Derivatization" },
    { value: "sonication", label: "Sonication" },
  ];

  // Load samples and reference data
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadReagents();
    loadSampleTypes();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              sampleItemId: sample.sampleItemId, // Keep sampleItemId for backend API calls
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              sampleCategory: sample.data?.sampleCategory,
              sampleMaterial: sample.data?.sampleMaterial,
              lotNumber: sample.data?.lotNumber,
              processingMethod: sample.data?.processingMethod,
              technicianName: sample.data?.technicianName,
              processingDate: sample.data?.processingDate,
              hasChildren: sample.hasChildren || false,
              childAliquotCount: sample.childAliquotCount || 0,
              isAliquot: sample.isAliquot || false,
              parentSampleItemId: sample.parentSampleItemId,
              parentExternalId: sample.parentExternalId,
              isLocked: sample.data?.isLocked || false,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const loadReagents = useCallback(() => {
    setLoadingReagents(true);
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setReagents(
              response.map((r) => ({
                id: r.id,
                label: `${r.name} (Lot: ${r.lotNumber || "N/A"})`,
                name: r.name,
                lotNumber: r.lotNumber,
                ...r,
              })),
            );
          } else {
            // Mock data for pharma reagents
            setReagents([
              {
                id: "1",
                label: "HPLC Grade Methanol (Lot: MeOH-2024-01)",
                name: "HPLC Grade Methanol",
                lotNumber: "MeOH-2024-01",
              },
              {
                id: "2",
                label: "HPLC Grade Acetonitrile (Lot: ACN-2024-02)",
                name: "HPLC Grade Acetonitrile",
                lotNumber: "ACN-2024-02",
              },
              {
                id: "3",
                label: "Phosphate Buffer pH 7.4 (Lot: PB74-2024-03)",
                name: "Phosphate Buffer pH 7.4",
                lotNumber: "PB74-2024-03",
              },
              {
                id: "4",
                label: "Mobile Phase A (Lot: MPA-2024-04)",
                name: "Mobile Phase A",
                lotNumber: "MPA-2024-04",
              },
              {
                id: "5",
                label: "Mobile Phase B (Lot: MPB-2024-05)",
                name: "Mobile Phase B",
                lotNumber: "MPB-2024-05",
              },
            ]);
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  const loadInstruments = useCallback(() => {
    // If template has configured instruments, use those exclusively
    if (templateInstruments && templateInstruments.length > 0) {
      setInstruments(
        templateInstruments.map((analyzer) => ({
          id: analyzer.id,
          label: analyzer.value,
          name: analyzer.value,
        })),
      );
      setLoadingInstruments(false);
      return;
    }

    // Fallback: load from inventory if no template instruments configured
    setLoadingInstruments(true);
    getFromOpenElisServer(
      "/rest/inventory/instruments?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setInstruments(
              response.map((i) => ({
                id: i.id,
                label: `${i.name} (${i.serialNumber || "N/A"})`,
                name: i.name,
                serialNumber: i.serialNumber,
                ...i,
              })),
            );
          } else {
            // Mock data for pharma instruments (development fallback)
            setInstruments([
              {
                id: "1",
                label: "Analytical Balance (SN: AB-001)",
                name: "Analytical Balance",
                serialNumber: "AB-001",
              },
              {
                id: "2",
                label: "HPLC System (SN: HPLC-002)",
                name: "HPLC System",
                serialNumber: "HPLC-002",
              },
              {
                id: "3",
                label: "UV-Vis Spectrophotometer (SN: UV-003)",
                name: "UV-Vis Spectrophotometer",
                serialNumber: "UV-003",
              },
              {
                id: "4",
                label: "Dissolution Apparatus (SN: DA-004)",
                name: "Dissolution Apparatus",
                serialNumber: "DA-004",
              },
              {
                id: "5",
                label: "Centrifuge (SN: CF-005)",
                name: "Centrifuge",
                serialNumber: "CF-005",
              },
            ]);
          }
          setLoadingInstruments(false);
        }
      },
    );
  }, [templateInstruments]);

  // Load instruments on mount and when template instruments change
  useEffect(() => {
    loadInstruments();
  }, [loadInstruments]);

  const loadSampleTypes = useCallback(() => {
    getFromOpenElisServer(
      "/rest/displayList/SAMPLE_TYPE_ACTIVE",
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setSampleTypes(response);
        }
      },
    );
  }, []);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Reset bulk prepare values
  const resetBulkPrepareValues = () => {
    setBulkPrepareValues({
      processingMethod: "",
      technicianName: "",
      processingDate: new Date().toISOString().slice(0, 10),
      selectedReagents: [],
      selectedInstruments: [],
      batchNumber: "",
      lotNumber: "",
      notes: "",
    });
  };

  // Handle bulk prepare (apply processing details)
  const handleBulkPrepare = useCallback(() => {
    if (selectedParentIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.processing.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    if (!bulkPrepareValues.processingMethod) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.processing.error.noMethod",
          defaultMessage: "Processing method is required.",
        }),
      );
      return;
    }

    setIsBulkApplying(true);
    setError(null);

    // Convert selected IDs to sampleItemIds for the backend API
    const sampleItemIds = selectedParentIds
      .map((id) => {
        const sample = samples.find((s) => s.id === id);
        return sample?.sampleItemId;
      })
      .filter((id) => id != null);

    const applyData = {
      sampleIds: sampleItemIds,
      data: {
        processingMethod: bulkPrepareValues.processingMethod,
        technicianName: bulkPrepareValues.technicianName,
        processingDate: bulkPrepareValues.processingDate,
        selectedReagents: bulkPrepareValues.selectedReagents,
        selectedInstruments: bulkPrepareValues.selectedInstruments,
        batchNumber: bulkPrepareValues.batchNumber,
        lotNumber: bulkPrepareValues.lotNumber,
        notes: bulkPrepareValues.notes,
        isLocked: true,
      },
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(applyData),
      (status) => {
        setIsBulkApplying(false);
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.page.pharma.processing.success.prepared",
                defaultMessage:
                  "Applied processing preparation to {count} sample(s).",
              },
              { count: selectedParentIds.length },
            ),
          );
          setBulkPrepareModalOpen(false);
          setSelectedParentIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.pharma.processing.error.apply",
              defaultMessage: "Failed to apply values. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedParentIds,
    hasRealPageId,
    bulkPrepareValues,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Handle open create aliquot modal
  const handleOpenCreateModal = useCallback(() => {
    if (selectedParentIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.processing.error.noSelection",
          defaultMessage: "Please select at least one parent sample.",
        }),
      );
      return;
    }
    setCreateModalOpen(true);
  }, [selectedParentIds, intl]);

  // Handle create children/aliquots
  const handleCreateChildren = useCallback(() => {
    if (selectedParentIds.length === 0 || !hasRealPageId) return;

    if (!notebookId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.processing.error.noNotebook",
          defaultMessage: "Notebook ID is required to create aliquots.",
        }),
      );
      return;
    }

    setCreating(true);
    setError(null);

    // Convert selected IDs to sampleItemIds for the backend API
    const parentSampleItemIds = selectedParentIds
      .map((id) => {
        const sample = samples.find((s) => s.id === id);
        return sample?.sampleItemId;
      })
      .filter((id) => id != null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/create-children`,
      JSON.stringify({
        parentSampleIds: parentSampleItemIds,
        childCountPerParent: childCount,
        externalIdPrefix: externalIdPrefix,
        pageId: pageData?.id,
      }),
      (response) => {
        setCreating(false);
        setCreateModalOpen(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.page.pharma.processing.success.created",
                defaultMessage:
                  "Successfully created {count} aliquots/child samples.",
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
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.pharma.processing.error.create",
                defaultMessage: "Failed to create aliquots.",
              }),
          );
        }
      },
    );
  }, [
    selectedParentIds,
    hasRealPageId,
    notebookId,
    childCount,
    externalIdPrefix,
    pageData?.id,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle view children
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

  // Handle bulk mark as processed
  const handleBulkMarkProcessed = useCallback(() => {
    if (selectedParentIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.error.noPage",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    // Check if all selected samples have processing method
    const selectedSamples = samples.filter((s) =>
      selectedParentIds.includes(s.id),
    );
    const missingMethod = selectedSamples.filter((s) => !s.processingMethod);
    if (missingMethod.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.pharma.processing.error.missingMethod",
            defaultMessage:
              "{count} sample(s) are missing processing method. Please complete preparation first.",
          },
          { count: missingMethod.length },
        ),
      );
      return;
    }

    // Convert selected IDs to sampleItemIds for the backend API
    const statusSampleItemIds = selectedParentIds
      .map((id) => {
        const sample = samples.find((s) => s.id === id);
        return sample?.sampleItemId;
      })
      .filter((id) => id != null);

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: statusSampleItemIds,
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.page.pharma.processing.success.processed",
                defaultMessage:
                  "Marked {count} sample(s) as Processing Complete.",
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
          setError(
            intl.formatMessage({
              id: "notebook.page.pharma.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedParentIds,
    samples,
    pageData?.id,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Calculate stats
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const preparedCount = samples.filter((s) => s.processingMethod).length;
  const lockedCount = samples.filter((s) => s.isLocked).length;
  const totalAliquots = samples.reduce(
    (sum, s) => sum + (s.childAliquotCount || 0),
    0,
  );

  // Get processing method label
  const getProcessingMethodLabel = (method) => {
    const found = processingMethods.find((m) => m.value === method);
    return found ? found.label : method;
  };

  // Get processing tag
  const getProcessingTag = (sample) => {
    if (sample.processingMethod) {
      return (
        <Tag type="blue">
          {getProcessingMethodLabel(sample.processingMethod)}
        </Tag>
      );
    }
    return <Tag type="gray">Not Set</Tag>;
  };

  // Render children action button
  const renderChildrenAction = (sample) => {
    return (
      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
        {sample.childAliquotCount > 0 && (
          <Tag type="blue" size="sm">
            {sample.childAliquotCount}
          </Tag>
        )}
        <Button
          kind="ghost"
          size="sm"
          hasIconOnly
          iconDescription={intl.formatMessage({
            id: "notebook.page.pharma.processing.viewChildren",
            defaultMessage: "View Aliquots",
          })}
          renderIcon={View}
          onClick={() => handleViewChildren(sample.id)}
        />
      </div>
    );
  };

  return (
    <div className="pharma-processing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.processing.title"
            defaultMessage="Sample Processing &amp; Aliquoting"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.processing.description"
            defaultMessage="Prepare samples for downstream assays. Apply processing details (method, technician, reagents, instruments), create aliquots, and track parent-child traceability."
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
                  id="notebook.page.pharma.processing.parentSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.processing.prepared"
                  defaultMessage="Prepared"
                />
              </span>
              <span className="progress-value">{preparedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.processing.pending"
                  defaultMessage="Pending Prep"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.processing.locked"
                  defaultMessage="Locked"
                />
              </span>
              <span className="progress-value">{lockedCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.processing.totalAliquots"
                  defaultMessage="Aliquots"
                />
              </span>
              <span className="progress-value">{totalAliquots}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={() => {
            resetBulkPrepareValues();
            setBulkPrepareModalOpen(true);
          }}
          disabled={selectedParentIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.pharma.processing.bulkPrepare"
            defaultMessage="Bulk Prepare ({count})"
            values={{ count: selectedParentIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Add}
          onClick={handleOpenCreateModal}
          disabled={selectedParentIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.pharma.processing.createAliquots"
            defaultMessage="Create Aliquots ({count})"
            values={{ count: selectedParentIds.length }}
          />
        </Button>

        {selectedParentIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={ArrowRight}
            onClick={handleBulkMarkProcessed}
          >
            <FormattedMessage
              id="notebook.page.pharma.processing.markComplete"
              defaultMessage="Mark Complete ({count})"
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
            id="notebook.page.pharma.processing.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

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

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="pharma-processing"
          samples={samples}
          selectedIds={selectedParentIds}
          onSelectionChange={setSelectedParentIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          showHierarchy={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "sampleType", header: "Sample Type" },
            {
              key: "processingMethod",
              header: "Processing Method",
              render: (value, row) => getProcessingTag(row),
            },
            { key: "technicianName", header: "Technician" },
            {
              key: "isLocked",
              header: "Locked",
              render: (value) =>
                value ? <Tag type="purple">Locked</Tag> : "-",
            },
            { key: "status", header: "Status" },
          ]}
          additionalColumns={[
            {
              key: "aliquots",
              header: "Aliquots",
              render: renderChildrenAction,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.pharma.processing.empty"
              defaultMessage="No samples available for processing. Please complete Sample Creation and QC first."
            />
          </p>
        </div>
      )}

      {/* Bulk Prepare Modal */}
      <Modal
        open={bulkPrepareModalOpen}
        onRequestClose={() => setBulkPrepareModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.bulkPrepare.title",
          defaultMessage: "Bulk Processing Preparation",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : intl.formatMessage({ id: "label.apply", defaultMessage: "Apply" })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkPrepare}
        onSecondarySubmit={() => setBulkPrepareModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isBulkApplying}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.pharma.bulkPrepare.description"
            defaultMessage="Apply processing preparation values to {count} selected sample(s). Samples will be locked for processing."
            values={{ count: selectedParentIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Processing Details */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="notebook.pharma.bulkPrepare.section.processing"
                defaultMessage="Processing Details"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="processingMethod"
              labelText={intl.formatMessage({
                id: "notebook.pharma.processingMethod",
                defaultMessage: "Processing Method *",
              })}
              value={bulkPrepareValues.processingMethod}
              onChange={(e) =>
                setBulkPrepareValues((prev) => ({
                  ...prev,
                  processingMethod: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select processing method..." />
              {processingMethods.map((method) => (
                <SelectItem
                  key={method.value}
                  value={method.value}
                  text={method.label}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="technicianName"
              labelText={intl.formatMessage({
                id: "notebook.pharma.technicianName",
                defaultMessage: "Technician Name",
              })}
              value={bulkPrepareValues.technicianName}
              onChange={(e) =>
                setBulkPrepareValues((prev) => ({
                  ...prev,
                  technicianName: e.target.value,
                }))
              }
              placeholder="Enter technician name"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="notebook.pharma.processingDate"
                  defaultMessage="Processing Date"
                />
              </label>
              <input
                type="date"
                className="cds--text-input"
                value={bulkPrepareValues.processingDate}
                onChange={(e) =>
                  setBulkPrepareValues((prev) => ({
                    ...prev,
                    processingDate: e.target.value,
                  }))
                }
              />
            </div>
          </Column>

          {/* Reagent & Instrument Selection */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <InventoryManagement
                size={16}
                style={{ marginRight: "0.5rem" }}
              />
              <FormattedMessage
                id="notebook.pharma.bulkPrepare.section.inventory"
                defaultMessage="Reagent & Instrument Selection"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <MultiSelect
              id="selectedReagents"
              titleText={intl.formatMessage({
                id: "notebook.pharma.reagents",
                defaultMessage: "Reagents",
              })}
              label="Select reagents..."
              items={reagents}
              itemToString={(item) => (item ? item.label : "")}
              selectedItems={reagents.filter((r) =>
                bulkPrepareValues.selectedReagents.includes(r.id),
              )}
              onChange={({ selectedItems }) =>
                setBulkPrepareValues((prev) => ({
                  ...prev,
                  selectedReagents: selectedItems.map((i) => i.id),
                }))
              }
              disabled={loadingReagents}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <MultiSelect
              id="selectedInstruments"
              titleText={intl.formatMessage({
                id: "notebook.pharma.instruments",
                defaultMessage: "Instruments",
              })}
              label="Select instruments..."
              items={instruments}
              itemToString={(item) => (item ? item.label : "")}
              selectedItems={instruments.filter((i) =>
                bulkPrepareValues.selectedInstruments.includes(i.id),
              )}
              onChange={({ selectedItems }) =>
                setBulkPrepareValues((prev) => ({
                  ...prev,
                  selectedInstruments: selectedItems.map((i) => i.id),
                }))
              }
              disabled={loadingInstruments}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="batchNumber"
              labelText={intl.formatMessage({
                id: "notebook.pharma.batchNumber",
                defaultMessage: "Batch Number",
              })}
              value={bulkPrepareValues.batchNumber}
              onChange={(e) =>
                setBulkPrepareValues((prev) => ({
                  ...prev,
                  batchNumber: e.target.value,
                }))
              }
              placeholder="e.g., BATCH-2025-001"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="lotNumber"
              labelText={intl.formatMessage({
                id: "notebook.pharma.lotNumber",
                defaultMessage: "Lot Number",
              })}
              value={bulkPrepareValues.lotNumber}
              onChange={(e) =>
                setBulkPrepareValues((prev) => ({
                  ...prev,
                  lotNumber: e.target.value,
                }))
              }
              placeholder="e.g., LOT-2025-001"
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="processingNotes"
              labelText={intl.formatMessage({
                id: "notebook.pharma.notes",
                defaultMessage: "Notes",
              })}
              value={bulkPrepareValues.notes}
              onChange={(e) =>
                setBulkPrepareValues((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Optional processing notes..."
            />
          </Column>
        </Grid>
      </Modal>

      {/* Create Aliquots Modal */}
      <Modal
        open={createModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.pharma.processing.modal.title",
          defaultMessage: "Create Aliquots / Child Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.page.pharma.processing.modal.create",
          defaultMessage: "Create",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.page.pharma.processing.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setCreateModalOpen(false)}
        onRequestSubmit={handleCreateChildren}
        primaryButtonDisabled={creating}
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.pharma.processing.modal.description"
              defaultMessage="Create aliquots from {count} selected parent sample(s). Each aliquot will maintain parent-child traceability."
              values={{ count: selectedParentIds.length }}
            />
          </p>
        </div>

        <NumberInput
          id="childCount"
          label={intl.formatMessage({
            id: "notebook.page.pharma.processing.modal.count",
            defaultMessage: "Aliquots per Parent",
          })}
          value={childCount}
          onChange={(e, { value }) => setChildCount(value)}
          min={1}
          max={20}
          step={1}
          style={{ marginBottom: "1rem" }}
        />

        <TextInput
          id="externalIdPrefix"
          labelText={intl.formatMessage({
            id: "notebook.page.pharma.processing.modal.prefix",
            defaultMessage: "External ID Prefix",
          })}
          value={externalIdPrefix}
          onChange={(e) => setExternalIdPrefix(e.target.value)}
          helperText={intl.formatMessage(
            {
              id: "notebook.page.pharma.processing.modal.prefixHelp",
              defaultMessage: "Aliquots will be named: {prefix}-{year}-{seq}",
            },
            {
              prefix: externalIdPrefix || "PREFIX",
              year: new Date().getFullYear(),
              seq: "001",
            },
          )}
        />

        <div style={{ marginTop: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.pharma.processing.modal.total"
              defaultMessage="Total aliquots to create: {total}"
              values={{ total: selectedParentIds.length * childCount }}
            />
          </p>
        </div>
      </Modal>

      {/* View Children Modal */}
      <Modal
        open={viewChildrenModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.pharma.processing.viewModal.title",
          defaultMessage: "Aliquots / Child Samples",
        })}
        passiveModal
        onRequestClose={() => {
          setViewChildrenModalOpen(false);
          setParentChildren([]);
        }}
      >
        {loadingChildren ? (
          <p>
            <FormattedMessage
              id="notebook.page.pharma.processing.loading"
              defaultMessage="Loading..."
            />
          </p>
        ) : parentChildren.length === 0 ? (
          <p>
            <FormattedMessage
              id="notebook.page.pharma.processing.viewModal.noChildren"
              defaultMessage="No aliquots found for this parent sample."
            />
          </p>
        ) : (
          <DataTable
            rows={parentChildren.map((child) => ({
              id: String(child.id),
              externalId: child.externalId || "-",
              sampleType: child.sampleType || "-",
              processingMethod: child.processingMethod || "-",
              status: child.status || "PENDING",
            }))}
            headers={[
              { key: "externalId", header: "External ID" },
              { key: "sampleType", header: "Sample Type" },
              { key: "processingMethod", header: "Processing Method" },
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
    </div>
  );
}

export default PharmaceuticalProcessingPage;
