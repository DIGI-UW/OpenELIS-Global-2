import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  NumberInput,
  Tag,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import {
  Archive,
  Temperature,
  Checkmark,
  Location,
  Automatic,
  Warning,
  Renew,
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
 * Storage type options for Bacteriology samples
 * - Temporary Storage: For samples awaiting processing after initial QC
 * - Long-Term Storage: For processed samples, isolates, DNA/RNA
 */
const STORAGE_TYPES = [
  {
    id: "TEMPORARY",
    label: "Temporary Storage",
    description: "For samples awaiting processing after QC",
  },
  {
    id: "LONG_TERM",
    label: "Long-Term Storage",
    description: "For processed samples, isolates, DNA/RNA",
  },
];

/**
 * Storage temperature options for Bacteriology samples
 * Based on user requirements:
 * - Room Temperature (for certain isolates on agar)
 * - Refrigerated (2-8°C)
 * - Frozen (-20°C)
 * - Ultra-Low Frozen (-80°C)
 * - Liquid Nitrogen (-196°C)
 * - Incubator (37°C for cultures)
 */
const STORAGE_CONDITIONS = [
  {
    id: "ROOM_TEMP",
    label: "Room Temperature (15-25°C)",
    tempRange: "15-25°C",
    description: "For certain isolates on agar",
  },
  {
    id: "REFRIGERATED",
    label: "Refrigerator (2-8°C)",
    tempRange: "2-8°C",
    description: "Standard refrigerated storage",
  },
  {
    id: "FROZEN_MINUS20",
    label: "Freezer (-20°C)",
    tempRange: "-20°C",
    description: "Standard frozen storage",
  },
  {
    id: "FROZEN_MINUS80",
    label: "Ultra-Low Freezer (-80°C)",
    tempRange: "-80°C",
    description: "Long-term sample preservation",
  },
  {
    id: "LIQUID_NITROGEN",
    label: "Liquid Nitrogen (-196°C)",
    tempRange: "-196°C",
    description: "Cryopreservation",
  },
  {
    id: "INCUBATOR_37",
    label: "Incubator (37°C)",
    tempRange: "37°C",
    description: "For active cultures",
  },
];

/**
 * BacteriologyTemporaryStoragePage - Page 3 of the Bacteriology workflow.
 * Handles storage assignment and environmental monitoring for Bacteriology samples.
 *
 * Storage Types:
 * - Temporary Storage: For samples awaiting processing after initial QC
 * - Long-Term Storage: For processed samples, isolates, DNA/RNA
 *
 * Storage Hierarchy: Room → Device/Freezer → Shelf → Rack → Box → Well
 * Location recording is critical: Anyone needing samples must know exact placement
 *
 * Storage Temperatures:
 * - Refrigerator: 2-8°C
 * - Freezer: -20°C or -80°C
 * - Room temperature (for certain isolates on agar)
 *
 * Environmental Monitoring:
 * - Freezer temperature monitoring (twice daily or continuous if automated)
 * - Logged with date, time, temperature, staff initials
 * - Alerts for temperature excursions
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function BacteriologyTemporaryStoragePage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
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
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [selectedWell, setSelectedWell] = useState(null);
  const [isAssigning, setIsAssigning] = useState(false);
  const [wellAssignments, setWellAssignments] = useState({});
  const [isReassignment, setIsReassignment] = useState(false);
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [bulkAssignValues, setBulkAssignValues] = useState({
    storageType: "TEMPORARY", // Default to temporary storage
    storageCondition: "",
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
  });

  // Reassignment confirmation modal state
  const [confirmReassignModalOpen, setConfirmReassignModalOpen] =
    useState(false);
  const [samplesToReassign, setSamplesToReassign] = useState([]);

  // Environmental monitoring modal state
  const [tempMonitoringModalOpen, setTempMonitoringModalOpen] = useState(false);
  const [temperatureLog, setTemperatureLog] = useState({
    deviceId: "",
    deviceType: "",
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
              // Bacteriology specific fields
              sampleOrigin: sample.data?.sampleOrigin,
              projectName: sample.data?.projectName,
              // Storage assignment fields from data
              storageRoom: sample.data?.storageRoom,
              storageFreezer: sample.data?.storageFreezer,
              storageRack: sample.data?.storageRack,
              storageBox: sample.data?.storageBox,
              storageWell: sample.data?.storageWell,
              storagePath: sample.data?.storagePath,
              storageCondition: sample.data?.storageCondition,
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
      setWellAssignments({});
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
        setError(
          intl.formatMessage(
            {
              id: "notebook.bacteriology.storage.wellOccupied",
              defaultMessage:
                "Well {well} is already occupied by {sample}. Choose another position.",
            },
            { well: wellCoord, sample: wellInfo.externalId || "a sample" },
          ),
        );
        return;
      }

      if (storageModalOpen) {
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
        if (selectedSampleIds.length === 0) {
          setError(
            intl.formatMessage({
              id: "notebook.bacteriology.storage.selectSamplesFirst",
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
          id: "notebook.bacteriology.storage.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

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
    setBulkAssignValues({
      storageType: "TEMPORARY",
      storageCondition: "",
      assignedBy: "",
      assignedDateTime: new Date().toISOString().slice(0, 16),
      notes: "",
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
          id: "notebook.bacteriology.storage.selectBoxFirst",
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
            id: "notebook.bacteriology.storage.notEnoughWells",
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
            id: "notebook.bacteriology.storage.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Build combined layout for visualization
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

  // Handle bulk storage assignment
  const handleAssignStorage = useCallback(() => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.storage.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.storage.noWellAssignments",
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

    const assignData = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      boxId: storageSelection.box?.id,
      wellAssignments: wellAssignments,
      reassign: isReassignment,
      data: {
        storageType: bulkAssignValues.storageType,
        storageRoom: storageSelection.room?.label,
        storageFreezer: storageSelection.device?.label,
        storageShelf: storageSelection.shelf?.label,
        storageRack: storageSelection.rack?.label,
        storageBox: storageSelection.box?.label,
        storagePath: storagePath,
        storageCondition: bulkAssignValues.storageCondition,
        assignedBy: bulkAssignValues.assignedBy,
        assignedDateTime:
          bulkAssignValues.assignedDateTime || new Date().toISOString(),
        notes: bulkAssignValues.notes,
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

        if (response && (response.success || assignedCount > 0)) {
          const messageId = isReassignment
            ? "notebook.bacteriology.storage.reassignSuccess"
            : "notebook.bacteriology.storage.assignSuccess";
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
          if (storageSelection.box) {
            loadBoxOccupancy(storageSelection.box.id);
          }
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
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
    bulkAssignValues,
    isReassignment,
    intl,
    loadPageSamples,
    loadBoxOccupancy,
    onProgressUpdate,
  ]);

  // Handle temperature logging
  const handleLogTemperature = useCallback(() => {
    if (
      !temperatureLog.deviceId ||
      !temperatureLog.temperatureValue ||
      !temperatureLog.checkedBy
    ) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.storage.tempLogRequired",
          defaultMessage:
            "Device ID, temperature value, and staff initials are required.",
        }),
      );
      return;
    }

    const tempValue = parseFloat(temperatureLog.temperatureValue);
    if (isNaN(tempValue)) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.storage.tempInvalid",
          defaultMessage: "Temperature value must be a valid number.",
        }),
      );
      return;
    }

    setIsLoggingTemp(true);
    setError(null);

    const logData = {
      freezerId: temperatureLog.deviceId,
      deviceType: temperatureLog.deviceType,
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
          setSuccessMessage(
            intl.formatMessage({
              id: "notebook.bacteriology.storage.tempLogSuccess",
              defaultMessage: "Temperature logged successfully.",
            }),
          );
          setTempMonitoringModalOpen(false);
          loadTemperatureLogs();
          setTemperatureLog((prev) => ({
            deviceId: prev.deviceId,
            deviceType: prev.deviceType,
            checkTime: "AM",
            temperatureValue: "",
            temperatureUnit: "C",
            checkedBy: "",
            checkedDateTime: new Date().toISOString().slice(0, 16),
            notes: "",
          }));
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.bacteriology.storage.tempLogError",
              defaultMessage: "Failed to log temperature. Please try again.",
            }),
          );
        }
      },
    );
  }, [temperatureLog, entryId, intl, loadTemperatureLogs]);

  // Handle marking samples as stored (complete)
  const handleMarkStored = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

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
                id: "notebook.page.bacteriology.completedSuccess",
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
              id: "notebook.page.bacteriology.completedError",
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

    const condition = STORAGE_CONDITIONS.find(
      (c) => c.id === sample.storageCondition,
    );
    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {condition ? condition.tempRange : sample.storageCondition}
      </Tag>
    );
  };

  // Get origin badge type
  const getOriginBadgeType = (origin) => {
    switch (origin?.toLowerCase()) {
      case "human":
        return "blue";
      case "animal":
        return "purple";
      case "environmental":
        return "green";
      case "food/beverage":
        return "orange";
      default:
        return "gray";
    }
  };

  // Get temperature status tag for logs
  const getTemperatureStatusTag = (log) => {
    const ranges = {
      Refrigerator: { min: 2, max: 8 },
      "Freezer-20": { min: -25, max: -15 },
      "Freezer-80": { min: -85, max: -75 },
      LN2Tank: { min: -200, max: -180 },
      Incubator: { min: 35, max: 38 },
      ColdRoom: { min: 2, max: 8 },
    };

    const range = ranges[log.deviceType] || { min: -999, max: 999 };
    const temp = log.temperatureValue;
    const inRange = temp >= range.min && temp <= range.max;

    return inRange ? (
      <Tag type="green" size="sm">
        {temp}°{log.temperatureUnit}
      </Tag>
    ) : (
      <Tag type="red" size="sm" renderIcon={Warning}>
        {temp}°{log.temperatureUnit} - OUT OF RANGE
      </Tag>
    );
  };

  // Check if page has real ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  return (
    <div className="bacteriology-temporary-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.temporaryStorage.title"
            defaultMessage="Sample Storage Assignment"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.temporaryStorage.description"
            defaultMessage="Assign samples to storage locations after QC. Select storage type (Temporary for awaiting processing, Long-Term for processed samples/isolates). Use hierarchy: Room → Device/Freezer → Shelf → Rack → Box → Well. Record exact placement for retrieval. Log temperature readings twice daily with staff initials."
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
                  id="notebook.page.bacteriology.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.awaitingStorage"
                  defaultMessage="Awaiting Storage"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.inStorage"
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
                  id="notebook.page.bacteriology.sentToNextStep"
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
            id="notebook.page.bacteriology.assignToStorage"
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
            id="notebook.page.bacteriology.markCompleteAndAdvance"
            defaultMessage="Complete & Send to Next Step ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Temperature}
          onClick={() => {
            setTemperatureLog((prev) => ({
              ...prev,
              checkedDateTime: new Date().toISOString().slice(0, 16),
            }));
            setTempMonitoringModalOpen(true);
          }}
        >
          <FormattedMessage
            id="notebook.page.bacteriology.logTemperature"
            defaultMessage="Log Temperature"
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.page.bacteriology.refresh"
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

      {/* Temperature Logs Summary */}
      {temperatureLogs.length > 0 && (
        <div style={{ marginTop: "1.5rem" }}>
          <h5>
            <Temperature size={16} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="notebook.page.bacteriology.recentTempLogs"
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
            {temperatureLogs.slice(0, 6).map((log, index) => (
              <Tile
                key={index}
                style={{
                  padding: "0.5rem 1rem",
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                }}
              >
                <strong>{log.freezerId}:</strong>
                {getTemperatureStatusTag(log)}
                <span style={{ fontSize: "0.75rem", color: "#525252" }}>
                  ({log.checkTime})
                </span>
              </Tile>
            ))}
          </div>
        </div>
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container" style={{ marginTop: "1.5rem" }}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="notebook.page.bacteriology.sampleList"
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
              key: "sampleOrigin",
              header: intl.formatMessage({
                id: "notebook.column.origin",
                defaultMessage: "Origin",
              }),
              render: (value) =>
                value ? (
                  <Tag type={getOriginBadgeType(value)} size="sm">
                    {value}
                  </Tag>
                ) : (
                  "-"
                ),
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
              id="notebook.page.bacteriology.temporaryStorage.empty"
              defaultMessage="No samples available for storage assignment. Samples must pass verification in the Laboratory Reception & Verification step first."
            />
          </p>
        </div>
      )}

      {/* Unified Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={
          isAssigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.bacteriology.storage.modal.assign",
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
            id="notebook.bacteriology.storage.modal.description"
            defaultMessage="Assign {count} selected samples to storage. Select a storage location, then use Auto-Populate or click wells to assign samples."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Storage Type and Location Selection */}
          <Column lg={8} md={4} sm={4}>
            {/* Storage Type Selection */}
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Archive size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.bacteriology.storage.storageType"
                  defaultMessage="Storage Type"
                />
              </h5>
              <RadioButtonGroup
                name="storageType"
                valueSelected={bulkAssignValues.storageType}
                onChange={(value) =>
                  setBulkAssignValues((prev) => ({
                    ...prev,
                    storageType: value,
                  }))
                }
                orientation="horizontal"
              >
                {STORAGE_TYPES.map((type) => (
                  <RadioButton
                    key={type.id}
                    labelText={type.label}
                    value={type.id}
                    id={`storage-type-${type.id}`}
                  />
                ))}
              </RadioButtonGroup>
              <p
                style={{
                  fontSize: "0.75rem",
                  color: "#525252",
                  marginTop: "0.25rem",
                }}
              >
                {bulkAssignValues.storageType === "TEMPORARY" ? (
                  <FormattedMessage
                    id="notebook.bacteriology.storage.temporaryHint"
                    defaultMessage="For samples awaiting processing after initial QC"
                  />
                ) : (
                  <FormattedMessage
                    id="notebook.bacteriology.storage.longTermHint"
                    defaultMessage="For processed samples, isolates, DNA/RNA"
                  />
                )}
              </p>
            </div>

            {/* Storage Location */}
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Location size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.bacteriology.storage.storageLocation"
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

            {/* Storage Condition Selection */}
            <div style={{ marginTop: "1rem" }}>
              <Select
                id="storageCondition"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.storage.condition",
                  defaultMessage: "Storage Temperature",
                })}
                value={bulkAssignValues.storageCondition}
                onChange={(e) =>
                  setBulkAssignValues((prev) => ({
                    ...prev,
                    storageCondition: e.target.value,
                  }))
                }
              >
                <SelectItem value="" text="Select temperature condition..." />
                {STORAGE_CONDITIONS.map((cond) => (
                  <SelectItem
                    key={cond.id}
                    value={cond.id}
                    text={`${cond.label} - ${cond.description}`}
                  />
                ))}
              </Select>
            </div>

            {/* Assigned By */}
            <div style={{ marginTop: "1rem" }}>
              <TextInput
                id="assignedBy"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.storage.assignedBy",
                  defaultMessage: "Assigned By (Staff Initials)",
                })}
                value={bulkAssignValues.assignedBy}
                onChange={(e) =>
                  setBulkAssignValues((prev) => ({
                    ...prev,
                    assignedBy: e.target.value,
                  }))
                }
                placeholder="Staff name/initials"
              />
            </div>

            {/* Notes */}
            <div style={{ marginTop: "1rem" }}>
              <TextArea
                id="storageNotes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.storage.notes",
                  defaultMessage: "Notes",
                })}
                value={bulkAssignValues.notes}
                onChange={(e) =>
                  setBulkAssignValues((prev) => ({
                    ...prev,
                    notes: e.target.value,
                  }))
                }
                placeholder="Any additional notes about storage..."
                rows={2}
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
                      id="notebook.bacteriology.storage.boxLayout"
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
                      id="notebook.bacteriology.storage.autoPopulate"
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
                    id="notebook.bacteriology.storage.assignmentSummary"
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
                    id="notebook.bacteriology.storage.selectBoxFirst"
                    defaultMessage="Select a storage location to preview box layout"
                  />
                </p>
              </div>
            )}
          </Column>
        </Grid>
      </Modal>

      {/* Temperature Monitoring Modal */}
      <Modal
        open={tempMonitoringModalOpen}
        onRequestClose={() => setTempMonitoringModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.tempLog.title",
          defaultMessage: "Log Environmental Monitoring",
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
            id="notebook.bacteriology.tempLog.description"
            defaultMessage="Record temperature monitoring data for Bacteriology laboratory environmental compliance. Temperature readings should be logged twice daily (AM/PM) or continuously if automated. Include staff initials for accountability."
          />
        </p>

        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="deviceId"
              labelText={intl.formatMessage({
                id: "notebook.bacteriology.deviceId",
                defaultMessage: "Device/Equipment ID *",
              })}
              value={temperatureLog.deviceId}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  deviceId: e.target.value,
                }))
              }
              placeholder="e.g., FRZ-001, LN2-002"
              required
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="deviceType"
              labelText={intl.formatMessage({
                id: "notebook.bacteriology.deviceType",
                defaultMessage: "Device Type",
              })}
              value={temperatureLog.deviceType}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  deviceType: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select type..." />
              <SelectItem value="Refrigerator" text="Refrigerator (2-8°C)" />
              <SelectItem value="Freezer-20" text="Freezer (-20°C)" />
              <SelectItem value="Freezer-80" text="Ultra-Low Freezer (-80°C)" />
              <SelectItem value="LN2Tank" text="Liquid Nitrogen Tank" />
              <SelectItem value="Incubator" text="Incubator (37°C)" />
              <SelectItem value="ColdRoom" text="Cold Room" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.bacteriology.checkTime",
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
                id: "notebook.bacteriology.temperatureValue",
                defaultMessage: "Temperature Value *",
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
                id: "notebook.bacteriology.temperatureUnit",
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
                id: "notebook.bacteriology.checkedBy",
                defaultMessage: "Checked By (Staff Initials) *",
              })}
              value={temperatureLog.checkedBy}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  checkedBy: e.target.value,
                }))
              }
              placeholder="e.g., JD, ABC"
              required
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="notebook.bacteriology.checkedDateTime"
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
                id: "notebook.bacteriology.notes",
                defaultMessage: "Notes",
              })}
              value={temperatureLog.notes}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Document any temperature excursions, deviations from expected range, corrective actions taken, etc."
            />
          </Column>
        </Grid>
      </Modal>

      {/* Reassignment Confirmation Modal */}
      <Modal
        open={confirmReassignModalOpen}
        onRequestClose={() => setConfirmReassignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.storage.reassignConfirm.title",
          defaultMessage: "Confirm Reassignment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.storage.reassignConfirm.confirm",
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
              id="notebook.bacteriology.storage.reassignConfirm.warning"
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
                id="notebook.bacteriology.storage.reassignConfirm.currentLocations"
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

export default BacteriologyTemporaryStoragePage;
