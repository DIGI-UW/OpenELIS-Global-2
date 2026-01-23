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
  Tag,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  RadioButtonGroup,
  RadioButton,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import {
  Temperature,
  Add,
  Renew,
  Warning,
  Checkmark,
  Time,
  Dashboard,
  Humidity,
  Building,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * Get current local datetime formatted for datetime-local input
 * Uses local timezone instead of UTC
 */
const getLocalDateTimeString = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  const hours = String(now.getHours()).padStart(2, "0");
  const minutes = String(now.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

/**
 * Device type temperature ranges for excursion detection
 */
const DEVICE_TEMP_RANGES = {
  Refrigerator: { min: 2, max: 8, label: "2-8°C" },
  "Freezer-20": { min: -25, max: -15, label: "-20°C" },
  "Freezer-80": { min: -85, max: -75, label: "-80°C" },
  LN2Tank: { min: -210, max: -180, label: "-196°C" },
  Incubator: { min: 35, max: 38, label: "35-38°C" },
  ColdRoom: { min: 2, max: 8, label: "2-8°C" },
};

/**
 * BiorepositoryEnvironmentalMonitoringPage - Environmental Monitoring workflow page
 * Stage 3 of the Biorepository workflow
 *
 * Handles:
 * - Temperature logging for storage devices
 * - Excursion detection and alerts
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration from notebook
 * @param {Object} props.progress - Progress tracking data
 * @param {Function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function BiorepositoryEnvironmentalMonitoringPage({
  entryId,
  pageData: _pageData,
  progress: _progress,
  onProgressUpdate: _onProgressUpdate,
  notebookId: _notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for temperature logs
  const [temperatureLogs, setTemperatureLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // State for storage devices
  const [devices, setDevices] = useState([]);
  const [loadingDevices, setLoadingDevices] = useState(true);

  // Temperature logging modal state
  const [tempModalOpen, setTempModalOpen] = useState(false);
  const [isLoggingTemp, setIsLoggingTemp] = useState(false);
  const [temperatureForm, setTemperatureForm] = useState({
    deviceId: "",
    deviceType: "",
    checkTime: "AM",
    temperatureValue: "",
    temperatureUnit: "C",
    checkedBy: "",
    checkedDateTime: getLocalDateTimeString(),
    notes: "",
  });

  // Filter state for overdue threshold
  const [overdueFilter, setOverdueFilter] = useState("24h"); // "24h", "12h", "today"

  // Room environment state
  const [roomEnvLogs, setRoomEnvLogs] = useState([]);
  const [loadingRoomEnv, setLoadingRoomEnv] = useState(true);
  const [rooms, setRooms] = useState([]);
  const [loadingRooms, setLoadingRooms] = useState(true);

  // Room environment logging modal state
  const [roomEnvModalOpen, setRoomEnvModalOpen] = useState(false);
  const [isLoggingRoomEnv, setIsLoggingRoomEnv] = useState(false);
  const [roomEnvForm, setRoomEnvForm] = useState({
    roomId: "",
    roomName: "",
    oxygenLevel: "",
    humidity: "",
    checkedBy: "",
    checkedDateTime: getLocalDateTimeString(),
    notes: "",
  });

  // Load temperature logs
  const loadTemperatureLogs = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook-entry/${entryId}/temperature-logs`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            // Sort by checked date descending
            const sorted = [...response].sort(
              (a, b) =>
                new Date(b.checkedDateTime) - new Date(a.checkedDateTime),
            );
            setTemperatureLogs(sorted);
          } else {
            setTemperatureLogs([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  // Load storage devices
  const loadDevices = useCallback(() => {
    setLoadingDevices(true);
    getFromOpenElisServer(`/rest/storage/devices`, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setDevices(response);
        } else {
          setDevices([]);
        }
        setLoadingDevices(false);
      }
    });
  }, []);

  // Load storage rooms
  const loadRooms = useCallback(() => {
    setLoadingRooms(true);
    getFromOpenElisServer(`/rest/storage/rooms`, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          // Response is already room objects from /rest/storage/rooms
          setRooms(response);
        } else {
          setRooms([]);
        }
        setLoadingRooms(false);
      }
    });
  }, []);

  // Load room environment logs
  const loadRoomEnvLogs = useCallback(() => {
    if (!entryId) {
      setLoadingRoomEnv(false);
      return;
    }

    setLoadingRoomEnv(true);
    getFromOpenElisServer(
      `/rest/notebook-entry/${entryId}/room-environment-logs`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const sorted = [...response].sort(
              (a, b) =>
                new Date(b.checkedDateTime) - new Date(a.checkedDateTime),
            );
            setRoomEnvLogs(sorted);
          } else {
            setRoomEnvLogs([]);
          }
          setLoadingRoomEnv(false);
        }
      },
    );
  }, [entryId]);

  // Initial load
  useEffect(() => {
    componentMounted.current = true;
    loadTemperatureLogs();
    loadDevices();
    loadRooms();
    loadRoomEnvLogs();

    return () => {
      componentMounted.current = false;
    };
  }, [loadTemperatureLogs, loadDevices, loadRooms, loadRoomEnvLogs]);

  // Handle temperature form change
  const handleTempFormChange = (field, value) => {
    setTemperatureForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Handle device selection - auto-fills deviceType from selected device
  const handleDeviceSelect = (deviceId) => {
    const selectedDevice = devices.find((d) => (d.name || d.code) === deviceId);
    setTemperatureForm((prev) => ({
      ...prev,
      deviceId: deviceId,
      deviceType: selectedDevice?.deviceType || "",
    }));
  };

  // Open temperature logging modal
  const openTempModal = (device = null) => {
    setTempModalOpen(true);
    setError(null);

    // Determine AM/PM based on current hour
    const currentHour = new Date().getHours();
    const defaultCheckTime = currentHour < 12 ? "AM" : "PM";

    if (device) {
      // Pre-fill with device info - use deviceType from database if available
      setTemperatureForm((prev) => ({
        ...prev,
        deviceId: device.name || device.code || "",
        deviceType: device.deviceType || "",
        checkTime: defaultCheckTime,
        checkedDateTime: getLocalDateTimeString(),
      }));
    } else {
      setTemperatureForm({
        deviceId: "",
        deviceType: "",
        checkTime: defaultCheckTime,
        temperatureValue: "",
        temperatureUnit: "C",
        checkedBy: "",
        checkedDateTime: getLocalDateTimeString(),
        notes: "",
      });
    }
  };

  // Handle room environment form change
  const handleRoomEnvFormChange = (field, value) => {
    setRoomEnvForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Handle room selection
  const handleRoomSelect = (roomId) => {
    const selectedRoom = rooms.find(
      (r) => String(r.id) === roomId || r.name === roomId,
    );
    setRoomEnvForm((prev) => ({
      ...prev,
      roomId: roomId,
      roomName: selectedRoom?.name || "",
    }));
  };

  // Open room environment modal
  const openRoomEnvModal = () => {
    setRoomEnvModalOpen(true);
    setError(null);
    setRoomEnvForm({
      roomId: "",
      roomName: "",
      oxygenLevel: "",
      humidity: "",
      checkedBy: "",
      checkedDateTime: getLocalDateTimeString(),
      notes: "",
    });
  };

  // Submit room environment log
  const handleLogRoomEnv = () => {
    // Validate - at least one measurement required
    if (!roomEnvForm.roomId) {
      setError(
        intl.formatMessage({
          id: "biorepository.environmental.roomRequired",
          defaultMessage: "Please select a room.",
        }),
      );
      return;
    }

    if (!roomEnvForm.oxygenLevel && !roomEnvForm.humidity) {
      setError(
        intl.formatMessage({
          id: "biorepository.environmental.measurementRequired",
          defaultMessage:
            "Please enter at least one measurement (O2 level or humidity).",
        }),
      );
      return;
    }

    if (!roomEnvForm.checkedBy) {
      setError(
        intl.formatMessage({
          id: "biorepository.environmental.checkedByRequired",
          defaultMessage: "Please enter staff initials.",
        }),
      );
      return;
    }

    setIsLoggingRoomEnv(true);
    setError(null);

    const logData = {
      roomId: roomEnvForm.roomId,
      roomName: roomEnvForm.roomName,
      oxygenLevel: roomEnvForm.oxygenLevel
        ? parseFloat(roomEnvForm.oxygenLevel)
        : null,
      humidity: roomEnvForm.humidity ? parseFloat(roomEnvForm.humidity) : null,
      checkedBy: roomEnvForm.checkedBy,
      checkedDateTime: roomEnvForm.checkedDateTime,
      notes: roomEnvForm.notes,
    };

    postToOpenElisServer(
      `/rest/notebook-entry/${entryId}/room-environment-logs`,
      JSON.stringify(logData),
      (status) => {
        setIsLoggingRoomEnv(false);
        if (status === 200 || status === 201) {
          setSuccessMessage(
            intl.formatMessage({
              id: "biorepository.environmental.roomEnvLogSuccess",
              defaultMessage: "Room environment logged successfully.",
            }),
          );
          setRoomEnvModalOpen(false);
          loadRoomEnvLogs();
        } else {
          setError(
            intl.formatMessage({
              id: "biorepository.environmental.roomEnvLogError",
              defaultMessage:
                "Failed to log room environment. Please try again.",
            }),
          );
        }
      },
    );
  };

  // Submit temperature log
  const handleLogTemperature = () => {
    // Validate each required field individually for clearer error messages
    const missingFields = [];
    if (!temperatureForm.deviceId) {
      missingFields.push("Device");
    }
    if (
      !temperatureForm.temperatureValue &&
      temperatureForm.temperatureValue !== 0
    ) {
      missingFields.push("Temperature value");
    }
    if (!temperatureForm.checkedBy) {
      missingFields.push("Staff initials");
    }

    if (missingFields.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "biorepository.environmental.tempLogRequired",
            defaultMessage: "Required field(s) missing: {fields}",
          },
          { fields: missingFields.join(", ") },
        ),
      );
      return;
    }

    const tempValue = parseFloat(temperatureForm.temperatureValue);
    if (isNaN(tempValue)) {
      setError(
        intl.formatMessage({
          id: "biorepository.environmental.tempInvalid",
          defaultMessage: "Temperature value must be a valid number.",
        }),
      );
      return;
    }

    setIsLoggingTemp(true);
    setError(null);

    const logData = {
      freezerId: temperatureForm.deviceId,
      deviceType: temperatureForm.deviceType,
      checkTime: temperatureForm.checkTime,
      temperatureValue: tempValue,
      temperatureUnit: temperatureForm.temperatureUnit,
      checkedBy: temperatureForm.checkedBy,
      checkedDateTime: temperatureForm.checkedDateTime,
      notes: temperatureForm.notes,
    };

    postToOpenElisServer(
      `/rest/notebook-entry/${entryId}/temperature-logs`,
      JSON.stringify(logData),
      (status) => {
        setIsLoggingTemp(false);
        if (status === 200 || status === 201) {
          setSuccessMessage(
            intl.formatMessage({
              id: "biorepository.environmental.tempLogSuccess",
              defaultMessage: "Temperature logged successfully.",
            }),
          );
          setTempModalOpen(false);
          loadTemperatureLogs();
        } else {
          setError(
            intl.formatMessage({
              id: "biorepository.environmental.tempLogError",
              defaultMessage: "Failed to log temperature. Please try again.",
            }),
          );
        }
      },
    );
  };

  // Check if temperature is within range for device type
  const isTemperatureInRange = (temp, deviceType) => {
    const range = DEVICE_TEMP_RANGES[deviceType];
    if (!range) return true; // No range defined, assume OK
    return temp >= range.min && temp <= range.max;
  };

  // Get temperature status tag
  const getTemperatureStatusTag = (log) => {
    const temp = log.temperatureValue;
    const deviceType = log.deviceType;
    const inRange = isTemperatureInRange(temp, deviceType);
    const range = DEVICE_TEMP_RANGES[deviceType];

    if (inRange) {
      return (
        <Tag type="green" size="sm" renderIcon={Checkmark}>
          {temp}°{log.temperatureUnit}
        </Tag>
      );
    } else {
      return (
        <Tag type="red" size="sm" renderIcon={Warning}>
          {temp}°{log.temperatureUnit} - EXCURSION
          {range && ` (Expected: ${range.label})`}
        </Tag>
      );
    }
  };

  // Format date for display
  const formatDateTime = (timestamp) => {
    if (!timestamp) return "-";
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  // Get recent logs for a device based on filter threshold
  const getRecentLogsForDevice = useCallback(
    (deviceId) => {
      let cutoffTime = new Date();

      switch (overdueFilter) {
        case "12h":
          cutoffTime.setHours(cutoffTime.getHours() - 12);
          break;
        case "today":
          // Start of today
          cutoffTime.setHours(0, 0, 0, 0);
          break;
        case "24h":
        default:
          cutoffTime.setHours(cutoffTime.getHours() - 24);
          break;
      }

      return temperatureLogs.filter(
        (log) =>
          log.freezerId === deviceId &&
          new Date(log.checkedDateTime) > cutoffTime,
      );
    },
    [temperatureLogs, overdueFilter],
  );

  // Check if device needs a temperature reading based on filter
  const deviceNeedsReading = useCallback(
    (device) => {
      const deviceId = device.name || device.code;
      const recentLogs = getRecentLogsForDevice(deviceId);

      // If no logs in the filter period, device needs reading
      return recentLogs.length === 0;
    },
    [getRecentLogsForDevice],
  );

  // Calculate stats
  const totalLogs = temperatureLogs.length;
  const excursionLogs = temperatureLogs.filter(
    (log) => !isTemperatureInRange(log.temperatureValue, log.deviceType),
  );
  const devicesNeedingReadings = devices.filter(deviceNeedsReading);

  // Table headers for temperature logs
  const logHeaders = [
    {
      key: "freezerId",
      header: intl.formatMessage({
        id: "biorepository.environmental.deviceId",
        defaultMessage: "Device ID",
      }),
    },
    {
      key: "deviceType",
      header: intl.formatMessage({
        id: "biorepository.environmental.deviceType",
        defaultMessage: "Type",
      }),
    },
    {
      key: "checkTime",
      header: intl.formatMessage({
        id: "biorepository.environmental.checkTime",
        defaultMessage: "Check",
      }),
    },
    {
      key: "temperature",
      header: intl.formatMessage({
        id: "biorepository.environmental.temperature",
        defaultMessage: "Temperature",
      }),
    },
    {
      key: "checkedBy",
      header: intl.formatMessage({
        id: "biorepository.environmental.checkedBy",
        defaultMessage: "Checked By",
      }),
    },
    {
      key: "checkedDateTime",
      header: intl.formatMessage({
        id: "biorepository.environmental.checkedDateTime",
        defaultMessage: "Date/Time",
      }),
    },
    {
      key: "notes",
      header: intl.formatMessage({
        id: "biorepository.environmental.notes",
        defaultMessage: "Notes",
      }),
    },
  ];

  // Transform logs for DataTable
  const logRows = temperatureLogs.slice(0, 50).map((log) => ({
    id: String(log.id),
    freezerId: log.freezerId || "-",
    deviceType: log.deviceType || "-",
    checkTime: log.checkTime || "-",
    temperature: getTemperatureStatusTag(log),
    checkedBy: log.checkedBy || "-",
    checkedDateTime: formatDateTime(log.checkedDateTime),
    notes: log.notes || "-",
  }));

  // Table headers for room environment logs
  const roomEnvHeaders = [
    {
      key: "roomName",
      header: intl.formatMessage({
        id: "biorepository.environmental.roomName",
        defaultMessage: "Room",
      }),
    },
    {
      key: "oxygenLevel",
      header: intl.formatMessage({
        id: "biorepository.environmental.oxygenLevel",
        defaultMessage: "O₂ Level (%)",
      }),
    },
    {
      key: "humidity",
      header: intl.formatMessage({
        id: "biorepository.environmental.humidity",
        defaultMessage: "Humidity (%)",
      }),
    },
    {
      key: "checkedBy",
      header: intl.formatMessage({
        id: "biorepository.environmental.checkedBy",
        defaultMessage: "Checked By",
      }),
    },
    {
      key: "checkedDateTime",
      header: intl.formatMessage({
        id: "biorepository.environmental.checkedDateTime",
        defaultMessage: "Date/Time",
      }),
    },
    {
      key: "notes",
      header: intl.formatMessage({
        id: "biorepository.environmental.notes",
        defaultMessage: "Notes",
      }),
    },
  ];

  // Get O2 level status tag
  const getO2StatusTag = (level) => {
    if (level === null || level === undefined) return "-";
    const numLevel = parseFloat(level);
    if (numLevel >= 19.5) {
      return (
        <Tag type="green" size="sm" renderIcon={Checkmark}>
          {numLevel.toFixed(1)}%
        </Tag>
      );
    } else {
      return (
        <Tag type="red" size="sm" renderIcon={Warning}>
          {numLevel.toFixed(1)}% - LOW
        </Tag>
      );
    }
  };

  // Get humidity status tag
  const getHumidityStatusTag = (level) => {
    if (level === null || level === undefined) return "-";
    const numLevel = parseFloat(level);
    if (numLevel >= 30 && numLevel <= 60) {
      return (
        <Tag type="green" size="sm" renderIcon={Checkmark}>
          {numLevel.toFixed(1)}%
        </Tag>
      );
    } else {
      return (
        <Tag type="magenta" size="sm" renderIcon={Warning}>
          {numLevel.toFixed(1)}%
        </Tag>
      );
    }
  };

  // Transform room env logs for DataTable
  const roomEnvRows = roomEnvLogs.slice(0, 50).map((log) => ({
    id: String(log.id),
    roomName: log.roomName || "-",
    oxygenLevel: getO2StatusTag(log.oxygenLevel),
    humidity: getHumidityStatusTag(log.humidity),
    checkedBy: log.checkedBy || "-",
    checkedDateTime: formatDateTime(log.checkedDateTime),
    notes: log.notes || "-",
  }));

  return (
    <div className="biorepository-environmental-monitoring-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="biorepository.environmental.title"
            defaultMessage="Ongoing Storage and Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="biorepository.environmental.description"
            defaultMessage="Record temperature readings for storage devices. Log readings twice daily (AM/PM). System flags excursions based on device-specific thresholds. Monitor environmental parameters for compliance."
          />
        </p>
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

      {/* Tabbed Content */}
      <Tabs>
        <TabList
          aria-label="Environmental monitoring tabs"
          style={{ minWidth: "400px" }}
        >
          <Tab style={{ minWidth: "180px" }}>
            <Temperature size={16} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="biorepository.environmental.tab.deviceMonitoring"
              defaultMessage="Device Monitoring"
            />
          </Tab>
          <Tab style={{ minWidth: "180px" }}>
            <Building size={16} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="biorepository.environmental.tab.roomEnvironment"
              defaultMessage="Room Environment"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Device Monitoring */}
          <TabPanel>
            {/* Progress Summary */}
            <Grid
              fullWidth
              className="progress-section"
              style={{ marginTop: "1rem" }}
            >
              <Column lg={16} md={8} sm={4}>
                <div className="progress-tiles">
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="biorepository.environmental.totalDevices"
                        defaultMessage="Storage Devices"
                      />
                    </span>
                    <span className="progress-value">{devices.length}</span>
                  </Tile>
                  <Tile
                    className={`progress-tile ${devicesNeedingReadings.length > 0 ? "warning" : "success"}`}
                  >
                    <span className="progress-label">
                      <FormattedMessage
                        id="biorepository.environmental.needsReading"
                        defaultMessage="Needs Reading"
                      />
                    </span>
                    <span className="progress-value">
                      {devicesNeedingReadings.length}
                    </span>
                  </Tile>
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="biorepository.environmental.totalReadings"
                        defaultMessage="Total Readings"
                      />
                    </span>
                    <span className="progress-value">{totalLogs}</span>
                  </Tile>
                  <Tile
                    className={`progress-tile ${excursionLogs.length > 0 ? "error" : "success"}`}
                  >
                    <span className="progress-label">
                      <FormattedMessage
                        id="biorepository.environmental.excursions"
                        defaultMessage="Excursions"
                      />
                    </span>
                    <span className="progress-value">
                      {excursionLogs.length}
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
                renderIcon={Add}
                onClick={() => openTempModal()}
                disabled={!entryId}
              >
                <FormattedMessage
                  id="biorepository.environmental.logTemperature"
                  defaultMessage="Log Temperature"
                />
              </Button>

              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                  marginLeft: "auto",
                }}
              >
                <Select
                  id="overdue-filter"
                  size="sm"
                  labelText=""
                  hideLabel
                  value={overdueFilter}
                  onChange={(e) => setOverdueFilter(e.target.value)}
                  style={{ minWidth: "200px" }}
                >
                  <SelectItem
                    value="24h"
                    text={intl.formatMessage({
                      id: "biorepository.environmental.filter.24h",
                      defaultMessage: "No reading in 24 hours",
                    })}
                  />
                  <SelectItem
                    value="12h"
                    text={intl.formatMessage({
                      id: "biorepository.environmental.filter.12h",
                      defaultMessage: "No reading in 12 hours",
                    })}
                  />
                  <SelectItem
                    value="today"
                    text={intl.formatMessage({
                      id: "biorepository.environmental.filter.today",
                      defaultMessage: "No reading today",
                    })}
                  />
                </Select>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={() => {
                    loadTemperatureLogs();
                    loadDevices();
                  }}
                >
                  <FormattedMessage
                    id="biorepository.environmental.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>
            </div>

            {/* Device Dashboard */}
            {devicesNeedingReadings.length > 0 && (
              <div style={{ marginTop: "1.5rem" }}>
                <h5>
                  <Dashboard size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="biorepository.environmental.devicesNeedingReadings"
                    defaultMessage="Devices Needing Readings"
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
                  {devicesNeedingReadings.map((device) => (
                    <Tile
                      key={device.id}
                      style={{
                        padding: "0.75rem 1rem",
                        cursor: "pointer",
                        border: "1px solid #da1e28",
                        backgroundColor: "#fff1f1",
                      }}
                      onClick={() => openTempModal(device)}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <Time size={16} style={{ color: "#da1e28" }} />
                        <strong>{device.name || device.code}</strong>
                        <Tag type="red" size="sm">
                          <FormattedMessage
                            id="biorepository.environmental.overdue"
                            defaultMessage="Overdue"
                          />
                        </Tag>
                      </div>
                      <div style={{ fontSize: "0.75rem", color: "#525252" }}>
                        {device.deviceType || "Unknown Type"}
                      </div>
                    </Tile>
                  ))}
                </div>
              </div>
            )}

            {/* Recent Temperature Logs */}
            <div style={{ marginTop: "1.5rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Temperature size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="biorepository.environmental.recentLogs"
                  defaultMessage="Recent Temperature Logs"
                />
              </h5>

              {loading ? (
                <p>
                  <FormattedMessage
                    id="common.loading"
                    defaultMessage="Loading..."
                  />
                </p>
              ) : temperatureLogs.length === 0 ? (
                <InlineNotification
                  kind="info"
                  title={intl.formatMessage({
                    id: "biorepository.environmental.noLogs",
                    defaultMessage: "No temperature logs recorded yet.",
                  })}
                  lowContrast
                  hideCloseButton
                />
              ) : (
                <DataTable rows={logRows} headers={logHeaders} size="sm">
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer>
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
                                  {cell.value}
                                </TableCell>
                              ))}
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              )}
            </div>
          </TabPanel>

          {/* Tab 2: Room Environment */}
          <TabPanel>
            {/* Room Environment Summary */}
            <Grid
              fullWidth
              className="progress-section"
              style={{ marginTop: "1rem" }}
            >
              <Column lg={16} md={8} sm={4}>
                <div className="progress-tiles">
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="biorepository.environmental.totalRooms"
                        defaultMessage="Storage Rooms"
                      />
                    </span>
                    <span className="progress-value">{rooms.length}</span>
                  </Tile>
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="biorepository.environmental.roomEnvReadings"
                        defaultMessage="Total Readings"
                      />
                    </span>
                    <span className="progress-value">{roomEnvLogs.length}</span>
                  </Tile>
                </div>
              </Column>
            </Grid>

            {/* Room Environment Action Buttons */}
            <div className="page-actions-bar">
              <Button
                kind="primary"
                size="sm"
                renderIcon={Add}
                onClick={openRoomEnvModal}
                disabled={!entryId}
              >
                <FormattedMessage
                  id="biorepository.environmental.logRoomEnvironment"
                  defaultMessage="Log Room Environment"
                />
              </Button>

              <div style={{ marginLeft: "auto" }}>
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={() => {
                    loadRoomEnvLogs();
                    loadRooms();
                  }}
                >
                  <FormattedMessage
                    id="biorepository.environmental.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>
            </div>

            {/* Room Environment Info */}
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "biorepository.environmental.roomEnvInfo",
                defaultMessage: "Room environment monitoring",
              })}
              subtitle={intl.formatMessage({
                id: "biorepository.environmental.roomEnvInfoSubtitle",
                defaultMessage:
                  "O₂ levels should be ≥19.5% (safety threshold for rooms with cryogenic equipment). Humidity should be 30-60% for optimal storage conditions.",
              })}
              lowContrast
              hideCloseButton
              style={{ marginTop: "1rem" }}
            />

            {/* Recent Room Environment Logs */}
            <div style={{ marginTop: "1.5rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Humidity size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="biorepository.environmental.recentRoomEnvLogs"
                  defaultMessage="Recent Room Environment Logs"
                />
              </h5>

              {loadingRoomEnv ? (
                <p>
                  <FormattedMessage
                    id="common.loading"
                    defaultMessage="Loading..."
                  />
                </p>
              ) : roomEnvLogs.length === 0 ? (
                <InlineNotification
                  kind="info"
                  title={intl.formatMessage({
                    id: "biorepository.environmental.noRoomEnvLogs",
                    defaultMessage: "No room environment logs recorded yet.",
                  })}
                  lowContrast
                  hideCloseButton
                />
              ) : (
                <DataTable
                  rows={roomEnvRows}
                  headers={roomEnvHeaders}
                  size="sm"
                >
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer>
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
                                  {cell.value}
                                </TableCell>
                              ))}
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              )}
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Temperature Logging Modal */}
      <Modal
        open={tempModalOpen}
        onRequestClose={() => setTempModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "biorepository.environmental.logTemperatureTitle",
          defaultMessage: "Log Temperature Reading",
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
        onSecondarySubmit={() => setTempModalOpen(false)}
        size="md"
        primaryButtonDisabled={isLoggingTemp}
      >
        <p className="modal-description">
          <FormattedMessage
            id="biorepository.environmental.tempLogDescription"
            defaultMessage="Record temperature monitoring data for environmental compliance. Temperature readings should be logged twice daily (AM/PM)."
          />
        </p>

        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="deviceId"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.deviceLabel",
                defaultMessage: "Device *",
              })}
              value={temperatureForm.deviceId}
              onChange={(e) => handleDeviceSelect(e.target.value)}
            >
              <SelectItem value="" text="Select device..." />
              {devices.map((device) => (
                <SelectItem
                  key={device.id}
                  value={device.name || device.code}
                  text={`${device.name || device.code}${device.deviceType ? ` (${device.deviceType})` : ""}`}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="deviceType"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.deviceTypeLabel",
                defaultMessage: "Device Type",
              })}
              value={temperatureForm.deviceType}
              disabled
              helperText={intl.formatMessage({
                id: "biorepository.environmental.deviceTypeHelper",
                defaultMessage: "Auto-filled from selected device",
              })}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "biorepository.environmental.checkTimeLabel",
                defaultMessage: "Check Time",
              })}
              name="checkTime"
              valueSelected={temperatureForm.checkTime}
              onChange={(value) => handleTempFormChange("checkTime", value)}
            >
              <RadioButton labelText="AM" value="AM" id="check-am" />
              <RadioButton labelText="PM" value="PM" id="check-pm" />
            </RadioButtonGroup>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="temperatureValue"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.tempValueLabel",
                defaultMessage: "Temperature Value *",
              })}
              value={temperatureForm.temperatureValue}
              onChange={(e) =>
                handleTempFormChange("temperatureValue", e.target.value)
              }
              placeholder="e.g., -80.5"
              type="number"
              step="0.1"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="temperatureUnit"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.tempUnitLabel",
                defaultMessage: "Unit",
              })}
              value={temperatureForm.temperatureUnit}
              onChange={(e) =>
                handleTempFormChange("temperatureUnit", e.target.value)
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
                id: "biorepository.environmental.checkedByLabel",
                defaultMessage: "Checked By (Staff Initials) *",
              })}
              value={temperatureForm.checkedBy}
              onChange={(e) =>
                handleTempFormChange("checkedBy", e.target.value)
              }
              placeholder="e.g., JD, ABC"
              required
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="biorepository.environmental.checkedDateTimeLabel"
                  defaultMessage="Checked Date/Time"
                />
              </label>
              <input
                type="datetime-local"
                className="cds--text-input"
                value={temperatureForm.checkedDateTime}
                onChange={(e) =>
                  handleTempFormChange("checkedDateTime", e.target.value)
                }
              />
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="tempNotes"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.notesLabel",
                defaultMessage: "Notes",
              })}
              value={temperatureForm.notes}
              onChange={(e) => handleTempFormChange("notes", e.target.value)}
              placeholder="Document any temperature excursions, deviations, corrective actions, etc."
            />
          </Column>
        </Grid>
      </Modal>

      {/* Room Environment Logging Modal */}
      <Modal
        open={roomEnvModalOpen}
        onRequestClose={() => setRoomEnvModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "biorepository.environmental.logRoomEnvTitle",
          defaultMessage: "Log Room Environment",
        })}
        primaryButtonText={
          isLoggingRoomEnv
            ? intl.formatMessage({
                id: "label.logging",
                defaultMessage: "Logging...",
              })
            : intl.formatMessage({
                id: "label.logEnvironment",
                defaultMessage: "Log Environment",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleLogRoomEnv}
        onSecondarySubmit={() => setRoomEnvModalOpen(false)}
        size="md"
        primaryButtonDisabled={isLoggingRoomEnv}
      >
        <p className="modal-description">
          <FormattedMessage
            id="biorepository.environmental.roomEnvLogDescription"
            defaultMessage="Record room-level environmental parameters. O₂ monitoring is critical for rooms with cryogenic equipment (liquid nitrogen). Humidity monitoring ensures optimal storage conditions."
          />
        </p>

        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="roomId"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.roomLabel",
                defaultMessage: "Room / Storage Area *",
              })}
              value={roomEnvForm.roomId}
              onChange={(e) => handleRoomSelect(e.target.value)}
            >
              <SelectItem value="" text="Select room..." />
              {rooms.map((room) => (
                <SelectItem
                  key={room.id}
                  value={String(room.id)}
                  text={room.name || room.code || `Room ${room.id}`}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="oxygenLevel"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.oxygenLevelLabel",
                defaultMessage: "O₂ Level (%)",
              })}
              value={roomEnvForm.oxygenLevel}
              onChange={(e) =>
                handleRoomEnvFormChange("oxygenLevel", e.target.value)
              }
              placeholder="e.g., 20.9"
              type="number"
              step="0.1"
              helperText={intl.formatMessage({
                id: "biorepository.environmental.oxygenLevelHelper",
                defaultMessage: "Normal: ~21%. Alert if <19.5%",
              })}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="humidity"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.humidityLabel",
                defaultMessage: "Humidity (%)",
              })}
              value={roomEnvForm.humidity}
              onChange={(e) =>
                handleRoomEnvFormChange("humidity", e.target.value)
              }
              placeholder="e.g., 45"
              type="number"
              step="0.1"
              helperText={intl.formatMessage({
                id: "biorepository.environmental.humidityHelper",
                defaultMessage: "Optimal range: 30-60%",
              })}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="roomEnvCheckedBy"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.checkedByLabel",
                defaultMessage: "Checked By (Staff Initials) *",
              })}
              value={roomEnvForm.checkedBy}
              onChange={(e) =>
                handleRoomEnvFormChange("checkedBy", e.target.value)
              }
              placeholder="e.g., JD, ABC"
              required
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="biorepository.environmental.checkedDateTimeLabel"
                  defaultMessage="Checked Date/Time"
                />
              </label>
              <input
                type="datetime-local"
                className="cds--text-input"
                value={roomEnvForm.checkedDateTime}
                onChange={(e) =>
                  handleRoomEnvFormChange("checkedDateTime", e.target.value)
                }
              />
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="roomEnvNotes"
              labelText={intl.formatMessage({
                id: "biorepository.environmental.notesLabel",
                defaultMessage: "Notes",
              })}
              value={roomEnvForm.notes}
              onChange={(e) => handleRoomEnvFormChange("notes", e.target.value)}
              placeholder="Document any environmental concerns, corrective actions, etc."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

BiorepositoryEnvironmentalMonitoringPage.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
  progress: PropTypes.object,
  onProgressUpdate: PropTypes.func,
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default BiorepositoryEnvironmentalMonitoringPage;
