import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  Modal,
  NumberInput,
  Dropdown,
  Select,
  SelectItem,
} from "@carbon/react";
import {
  Archive,
  Temperature,
  Undo,
  Checkmark,
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
 * PathologyStorageInventoryPage - Page 5 of the pathology workflow.
 * Purpose: Track physical storage and retrieval of pathology materials.
 * Who uses it: Store manager / lab staff
 *
 * Features:
 * - Hierarchical storage selection (Room → Device → Shelf → Rack → Box)
 * - 96-well grid visualization with auto-populate functionality
 * - Storage condition and retention period tracking
 * - Temperature monitoring (AM/PM checks)
 * - Sample retrieval with signature tracking
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PathologyStorageInventoryPage({
  entryId,
  notebookId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Storage assignment modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);

  // Reassignment confirmation modal state
  const [confirmReassignModalOpen, setConfirmReassignModalOpen] =
    useState(false);
  const [samplesToReassign, setSamplesToReassign] = useState([]);
  const [isReassignment, setIsReassignment] = useState(false);

  // Auto-assign modal state
  const [autoAssignModalOpen, setAutoAssignModalOpen] = useState(false);
  const [isAutoAssigning, setIsAutoAssigning] = useState(false);
  const [autoAssignValues, setAutoAssignValues] = useState({
    storageType: "",
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
  });

  // Storage hierarchy using StorageHierarchySelector
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });

  // Temperature logs state
  const [temperatureLogs, setTemperatureLogs] = useState([]);

  // Box layout state
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});

  // Storage form fields
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [expectedDuration, setExpectedDuration] = useState("");

  // Temperature log modal state
  const [tempLogModalOpen, setTempLogModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [tempLogData, setTempLogData] = useState({
    storageUnit: "",
    temperatureCheckAM: 4,
    temperatureCheckPM: 4,
    checkedBy: "",
    checkDate: "",
  });

  // Retrieval modal state
  const [retrievalModalOpen, setRetrievalModalOpen] = useState(false);
  const [selectedSampleForRetrieval, setSelectedSampleForRetrieval] =
    useState(null);
  const [retrievalData, setRetrievalData] = useState({
    dateRetrieved: "",
    retrievedBy: "",
    recipientSignature: "",
  });

  // Storage condition options matching backend StorageCondition enum
  const storageConditionOptions = [
    {
      id: "ROOM_TEMP",
      label: intl.formatMessage({
        id: "pathology.storage.condition.roomTemp",
        defaultMessage: "Room Temperature Cabinet (15-25°C)",
      }),
    },
    {
      id: "REFRIGERATED",
      label: intl.formatMessage({
        id: "pathology.storage.condition.refrigerated",
        defaultMessage: "Refrigerated (2-8°C)",
      }),
    },
    {
      id: "FROZEN_MINUS20",
      label: intl.formatMessage({
        id: "pathology.storage.condition.frozen20",
        defaultMessage: "Frozen (-20°C)",
      }),
    },
    {
      id: "FROZEN_MINUS80",
      label: intl.formatMessage({
        id: "pathology.storage.condition.frozen80",
        defaultMessage: "Frozen (-80°C)",
      }),
    },
    {
      id: "LIQUID_NITROGEN",
      label: intl.formatMessage({
        id: "pathology.storage.condition.liquidNitrogen",
        defaultMessage: "LN2 Vapor (-196°C)",
      }),
    },
    {
      id: "SLIDE_BOX",
      label: intl.formatMessage({
        id: "pathology.storage.condition.slideBox",
        defaultMessage: "Slide Box",
      }),
    },
  ];

  // Summary counts
  const [storageSummary, setStorageSummary] = useState({
    pending: 0,
    stored: 0,
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

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => {
              const sampleId = String(sample.id || sample.sampleItemId);

              // Get storage location from sample data
              const storageLocation = sample.data?.storageLocation || null;
              const storageCondition = sample.data?.storageCondition || null;

              // Determine status
              let status = sample.pageStatus || "PENDING";
              if (storageLocation && status === "PENDING") {
                status = "IN_PROGRESS";
              }

              return {
                id: sampleId,
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType:
                  sample.sampleType || sample.typeOfSample?.description,
                specimenType:
                  sample.sampleType || sample.typeOfSample?.description,
                collectionDate: sample.collectionDate,
                status: status,
                storageLocation: storageLocation,
                storageCondition: storageCondition,
                retentionExpiry: sample.data?.retentionExpiry || null,
                boxId: sample.data?.boxId || null,
                wellCoordinate: sample.data?.wellCoordinate || null,
                data: sample.data,
              };
            });

            setSamples(transformedSamples);

            // Calculate summary
            const stored = transformedSamples.filter(
              (s) => s.storageLocation || s.status === "COMPLETED",
            ).length;
            setStorageSummary({
              pending: transformedSamples.length - stored,
              stored: stored,
              total: transformedSamples.length,
            });
          } else {
            setSamples([]);
            setStorageSummary({ pending: 0, stored: 0, total: 0 });
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Load temperature logs
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

  // Handle box layout loaded from StorageHierarchySelector
  const handleBoxLayoutLoaded = useCallback((wells) => {
    setBoxLayout(wells || {});
  }, []);

  // Handle selection change
  const handleSelectionChange = useCallback((selectedIds) => {
    setSelectedSampleIds(selectedIds.map(String));
  }, []);

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    // Check if any selected samples already have storage assignments
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

  // Open the storage assignment modal
  const openStorageAssignmentModal = (reassigning) => {
    setIsReassignment(reassigning);
    setStorageModalOpen(true);
    setError(null);
    // Keep storage selection from main page
    setWellAssignments({});
    setSelectedCondition(null);
    setRetentionYears(5);
    setExpectedDuration("");
  };

  // Handle confirmation of reassignment
  const handleConfirmReassignment = () => {
    setConfirmReassignModalOpen(false);
    openStorageAssignmentModal(true);
  };

  // Auto-populate wells
  const handleAutoPopulate = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectBoxFirst",
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
            id: "pathology.storage.notEnoughWells",
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
            id: "pathology.storage.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Handle well click from BoxLayoutViewer
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        // Well is occupied by existing sample
        setError(
          intl.formatMessage(
            {
              id: "pathology.storage.wellOccupied",
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
        // Quick assignment outside modal - open modal if samples selected
        if (selectedSampleIds.length === 0) {
          setError(
            intl.formatMessage({
              id: "pathology.storage.selectSamplesFirst",
              defaultMessage:
                "Please select samples to assign to storage first.",
            }),
          );
          return;
        }
        setStorageModalOpen(true);
      }
    },
    [selectedSampleIds, wellAssignments, storageModalOpen, intl],
  );

  // Build combined layout for visualization
  const getCombinedLayout = () => {
    const combined = { ...boxLayout };

    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      if (!combined[wellCoord]) {
        const sample = samples.find((s) => s.id === sampleId);
        combined[wellCoord] = {
          sampleItemId: sampleId,
          externalId: sample?.externalId || sample?.accessionNumber || sampleId,
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
          id: "pathology.storage.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (!selectedCondition) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectCondition",
          defaultMessage: "Please select a storage condition.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noWellAssignments",
          defaultMessage:
            "Please assign samples to wells using Auto-Populate or click on wells.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    // Build well assignments with string keys (backend expects Map<String, String>)
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
            ? "pathology.storage.reassignSuccess"
            : "pathology.storage.assignSuccess";
          const defaultMessage = isReassignment
            ? "Successfully reassigned {count} samples to new storage location."
            : "Successfully assigned {count} samples to storage.";

          setSuccess(
            intl.formatMessage(
              {
                id: messageId,
                defaultMessage: defaultMessage,
              },
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
                id: "pathology.storage.assignError",
                defaultMessage: "Failed to assign samples to storage.",
              }),
          );
        }
      },
    );
  };

  // Handle mark complete
  const handleMarkComplete = () => {
    const pendingSamples = samples.filter(
      (s) => s.status !== "COMPLETED" && s.storageLocation,
    );

    if (pendingSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noStoredSamples",
          defaultMessage:
            "No stored samples to mark complete. Assign storage first.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const sampleIds = pendingSamples.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.storage.completeSuccess",
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

  // Temperature log handlers
  const handleInputChange = (e, setState) => {
    const { name, value } = e.target;
    setState((prev) => ({ ...prev, [name]: value }));
  };

  const handleDateChange = (dates, fieldName, setState) => {
    if (dates?.[0]) {
      setState((prev) => ({
        ...prev,
        [fieldName]: dates[0].toISOString().split("T")[0],
      }));
    }
  };

  const handleSubmitTempLog = () => {
    if (submitting) return;
    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/temperature-log`,
      JSON.stringify({ pageId: pageData?.id, ...tempLogData }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setTempLogModalOpen(false);
          setSuccess(
            intl.formatMessage({
              id: "pathology.storage.tempLogSuccess",
              defaultMessage: "Temperature logged successfully.",
            }),
          );
        } else {
          setError("Failed to log temperature. Please try again.");
        }
      },
    );
  };

  // Retrieval handlers
  const openRetrievalModal = (sample) => {
    setSelectedSampleForRetrieval(sample);
    setRetrievalData({
      dateRetrieved: new Date().toISOString().split("T")[0],
      retrievedBy: "",
      recipientSignature: "",
    });
    setRetrievalModalOpen(true);
  };

  const handleSubmitRetrieval = () => {
    if (submitting) return;
    if (!retrievalData.retrievedBy || !retrievalData.recipientSignature) {
      setError("Please fill in Retrieved By and Recipient Signature");
      return;
    }

    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/retrieve`,
      JSON.stringify({
        sampleId: selectedSampleForRetrieval?.id,
        pageId: pageData?.id,
        ...retrievalData,
      }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setRetrievalModalOpen(false);
          setSuccess(
            intl.formatMessage({
              id: "pathology.storage.retrievalSuccess",
              defaultMessage: "Sample retrieval recorded successfully.",
            }),
          );
          loadPageSamples();
          onProgressUpdate?.();
        } else {
          setError("Failed to record retrieval. Please try again.");
        }
      },
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
      REFRIGERATED: "2-8°C",
      FROZEN_MINUS20: "-20°C",
      FROZEN_MINUS80: "-80°C",
      ROOM_TEMP: "15-25°C",
      LIQUID_NITROGEN: "-196°C",
      SLIDE_BOX: "Slide Box",
    };

    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {conditionLabels[sample.storageCondition] || sample.storageCondition}
      </Tag>
    );
  };

  // Handle auto-assign
  const handleAutoAssign = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noSamplesSelected",
          defaultMessage: "Please select samples to auto-assign.",
        }),
      );
      return;
    }

    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectBoxFirst",
          defaultMessage: "Please select a storage box first.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.pageNotInit",
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
        storageDevice: storageSelection.device?.label,
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
                id: "pathology.storage.autoAssignSuccess",
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
                id: "pathology.storage.autoAssignError",
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
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

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
      ROOM_TEMP: { min: 15, max: 25 },
      REFRIGERATED: { min: 2, max: 8 },
      FROZEN_MINUS20: { min: -25, max: -15 },
      FROZEN_MINUS80: { min: -85, max: -75 },
      LIQUID_NITROGEN: { min: -200, max: -180 },
    };

    const range = ranges[log.deviceType] ||
      ranges[log.storageCondition] || { min: -999, max: 999 };
    const temp =
      log.temperatureValue || log.temperatureCheckAM || log.temperatureCheckPM;
    const inRange = temp >= range.min && temp <= range.max;

    return inRange ? (
      <Tag type="green" size="sm">
        {temp}°{log.temperatureUnit || "C"}
      </Tag>
    ) : (
      <Tag type="red" size="sm" renderIcon={Warning}>
        {temp}°{log.temperatureUnit || "C"} - OUT OF RANGE
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
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.column.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "specimenType",
      header: intl.formatMessage({
        id: "pathology.column.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "storage",
      header: intl.formatMessage({
        id: "pathology.column.storage",
        defaultMessage: "Storage Status",
      }),
      render: (sample) => (
        <div style={{ display: "flex", gap: "4px", alignItems: "center" }}>
          {renderStorageTag(sample)}
          {renderConditionTag(sample)}
        </div>
      ),
    },
    {
      key: "retentionExpiry",
      header: intl.formatMessage({
        id: "pathology.column.expiry",
        defaultMessage: "Retention Expiry",
      }),
      render: (sample) =>
        sample.retentionExpiry ? (
          <span>{sample.retentionExpiry}</span>
        ) : (
          <span className="text-muted">-</span>
        ),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "pathology.column.actions",
        defaultMessage: "Actions",
      }),
      render: (sample) =>
        sample.status === "COMPLETED" && sample.storageLocation ? (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Undo}
            onClick={(e) => {
              e.stopPropagation();
              openRetrievalModal(sample);
            }}
          >
            <FormattedMessage
              id="pathology.storage.retrieve"
              defaultMessage="Retrieve"
            />
          </Button>
        ) : null,
    },
  ];

  return (
    <div className="pathology-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.storage.title"
            defaultMessage="Storage & Inventory Management"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.storage.description"
            defaultMessage="Track physical storage and retrieval of pathology materials. Assign locations and log environmental monitoring."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
        />
      )}

      {/* Storage Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.total"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{storageSummary.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.stored"
                  defaultMessage="Stored"
                />
              </span>
              <span className="progress-value">{storageSummary.stored}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.pending"
                  defaultMessage="Pending Storage"
                />
              </span>
              <span className="progress-value">{storageSummary.pending}</span>
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
          onClick={() => setTempLogModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.storage.logTemperature"
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
              id="pathology.storage.autoAssign"
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
            id="pathology.storage.assignSelected"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Checkmark}
          onClick={handleMarkComplete}
          disabled={storageSummary.stored === 0 || assigning || !hasRealPageId}
        >
          <FormattedMessage
            id="pathology.storage.markComplete"
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
            id="pathology.storage.refresh"
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
                id="pathology.storage.storageLocation"
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
                  id="pathology.storage.boxLayout"
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
                    id="pathology.storage.selectBoxPrompt"
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
              id="pathology.storage.recentTempLogs"
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
                <strong>
                  {log.storageUnit || log.freezerId || log.deviceId}:
                </strong>
                {getTemperatureStatusTag(log)}
                <span style={{ fontSize: "0.75rem", color: "#525252" }}>
                  ({log.checkTime || "Check"})
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
            id="pathology.storage.sampleList"
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
          emptyStateMessage={intl.formatMessage({
            id: "pathology.storage.noSamples",
            defaultMessage: "No samples available for storage assignment.",
          })}
        />
      </div>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "pathology.storage.modal.assign",
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
              id="pathology.storage.modal.description"
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
                  id="pathology.storage.path"
                  defaultMessage="Storage Path:"
                />
              </strong>{" "}
              {getHierarchicalPath()}
            </div>
          )}

          {/* Box Layout Viewer */}
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
                    id="pathology.storage.boxLayout"
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
                    id="pathology.storage.autoPopulate"
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

              {/* Assignment Summary */}
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="pathology.storage.assignmentSummary"
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
              id: "pathology.storage.condition",
              defaultMessage: "Storage Condition",
            })}
            label={intl.formatMessage({
              id: "pathology.storage.selectCondition",
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
              id: "pathology.storage.retentionYears",
              defaultMessage: "Retention Period (Years)",
            })}
            value={retentionYears}
            min={1}
            max={30}
            step={1}
            onChange={(e, { value }) => setRetentionYears(value)}
            helperText={intl.formatMessage(
              {
                id: "pathology.storage.expiryDate",
                defaultMessage: "Expiry date will be: {date}",
              },
              {
                date: new Date(
                  Date.now() + retentionYears * 365 * 24 * 60 * 60 * 1000,
                ).toLocaleDateString(),
              },
            )}
          />

          {/* Expected Duration (pathology-specific) */}
          <TextInput
            id="expectedDuration"
            labelText={intl.formatMessage({
              id: "pathology.storage.expectedDuration",
              defaultMessage: "Expected Duration (Optional)",
            })}
            value={expectedDuration}
            onChange={(e) => setExpectedDuration(e.target.value)}
            placeholder={intl.formatMessage({
              id: "pathology.storage.expectedDurationPlaceholder",
              defaultMessage: "e.g., 6 months, 1 year",
            })}
          />
        </div>
      </Modal>

      {/* Reassignment Confirmation Modal */}
      <Modal
        open={confirmReassignModalOpen}
        onRequestClose={() => setConfirmReassignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.reassignConfirm.title",
          defaultMessage: "Confirm Reassignment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.storage.reassignConfirm.confirm",
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
              id="pathology.storage.reassignConfirm.warning"
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
                id="pathology.storage.reassignConfirm.currentLocations"
                defaultMessage="Current storage locations:"
              />
            </strong>
            <ul style={{ marginTop: "0.5rem", paddingLeft: "1.5rem" }}>
              {samplesToReassign.map((sample) => (
                <li key={sample.id} style={{ fontSize: "0.875rem" }}>
                  <strong>{sample.accessionNumber || sample.externalId}</strong>
                  : {sample.storageLocation}
                  {sample.storageCondition && ` (${sample.storageCondition})`}
                </li>
              ))}
            </ul>
          </div>

          <p style={{ fontStyle: "italic", color: "#525252" }}>
            <FormattedMessage
              id="pathology.storage.reassignConfirm.proceed"
              defaultMessage="Do you want to proceed with reassigning these samples to a new storage location?"
            />
          </p>
        </div>
      </Modal>

      {/* Temperature Log Modal */}
      <Modal
        open={tempLogModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.tempLog.title",
          defaultMessage: "Log Temperature",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setTempLogModalOpen(false)}
        onRequestSubmit={handleSubmitTempLog}
        primaryButtonDisabled={submitting}
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="tempStorageUnit"
              name="storageUnit"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.storageUnit",
                defaultMessage: "Storage Unit",
              })}
              value={tempLogData.storageUnit}
              onChange={(e) => handleInputChange(e, setTempLogData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="temperatureCheckAM"
              label={intl.formatMessage({
                id: "pathology.storage.tempLog.tempAM",
                defaultMessage: "Temperature AM (°C)",
              })}
              value={tempLogData.temperatureCheckAM}
              onChange={(e, { value }) =>
                setTempLogData((prev) => ({
                  ...prev,
                  temperatureCheckAM: value,
                }))
              }
              min={-200}
              max={200}
              step={0.1}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="temperatureCheckPM"
              label={intl.formatMessage({
                id: "pathology.storage.tempLog.tempPM",
                defaultMessage: "Temperature PM (°C)",
              })}
              value={tempLogData.temperatureCheckPM}
              onChange={(e, { value }) =>
                setTempLogData((prev) => ({
                  ...prev,
                  temperatureCheckPM: value,
                }))
              }
              min={-200}
              max={200}
              step={0.1}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="tempCheckedBy"
              name="checkedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.checkedBy",
                defaultMessage: "Checked By",
              })}
              value={tempLogData.checkedBy}
              onChange={(e) => handleInputChange(e, setTempLogData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "checkDate", setTempLogData)
              }
            >
              <DatePickerInput
                id="checkDate"
                labelText={intl.formatMessage({
                  id: "pathology.storage.tempLog.checkDate",
                  defaultMessage: "Check Date",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
        </Grid>
      </Modal>

      {/* Auto-Assign Modal */}
      <Modal
        open={autoAssignModalOpen}
        onRequestClose={() => setAutoAssignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.autoAssign.title",
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
            id="pathology.storage.autoAssign.description"
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
                  id="pathology.storage.path"
                  defaultMessage="Storage Path:"
                />
              </strong>{" "}
              {getHierarchicalPath()}
              <div style={{ marginTop: "0.5rem" }}>
                <Tag type="blue">
                  <FormattedMessage
                    id="pathology.storage.availableWells"
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
                id: "pathology.storage.storageType",
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
              <SelectItem value="Block" text="Tissue Block" />
              <SelectItem value="Slide" text="Slide" />
              <SelectItem value="Cassette" text="Cassette" />
              <SelectItem value="Sample" text="Sample" />
              <SelectItem value="Archive" text="Archive" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="autoAssignAssignedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.assignedBy",
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
                id: "pathology.storage.notes",
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

      {/* Retrieval Modal */}
      <Modal
        open={retrievalModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.storage.retrieval.title",
            defaultMessage: "Retrieve Sample - {accession}",
          },
          {
            accession:
              selectedSampleForRetrieval?.accessionNumber ||
              selectedSampleForRetrieval?.externalId ||
              "",
          },
        )}
        primaryButtonText={intl.formatMessage({
          id: "pathology.storage.retrieval.submit",
          defaultMessage: "Record Retrieval",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setRetrievalModalOpen(false)}
        onRequestSubmit={handleSubmitRetrieval}
        primaryButtonDisabled={submitting}
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "dateRetrieved", setRetrievalData)
              }
            >
              <DatePickerInput
                id="dateRetrieved"
                labelText={intl.formatMessage({
                  id: "pathology.storage.retrieval.dateRetrieved",
                  defaultMessage: "Date Retrieved *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="retrievedBy"
              name="retrievedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.retrieval.retrievedBy",
                defaultMessage: "Retrieved By *",
              })}
              value={retrievalData.retrievedBy}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="recipientSignature"
              name="recipientSignature"
              labelText={intl.formatMessage({
                id: "pathology.storage.retrieval.recipientSignature",
                defaultMessage: "Recipient Signature *",
              })}
              value={retrievalData.recipientSignature}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologyStorageInventoryPage;
