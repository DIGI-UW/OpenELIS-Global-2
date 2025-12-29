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
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  Tag,
} from "@carbon/react";
import {
  Archive,
  Checkmark,
  Edit,
  Location,
  Automatic,
  Renew,
  Temperature,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDTemporaryStoragePage - Page 3 of the MNTD workflow.
 * Handles temporary storage assignment for samples.
 *
 * Data Points:
 * - Storage Hierarchy: Room, Freezer, Storage type, Rack, Box, Well/position
 *
 * Note: Environmental monitoring (temperature logging) has been moved to
 * Page 11 (MNTDEnvironmentalMonitoringPage) for flexible access control.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDTemporaryStoragePage({
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

  // Storage hierarchy state
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [boxLayout, setBoxLayout] = useState({});

  // Storage assignment modal state (unified single modal like Pharmaceutical)
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [selectedWell, setSelectedWell] = useState(null);
  const [isAssigning, setIsAssigning] = useState(false);
  const [wellAssignments, setWellAssignments] = useState({});
  const [isReassignment, setIsReassignment] = useState(false);
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [bulkAssignValues, setBulkAssignValues] = useState({
    storageType: "",
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
    temperatureMonitoringConfirmed: false,
  });

  // Reassignment confirmation modal state
  const [confirmReassignModalOpen, setConfirmReassignModalOpen] =
    useState(false);
  const [samplesToReassign, setSamplesToReassign] = useState([]);

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
              // Storage assignment fields from data
              storageRoom: sample.data?.storageRoom,
              storageFreezer: sample.data?.storageFreezer,
              storageType: sample.data?.storageType,
              storageRack: sample.data?.storageRack,
              storageBox: sample.data?.storageBox,
              storageWell: sample.data?.storageWell,
              storagePath: sample.data?.storagePath,
              assignedBy: sample.data?.assignedBy,
              assignedDateTime: sample.data?.assignedDateTime,
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

  // Load box occupancy from storage API
  const loadBoxOccupancy = useCallback((boxId) => {
    if (!boxId) return;

    getFromOpenElisServer(
      `/rest/storage/boxes/${boxId}/occupancy`,
      (response) => {
        if (componentMounted.current && response) {
          // Convert occupiedCoordinates map to the format expected by BoxLayoutViewer
          // occupiedCoordinates is { "A1": { sampleId: "...", accessionNumber: "..." }, ... }
          const occupiedCoordinates = response.occupiedCoordinates || {};
          setBoxLayout(occupiedCoordinates);
        }
      },
    );
  }, []);

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = useCallback(
    (selection) => {
      setStorageSelection(selection);
      setWellAssignments({}); // Reset well assignments when storage location changes
      // When box selection changes, load its occupancy from storage system
      if (selection.box?.id) {
        loadBoxOccupancy(selection.box.id);
      } else {
        setBoxLayout({});
      }
    },
    [loadBoxOccupancy],
  );

  // Handle box layout loaded (from StorageHierarchySelector - fallback)
  const handleBoxLayoutLoaded = useCallback(
    (wells) => {
      // Only use if we don't already have layout from storage API
      if (Object.keys(boxLayout).length === 0) {
        setBoxLayout(wells || {});
      }
    },
    [boxLayout],
  );

  // Handle well click from BoxLayoutViewer
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        // Well is occupied by existing sample
        setError(
          intl.formatMessage(
            {
              id: "notebook.mntd.storage.wellOccupied",
              defaultMessage:
                "Well {well} is already occupied by {sample}. Choose another position.",
            },
            { well: wellCoord, sample: wellInfo.externalId || "a sample" },
          ),
        );
        return;
      }

      if (storageModalOpen) {
        // Single well assignment during modal - assign to next unassigned sample
        const unassignedSamples = selectedSampleIds.filter(
          (id) => !wellAssignments[id],
        );
        if (unassignedSamples.length > 0) {
          setWellAssignments((prev) => ({
            ...prev,
            [unassignedSamples[0]]: wellCoord,
          }));
        }
      } else {
        // Quick assignment (outside modal)
        if (selectedSampleIds.length === 0) {
          setError(
            intl.formatMessage({
              id: "notebook.mntd.storage.selectSamplesFirst",
              defaultMessage:
                "Please select samples to assign to storage first.",
            }),
          );
          return;
        }

        setSelectedWell(wellCoord);
        setStorageModalOpen(true);
      }
    },
    [selectedSampleIds, wellAssignments, storageModalOpen, intl],
  );

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.storage.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    // Check if any selected samples already have storage
    const alreadyAssigned = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.storageWell,
    );

    if (alreadyAssigned.length > 0) {
      setSamplesToReassign(alreadyAssigned);
      setConfirmReassignModalOpen(true);
      return;
    }

    openStorageAssignmentModal(false);
  };

  const openStorageAssignmentModal = (reassigning) => {
    setIsReassignment(reassigning);
    setStorageModalOpen(true);
    setError(null);
    setWellAssignments({});
    setSelectedCondition(null);
    setRetentionYears(5);
    setBulkAssignValues({
      storageType: "",
      assignedBy: "",
      assignedDateTime: new Date().toISOString().slice(0, 16),
      notes: "",
      temperatureMonitoringConfirmed: false,
    });
  };

  const handleConfirmReassignment = () => {
    setConfirmReassignModalOpen(false);
    openStorageAssignmentModal(true);
  };

  // Auto-populate wells with selected samples
  const handleAutoPopulate = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.storage.selectBoxFirst",
          defaultMessage: "Please select a storage box first.",
        }),
      );
      return;
    }

    const rows = storageSelection.box.rows || 8;
    const columns = storageSelection.box.columns || 12;
    const rowLetters = Array.from({ length: rows }, (_, i) =>
      String.fromCharCode("A".charCodeAt(0) + i),
    );

    const newAssignments = {};
    let sampleIndex = 0;

    for (let row of rowLetters) {
      for (let col = 1; col <= columns; col++) {
        if (sampleIndex >= selectedSampleIds.length) break;

        const wellCoord = `${row}${col}`;
        if (!boxLayout[wellCoord]) {
          newAssignments[selectedSampleIds[sampleIndex]] = wellCoord;
          sampleIndex++;
        }
      }
      if (sampleIndex >= selectedSampleIds.length) break;
    }

    setWellAssignments(newAssignments);

    if (sampleIndex < selectedSampleIds.length) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.mntd.storage.notEnoughWells",
            defaultMessage:
              "Not enough empty wells. {assigned} of {total} samples assigned.",
          },
          { assigned: sampleIndex, total: selectedSampleIds.length },
        ),
      );
    } else {
      setError(null);
      setSuccessMessage(
        intl.formatMessage(
          {
            id: "notebook.mntd.storage.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Build combined layout for visualization (existing + pending assignments)
  const getCombinedLayout = () => {
    const combined = { ...boxLayout };

    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      if (!combined[wellCoord]) {
        const sample = samples.find((s) => s.id === sampleId);
        combined[wellCoord] = {
          sampleItemId: sampleId,
          externalId: sample?.externalId || sampleId,
          pending: true,
        };
      }
    });

    return combined;
  };

  // Handle bulk storage assignment (unified modal submit)
  const handleAssignStorage = useCallback(() => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.storage.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.storage.noWellAssignments",
          defaultMessage:
            "Please assign samples to wells using Auto-Populate or click on wells.",
        }),
      );
      return;
    }

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    setIsAssigning(true);
    setError(null);

    // Build storage path
    const storagePath = [
      storageSelection.room?.label,
      storageSelection.device?.label,
      storageSelection.shelf?.label,
      storageSelection.rack?.label,
      storageSelection.box?.label,
    ]
      .filter(Boolean)
      .join(" > ");

    // Prepare the data to apply - using wellAssignments for individual sample placements
    const assignData = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      boxId: storageSelection.box?.id,
      wellAssignments: wellAssignments,
      reassign: isReassignment,
      data: {
        storageRoom: storageSelection.room?.label,
        storageFreezer: storageSelection.device?.label,
        storageRack: storageSelection.rack?.label,
        storageBox: storageSelection.box?.label,
        storagePath: storagePath,
        assignedDateTime: new Date().toISOString(),
      },
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/storage`,
      JSON.stringify(assignData),
      (response) => {
        setIsAssigning(false);
        const assignedCount =
          response?.assignedCount || Object.keys(wellAssignments).length;
        const hasErrors =
          response?.errors &&
          Array.isArray(response.errors) &&
          response.errors.length > 0;

        // Check for success
        if (response && (response.success || assignedCount > 0)) {
          const messageId = isReassignment
            ? "notebook.mntd.storage.reassignSuccess"
            : "notebook.mntd.storage.assignSuccess";
          const defaultMessage = isReassignment
            ? "Successfully reassigned {count} samples to new storage location."
            : "Successfully assigned {count} samples to storage.";

          setSuccessMessage(
            intl.formatMessage(
              { id: messageId, defaultMessage: defaultMessage },
              { count: assignedCount },
            ),
          );

          if (hasErrors) {
            setError(
              `Some samples could not be assigned: ${response.errors.join("; ")}`,
            );
          }

          setIsReassignment(false);
          setStorageModalOpen(false);
          setSelectedSampleIds([]);
          setWellAssignments({});
          loadPageSamples();
          // Reload box layout from storage API
          if (storageSelection.box) {
            loadBoxOccupancy(storageSelection.box.id);
          }
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          // No samples assigned - show error
          let errorMessage =
            response?.error || "Failed to assign storage. Please try again.";
          if (hasErrors) {
            errorMessage = response.errors.join("; ");
          }
          setError(errorMessage);
        }
      },
    );
  }, [
    pageData?.id,
    storageSelection,
    wellAssignments,
    isReassignment,
    intl,
    loadPageSamples,
    loadBoxOccupancy,
    onProgressUpdate,
  ]);

  // Handle marking samples as stored
  const handleMarkStored = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    // Check if all selected samples have storage assignment
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingStorage = selectedSamples.filter((s) => !s.storageWell);
    if (missingStorage.length > 0) {
      setError(
        `${missingStorage.length} sample(s) are missing storage assignment. Please assign storage first.`,
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
            intl.formatMessage(
              {
                id: "notebook.page.mntd.completedSuccess",
                defaultMessage:
                  "{count} sample(s) completed and sent to next workflow step.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.mntd.completedError",
              defaultMessage: "Failed to complete samples. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Calculate stats
  const storedCount = samples.filter((s) => s.storageWell).length;
  const pendingCount = samples.filter((s) => !s.storageWell).length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;

  // Calculate preview wells for auto-assignment
  const getAutoAssignPreview = useCallback(() => {
    if (!storageSelection.box || selectedSampleIds.length === 0) {
      return { previewWells: [], availableCount: 0 };
    }

    const rows = storageSelection.box?.rows || 8;
    const columns = storageSelection.box?.columns || 12;
    const occupiedWells = new Set(Object.keys(boxLayout));

    // Generate all wells in order (A1, A2, ..., A12, B1, B2, ...)
    const allWells = [];
    for (let row = 0; row < rows; row++) {
      const rowLetter = String.fromCharCode("A".charCodeAt(0) + row);
      for (let col = 1; col <= columns; col++) {
        allWells.push(`${rowLetter}${col}`);
      }
    }

    // Find available wells
    const availableWells = allWells.filter((well) => !occupiedWells.has(well));

    // Get preview wells (wells that will be assigned)
    const previewWells = availableWells.slice(0, selectedSampleIds.length);

    return {
      previewWells,
      availableCount: availableWells.length,
      totalWells: rows * columns,
      occupiedCount: occupiedWells.size,
    };
  }, [storageSelection.box, selectedSampleIds, boxLayout]);

  const autoAssignPreview = getAutoAssignPreview();

  // Get storage status tag
  const getStorageTag = (sample) => {
    if (sample.status === "COMPLETED" && sample.storageWell) {
      return (
        <Tag type="green" renderIcon={Checkmark} title={sample.storagePath}>
          {sample.storageWell} (
          <FormattedMessage
            id="notebook.status.sentToNext"
            defaultMessage="Sent"
          />
          )
        </Tag>
      );
    }
    if (sample.storageWell) {
      return (
        <Tag type="cyan" renderIcon={Archive} title={sample.storagePath}>
          {sample.storageWell} (
          <FormattedMessage
            id="notebook.status.inProgress"
            defaultMessage="In Progress"
          />
          )
        </Tag>
      );
    }
    return (
      <Tag type="gray">
        <FormattedMessage
          id="notebook.status.awaitingStorage"
          defaultMessage="Awaiting Storage"
        />
      </Tag>
    );
  };

  // Get storage condition tag
  const getConditionTag = (sample) => {
    if (!sample.storageCondition) return null;

    const conditionLabels = {
      ROOM_TEMP: "15-25°C",
      REFRIGERATED: "2-8°C",
      FROZEN_MINUS20: "-20°C",
      FROZEN_MINUS80: "-80°C",
      LIQUID_NITROGEN: "LN2",
      INCUBATOR_37: "37°C",
    };

    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {conditionLabels[sample.storageCondition] || sample.storageCondition}
      </Tag>
    );
  };

  // Check if page has real ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  return (
    <div className="mntd-temporary-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.temporaryStorage.title"
            defaultMessage="Temporary Storage Assignment"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.temporaryStorage.description"
            defaultMessage="Assign samples to temporary storage locations. Samples remain in progress after storage assignment. Use 'Complete & Send to Next Step' to finalize and advance samples to the next workflow stage."
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
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.awaitingStorage"
                  defaultMessage="Awaiting Storage"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.inStorage"
                  defaultMessage="In Storage (In Progress)"
                />
              </span>
              <span className="progress-value">
                {storedCount - completedCount}
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.sentToNextStep"
                  defaultMessage="Sent to Next Step"
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
          renderIcon={Archive}
          onClick={handleOpenStorageModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.mntd.assignToStorage"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Checkmark}
          onClick={handleMarkStored}
          disabled={
            selectedSampleIds.length === 0 || isAssigning || !hasRealPageId
          }
        >
          <FormattedMessage
            id="notebook.page.mntd.markCompleteAndAdvance"
            defaultMessage="Complete & Send to Next Step ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.page.mntd.refresh"
            defaultMessage="Refresh"
          />
        </Button>
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
      <div className="sample-grid-container" style={{ marginTop: "1.5rem" }}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="notebook.page.mntd.sampleList"
            defaultMessage="Sample List"
          />
        </h5>
        <SampleGrid
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            {
              key: "externalId",
              header: intl.formatMessage({
                id: "notebook.column.externalId",
                defaultMessage: "Sample ID",
              }),
            },
            {
              key: "sampleType",
              header: intl.formatMessage({
                id: "notebook.column.sampleType",
                defaultMessage: "Sample Type",
              }),
            },
            {
              key: "storage",
              header: intl.formatMessage({
                id: "notebook.column.storage",
                defaultMessage: "Storage Location",
              }),
              render: (value, row) => (
                <div
                  style={{ display: "flex", gap: "4px", alignItems: "center" }}
                >
                  {getStorageTag(row)}
                  {getConditionTag(row)}
                </div>
              ),
            },
            {
              key: "retentionExpiry",
              header: intl.formatMessage({
                id: "notebook.column.expiry",
                defaultMessage: "Retention Expiry",
              }),
              render: (value, row) =>
                row.retentionExpiry ? (
                  <span>{row.retentionExpiry}</span>
                ) : (
                  <span style={{ color: "#8d8d8d" }}>-</span>
                ),
            },
            {
              key: "assignedBy",
              header: intl.formatMessage({
                id: "notebook.column.assignedBy",
                defaultMessage: "Assigned By",
              }),
            },
            {
              key: "status",
              header: intl.formatMessage({
                id: "notebook.column.status",
                defaultMessage: "Status",
              }),
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.mntd.temporaryStorage.empty"
              defaultMessage="No samples available for storage assignment. Samples must pass verification in the previous step first."
            />
          </p>
        </div>
      )}

      {/* Unified Storage Assignment Modal (like Pharmaceutical page) */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={
          isAssigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.mntd.storage.modal.assign",
                defaultMessage: "Assign",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        primaryButtonDisabled={
          !storageSelection.box ||
          Object.keys(wellAssignments).length === 0 ||
          isAssigning
        }
        size="lg"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.mntd.storage.modal.description"
            defaultMessage="Assign {count} selected samples to storage. Select a storage location, then use Auto-Populate or click wells to assign samples."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Storage Location Selection */}
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Location size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.mntd.storage.storageLocation"
                  defaultMessage="Storage Location"
                />
              </h5>
              <StorageHierarchySelector
                onSelectionChange={handleStorageSelectionChange}
                entryId={entryId}
                onBoxLayoutLoaded={handleBoxLayoutLoaded}
                boxRequired={true}
                showPath={true}
              />
            </div>
          </Column>

          {/* Box Layout Preview with Auto-Populate */}
          <Column lg={8} md={4} sm={4}>
            {storageSelection.box ? (
              <div>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "0.5rem",
                  }}
                >
                  <h5>
                    <Archive size={16} style={{ marginRight: "0.5rem" }} />
                    <FormattedMessage
                      id="notebook.mntd.storage.boxLayout"
                      defaultMessage="Box Layout"
                    />
                  </h5>
                  <Button
                    kind="tertiary"
                    size="sm"
                    renderIcon={Automatic}
                    onClick={handleAutoPopulate}
                    disabled={selectedSampleIds.length === 0}
                  >
                    <FormattedMessage
                      id="notebook.mntd.storage.autoPopulate"
                      defaultMessage="Auto-Populate"
                    />
                  </Button>
                </div>

                <BoxLayoutViewer
                  boxId={storageSelection.box.id}
                  layout={getCombinedLayout()}
                  rows={storageSelection.box.rows || 8}
                  columns={storageSelection.box.columns || 12}
                  onWellClick={handleWellClick}
                />

                <div
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  <FormattedMessage
                    id="notebook.mntd.storage.assignmentSummary"
                    defaultMessage="{assigned} of {total} samples assigned to wells"
                    values={{
                      assigned: Object.keys(wellAssignments).length,
                      total: selectedSampleIds.length,
                    }}
                  />
                </div>
              </div>
            ) : (
              <div
                style={{
                  padding: "2rem",
                  textAlign: "center",
                  backgroundColor: "#f4f4f4",
                  borderRadius: "4px",
                }}
              >
                <Archive size={32} />
                <p style={{ marginTop: "0.5rem", color: "#525252" }}>
                  <FormattedMessage
                    id="notebook.mntd.storage.selectBoxFirst"
                    defaultMessage="Select a storage location to preview box layout"
                  />
                </p>
              </div>
            )}
          </Column>
        </Grid>
      </Modal>

      {/* Reassignment Confirmation Modal */}
      <Modal
        open={confirmReassignModalOpen}
        onRequestClose={() => setConfirmReassignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.storage.reassignConfirm.title",
          defaultMessage: "Confirm Reassignment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.storage.reassignConfirm.confirm",
          defaultMessage: "Reassign",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleConfirmReassignment}
        danger
        size="sm"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.mntd.storage.reassignConfirm.warning"
              defaultMessage="{count} of the selected samples already have storage assignments. Reassigning will remove them from their current locations."
              values={{ count: samplesToReassign.length }}
            />
          </p>

          <div
            style={{
              backgroundColor: "#fff1f1",
              border: "1px solid #da1e28",
              borderRadius: "4px",
              padding: "1rem",
            }}
          >
            <strong style={{ color: "#da1e28" }}>
              <FormattedMessage
                id="notebook.mntd.storage.reassignConfirm.currentLocations"
                defaultMessage="Current storage locations:"
              />
            </strong>
            <ul style={{ marginTop: "0.5rem", paddingLeft: "1.5rem" }}>
              {samplesToReassign.map((sample) => (
                <li key={sample.id} style={{ fontSize: "0.875rem" }}>
                  <strong>{sample.externalId}</strong>: {sample.storageWell}
                  {sample.storageCondition && ` (${sample.storageCondition})`}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default MNTDTemporaryStoragePage;
