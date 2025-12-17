import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  Dropdown,
  NumberInput,
  Tag,
  Loading,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import {
  Archive,
  Checkmark,
  Temperature,
  Renew,
  Location,
  Automatic,
  Warning,
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
 * PharmaceuticalStoragePage - Page 5 of the Pharmaceuticals workflow.
 * Handles storage assignment with hierarchical location selection,
 * pharmaceutical-specific storage conditions, and environmental monitoring.
 *
 * Storage conditions per sample type:
 * - Pharmaceuticals: 15-25C (CRT), 2-8C (refrigerated), -20C (frozen)
 * - Biologicals: -80C, LN2 vapor phase
 * - Microbiologicals: -80C stocks, refrigerated water samples
 *
 * Features:
 * - Hierarchical storage location selection (Room > Device > Shelf > Rack > Box > Well)
 * - Environmental temperature monitoring/logging
 * - Auto-assign samples to wells
 * - Retention period tracking
 * - Reassignment with confirmation
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PharmaceuticalStoragePage({
  entryId,
  notebookId,
  pageData,
  progress,
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
  const [success, setSuccess] = useState(null);

  // Storage hierarchy state (using StorageHierarchySelector)
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
  const [assigning, setAssigning] = useState(false);
  const [selectedWell, setSelectedWell] = useState(null);

  // Auto-assign modal state
  const [autoAssignModalOpen, setAutoAssignModalOpen] = useState(false);
  const [isAutoAssigning, setIsAutoAssigning] = useState(false);
  const [autoAssignValues, setAutoAssignValues] = useState({
    storageType: "",
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
  });

  // Reassignment confirmation modal state
  const [confirmReassignModalOpen, setConfirmReassignModalOpen] =
    useState(false);
  const [samplesToReassign, setSamplesToReassign] = useState([]);
  const [isReassignment, setIsReassignment] = useState(false);

  // Temperature monitoring modal state
  const [tempMonitoringModalOpen, setTempMonitoringModalOpen] = useState(false);
  const [temperatureLog, setTemperatureLog] = useState({
    deviceId: "",
    deviceType: "Refrigerator",
    checkTime: "AM",
    temperatureValue: "",
    temperatureUnit: "C",
    humidityValue: "",
    checkedBy: "",
    checkedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
  });
  const [isLoggingTemp, setIsLoggingTemp] = useState(false);
  const [temperatureLogs, setTemperatureLogs] = useState([]);

  // Bulk assignment state
  const [bulkAssignValues, setBulkAssignValues] = useState({
    storageType: "",
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
  });
  const [wellAssignments, setWellAssignments] = useState({});

  // Storage form fields
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);

  // Storage condition options - pharmaceutical specific
  const storageConditionOptions = [
    {
      id: "ROOM_TEMP",
      label: intl.formatMessage({
        id: "notebook.pharma.storage.condition.roomTemp",
        defaultMessage: "Controlled Room Temperature (15-25°C)",
      }),
      tempRange: "15-25°C",
    },
    {
      id: "REFRIGERATED",
      label: intl.formatMessage({
        id: "notebook.pharma.storage.condition.refrigerated",
        defaultMessage: "Refrigerated (2-8°C)",
      }),
      tempRange: "2-8°C",
    },
    {
      id: "FROZEN_MINUS20",
      label: intl.formatMessage({
        id: "notebook.pharma.storage.condition.frozen20",
        defaultMessage: "Frozen (-20°C)",
      }),
      tempRange: "-20°C",
    },
    {
      id: "FROZEN_MINUS80",
      label: intl.formatMessage({
        id: "notebook.pharma.storage.condition.frozen80",
        defaultMessage: "Ultra-Low (-80°C)",
      }),
      tempRange: "-80°C",
    },
    {
      id: "LIQUID_NITROGEN",
      label: intl.formatMessage({
        id: "notebook.pharma.storage.condition.liquidNitrogen",
        defaultMessage: "Liquid Nitrogen Vapor Phase (-196°C)",
      }),
      tempRange: "-196°C",
    },
    {
      id: "PROTECT_LIGHT",
      label: intl.formatMessage({
        id: "notebook.pharma.storage.condition.protectLight",
        defaultMessage: "Protected from Light (Amber)",
      }),
      tempRange: "Ambient",
    },
    {
      id: "HUMIDITY_CONTROLLED",
      label: intl.formatMessage({
        id: "notebook.pharma.storage.condition.humidity",
        defaultMessage: "Humidity Controlled (≤60% RH)",
      }),
      tempRange: "15-25°C, ≤60% RH",
    },
    {
      id: "DESICCATED",
      label: intl.formatMessage({
        id: "notebook.pharma.storage.condition.desiccated",
        defaultMessage: "Desiccated (Low Humidity)",
      }),
      tempRange: "15-25°C, <20% RH",
    },
  ];

  // Storage device types for temperature monitoring
  const deviceTypes = [
    { id: "Refrigerator", label: "Refrigerator (2-8°C)" },
    { id: "Freezer-20", label: "Freezer (-20°C)" },
    { id: "Freezer-80", label: "Ultra-Low Freezer (-80°C)" },
    { id: "LN2Tank", label: "Liquid Nitrogen Tank" },
    { id: "Incubator", label: "Incubator" },
    { id: "StabilityRoom", label: "Stability Room" },
    { id: "ColdRoom", label: "Cold Room" },
  ];

  // Summary counts
  const [storageSummary, setStorageSummary] = useState({
    pending: 0,
    assigned: 0,
    completed: 0,
    total: 0,
  });

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples and temperature logs
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

    let samplesData = [];
    let routingData = [];
    let loadCount = 0;

    const processData = () => {
      loadCount++;
      if (loadCount < 2) return;

      if (componentMounted.current) {
        const routingMap = {};
        routingData.forEach((routing) => {
          if (routing.destinationType === "STORAGE" && routing.sampleItemId) {
            routingMap[String(routing.sampleItemId)] = {
              boxId: routing.boxId,
              boxName: routing.boxName,
              wellCoordinate: routing.wellCoordinate,
              routedAt: routing.routedAt,
            };
          }
        });

        const transformedSamples = samplesData.map((sample) => {
          const sampleId = String(sample.id || sample.sampleItemId);
          const routing = routingMap[sampleId];

          let storageLocation = sample.data?.storageLocation || null;
          if (!storageLocation && routing) {
            storageLocation = routing.boxName
              ? `${routing.boxName} - ${routing.wellCoordinate}`
              : routing.wellCoordinate;
          }

          const storageCondition = sample.data?.storageCondition || null;

          let status = sample.pageStatus || "PENDING";
          if (storageLocation && status === "PENDING") {
            status = "IN_PROGRESS";
          }

          return {
            id: sampleId,
            externalId: sample.externalId,
            accessionNumber: sample.accessionNumber,
            sampleType: sample.sampleType || sample.typeOfSample?.description,
            collectionDate: sample.collectionDate,
            status: status,
            sampleCategory: sample.data?.sampleCategory,
            sampleMaterial: sample.data?.sampleMaterial,
            lotNumber: sample.data?.lotNumber,
            chemicalName: sample.data?.chemicalName,
            grade: sample.data?.grade,
            storageLocation: storageLocation,
            storageCondition: storageCondition,
            retentionExpiry: sample.data?.retentionExpiry || null,
            boxId: sample.data?.boxId || routing?.boxId || null,
            wellCoordinate:
              sample.data?.wellCoordinate || routing?.wellCoordinate || null,
            data: sample.data,
          };
        });

        setSamples(transformedSamples);

        const assigned = transformedSamples.filter(
          (s) => s.storageLocation,
        ).length;
        const completed = transformedSamples.filter(
          (s) => s.status === "COMPLETED",
        ).length;
        setStorageSummary({
          pending: transformedSamples.length - assigned,
          assigned: assigned,
          completed: completed,
          total: transformedSamples.length,
        });
        setLoading(false);
      }
    };

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (response && Array.isArray(response)) {
          samplesData = response;
        }
        processData();
      },
    );

    const nbId = notebookId || entryId;
    if (nbId) {
      getFromOpenElisServer(
        `/rest/notebook/${nbId}/routing?destinationType=STORAGE`,
        (response) => {
          if (response && Array.isArray(response)) {
            routingData = response;
          }
          processData();
        },
      );
    } else {
      processData();
    }
  }, [pageData?.id, entryId, notebookId]);

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

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = useCallback((selection) => {
    setStorageSelection(selection);
    setWellAssignments({});
  }, []);

  // Handle box layout loaded
  const handleBoxLayoutLoaded = useCallback((wells) => {
    setBoxLayout(wells || {});
  }, []);

  // Handle well click from BoxLayoutViewer
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        // Well is occupied by existing sample
        setError(
          intl.formatMessage(
            {
              id: "notebook.pharma.storage.wellOccupied",
              defaultMessage:
                "Well {well} is already occupied by {sample}. Choose another position.",
            },
            { well: wellCoord, sample: wellInfo.externalId || "a sample" },
          ),
        );
        return;
      }

      if (storageModalOpen) {
        // Single well assignment during modal
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
              id: "notebook.pharma.storage.selectSamplesFirst",
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

  // Handle selection change
  const handleSelectionChange = useCallback((selectedIds) => {
    setSelectedSampleIds(selectedIds.map(String));
  }, []);

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    const alreadyAssigned = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.storageLocation,
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
    });
  };

  const handleConfirmReassignment = () => {
    setConfirmReassignModalOpen(false);
    openStorageAssignmentModal(true);
  };

  // Auto-populate wells
  const handleAutoPopulate = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.selectBoxFirst",
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
            id: "notebook.pharma.storage.notEnoughWells",
            defaultMessage:
              "Not enough empty wells. {assigned} of {total} samples assigned.",
          },
          { assigned: sampleIndex, total: selectedSampleIds.length },
        ),
      );
    } else {
      setError(null);
      setSuccess(
        intl.formatMessage(
          {
            id: "notebook.pharma.storage.autoPopulateSuccess",
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

  // Handle storage assignment
  const handleAssignStorage = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (!selectedCondition) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.selectCondition",
          defaultMessage: "Please select a storage condition.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.noWellAssignments",
          defaultMessage:
            "Please assign samples to wells using Auto-Populate or click on wells.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const wellAssignmentsForBackend = {};
    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      wellAssignmentsForBackend[sampleId] = wellCoord;
    });

    const nbId = notebookId || entryId;
    const payload = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      boxId: storageSelection.box.id,
      wellAssignments: wellAssignmentsForBackend,
      condition: selectedCondition.id,
      retentionYears: retentionYears,
      reassign: isReassignment,
      pageId: pageData?.id,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${nbId}/samples/assign-storage`,
      JSON.stringify(payload),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          const messageId = isReassignment
            ? "notebook.pharma.storage.reassignSuccess"
            : "notebook.pharma.storage.assignSuccess";
          const defaultMessage = isReassignment
            ? "Successfully reassigned {count} samples to new storage location."
            : "Successfully assigned {count} samples to storage.";

          setSuccess(
            intl.formatMessage(
              { id: messageId, defaultMessage: defaultMessage },
              {
                count:
                  response.assignedCount || Object.keys(wellAssignments).length,
              },
            ),
          );
          setIsReassignment(false);
          setStorageModalOpen(false);
          setSelectedSampleIds([]);
          setWellAssignments({});
          loadPageSamples();
          // Reload box layout
          if (storageSelection.box && nbId) {
            getFromOpenElisServer(
              `/rest/notebook/${nbId}/box/${storageSelection.box.id}/layout`,
              (layoutResponse) => {
                if (componentMounted.current && layoutResponse) {
                  setBoxLayout(layoutResponse.wells || {});
                }
              },
            );
          }
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.pharma.storage.assignError",
                defaultMessage: "Failed to assign samples to storage.",
              }),
          );
        }
      },
    );
  };

  // Handle auto-assign (MNTD-style)
  const handleAutoAssign = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.noSamplesSelected",
          defaultMessage: "Please select samples to auto-assign.",
        }),
      );
      return;
    }

    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.selectBoxFirst",
          defaultMessage: "Please select a storage box first.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.pageNotInit",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setIsAutoAssigning(true);
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

    // Get list of occupied wells from current box layout
    const occupiedWells = Object.keys(boxLayout);

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
      },
      boxId: storageSelection.box?.id,
      rows: storageSelection.box?.rows || 8,
      columns: storageSelection.box?.columns || 12,
      occupiedWells: occupiedWells,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/storage/auto-assign`,
      JSON.stringify(autoAssignData),
      (status, response) => {
        setIsAutoAssigning(false);
        if (status === 200) {
          const responseData =
            typeof response === "string" ? JSON.parse(response) : response;
          const assignmentCount = responseData?.updatedCount || 0;

          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.pharma.storage.autoAssignSuccess",
                defaultMessage:
                  "Auto-assigned {count} sample(s) to storage in {box}.",
              },
              {
                count: assignmentCount,
                box: storageSelection.box?.label,
              },
            ),
          );
          setAutoAssignModalOpen(false);
          loadPageSamples();
          setSelectedSampleIds([]);
          // Reload box layout
          const nbId = notebookId || entryId;
          if (storageSelection.box && nbId) {
            getFromOpenElisServer(
              `/rest/notebook/${nbId}/box/${storageSelection.box.id}/layout`,
              (layoutResponse) => {
                if (componentMounted.current && layoutResponse) {
                  setBoxLayout(layoutResponse.wells || {});
                }
              },
            );
          }
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          const errorData =
            typeof response === "string" ? JSON.parse(response) : response;
          setError(
            errorData?.error ||
              intl.formatMessage({
                id: "notebook.pharma.storage.autoAssignError",
                defaultMessage:
                  "Failed to auto-assign storage. Please try again.",
              }),
          );
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
    notebookId,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle temperature logging
  const handleLogTemperature = useCallback(() => {
    if (!temperatureLog.deviceId || !temperatureLog.temperatureValue) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.tempLogRequired",
          defaultMessage: "Device ID and temperature value are required.",
        }),
      );
      return;
    }

    setIsLoggingTemp(true);
    setError(null);

    const logData = {
      entryId: entryId,
      freezerId: temperatureLog.deviceId, // Using freezerId field for device ID
      deviceType: temperatureLog.deviceType,
      checkTime: temperatureLog.checkTime,
      temperatureValue: parseFloat(temperatureLog.temperatureValue),
      temperatureUnit: temperatureLog.temperatureUnit,
      humidityValue: temperatureLog.humidityValue
        ? parseFloat(temperatureLog.humidityValue)
        : null,
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
          setSuccess(
            intl.formatMessage({
              id: "notebook.pharma.storage.tempLogSuccess",
              defaultMessage: "Temperature logged successfully.",
            }),
          );
          setTempMonitoringModalOpen(false);
          loadTemperatureLogs();
          // Reset form but keep device ID
          setTemperatureLog((prev) => ({
            deviceId: prev.deviceId,
            deviceType: prev.deviceType,
            checkTime: "AM",
            temperatureValue: "",
            temperatureUnit: "C",
            humidityValue: "",
            checkedBy: "",
            checkedDateTime: new Date().toISOString().slice(0, 16),
            notes: "",
          }));
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.pharma.storage.tempLogError",
              defaultMessage: "Failed to log temperature. Please try again.",
            }),
          );
        }
      },
    );
  }, [temperatureLog, entryId, intl, loadTemperatureLogs]);

  // Handle mark samples complete
  const handleMarkComplete = () => {
    const storedSamples = samples.filter(
      (s) => s.status !== "COMPLETED" && s.storageLocation,
    );

    if (storedSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.storage.noStoredSamples",
          defaultMessage:
            "No stored samples to mark complete. Assign storage first.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const sampleIds = storedSamples.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.pharma.storage.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to mark samples complete.");
        }
      },
    );
  };

  // Get temperature status tag for logs
  const getTemperatureStatusTag = (log) => {
    // Define acceptable ranges per device type
    const ranges = {
      Refrigerator: { min: 2, max: 8 },
      "Freezer-20": { min: -25, max: -15 },
      "Freezer-80": { min: -85, max: -75 },
      LN2Tank: { min: -200, max: -180 },
      Incubator: { min: 35, max: 38 },
      StabilityRoom: { min: 20, max: 25 },
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

  // Render storage status tag
  const renderStorageTag = (sample) => {
    if (sample.status === "COMPLETED" && sample.storageLocation) {
      return (
        <Tag type="green" renderIcon={Checkmark}>
          {sample.storageLocation}
        </Tag>
      );
    }
    if (sample.storageLocation) {
      return (
        <Tag type="cyan" renderIcon={Archive}>
          {sample.storageLocation}
        </Tag>
      );
    }
    return (
      <Tag type="gray">
        <FormattedMessage
          id="notebook.status.pending"
          defaultMessage="Pending"
        />
      </Tag>
    );
  };

  // Render condition tag
  const renderConditionTag = (sample) => {
    if (!sample.storageCondition) return null;

    const conditionLabels = {
      ROOM_TEMP: "15-25°C",
      REFRIGERATED: "2-8°C",
      FROZEN_MINUS20: "-20°C",
      FROZEN_MINUS80: "-80°C",
      LIQUID_NITROGEN: "LN2",
      PROTECT_LIGHT: "Amber",
      HUMIDITY_CONTROLLED: "≤60% RH",
      DESICCATED: "Dry",
    };

    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {conditionLabels[sample.storageCondition] || sample.storageCondition}
      </Tag>
    );
  };

  // Build hierarchical path
  const getHierarchicalPath = () => {
    const parts = [];
    if (storageSelection.room) parts.push(storageSelection.room.label);
    if (storageSelection.device) parts.push(storageSelection.device.label);
    if (storageSelection.shelf) parts.push(storageSelection.shelf.label);
    if (storageSelection.rack) parts.push(storageSelection.rack.label);
    if (storageSelection.box) parts.push(storageSelection.box.label);
    return parts.join(" > ");
  };

  // Grid columns
  const columns = [
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "notebook.column.externalId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "chemicalName",
      header: intl.formatMessage({
        id: "notebook.column.chemicalName",
        defaultMessage: "Chemical/Product",
      }),
    },
    {
      key: "lotNumber",
      header: intl.formatMessage({
        id: "notebook.column.lotNumber",
        defaultMessage: "Lot / Batch",
      }),
    },
    {
      key: "storage",
      header: intl.formatMessage({
        id: "notebook.column.storage",
        defaultMessage: "Storage Status",
      }),
      render: (value, sample) => (
        <div style={{ display: "flex", gap: "4px", alignItems: "center" }}>
          {renderStorageTag(sample)}
          {renderConditionTag(sample)}
        </div>
      ),
    },
    {
      key: "retentionExpiry",
      header: intl.formatMessage({
        id: "notebook.column.expiry",
        defaultMessage: "Retention Expiry",
      }),
      render: (value, sample) =>
        sample.retentionExpiry ? (
          <span>{sample.retentionExpiry}</span>
        ) : (
          <span style={{ color: "#8d8d8d" }}>-</span>
        ),
    },
  ];

  return (
    <div className="pharma-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.storage.title"
            defaultMessage="Storage &amp; Inventory Management"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.storage.description"
            defaultMessage="Assign samples to hierarchical storage locations with pharmaceutical-specific conditions, retention tracking, and environmental monitoring."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Storage Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.storage.total"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{storageSummary.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.storage.assigned"
                  defaultMessage="Assigned"
                />
              </span>
              <span className="progress-value">{storageSummary.assigned}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.storage.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{storageSummary.pending}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.storage.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{storageSummary.completed}</span>
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
            id="notebook.pharma.storage.logTemperature"
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
              id="notebook.pharma.storage.autoAssign"
              defaultMessage="Auto-Assign ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Archive}
          onClick={handleOpenStorageModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.pharma.storage.assignSelected"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Checkmark}
          onClick={handleMarkComplete}
          disabled={
            storageSummary.assigned === 0 || assigning || !hasRealPageId
          }
        >
          <FormattedMessage
            id="notebook.pharma.storage.markComplete"
            defaultMessage="Mark Complete"
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.pharma.storage.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Storage Location & Box Layout */}
      <Grid fullWidth style={{ marginTop: "1rem" }}>
        <Column lg={8} md={4} sm={4}>
          <Tile>
            <h5 style={{ marginBottom: "1rem" }}>
              <Location size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="notebook.pharma.storage.storageLocation"
                defaultMessage="Storage Location"
              />
            </h5>
            <StorageHierarchySelector
              onSelectionChange={handleStorageSelectionChange}
              entryId={notebookId || entryId}
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
                  id="notebook.pharma.storage.boxLayout"
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
                    id="notebook.pharma.storage.selectBoxPrompt"
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
              id="notebook.pharma.storage.recentTempLogs"
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
                <strong>{log.freezerId || log.deviceId}:</strong>
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
            id="notebook.pharma.storage.sampleList"
            defaultMessage="Sample List"
          />
        </h5>
        <SampleGrid
          samples={samples}
          loading={loading}
          columns={columns}
          onSelectionChange={handleSelectionChange}
          selectedIds={selectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          emptyStateMessage={intl.formatMessage({
            id: "notebook.pharma.storage.noSamples",
            defaultMessage: "No samples available for storage assignment.",
          })}
        />
      </div>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.pharma.storage.modal.assign",
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
          !selectedCondition ||
          Object.keys(wellAssignments).length === 0 ||
          assigning
        }
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.pharma.storage.modal.description"
              defaultMessage="Assign {count} selected samples to storage."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Hierarchical Path Display */}
          {getHierarchicalPath() && (
            <div
              style={{
                backgroundColor: "#f4f4f4",
                padding: "0.75rem 1rem",
                borderRadius: "4px",
              }}
            >
              <strong>
                <FormattedMessage
                  id="notebook.pharma.storage.path"
                  defaultMessage="Storage Path:"
                />
              </strong>{" "}
              {getHierarchicalPath()}
            </div>
          )}

          {/* Box Layout in Modal */}
          {storageSelection.box && (
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
                  <FormattedMessage
                    id="notebook.pharma.storage.boxLayout"
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
                    id="notebook.pharma.storage.autoPopulate"
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
                  id="notebook.pharma.storage.assignmentSummary"
                  defaultMessage="{assigned} of {total} samples assigned to wells"
                  values={{
                    assigned: Object.keys(wellAssignments).length,
                    total: selectedSampleIds.length,
                  }}
                />
              </div>
            </div>
          )}

          {/* Storage Condition Selector */}
          <Dropdown
            id="storage-condition-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.pharma.storage.condition",
              defaultMessage: "Storage Condition *",
            })}
            label={intl.formatMessage({
              id: "notebook.pharma.storage.selectCondition",
              defaultMessage: "Select condition...",
            })}
            items={storageConditionOptions}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedCondition}
            onChange={({ selectedItem }) => setSelectedCondition(selectedItem)}
          />

          {/* Retention Period */}
          <NumberInput
            id="retention-years"
            label={intl.formatMessage({
              id: "notebook.pharma.storage.retentionYears",
              defaultMessage: "Retention Period (Years)",
            })}
            value={retentionYears}
            min={1}
            max={30}
            step={1}
            onChange={(e, { value }) => setRetentionYears(value)}
            helperText={intl.formatMessage(
              {
                id: "notebook.pharma.storage.expiryDate",
                defaultMessage: "Expiry date will be: {date}",
              },
              {
                date: new Date(
                  Date.now() + retentionYears * 365 * 24 * 60 * 60 * 1000,
                ).toLocaleDateString(),
              },
            )}
          />

          {/* Notes */}
          <TextArea
            id="storage-notes"
            labelText={intl.formatMessage({
              id: "notebook.pharma.storage.notes",
              defaultMessage: "Notes",
            })}
            value={bulkAssignValues.notes}
            onChange={(e) =>
              setBulkAssignValues((prev) => ({
                ...prev,
                notes: e.target.value,
              }))
            }
            placeholder="Optional notes about storage assignment..."
          />
        </div>
      </Modal>

      {/* Auto-Assign Modal */}
      <Modal
        open={autoAssignModalOpen}
        onRequestClose={() => setAutoAssignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.storage.autoAssign.title",
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
        onSecondarySubmit={() => setAutoAssignModalOpen(false)}
        size="md"
        primaryButtonDisabled={isAutoAssigning}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.pharma.storage.autoAssign.description"
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
                  id="notebook.pharma.storage.path"
                  defaultMessage="Storage Path:"
                />
              </strong>{" "}
              {getHierarchicalPath()}
              <div style={{ marginTop: "0.5rem" }}>
                <Tag type="blue">
                  <FormattedMessage
                    id="notebook.pharma.storage.availableWells"
                    defaultMessage="{available} wells available"
                    values={{
                      available:
                        (storageSelection.box?.rows || 8) *
                          (storageSelection.box?.columns || 12) -
                        Object.keys(boxLayout).length,
                    }}
                  />
                </Tag>
              </div>
            </div>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="autoAssignStorageType"
              labelText={intl.formatMessage({
                id: "notebook.pharma.storage.storageType",
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
              <SelectItem value="Raw" text="Raw Material" />
              <SelectItem value="API" text="Active Pharmaceutical Ingredient" />
              <SelectItem value="Finished" text="Finished Product" />
              <SelectItem value="Stability" text="Stability Sample" />
              <SelectItem value="Retention" text="Retention Sample" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="autoAssignAssignedBy"
              labelText={intl.formatMessage({
                id: "notebook.pharma.storage.assignedBy",
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

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="autoAssignNotes"
              labelText={intl.formatMessage({
                id: "notebook.pharma.storage.notes",
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
        </Grid>
      </Modal>

      {/* Temperature Monitoring Modal */}
      <Modal
        open={tempMonitoringModalOpen}
        onRequestClose={() => setTempMonitoringModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.storage.tempLog.title",
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
            id="notebook.pharma.storage.tempLog.description"
            defaultMessage="Record temperature and humidity monitoring data for environmental compliance and GMP documentation."
          />
        </p>

        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="deviceId"
              labelText={intl.formatMessage({
                id: "notebook.pharma.storage.deviceId",
                defaultMessage: "Device/Equipment ID *",
              })}
              value={temperatureLog.deviceId}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  deviceId: e.target.value,
                }))
              }
              placeholder="e.g., REF-001, FRZ-002"
              required
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="deviceType"
              labelText={intl.formatMessage({
                id: "notebook.pharma.storage.deviceType",
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
              {deviceTypes.map((type) => (
                <SelectItem key={type.id} value={type.id} text={type.label} />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.pharma.storage.checkTime",
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
                id: "notebook.pharma.storage.temperatureValue",
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
                id: "notebook.pharma.storage.temperatureUnit",
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
            <NumberInput
              id="humidityValue"
              label={intl.formatMessage({
                id: "notebook.pharma.storage.humidityValue",
                defaultMessage: "Humidity (% RH)",
              })}
              value={temperatureLog.humidityValue}
              onChange={(e, { value }) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  humidityValue: value,
                }))
              }
              min={0}
              max={100}
              step={1}
              helperText="Optional - for humidity-controlled areas"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="checkedBy"
              labelText={intl.formatMessage({
                id: "notebook.pharma.storage.checkedBy",
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
                  id="notebook.pharma.storage.checkedDateTime"
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
                id: "notebook.pharma.storage.notes",
                defaultMessage: "Notes",
              })}
              value={temperatureLog.notes}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Optional notes about the temperature check, deviations, etc."
            />
          </Column>
        </Grid>
      </Modal>

      {/* Reassignment Confirmation Modal */}
      <Modal
        open={confirmReassignModalOpen}
        onRequestClose={() => setConfirmReassignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.storage.reassignConfirm.title",
          defaultMessage: "Confirm Reassignment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.pharma.storage.reassignConfirm.confirm",
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
              id="notebook.pharma.storage.reassignConfirm.warning"
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
                id="notebook.pharma.storage.reassignConfirm.currentLocations"
                defaultMessage="Current storage locations:"
              />
            </strong>
            <ul style={{ marginTop: "0.5rem", paddingLeft: "1.5rem" }}>
              {samplesToReassign.map((sample) => (
                <li key={sample.id} style={{ fontSize: "0.875rem" }}>
                  <strong>{sample.externalId}</strong>: {sample.storageLocation}
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

export default PharmaceuticalStoragePage;
