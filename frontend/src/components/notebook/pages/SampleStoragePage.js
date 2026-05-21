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
  Toggle,
  TimePicker,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import {
  Archive,
  Checkmark,
  Temperature,
  Renew,
  WarningAlt,
  Add,
  Time,
  DataCheck,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";
import BoxLayoutViewer from "../workflow/BoxLayoutViewer";
import "../workflow/NotebookWorkflow.css";

/**
 * SampleStoragePage - Storage Assignment & Environmental Monitoring page for MedLab workflow.
 *
 * Features:
 * - Storage Hierarchy: Room -> Device -> Shelf -> Rack -> Box -> Position
 * - Aliquot Mapping: Cryobox label (A01-A100), Well mapping (1-81), Link cryovial to box position
 * - Environmental Monitoring: Temperature reading, Time (AM/PM), Recorded by, Alarm triggered
 * - Location history log
 * - Temperature excursion alerts
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function SampleStoragePage({ entryId, pageData, progress, onProgressUpdate }) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [activeTab, setActiveTab] = useState(0);

  // Storage assignment modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);

  // Environmental monitoring modal state
  const [envMonitorModalOpen, setEnvMonitorModalOpen] = useState(false);
  const [savingEnvReading, setSavingEnvReading] = useState(false);

  // Environmental monitoring form
  const [envForm, setEnvForm] = useState({
    deviceId: null,
    temperatureReading: "",
    readingTime: "",
    readingPeriod: "AM",
    recordedBy: "",
    alarmTriggered: false,
    notes: "",
  });

  // Environmental readings history
  const [envReadings, setEnvReadings] = useState([]);
  const [temperatureAlerts, setTemperatureAlerts] = useState([]);

  // Reassignment confirmation modal state
  const [confirmReassignModalOpen, setConfirmReassignModalOpen] =
    useState(false);
  const [samplesToReassign, setSamplesToReassign] = useState([]);
  const [isReassignment, setIsReassignment] = useState(false);

  // Hierarchical storage selection state
  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);
  const [boxes, setBoxes] = useState([]);

  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);
  const [selectedBox, setSelectedBox] = useState(null);

  const [loadingHierarchy, setLoadingHierarchy] = useState(false);

  // Box layout state
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});

  // Storage form fields
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [cryovialId, setCryovialId] = useState("");

  // Storage condition options matching backend StorageCondition enum
  const storageConditionOptions = [
    {
      id: "REFRIGERATED",
      label: intl.formatMessage({
        id: "notebook.storage.condition.refrigerated",
        defaultMessage: "Refrigerated (2-8°C)",
      }),
      minTemp: 2,
      maxTemp: 8,
    },
    {
      id: "FROZEN_MINUS20",
      label: intl.formatMessage({
        id: "notebook.storage.condition.frozen20",
        defaultMessage: "Frozen (-20°C)",
      }),
      minTemp: -25,
      maxTemp: -15,
    },
    {
      id: "FROZEN_MINUS80",
      label: intl.formatMessage({
        id: "notebook.storage.condition.frozen80",
        defaultMessage: "Frozen (-80°C)",
      }),
      minTemp: -85,
      maxTemp: -75,
    },
    {
      id: "ROOM_TEMP",
      label: intl.formatMessage({
        id: "notebook.storage.condition.roomTemp",
        defaultMessage: "Room Temperature (15-25°C)",
      }),
      minTemp: 15,
      maxTemp: 25,
    },
    {
      id: "LIQUID_NITROGEN",
      label: intl.formatMessage({
        id: "notebook.storage.condition.liquidNitrogen",
        defaultMessage: "Liquid Nitrogen (-196°C)",
      }),
      minTemp: -200,
      maxTemp: -190,
    },
  ];

  // Time period options
  const timePeriodOptions = [
    { id: "AM", label: "AM" },
    { id: "PM", label: "PM" },
  ];

  // Summary counts
  const [storageSummary, setStorageSummary] = useState({
    pending: 0,
    assigned: 0,
    total: 0,
  });

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples
  const loadPageSamples = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Use the dedicated endpoint for storage samples (samples that passed QC)
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/samples-for-storage`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.sampleItemId || sample.id),
              sampleItemId: sample.sampleItemId || sample.id,
              externalId: sample.labNo || sample.accessionNumber,
              accessionNumber: sample.labNo || sample.accessionNumber,
              sampleType: sample.sampleType,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              storageLocation: sample.storageLocation || null,
              storageCondition: sample.storageCondition || null,
              retentionExpiry: sample.retentionExpiry || null,
              boxId: sample.boxId || null,
              wellCoordinate: sample.wellCoordinate || null,
              cryovialId: sample.cryovialId || null,
              data: sample.data || {},
            }));

            setSamples(transformedSamples);

            // Calculate summary
            const assigned = transformedSamples.filter(
              (s) => s.storageLocation || s.status === "COMPLETED",
            ).length;
            setStorageSummary({
              pending: transformedSamples.length - assigned,
              assigned: assigned,
              total: transformedSamples.length,
            });
          } else {
            setSamples([]);
            setStorageSummary({ pending: 0, assigned: 0, total: 0 });
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  // Load environmental readings
  const loadEnvironmentalReadings = useCallback(() => {
    if (!entryId) return;

    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/environmental-readings`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setEnvReadings(response);

          // Check for temperature excursions
          const alerts = response.filter(
            (r) => r.alarmTriggered || r.excursion,
          );
          setTemperatureAlerts(alerts);
        }
      },
    );
  }, [entryId]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadEnvironmentalReadings();
    loadRooms();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples, loadEnvironmentalReadings]);

  // Load rooms
  const loadRooms = () => {
    getFromOpenElisServer("/rest/storage/rooms?status=active", (response) => {
      if (componentMounted.current && response && Array.isArray(response)) {
        setRooms(
          response.map((r) => ({
            id: r.id,
            label: r.name,
            ...r,
          })),
        );
      }
    });
  };

  // Load devices when room changes
  const handleRoomChange = ({ selectedItem }) => {
    setSelectedRoom(selectedItem);
    setSelectedDevice(null);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setDevices([]);
    setShelves([]);
    setRacks([]);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/devices?roomId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setDevices(
              response.map((d) => ({
                id: d.id,
                label: d.name,
                type: d.type,
                ...d,
              })),
            );
          }
        },
      );
    }
  };

  // Load shelves when device changes
  const handleDeviceChange = ({ selectedItem }) => {
    setSelectedDevice(selectedItem);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setShelves([]);
    setRacks([]);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/shelves?deviceId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setShelves(
              response.map((s) => ({
                id: s.id,
                label: s.label || s.name,
                ...s,
              })),
            );
          }
        },
      );
    }
  };

  // Load racks when shelf changes
  const handleShelfChange = ({ selectedItem }) => {
    setSelectedShelf(selectedItem);
    setSelectedRack(null);
    setSelectedBox(null);
    setRacks([]);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/racks?shelfId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setRacks(
              response.map((r) => ({
                id: r.id,
                label: r.label || r.name,
                ...r,
              })),
            );
          }
        },
      );
    }
  };

  // Load boxes when rack changes
  const handleRackChange = ({ selectedItem }) => {
    setSelectedRack(selectedItem);
    setSelectedBox(null);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/boxes?rackId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setBoxes(
              response.map((b) => ({
                id: b.id,
                label: b.label || b.name,
                rows: b.rows || 9,
                columns: b.columns || 9,
                ...b,
              })),
            );
          }
        },
      );
    }
  };

  // Load box layout when box changes
  const handleBoxChange = ({ selectedItem }) => {
    setSelectedBox(selectedItem);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem && entryId) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/notebook/${entryId}/box/${selectedItem.id}/layout`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response) {
            const layoutData = response.wells || {};
            setBoxLayout(layoutData);
          }
        },
      );
    }
  };

  // Handle selection change
  const handleSelectionChange = useCallback((selectedIds) => {
    setSelectedSampleIds(selectedIds.map(String));
  }, []);

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.noSamplesSelected",
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
    // Reset selections
    setSelectedRoom(null);
    setSelectedDevice(null);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setDevices([]);
    setShelves([]);
    setRacks([]);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});
    setSelectedCondition(null);
    setRetentionYears(5);
    setCryovialId("");
  };

  // Handle confirmation of reassignment
  const handleConfirmReassignment = () => {
    setConfirmReassignModalOpen(false);
    openStorageAssignmentModal(true);
  };

  // Auto-populate wells (for cryobox: 9x9 = 81 wells, labeled 1-81 or A01-A81)
  const handleAutoPopulate = () => {
    if (!selectedBox) return;

    const rows = selectedBox.rows || 9;
    const columns = selectedBox.columns || 9;
    const rowLetters = Array.from({ length: rows }, (_, i) =>
      String.fromCharCode("A".charCodeAt(0) + i),
    );

    const newAssignments = {};
    let sampleIndex = 0;

    // Iterate through wells row by row
    for (let row of rowLetters) {
      for (let col = 1; col <= columns; col++) {
        if (sampleIndex >= selectedSampleIds.length) break;

        const wellCoord = `${row}${String(col).padStart(2, "0")}`;
        // Skip if well is already occupied
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
            id: "notebook.storage.notEnoughWells",
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
            id: "notebook.storage.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Handle well click
  const handleWellClick = (wellCoord, wellInfo) => {
    if (wellInfo) return; // Well is occupied

    const unassignedSamples = selectedSampleIds.filter(
      (id) => !wellAssignments[id],
    );
    if (unassignedSamples.length > 0) {
      setWellAssignments((prev) => ({
        ...prev,
        [unassignedSamples[0]]: wellCoord,
      }));
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
    if (!selectedBox) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (!selectedCondition) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.selectCondition",
          defaultMessage: "Please select a storage condition.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.noWellAssignments",
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
      wellAssignmentsForBackend[parseInt(sampleId, 10)] = wellCoord;
    });

    const payload = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      boxId: selectedBox.id,
      boxLabel: selectedBox.label,
      wellAssignments: wellAssignmentsForBackend,
      condition: selectedCondition.id,
      retentionYears: retentionYears,
      reassign: isReassignment,
      cryovialId: cryovialId || null,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServerJsonResponse(
      `/rest/medlab/entry/${entryId}/assign-storage`,
      JSON.stringify(payload),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.storage.assignSuccess",
                defaultMessage:
                  "Successfully assigned {count} samples to storage.",
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
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.storage.assignError",
                defaultMessage: "Failed to assign samples to storage.",
              }),
          );
        }
      },
    );
  };

  // Open environmental monitoring modal
  const handleOpenEnvMonitorModal = () => {
    setEnvMonitorModalOpen(true);
    setEnvForm({
      deviceId: selectedDevice?.id || null,
      temperatureReading: "",
      readingTime: new Date().toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      }),
      readingPeriod: new Date().getHours() < 12 ? "AM" : "PM",
      recordedBy: "",
      alarmTriggered: false,
      notes: "",
    });
  };

  // Save environmental reading
  const handleSaveEnvReading = () => {
    if (!envForm.temperatureReading) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.env.tempRequired",
          defaultMessage: "Temperature reading is required.",
        }),
      );
      return;
    }
    if (!envForm.recordedBy) {
      setError(
        intl.formatMessage({
          id: "notebook.storage.env.recordedByRequired",
          defaultMessage: "Recorded by is required.",
        }),
      );
      return;
    }

    setSavingEnvReading(true);
    setError(null);

    const payload = {
      entryId: entryId,
      deviceId: envForm.deviceId,
      temperatureReading: parseFloat(envForm.temperatureReading),
      readingTime: envForm.readingTime,
      readingPeriod: envForm.readingPeriod,
      recordedBy: envForm.recordedBy,
      alarmTriggered: envForm.alarmTriggered,
      notes: envForm.notes,
    };

    postToOpenElisServerJsonResponse(
      `/rest/medlab/entry/${entryId}/environmental-reading`,
      JSON.stringify(payload),
      (response) => {
        setSavingEnvReading(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage({
              id: "notebook.storage.env.saveSuccess",
              defaultMessage: "Environmental reading saved successfully.",
            }),
          );
          setEnvMonitorModalOpen(false);
          loadEnvironmentalReadings();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.storage.env.saveError",
                defaultMessage: "Failed to save environmental reading.",
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
          id: "notebook.storage.noStoredSamples",
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
                id: "notebook.storage.completeSuccess",
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
    if (selectedRoom) parts.push(selectedRoom.label);
    if (selectedDevice) parts.push(selectedDevice.label);
    if (selectedShelf) parts.push(selectedShelf.label);
    if (selectedRack) parts.push(selectedRack.label);
    if (selectedBox) parts.push(selectedBox.label);
    return parts.join(" > ");
  };

  // Grid columns
  const columns = [
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "notebook.column.labNo",
        defaultMessage: "Lab No",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "notebook.column.sampleType",
        defaultMessage: "Type",
      }),
    },
    {
      key: "cryovialId",
      header: intl.formatMessage({
        id: "notebook.column.cryovialId",
        defaultMessage: "Cryovial ID",
      }),
      render: (sample) => sample.cryovialId || "-",
    },
    {
      key: "storage",
      header: intl.formatMessage({
        id: "notebook.column.storage",
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
        id: "notebook.column.expiry",
        defaultMessage: "Retention Expiry",
      }),
      render: (sample) =>
        sample.retentionExpiry ? (
          <span>{sample.retentionExpiry}</span>
        ) : (
          <span className="text-muted">-</span>
        ),
    },
  ];

  // Environmental readings columns
  const envReadingHeaders = [
    { key: "recordedAt", header: "Date/Time" },
    { key: "deviceName", header: "Device" },
    { key: "temperature", header: "Temperature" },
    { key: "recordedBy", header: "Recorded By" },
    { key: "alarm", header: "Alarm" },
  ];

  return (
    <div className="sample-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.storage.title"
            defaultMessage="Storage Assignment & Environmental Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.storage.description"
            defaultMessage="Assign samples to storage locations, track cryovial positions, and record environmental readings."
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

      {/* Temperature Alerts */}
      {temperatureAlerts.length > 0 && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "notebook.storage.tempAlert",
            defaultMessage: "Temperature Excursion Alert",
          })}
          subtitle={intl.formatMessage(
            {
              id: "notebook.storage.tempAlertDesc",
              defaultMessage:
                "{count} temperature excursions recorded. Review environmental log.",
            },
            { count: temperatureAlerts.length },
          )}
          lowContrast
        />
      )}

      {/* Tabs */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Storage tabs">
          <Tab>
            <FormattedMessage
              id="notebook.storage.tab.assignment"
              defaultMessage="Storage Assignment"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.storage.tab.environmental"
              defaultMessage="Environmental Monitoring"
            />
          </Tab>
        </TabList>
        <TabPanels>
          {/* Storage Assignment Tab */}
          <TabPanel>
            {/* Storage Summary */}
            <Grid fullWidth className="progress-section">
              <Column lg={16} md={8} sm={4}>
                <div className="progress-tiles">
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="notebook.storage.pending"
                        defaultMessage="Pending Assignment"
                      />
                    </span>
                    <span className="progress-value">
                      {storageSummary.pending}
                    </span>
                  </Tile>
                  <Tile className="progress-tile verified">
                    <span className="progress-label">
                      <FormattedMessage
                        id="notebook.storage.assigned"
                        defaultMessage="Assigned to Storage"
                      />
                    </span>
                    <span className="progress-value">
                      {storageSummary.assigned}
                    </span>
                  </Tile>
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="notebook.storage.total"
                        defaultMessage="Total"
                      />
                    </span>
                    <span className="progress-value">
                      {storageSummary.total}
                    </span>
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
                  id="notebook.storage.assignSelected"
                  defaultMessage="Assign to Storage ({count})"
                  values={{ count: selectedSampleIds.length }}
                />
              </Button>

              <Button
                kind="secondary"
                size="sm"
                renderIcon={Checkmark}
                onClick={handleMarkComplete}
                disabled={
                  storageSummary.assigned === 0 || assigning || !hasRealPageId
                }
              >
                <FormattedMessage
                  id="notebook.storage.markComplete"
                  defaultMessage="Mark Stored Samples Complete"
                />
              </Button>
            </div>

            {/* Sample Grid */}
            <SampleGrid
              samples={samples}
              loading={loading}
              columns={columns}
              onSelectionChange={handleSelectionChange}
              selectedIds={selectedSampleIds}
              statusFilter={statusFilter}
              onStatusFilterChange={setStatusFilter}
              emptyStateMessage={intl.formatMessage({
                id: "notebook.storage.noSamples",
                defaultMessage:
                  "No samples available for storage assignment. Samples must pass QC first.",
              })}
            />
          </TabPanel>

          {/* Environmental Monitoring Tab */}
          <TabPanel>
            <Grid fullWidth className="progress-section">
              <Column lg={16} md={8} sm={4}>
                <div className="page-actions-bar">
                  <Button
                    kind="primary"
                    size="sm"
                    renderIcon={Add}
                    onClick={handleOpenEnvMonitorModal}
                  >
                    <FormattedMessage
                      id="notebook.storage.env.addReading"
                      defaultMessage="Record Temperature Reading"
                    />
                  </Button>
                </div>
              </Column>
            </Grid>

            {/* Environmental Readings Table */}
            <DataTable
              rows={envReadings.map((r, idx) => ({
                id: r.id || String(idx),
                recordedAt: r.recordedAt
                  ? new Date(r.recordedAt).toLocaleString()
                  : "-",
                deviceName: r.deviceName || "-",
                temperature: r.temperatureReading
                  ? `${r.temperatureReading}°C`
                  : "-",
                recordedBy: r.recordedBy || "-",
                alarm: r.alarmTriggered ? (
                  <Tag type="red" renderIcon={WarningAlt}>
                    Yes
                  </Tag>
                ) : (
                  <Tag type="green">No</Tag>
                ),
              }))}
              headers={envReadingHeaders}
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <TableContainer
                  title={intl.formatMessage({
                    id: "notebook.storage.env.history",
                    defaultMessage: "Temperature Reading History",
                  })}
                >
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
                      {rows.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={headers.length}>
                            <div
                              style={{ textAlign: "center", padding: "2rem" }}
                            >
                              <FormattedMessage
                                id="notebook.storage.env.noReadings"
                                defaultMessage="No environmental readings recorded yet."
                              />
                            </div>
                          </TableCell>
                        </TableRow>
                      ) : (
                        rows.map((row) => (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="medlab.page.storage.empty"
              defaultMessage="No samples available for storage assignment. Samples must pass QC assessment first."
            />
          </p>
        </div>
      )}

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.storage.modal.assign",
          defaultMessage: "Assign",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        primaryButtonDisabled={
          !selectedBox ||
          !selectedCondition ||
          Object.keys(wellAssignments).length === 0 ||
          assigning
        }
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.storage.modal.description"
              defaultMessage="Assign {count} selected samples to storage."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Hierarchical Storage Selection */}
          <div className="storage-hierarchy-selection">
            <Grid fullWidth narrow>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="room-dropdown"
                  titleText={intl.formatMessage({
                    id: "notebook.storage.room",
                    defaultMessage: "Room",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.storage.selectRoom",
                    defaultMessage: "Select room...",
                  })}
                  items={rooms}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={selectedRoom}
                  onChange={handleRoomChange}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="device-dropdown"
                  titleText={intl.formatMessage({
                    id: "notebook.storage.device",
                    defaultMessage: "Device (Refrigerator/Freezer)",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.storage.selectDevice",
                    defaultMessage: "Select device...",
                  })}
                  items={devices}
                  itemToString={(item) =>
                    item ? `${item.label} (${item.type || "Device"})` : ""
                  }
                  selectedItem={selectedDevice}
                  onChange={handleDeviceChange}
                  disabled={!selectedRoom}
                />
              </Column>
            </Grid>

            <Grid fullWidth narrow style={{ marginTop: "0.5rem" }}>
              <Column lg={5} md={3} sm={4}>
                <Dropdown
                  id="shelf-dropdown"
                  titleText={intl.formatMessage({
                    id: "notebook.storage.shelf",
                    defaultMessage: "Shelf/Drawer",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.storage.selectShelf",
                    defaultMessage: "Select shelf...",
                  })}
                  items={shelves}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={selectedShelf}
                  onChange={handleShelfChange}
                  disabled={!selectedDevice}
                />
              </Column>
              <Column lg={5} md={3} sm={4}>
                <Dropdown
                  id="rack-dropdown"
                  titleText={intl.formatMessage({
                    id: "notebook.storage.rack",
                    defaultMessage: "Rack",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.storage.selectRack",
                    defaultMessage: "Select rack...",
                  })}
                  items={racks}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={selectedRack}
                  onChange={handleRackChange}
                  disabled={!selectedShelf}
                />
              </Column>
              <Column lg={6} md={2} sm={4}>
                <Dropdown
                  id="box-dropdown"
                  titleText={intl.formatMessage({
                    id: "notebook.storage.box",
                    defaultMessage: "Box/Tray (Cryobox)",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.storage.selectBox",
                    defaultMessage: "Select box...",
                  })}
                  items={boxes}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={selectedBox}
                  onChange={handleBoxChange}
                  disabled={!selectedRack}
                />
              </Column>
            </Grid>

            {/* Hierarchical Path Display */}
            {getHierarchicalPath() && (
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                  backgroundColor: "#f4f4f4",
                  padding: "0.5rem",
                  borderRadius: "4px",
                }}
              >
                <strong>
                  <FormattedMessage
                    id="notebook.storage.path"
                    defaultMessage="Path:"
                  />
                </strong>{" "}
                {getHierarchicalPath()}
              </div>
            )}
          </div>

          {/* Cryovial ID Input */}
          <TextInput
            id="cryovial-id"
            labelText={intl.formatMessage({
              id: "notebook.storage.cryovialId",
              defaultMessage: "Cryovial ID (Optional)",
            })}
            placeholder="e.g., CV-2024-001"
            value={cryovialId}
            onChange={(e) => setCryovialId(e.target.value)}
            helperText={intl.formatMessage({
              id: "notebook.storage.cryovialIdHelp",
              defaultMessage: "Enter cryovial ID to link with box position",
            })}
          />

          {/* Box Layout Viewer */}
          {selectedBox && (
            <div style={{ marginTop: "1rem" }}>
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
                    id="notebook.storage.boxLayout"
                    defaultMessage="Cryobox Layout"
                  />{" "}
                  ({selectedBox.rows || 9}x{selectedBox.columns || 9} ={" "}
                  {(selectedBox.rows || 9) * (selectedBox.columns || 9)} wells)
                </h5>
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Renew}
                  onClick={handleAutoPopulate}
                  disabled={selectedSampleIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.storage.autoPopulate"
                    defaultMessage="Auto-Populate"
                  />
                </Button>
              </div>

              {loadingHierarchy ? (
                <Loading withOverlay={false} small />
              ) : (
                <BoxLayoutViewer
                  boxId={selectedBox.id}
                  layout={getCombinedLayout()}
                  rows={selectedBox.rows || 9}
                  columns={selectedBox.columns || 9}
                  onWellClick={handleWellClick}
                />
              )}

              {/* Assignment Summary */}
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="notebook.storage.assignmentSummary"
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
              id: "notebook.storage.condition",
              defaultMessage: "Storage Condition",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectCondition",
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
              id: "notebook.storage.retentionYears",
              defaultMessage: "Retention Period (Years)",
            })}
            value={retentionYears}
            min={1}
            max={30}
            step={1}
            onChange={(e, { value }) => setRetentionYears(value)}
            helperText={intl.formatMessage(
              {
                id: "notebook.storage.expiryDate",
                defaultMessage: "Expiry date will be: {date}",
              },
              {
                date: new Date(
                  Date.now() + retentionYears * 365 * 24 * 60 * 60 * 1000,
                ).toLocaleDateString(),
              },
            )}
          />
        </div>
      </Modal>

      {/* Environmental Monitoring Modal */}
      <Modal
        open={envMonitorModalOpen}
        onRequestClose={() => setEnvMonitorModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.storage.env.modal.title",
          defaultMessage: "Record Temperature Reading",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveEnvReading}
        primaryButtonDisabled={
          !envForm.temperatureReading || !envForm.recordedBy || savingEnvReading
        }
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <Dropdown
            id="env-device-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.env.device",
              defaultMessage: "Storage Device",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.env.selectDevice",
              defaultMessage: "Select device...",
            })}
            items={devices.length > 0 ? devices : rooms}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={
              devices.find((d) => d.id === envForm.deviceId) ||
              rooms.find((r) => r.id === envForm.deviceId) ||
              null
            }
            onChange={({ selectedItem }) =>
              setEnvForm((prev) => ({
                ...prev,
                deviceId: selectedItem?.id || null,
              }))
            }
          />

          <NumberInput
            id="env-temperature"
            label={intl.formatMessage({
              id: "notebook.storage.env.temperature",
              defaultMessage: "Temperature (°C)",
            })}
            value={envForm.temperatureReading}
            step={0.1}
            onChange={(e, { value }) =>
              setEnvForm((prev) => ({ ...prev, temperatureReading: value }))
            }
            invalidText={intl.formatMessage({
              id: "notebook.storage.env.tempInvalid",
              defaultMessage: "Please enter a valid temperature",
            })}
          />

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={2}>
              <TextInput
                id="env-time"
                labelText={intl.formatMessage({
                  id: "notebook.storage.env.time",
                  defaultMessage: "Time",
                })}
                placeholder="HH:MM"
                value={envForm.readingTime}
                onChange={(e) =>
                  setEnvForm((prev) => ({
                    ...prev,
                    readingTime: e.target.value,
                  }))
                }
              />
            </Column>
            <Column lg={8} md={4} sm={2}>
              <Dropdown
                id="env-period"
                titleText={intl.formatMessage({
                  id: "notebook.storage.env.period",
                  defaultMessage: "Period",
                })}
                items={timePeriodOptions}
                itemToString={(item) => (item ? item.label : "")}
                selectedItem={
                  timePeriodOptions.find(
                    (p) => p.id === envForm.readingPeriod,
                  ) || timePeriodOptions[0]
                }
                onChange={({ selectedItem }) =>
                  setEnvForm((prev) => ({
                    ...prev,
                    readingPeriod: selectedItem?.id || "AM",
                  }))
                }
              />
            </Column>
          </Grid>

          <TextInput
            id="env-recorded-by"
            labelText={intl.formatMessage({
              id: "notebook.storage.env.recordedBy",
              defaultMessage: "Recorded By",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.storage.env.recordedByPlaceholder",
              defaultMessage: "Enter your name or initials",
            })}
            value={envForm.recordedBy}
            onChange={(e) =>
              setEnvForm((prev) => ({ ...prev, recordedBy: e.target.value }))
            }
          />

          <Toggle
            id="env-alarm"
            labelText={intl.formatMessage({
              id: "notebook.storage.env.alarmTriggered",
              defaultMessage: "Alarm Triggered?",
            })}
            toggled={envForm.alarmTriggered}
            onToggle={(checked) =>
              setEnvForm((prev) => ({ ...prev, alarmTriggered: checked }))
            }
            labelA="No"
            labelB="Yes"
          />

          <TextInput
            id="env-notes"
            labelText={intl.formatMessage({
              id: "notebook.storage.env.notes",
              defaultMessage: "Notes (Optional)",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.storage.env.notesPlaceholder",
              defaultMessage: "Any additional observations...",
            })}
            value={envForm.notes}
            onChange={(e) =>
              setEnvForm((prev) => ({ ...prev, notes: e.target.value }))
            }
          />
        </div>
      </Modal>

      {/* Reassignment Confirmation Modal */}
      <Modal
        open={confirmReassignModalOpen}
        onRequestClose={() => setConfirmReassignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.storage.reassignConfirm.title",
          defaultMessage: "Confirm Reassignment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.storage.reassignConfirm.confirm",
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
              id="notebook.storage.reassignConfirm.warning"
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
                id="notebook.storage.reassignConfirm.currentLocations"
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

export default SampleStoragePage;
