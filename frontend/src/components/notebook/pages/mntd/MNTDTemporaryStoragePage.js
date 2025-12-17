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
  RadioButtonGroup,
  RadioButton,
  NumberInput,
  Checkbox,
} from "@carbon/react";
import {
  Archive,
  Temperature,
  Checkmark,
  Edit,
  Location,
  Automatic,
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
 * Handles temporary storage assignment and environmental monitoring.
 *
 * Data Points:
 * - Storage Hierarchy: Room, Freezer, Storage type, Rack, Box, Well/position
 * - Environmental Monitoring: Freezer ID, Temperature check (AM/PM), Temperature value, Checked by
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

  // Storage assignment modal state
  const [assignStorageModalOpen, setAssignStorageModalOpen] = useState(false);
  const [selectedWell, setSelectedWell] = useState(null);
  const [isAssigning, setIsAssigning] = useState(false);

  // Bulk assignment state
  const [bulkAssignValues, setBulkAssignValues] = useState({
    storageType: "", // Raw or Processed
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
    temperatureMonitoringConfirmed: false, // Confirmation that freezer temperature monitoring is done twice daily
  });

  // Auto-assignment modal state
  const [autoAssignModalOpen, setAutoAssignModalOpen] = useState(false);
  const [isAutoAssigning, setIsAutoAssigning] = useState(false);
  const [autoAssignModalError, setAutoAssignModalError] = useState(null);
  const [autoAssignValues, setAutoAssignValues] = useState({
    storageType: "",
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
    temperatureMonitoringConfirmed: false, // Confirmation that freezer temperature monitoring is done twice daily
  });

  // Environmental monitoring modal state
  const [tempMonitoringModalOpen, setTempMonitoringModalOpen] = useState(false);
  const [temperatureLog, setTemperatureLog] = useState({
    freezerId: "",
    checkTime: "AM",
    temperatureValue: "",
    temperatureUnit: "C",
    checkedBy: "",
    checkedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
  });
  const [isLoggingTemp, setIsLoggingTemp] = useState(false);

  // Temperature logs for display
  const [temperatureLogs, setTemperatureLogs] = useState([]);

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadTemperatureLogs();

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

  const loadTemperatureLogs = useCallback(() => {
    if (!entryId) return;

    getFromOpenElisServer(
      `/rest/notebook-entry/${entryId}/temperature-logs`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setTemperatureLogs(response);
        }
      },
    );
  }, [entryId]);

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
      if (selectedSampleIds.length === 0) {
        setError("Please select samples to assign to storage first.");
        return;
      }

      if (wellInfo) {
        // Well is occupied
        setError(`Well ${wellCoord} is already occupied.`);
        return;
      }

      setSelectedWell(wellCoord);
      setAssignStorageModalOpen(true);
    },
    [selectedSampleIds],
  );

  // Handle bulk storage assignment
  const handleAssignStorage = useCallback(() => {
    if (selectedSampleIds.length === 0 || !selectedWell) {
      setError("Please select samples and a storage well.");
      return;
    }

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    if (!storageSelection.box) {
      setError("Please select a storage box first.");
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

    // Prepare the data to apply
    const assignData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: {
        storageRoom: storageSelection.room?.label,
        storageFreezer: storageSelection.device?.label,
        storageType: bulkAssignValues.storageType,
        storageRack: storageSelection.rack?.label,
        storageBox: storageSelection.box?.label,
        storageWell: selectedWell,
        storagePath: storagePath,
        assignedBy: bulkAssignValues.assignedBy,
        assignedDateTime: bulkAssignValues.assignedDateTime,
        notes: bulkAssignValues.notes,
        temperatureMonitoringConfirmed:
          bulkAssignValues.temperatureMonitoringConfirmed,
      },
      boxId: storageSelection.box?.id,
      wellCoordinate: selectedWell,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/storage`,
      JSON.stringify(assignData),
      (response) => {
        setIsAssigning(false);
        const assignedCount = response?.assignedCount || 0;
        const hasErrors =
          response?.errors &&
          Array.isArray(response.errors) &&
          response.errors.length > 0;

        // Check if any samples were assigned successfully
        if (assignedCount > 0) {
          // Some or all samples assigned successfully
          if (hasErrors) {
            // Partial success - show both success and error messages
            setSuccessMessage(
              `Assigned ${assignedCount} sample(s) to storage at ${storagePath} > ${selectedWell}.`,
            );
            setError(
              `Some samples could not be assigned: ${response.errors.join("; ")}`,
            );
          } else {
            // Full success
            setSuccessMessage(
              `Assigned ${assignedCount} sample(s) to storage at ${storagePath} > ${selectedWell}.`,
            );
          }
          setAssignStorageModalOpen(false);
          setSelectedWell(null);
          loadPageSamples();
          setSelectedSampleIds([]);
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
            // Show all detailed errors
            errorMessage = response.errors.join("; ");
          }
          setError(errorMessage);
        }
      },
    );
  }, [
    selectedSampleIds,
    selectedWell,
    pageData?.id,
    storageSelection,
    bulkAssignValues,
    entryId,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle auto-assignment of samples to storage
  const handleAutoAssign = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError("Please select samples to auto-assign.");
      return;
    }

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    if (!storageSelection.box) {
      setError("Please select a storage box first.");
      return;
    }

    setIsAutoAssigning(true);
    setError(null);

    // Build storage path (without well since each sample gets a different one)
    const storagePath = [
      storageSelection.room?.label,
      storageSelection.device?.label,
      storageSelection.shelf?.label,
      storageSelection.rack?.label,
      storageSelection.box?.label,
    ]
      .filter(Boolean)
      .join(" > ");

    // Get list of occupied wells from current box layout
    const occupiedWells = Object.keys(boxLayout);

    // Prepare the auto-assign request
    const autoAssignData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: {
        storageRoom: storageSelection.room?.label,
        storageFreezer: storageSelection.device?.label,
        storageType: autoAssignValues.storageType,
        storageRack: storageSelection.rack?.label,
        storageBox: storageSelection.box?.label,
        storagePath: storagePath,
        assignedBy: autoAssignValues.assignedBy,
        assignedDateTime: autoAssignValues.assignedDateTime,
        notes: autoAssignValues.notes,
        temperatureMonitoringConfirmed:
          autoAssignValues.temperatureMonitoringConfirmed,
      },
      boxId: storageSelection.box?.id,
      rows: storageSelection.box?.rows || 8,
      columns: storageSelection.box?.columns || 12,
      occupiedWells: occupiedWells,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/storage/auto-assign`,
      JSON.stringify(autoAssignData),
      (response) => {
        setIsAutoAssigning(false);

        // Check for HTTP error status (set by postToOpenElisServerJsonResponse for non-OK responses)
        const isHttpError =
          response?.status >= 400 || response?.statusCode >= 400;

        const assignmentCount = response?.updatedCount || 0;
        const hasErrors =
          response?.errors &&
          Array.isArray(response.errors) &&
          response.errors.length > 0;

        // Check if any samples were assigned successfully and no HTTP error
        if (assignmentCount > 0 && !isHttpError) {
          // Some or all samples assigned successfully
          setAutoAssignModalError(null);
          if (hasErrors) {
            // Partial success - show both success and error messages
            setSuccessMessage(
              `Auto-assigned ${assignmentCount} sample(s) to storage in ${storageSelection.box?.label}.`,
            );
            setError(
              `Some samples could not be assigned: ${response.errors.join("; ")}`,
            );
          } else {
            // Full success
            setSuccessMessage(
              `Auto-assigned ${assignmentCount} sample(s) to storage in ${storageSelection.box?.label}.`,
            );
          }
          setAutoAssignModalOpen(false);
          loadPageSamples();
          setSelectedSampleIds([]);
          // Reload box layout from storage API
          if (storageSelection.box) {
            loadBoxOccupancy(storageSelection.box.id);
          }
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          // No samples assigned or HTTP error - show error in modal (don't close it)
          let errorMessage =
            response?.error ||
            response?.message ||
            "Failed to auto-assign storage. Please try again.";
          if (hasErrors) {
            // Show all detailed errors
            errorMessage = response.errors.join("; ");
          }
          setAutoAssignModalError(errorMessage);
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    storageSelection,
    autoAssignValues,
    boxLayout,
    entryId,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle temperature logging
  const handleLogTemperature = useCallback(() => {
    if (!temperatureLog.freezerId || !temperatureLog.temperatureValue) {
      setError("Freezer ID and temperature value are required.");
      return;
    }

    // Validate temperature value is a valid number
    const tempValue = parseFloat(temperatureLog.temperatureValue);
    if (isNaN(tempValue)) {
      setError("Temperature value must be a valid number.");
      return;
    }

    setIsLoggingTemp(true);
    setError(null);

    // Build log data - DO NOT include entryId as it's in the URL path
    const logData = {
      freezerId: temperatureLog.freezerId,
      checkTime: temperatureLog.checkTime,
      temperatureValue: tempValue,
      temperatureUnit: temperatureLog.temperatureUnit,
      checkedBy: temperatureLog.checkedBy,
      checkedDateTime: temperatureLog.checkedDateTime,
      notes: temperatureLog.notes,
    };

    postToOpenElisServer(
      `/rest/notebook-entry/${entryId}/temperature-logs`,
      JSON.stringify(logData),
      (status) => {
        setIsLoggingTemp(false);
        if (status === 200 || status === 201) {
          setSuccessMessage("Temperature logged successfully.");
          setTempMonitoringModalOpen(false);
          loadTemperatureLogs();
          // Reset form
          setTemperatureLog({
            freezerId: temperatureLog.freezerId, // Keep freezer ID
            checkTime: "AM",
            temperatureValue: "",
            temperatureUnit: "C",
            checkedBy: "",
            checkedDateTime: new Date().toISOString().slice(0, 16),
            notes: "",
          });
        } else {
          setError("Failed to log temperature. Please try again.");
        }
      },
    );
  }, [temperatureLog, entryId, loadTemperatureLogs]);

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
            `Marked ${selectedSampleIds.length} samples as stored.`,
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
    if (sample.storageWell) {
      return (
        <Tag type="green" title={sample.storagePath}>
          {sample.storageWell}
        </Tag>
      );
    }
    return <Tag type="gray">Unassigned</Tag>;
  };

  // Reset bulk assign values
  const resetBulkAssignValues = () => {
    setBulkAssignValues({
      storageType: "",
      assignedBy: "",
      assignedDateTime: new Date().toISOString().slice(0, 16),
      notes: "",
      temperatureMonitoringConfirmed: false,
    });
    setSelectedWell(null);
  };

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
            defaultMessage="Track exact physical location using storage hierarchy. Select a storage location, then click a well position to assign samples. Record temperature monitoring data for environmental compliance."
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
                  id="notebook.page.mntd.stored"
                  defaultMessage="Stored"
                />
              </span>
              <span className="progress-value">{storedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.pendingStorage"
                  defaultMessage="Pending Storage"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.completed"
                  defaultMessage="Completed"
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
          kind="tertiary"
          size="sm"
          renderIcon={Temperature}
          onClick={() => setTempMonitoringModalOpen(true)}
        >
          <FormattedMessage
            id="notebook.page.mntd.logTemperature"
            defaultMessage="Log Temperature"
          />
        </Button>

        {selectedSampleIds.length > 0 && storageSelection.box && (
          <Button
            kind="primary"
            size="sm"
            renderIcon={Automatic}
            onClick={() => setAutoAssignModalOpen(true)}
          >
            <FormattedMessage
              id="notebook.page.mntd.autoAssign"
              defaultMessage="Auto-Assign ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkStored}
          >
            <FormattedMessage
              id="notebook.page.mntd.markStored"
              defaultMessage="Mark as Stored ({count})"
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

      {/* Storage Hierarchy and Box Layout */}
      <Grid fullWidth style={{ marginTop: "1rem" }}>
        <Column lg={8} md={4} sm={4}>
          <Tile>
            <h5 style={{ marginBottom: "1rem" }}>
              <Location size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="notebook.page.mntd.storageLocation"
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
          </Tile>
        </Column>

        <Column lg={8} md={4} sm={4}>
          {storageSelection.box ? (
            <Tile>
              <h5 style={{ marginBottom: "1rem" }}>
                <Archive size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.page.mntd.boxLayout"
                  defaultMessage="Box Layout"
                />
                {selectedSampleIds.length > 0 && (
                  <Tag type="blue" style={{ marginLeft: "0.5rem" }}>
                    {selectedSampleIds.length} selected - Click well to assign
                  </Tag>
                )}
              </h5>
              <BoxLayoutViewer
                boxId={storageSelection.box.id}
                layout={boxLayout}
                rows={storageSelection.box.rows || 8}
                columns={storageSelection.box.columns || 12}
                onWellClick={handleWellClick}
              />
            </Tile>
          ) : (
            <Tile className="empty-box-tile">
              <div className="empty-state" style={{ textAlign: "center" }}>
                <Archive size={48} />
                <p style={{ marginTop: "1rem" }}>
                  <FormattedMessage
                    id="notebook.page.mntd.selectBoxPrompt"
                    defaultMessage="Select a storage location to view box layout"
                  />
                </p>
              </div>
            </Tile>
          )}
        </Column>
      </Grid>

      {/* Temperature Logs Summary */}
      {temperatureLogs.length > 0 && (
        <div style={{ marginTop: "1.5rem" }}>
          <h5>
            <Temperature size={16} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="notebook.page.mntd.recentTempLogs"
              defaultMessage="Recent Temperature Logs"
            />
          </h5>
          <div
            style={{
              display: "flex",
              gap: "0.5rem",
              flexWrap: "wrap",
              marginTop: "0.5rem",
            }}
          >
            {temperatureLogs.slice(0, 5).map((log, index) => (
              <Tag
                key={index}
                type={
                  log.temperatureValue > -20 || log.temperatureValue < -80
                    ? "red"
                    : "green"
                }
              >
                {log.freezerId}: {log.temperatureValue}°{log.temperatureUnit} (
                {log.checkTime})
              </Tag>
            ))}
          </div>
        </div>
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
            { key: "externalId", header: "Sample ID" },
            { key: "sampleType", header: "Sample Type" },
            { key: "storageFreezer", header: "Freezer" },
            {
              key: "storageWell",
              header: "Position",
              render: (value, row) => getStorageTag(row),
            },
            { key: "assignedBy", header: "Assigned By" },
            { key: "status", header: "Status" },
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

      {/* Storage Assignment Modal */}
      <Modal
        open={assignStorageModalOpen}
        onRequestClose={() => {
          setAssignStorageModalOpen(false);
          setSelectedWell(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.assignStorage.title",
          defaultMessage: "Assign Storage Location",
        })}
        primaryButtonText={
          isAssigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "label.assign",
                defaultMessage: "Assign",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        onSecondarySubmit={() => {
          setAssignStorageModalOpen(false);
          setSelectedWell(null);
        }}
        size="md"
        primaryButtonDisabled={isAssigning}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.mntd.assignStorage.description"
            defaultMessage="Assign {count} sample(s) to position {well} in {box}."
            values={{
              count: selectedSampleIds.length,
              well: selectedWell,
              box: storageSelection.box?.label || "selected box",
            }}
          />
        </p>

        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                backgroundColor: "#f4f4f4",
                padding: "1rem",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <strong>
                <FormattedMessage
                  id="notebook.mntd.storagePath"
                  defaultMessage="Storage Path:"
                />
              </strong>{" "}
              {[
                storageSelection.room?.label,
                storageSelection.device?.label,
                storageSelection.shelf?.label,
                storageSelection.rack?.label,
                storageSelection.box?.label,
                selectedWell,
              ]
                .filter(Boolean)
                .join(" > ")}
            </div>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="storageType"
              labelText={intl.formatMessage({
                id: "notebook.mntd.storageType",
                defaultMessage: "Storage Type",
              })}
              value={bulkAssignValues.storageType}
              onChange={(e) =>
                setBulkAssignValues((prev) => ({
                  ...prev,
                  storageType: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select type..." />
              <SelectItem value="Raw" text="Raw" />
              <SelectItem value="Processed" text="Processed" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="assignedBy"
              labelText={intl.formatMessage({
                id: "notebook.mntd.assignedBy",
                defaultMessage: "Assigned By",
              })}
              value={bulkAssignValues.assignedBy}
              onChange={(e) =>
                setBulkAssignValues((prev) => ({
                  ...prev,
                  assignedBy: e.target.value,
                }))
              }
              placeholder="Enter staff name"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="notebook.mntd.assignedDateTime"
                  defaultMessage="Assigned Date/Time"
                />
              </label>
              <input
                type="datetime-local"
                className="cds--text-input"
                value={bulkAssignValues.assignedDateTime}
                onChange={(e) =>
                  setBulkAssignValues((prev) => ({
                    ...prev,
                    assignedDateTime: e.target.value,
                  }))
                }
              />
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="storageNotes"
              labelText={intl.formatMessage({
                id: "notebook.mntd.notes",
                defaultMessage: "Notes",
              })}
              value={bulkAssignValues.notes}
              onChange={(e) =>
                setBulkAssignValues((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Optional notes..."
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Checkbox
              id="temperatureMonitoringConfirmed"
              labelText={intl.formatMessage({
                id: "notebook.mntd.temperatureMonitoringConfirmed",
                defaultMessage:
                  "I confirm that monitoring of freezer temperature is done twice a day",
              })}
              checked={bulkAssignValues.temperatureMonitoringConfirmed}
              onChange={(e, { checked }) =>
                setBulkAssignValues((prev) => ({
                  ...prev,
                  temperatureMonitoringConfirmed: checked,
                }))
              }
            />
          </Column>
        </Grid>
      </Modal>

      {/* Temperature Monitoring Modal */}
      <Modal
        open={tempMonitoringModalOpen}
        onRequestClose={() => setTempMonitoringModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.temperatureLog.title",
          defaultMessage: "Log Temperature Check",
        })}
        primaryButtonText={
          isLoggingTemp
            ? intl.formatMessage({
                id: "label.logging",
                defaultMessage: "Logging...",
              })
            : intl.formatMessage({
                id: "label.logTemperature",
                defaultMessage: "Log Temperature",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleLogTemperature}
        onSecondarySubmit={() => setTempMonitoringModalOpen(false)}
        size="md"
        primaryButtonDisabled={isLoggingTemp}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.mntd.temperatureLog.description"
            defaultMessage="Record temperature monitoring data for environmental compliance tracking."
          />
        </p>

        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="freezerId"
              labelText={intl.formatMessage({
                id: "notebook.mntd.freezerId",
                defaultMessage: "Freezer ID",
              })}
              value={temperatureLog.freezerId}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  freezerId: e.target.value,
                }))
              }
              placeholder="e.g., FRZ-001"
              required
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.mntd.checkTime",
                defaultMessage: "Check Time",
              })}
              name="checkTime"
              valueSelected={temperatureLog.checkTime}
              onChange={(value) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  checkTime: value,
                }))
              }
            >
              <RadioButton labelText="AM" value="AM" id="check-am" />
              <RadioButton labelText="PM" value="PM" id="check-pm" />
            </RadioButtonGroup>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="temperatureValue"
              label={intl.formatMessage({
                id: "notebook.mntd.temperatureValue",
                defaultMessage: "Temperature Value",
              })}
              value={temperatureLog.temperatureValue}
              onChange={(e, { value }) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  temperatureValue: value,
                }))
              }
              min={-200}
              max={50}
              step={0.1}
              invalidText="Enter a valid temperature"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="temperatureUnit"
              labelText={intl.formatMessage({
                id: "notebook.mntd.temperatureUnit",
                defaultMessage: "Unit",
              })}
              value={temperatureLog.temperatureUnit}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  temperatureUnit: e.target.value,
                }))
              }
            >
              <SelectItem value="C" text="Celsius (°C)" />
              <SelectItem value="F" text="Fahrenheit (°F)" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="checkedBy"
              labelText={intl.formatMessage({
                id: "notebook.mntd.checkedBy",
                defaultMessage: "Checked By",
              })}
              value={temperatureLog.checkedBy}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  checkedBy: e.target.value,
                }))
              }
              placeholder="Staff name"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="notebook.mntd.checkedDateTime"
                  defaultMessage="Checked Date/Time"
                />
              </label>
              <input
                type="datetime-local"
                className="cds--text-input"
                value={temperatureLog.checkedDateTime}
                onChange={(e) =>
                  setTemperatureLog((prev) => ({
                    ...prev,
                    checkedDateTime: e.target.value,
                  }))
                }
              />
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="tempNotes"
              labelText={intl.formatMessage({
                id: "notebook.mntd.notes",
                defaultMessage: "Notes",
              })}
              value={temperatureLog.notes}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Optional notes about the temperature check..."
            />
          </Column>
        </Grid>
      </Modal>

      {/* Auto-Assign Storage Modal */}
      <Modal
        open={autoAssignModalOpen}
        onRequestClose={() => {
          setAutoAssignModalOpen(false);
          setAutoAssignModalError(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.autoAssign.title",
          defaultMessage: "Auto-Assign Storage Locations",
        })}
        primaryButtonText={
          isAutoAssigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "label.autoAssign",
                defaultMessage: "Auto-Assign",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAutoAssign}
        onSecondarySubmit={() => {
          setAutoAssignModalOpen(false);
          setAutoAssignModalError(null);
        }}
        size="lg"
        primaryButtonDisabled={isAutoAssigning}
      >
        {/* Error notification inside modal */}
        {autoAssignModalError && (
          <InlineNotification
            kind="error"
            title={autoAssignModalError}
            onClose={() => setAutoAssignModalError(null)}
            lowContrast
            style={{ marginBottom: "1rem" }}
          />
        )}

        <p className="modal-description">
          <FormattedMessage
            id="notebook.mntd.autoAssign.description"
            defaultMessage="Automatically assign {count} sample(s) to the next available wells in {box}, starting from position A1."
            values={{
              count: selectedSampleIds.length,
              box: storageSelection.box?.label || "selected box",
            }}
          />
        </p>

        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                backgroundColor: "#f4f4f4",
                padding: "1rem",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <strong>
                <FormattedMessage
                  id="notebook.mntd.storagePath"
                  defaultMessage="Storage Path:"
                />
              </strong>{" "}
              {[
                storageSelection.room?.label,
                storageSelection.device?.label,
                storageSelection.shelf?.label,
                storageSelection.rack?.label,
                storageSelection.box?.label,
              ]
                .filter(Boolean)
                .join(" > ")}
              <div
                style={{ marginTop: "0.5rem", display: "flex", gap: "0.5rem" }}
              >
                <Tag type="blue">
                  {autoAssignPreview.availableCount} wells available
                </Tag>
                <Tag type="green">
                  {autoAssignPreview.previewWells.length} will be assigned
                </Tag>
              </div>
            </div>
          </Column>

          {/* Box Layout Preview */}
          <Column lg={16} md={8} sm={4}>
            <div style={{ marginBottom: "1rem" }}>
              <h6 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.mntd.autoAssign.preview"
                  defaultMessage="Assignment Preview"
                />
              </h6>
              <div
                className="box-grid-container"
                style={{
                  display: "grid",
                  gridTemplateColumns: `auto repeat(${storageSelection.box?.columns || 12}, 1fr)`,
                  gap: "2px",
                  fontSize: "10px",
                  maxWidth: "100%",
                  overflowX: "auto",
                }}
              >
                {/* Column headers */}
                <div style={{ padding: "2px 4px" }}></div>
                {Array.from(
                  { length: storageSelection.box?.columns || 12 },
                  (_, i) => (
                    <div
                      key={`col-${i}`}
                      style={{
                        textAlign: "center",
                        fontWeight: "bold",
                        padding: "2px",
                      }}
                    >
                      {i + 1}
                    </div>
                  ),
                )}

                {/* Grid rows */}
                {Array.from(
                  { length: storageSelection.box?.rows || 8 },
                  (_, rowIdx) => {
                    const rowLetter = String.fromCharCode(
                      "A".charCodeAt(0) + rowIdx,
                    );
                    return (
                      <React.Fragment key={`row-${rowIdx}`}>
                        <div
                          style={{
                            fontWeight: "bold",
                            padding: "2px 4px",
                            display: "flex",
                            alignItems: "center",
                          }}
                        >
                          {rowLetter}
                        </div>
                        {Array.from(
                          { length: storageSelection.box?.columns || 12 },
                          (_, colIdx) => {
                            const wellCoord = `${rowLetter}${colIdx + 1}`;
                            const isOccupied = boxLayout[wellCoord];
                            const willBeAssigned =
                              autoAssignPreview.previewWells.includes(
                                wellCoord,
                              );

                            let bgColor = "#e0e0e0"; // Empty
                            let borderColor = "#c0c0c0";
                            if (isOccupied) {
                              bgColor = "#a8a8a8"; // Already occupied
                              borderColor = "#808080";
                            } else if (willBeAssigned) {
                              bgColor = "#42be65"; // Will be assigned (green)
                              borderColor = "#24a148";
                            }

                            return (
                              <div
                                key={wellCoord}
                                title={
                                  isOccupied
                                    ? `${wellCoord} - Occupied`
                                    : willBeAssigned
                                      ? `${wellCoord} - Will be assigned`
                                      : `${wellCoord} - Empty`
                                }
                                style={{
                                  width: "20px",
                                  height: "20px",
                                  borderRadius: "50%",
                                  backgroundColor: bgColor,
                                  border: `1px solid ${borderColor}`,
                                  margin: "auto",
                                }}
                              />
                            );
                          },
                        )}
                      </React.Fragment>
                    );
                  },
                )}
              </div>
              <div
                style={{
                  display: "flex",
                  gap: "1rem",
                  marginTop: "0.5rem",
                  fontSize: "12px",
                }}
              >
                <div
                  style={{ display: "flex", alignItems: "center", gap: "4px" }}
                >
                  <div
                    style={{
                      width: "12px",
                      height: "12px",
                      borderRadius: "50%",
                      backgroundColor: "#42be65",
                    }}
                  />
                  <span>Will be assigned</span>
                </div>
                <div
                  style={{ display: "flex", alignItems: "center", gap: "4px" }}
                >
                  <div
                    style={{
                      width: "12px",
                      height: "12px",
                      borderRadius: "50%",
                      backgroundColor: "#a8a8a8",
                    }}
                  />
                  <span>Already occupied</span>
                </div>
                <div
                  style={{ display: "flex", alignItems: "center", gap: "4px" }}
                >
                  <div
                    style={{
                      width: "12px",
                      height: "12px",
                      borderRadius: "50%",
                      backgroundColor: "#e0e0e0",
                    }}
                  />
                  <span>Empty</span>
                </div>
              </div>
            </div>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="autoAssignStorageType"
              labelText={intl.formatMessage({
                id: "notebook.mntd.storageType",
                defaultMessage: "Storage Type",
              })}
              value={autoAssignValues.storageType}
              onChange={(e) =>
                setAutoAssignValues((prev) => ({
                  ...prev,
                  storageType: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select type..." />
              <SelectItem value="Raw" text="Raw" />
              <SelectItem value="Processed" text="Processed" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="autoAssignAssignedBy"
              labelText={intl.formatMessage({
                id: "notebook.mntd.assignedBy",
                defaultMessage: "Assigned By",
              })}
              value={autoAssignValues.assignedBy}
              onChange={(e) =>
                setAutoAssignValues((prev) => ({
                  ...prev,
                  assignedBy: e.target.value,
                }))
              }
              placeholder="Enter staff name"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="notebook.mntd.assignedDateTime"
                  defaultMessage="Assigned Date/Time"
                />
              </label>
              <input
                type="datetime-local"
                className="cds--text-input"
                value={autoAssignValues.assignedDateTime}
                onChange={(e) =>
                  setAutoAssignValues((prev) => ({
                    ...prev,
                    assignedDateTime: e.target.value,
                  }))
                }
              />
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="autoAssignNotes"
              labelText={intl.formatMessage({
                id: "notebook.mntd.notes",
                defaultMessage: "Notes",
              })}
              value={autoAssignValues.notes}
              onChange={(e) =>
                setAutoAssignValues((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Optional notes..."
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Checkbox
              id="autoAssignTemperatureMonitoringConfirmed"
              labelText={intl.formatMessage({
                id: "notebook.mntd.temperatureMonitoringConfirmed",
                defaultMessage:
                  "I confirm that monitoring of freezer temperature is done twice a day",
              })}
              checked={autoAssignValues.temperatureMonitoringConfirmed}
              onChange={(e, { checked }) =>
                setAutoAssignValues((prev) => ({
                  ...prev,
                  temperatureMonitoringConfirmed: checked,
                }))
              }
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default MNTDTemporaryStoragePage;
