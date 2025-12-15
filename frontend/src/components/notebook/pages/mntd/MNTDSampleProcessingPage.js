import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Tag,
  RadioButtonGroup,
  RadioButton,
  Checkbox,
  Dropdown,
  MultiSelect,
} from "@carbon/react";
import {
  Chemistry,
  Checkmark,
  Edit,
  Add,
  Laboratory,
  InventoryManagement,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDSampleProcessingPage - Page 4 of the MNTD workflow.
 * Handles sample processing preparation with bulk value entry.
 *
 * Data Points:
 * - Processing Details: Processing type, Technician name, Processing date
 * - Sample Review: Missed samples from field, Parent sample selection
 * - Reagent & Instrument Selection: Reagents, Instruments, Batch/lot numbers
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDSampleProcessingPage({
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
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Reagents and instruments from inventory
  const [reagents, setReagents] = useState([]);
  const [instruments, setInstruments] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);
  const [loadingInstruments, setLoadingInstruments] = useState(false);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [bulkApplyValues, setBulkApplyValues] = useState({
    processingType: "",
    technicianName: "",
    processingDate: new Date().toISOString().slice(0, 10),
    selectedReagents: [],
    selectedInstruments: [],
    batchNumber: "",
    lotNumber: "",
    notes: "",
  });
  const [isBulkApplying, setIsBulkApplying] = useState(false);

  // Add missed sample modal state
  const [addSampleModalOpen, setAddSampleModalOpen] = useState(false);
  const [newSampleData, setNewSampleData] = useState({
    externalId: "",
    sampleType: "",
    parentSampleId: "",
    notes: "",
  });
  const [isAddingSample, setIsAddingSample] = useState(false);

  // Sample types for dropdown
  const [sampleTypes, setSampleTypes] = useState([]);

  // Processing types
  const processingTypes = [
    { id: "aliquoting", label: "Aliquoting" },
    { id: "dbs_punching", label: "DBS Punching" },
    { id: "extraction", label: "Extraction" },
    { id: "culture", label: "Culture" },
  ];

  // Load samples and reference data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadReagents();
    loadInstruments();
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
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              // Processing preparation fields from data
              processingType: sample.data?.processingType,
              technicianName: sample.data?.technicianName,
              processingDate: sample.data?.processingDate,
              selectedReagents: sample.data?.selectedReagents || [],
              selectedInstruments: sample.data?.selectedInstruments || [],
              batchNumber: sample.data?.batchNumber,
              lotNumber: sample.data?.lotNumber,
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
            // Mock data if no reagents available
            setReagents([
              {
                id: "1",
                label: "DNA Extraction Kit (Lot: EK-2024-001)",
                name: "DNA Extraction Kit",
                lotNumber: "EK-2024-001",
              },
              {
                id: "2",
                label: "RNA Preservation Buffer (Lot: RPB-2024-002)",
                name: "RNA Preservation Buffer",
                lotNumber: "RPB-2024-002",
              },
              {
                id: "3",
                label: "PCR Master Mix (Lot: PM-2024-003)",
                name: "PCR Master Mix",
                lotNumber: "PM-2024-003",
              },
              {
                id: "4",
                label: "Malaria RDT Kit (Lot: MRK-2024-004)",
                name: "Malaria RDT Kit",
                lotNumber: "MRK-2024-004",
              },
            ]);
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  const loadInstruments = useCallback(() => {
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
            // Mock data if no instruments available
            setInstruments([
              {
                id: "1",
                label: "Centrifuge (SN: CF-001)",
                name: "Centrifuge",
                serialNumber: "CF-001",
              },
              {
                id: "2",
                label: "PCR Thermocycler (SN: TC-002)",
                name: "PCR Thermocycler",
                serialNumber: "TC-002",
              },
              {
                id: "3",
                label: "Vortex Mixer (SN: VM-003)",
                name: "Vortex Mixer",
                serialNumber: "VM-003",
              },
              {
                id: "4",
                label: "Micropipette Set (SN: MP-004)",
                name: "Micropipette Set",
                serialNumber: "MP-004",
              },
            ]);
          }
          setLoadingInstruments(false);
        }
      },
    );
  }, []);

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

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError("Please select samples to apply values to.");
      return;
    }

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    if (!bulkApplyValues.processingType) {
      setError("Processing type is required.");
      return;
    }

    setIsBulkApplying(true);
    setError(null);

    // Prepare the data to apply
    const applyData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: {
        processingType: bulkApplyValues.processingType,
        technicianName: bulkApplyValues.technicianName,
        processingDate: bulkApplyValues.processingDate,
        selectedReagents: bulkApplyValues.selectedReagents,
        selectedInstruments: bulkApplyValues.selectedInstruments,
        batchNumber: bulkApplyValues.batchNumber,
        lotNumber: bulkApplyValues.lotNumber,
        notes: bulkApplyValues.notes,
        isLocked: true, // Lock parent sample for processing
      },
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(applyData),
      (status) => {
        setIsBulkApplying(false);
        if (status === 200) {
          setSuccessMessage(
            `Applied processing values to ${selectedSampleIds.length} samples.`,
          );
          setBulkApplyModalOpen(false);
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to apply values. Please try again.");
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    bulkApplyValues,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle marking samples as ready for processing
  const handleMarkReady = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    // Check if all selected samples have processing type
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingType = selectedSamples.filter((s) => !s.processingType);
    if (missingType.length > 0) {
      setError(
        `${missingType.length} sample(s) are missing processing type. Please complete preparation first.`,
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            `Marked ${selectedSampleIds.length} samples as ready for processing.`,
          );
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status.");
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle adding missed sample
  const handleAddMissedSample = useCallback(() => {
    if (!newSampleData.externalId || !newSampleData.sampleType) {
      setError("Sample ID and Sample Type are required.");
      return;
    }

    setIsAddingSample(true);
    setError(null);

    const sampleData = {
      entryId: entryId,
      pageId: pageData?.id,
      externalId: newSampleData.externalId,
      sampleType: newSampleData.sampleType,
      parentSampleId: newSampleData.parentSampleId || null,
      notes: newSampleData.notes,
      source: "MISSED_FROM_FIELD",
    };

    postToOpenElisServer(
      `/rest/notebook-entry/${entryId}/samples/add`,
      JSON.stringify(sampleData),
      (status) => {
        setIsAddingSample(false);
        if (status === 200 || status === 201) {
          setSuccessMessage("Missed sample registered successfully.");
          setAddSampleModalOpen(false);
          setNewSampleData({
            externalId: "",
            sampleType: "",
            parentSampleId: "",
            notes: "",
          });
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to add sample. Please try again.");
        }
      },
    );
  }, [entryId, pageData?.id, newSampleData, loadPageSamples, onProgressUpdate]);

  // Calculate stats
  const preparedCount = samples.filter((s) => s.processingType).length;
  const pendingCount = samples.filter((s) => !s.processingType).length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const lockedCount = samples.filter((s) => s.isLocked).length;

  // Get processing type label
  const getProcessingTypeLabel = (type) => {
    const found = processingTypes.find((t) => t.id === type);
    return found ? found.label : type;
  };

  // Get processing tag
  const getProcessingTag = (sample) => {
    if (sample.processingType) {
      return (
        <Tag type="blue">{getProcessingTypeLabel(sample.processingType)}</Tag>
      );
    }
    return <Tag type="gray">Not Set</Tag>;
  };

  // Reset bulk apply values
  const resetBulkApplyValues = () => {
    setBulkApplyValues({
      processingType: "",
      technicianName: "",
      processingDate: new Date().toISOString().slice(0, 10),
      selectedReagents: [],
      selectedInstruments: [],
      batchNumber: "",
      lotNumber: "",
      notes: "",
    });
  };

  return (
    <div className="mntd-sample-processing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.sampleProcessing.title"
            defaultMessage="Sample Processing Preparation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.sampleProcessing.description"
            defaultMessage="Prepare samples for laboratory processing. Select processing type, technician, and link reagents and instruments. Register any missed samples from field collection."
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
                  id="notebook.page.mntd.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.prepared"
                  defaultMessage="Prepared"
                />
              </span>
              <span className="progress-value">{preparedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.pendingPrep"
                  defaultMessage="Pending Prep"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.locked"
                  defaultMessage="Locked"
                />
              </span>
              <span className="progress-value">{lockedCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.ready"
                  defaultMessage="Ready"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
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
            resetBulkApplyValues();
            setBulkApplyModalOpen(true);
          }}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.mntd.bulkPrepare"
            defaultMessage="Bulk Prepare ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={() => setAddSampleModalOpen(true)}
        >
          <FormattedMessage
            id="notebook.page.mntd.addMissedSample"
            defaultMessage="Add Missed Sample"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkReady}
          >
            <FormattedMessage
              id="notebook.page.mntd.markReady"
              defaultMessage="Mark as Ready ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container" style={{ marginTop: "1rem" }}>
        <SampleGrid
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "externalId", header: "Sample ID" },
            { key: "sampleType", header: "Sample Type" },
            {
              key: "processingType",
              header: "Processing Type",
              render: (value, row) => getProcessingTag(row),
            },
            { key: "technicianName", header: "Technician" },
            { key: "processingDate", header: "Processing Date" },
            {
              key: "isLocked",
              header: "Locked",
              render: (value) =>
                value ? <Tag type="purple">Locked</Tag> : "-",
            },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.mntd.sampleProcessing.empty"
              defaultMessage="No samples available for processing preparation. Samples must be assigned to storage in the previous step first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.bulkPrepare.title",
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
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isBulkApplying}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.mntd.bulkPrepare.description"
            defaultMessage="Apply processing preparation values to {count} selected sample(s). Samples will be locked for processing."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Processing Details */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
              Processing Details
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="processingType"
              labelText={intl.formatMessage({
                id: "notebook.mntd.processingType",
                defaultMessage: "Processing Type *",
              })}
              value={bulkApplyValues.processingType}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  processingType: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select processing type..." />
              {processingTypes.map((type) => (
                <SelectItem key={type.id} value={type.id} text={type.label} />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="technicianName"
              labelText={intl.formatMessage({
                id: "notebook.mntd.technicianName",
                defaultMessage: "Technician Name",
              })}
              value={bulkApplyValues.technicianName}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
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
                  id="notebook.mntd.processingDate"
                  defaultMessage="Processing Date"
                />
              </label>
              <input
                type="date"
                className="cds--text-input"
                value={bulkApplyValues.processingDate}
                onChange={(e) =>
                  setBulkApplyValues((prev) => ({
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
              Reagent & Instrument Selection
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <MultiSelect
              id="selectedReagents"
              titleText={intl.formatMessage({
                id: "notebook.mntd.reagents",
                defaultMessage: "Reagents",
              })}
              label="Select reagents..."
              items={reagents}
              itemToString={(item) => (item ? item.label : "")}
              selectedItems={reagents.filter((r) =>
                bulkApplyValues.selectedReagents.includes(r.id),
              )}
              onChange={({ selectedItems }) =>
                setBulkApplyValues((prev) => ({
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
                id: "notebook.mntd.instruments",
                defaultMessage: "Instruments",
              })}
              label="Select instruments..."
              items={instruments}
              itemToString={(item) => (item ? item.label : "")}
              selectedItems={instruments.filter((i) =>
                bulkApplyValues.selectedInstruments.includes(i.id),
              )}
              onChange={({ selectedItems }) =>
                setBulkApplyValues((prev) => ({
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
                id: "notebook.mntd.batchNumber",
                defaultMessage: "Batch Number (optional)",
              })}
              value={bulkApplyValues.batchNumber}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  batchNumber: e.target.value,
                }))
              }
              placeholder="e.g., BATCH-2024-001"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="lotNumber"
              labelText={intl.formatMessage({
                id: "notebook.mntd.lotNumber",
                defaultMessage: "Lot Number (optional)",
              })}
              value={bulkApplyValues.lotNumber}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  lotNumber: e.target.value,
                }))
              }
              placeholder="e.g., LOT-2024-001"
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="processingNotes"
              labelText={intl.formatMessage({
                id: "notebook.mntd.notes",
                defaultMessage: "Notes",
              })}
              value={bulkApplyValues.notes}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Optional processing notes..."
            />
          </Column>
        </Grid>
      </Modal>

      {/* Add Missed Sample Modal */}
      <Modal
        open={addSampleModalOpen}
        onRequestClose={() => setAddSampleModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.addMissedSample.title",
          defaultMessage: "Register Missed Sample from Field",
        })}
        primaryButtonText={
          isAddingSample
            ? intl.formatMessage({
                id: "label.adding",
                defaultMessage: "Adding...",
              })
            : intl.formatMessage({
                id: "label.add",
                defaultMessage: "Add Sample",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAddMissedSample}
        onSecondarySubmit={() => setAddSampleModalOpen(false)}
        size="md"
        primaryButtonDisabled={isAddingSample}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.mntd.addMissedSample.description"
            defaultMessage="Register a sample that was missed during initial field collection. This sample will be added to the current workflow."
          />
        </p>

        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="newExternalId"
              labelText={intl.formatMessage({
                id: "notebook.mntd.sampleId",
                defaultMessage: "Sample ID *",
              })}
              value={newSampleData.externalId}
              onChange={(e) =>
                setNewSampleData((prev) => ({
                  ...prev,
                  externalId: e.target.value,
                }))
              }
              placeholder="Enter sample ID"
              required
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Select
              id="newSampleType"
              labelText={intl.formatMessage({
                id: "notebook.mntd.sampleType",
                defaultMessage: "Sample Type *",
              })}
              value={newSampleData.sampleType}
              onChange={(e) =>
                setNewSampleData((prev) => ({
                  ...prev,
                  sampleType: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select sample type..." />
              {sampleTypes.map((type) => (
                <SelectItem
                  key={type.id || type.value}
                  value={type.value || type.id}
                  text={type.label || type.value}
                />
              ))}
            </Select>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Select
              id="parentSampleId"
              labelText={intl.formatMessage({
                id: "notebook.mntd.parentSample",
                defaultMessage: "Parent Sample (optional)",
              })}
              value={newSampleData.parentSampleId}
              onChange={(e) =>
                setNewSampleData((prev) => ({
                  ...prev,
                  parentSampleId: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select parent sample..." />
              {samples.map((sample) => (
                <SelectItem
                  key={sample.id}
                  value={sample.id}
                  text={`${sample.externalId} - ${sample.sampleType}`}
                />
              ))}
            </Select>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="missedSampleNotes"
              labelText={intl.formatMessage({
                id: "notebook.mntd.notes",
                defaultMessage: "Notes",
              })}
              value={newSampleData.notes}
              onChange={(e) =>
                setNewSampleData((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Reason for late registration, field notes, etc."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default MNTDSampleProcessingPage;
