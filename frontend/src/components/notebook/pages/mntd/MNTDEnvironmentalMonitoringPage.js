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
  NumberInput,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import { Temperature, Add, Renew, Warning } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDEnvironmentalMonitoringPage - Page 11 of the MNTD workflow.
 * Dedicated page for temperature and environmental monitoring.
 * Independent of sample workflow - allows non-clinicians to log temperatures.
 *
 * Features:
 * - Data table displaying all temperature logs for the entry
 * - Temperature logging modal with device ID, temperature, unit, check time, etc.
 * - Out-of-range temperature flagging
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress (not used - no samples)
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDEnvironmentalMonitoringPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for temperature logs
  const [temperatureLogs, setTemperatureLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Temperature logging modal state
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

  // Search filter for data table
  const [searchTerm, setSearchTerm] = useState("");

  // Load temperature logs on mount
  useEffect(() => {
    componentMounted.current = true;
    loadTemperatureLogs();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId]);

  const loadTemperatureLogs = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook-entry/${entryId}/temperature-logs`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setTemperatureLogs(response);
          } else {
            setTemperatureLogs([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  // Handle temperature logging
  const handleLogTemperature = useCallback(() => {
    if (!temperatureLog.freezerId || !temperatureLog.temperatureValue) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.envMonitoring.requiredFields",
          defaultMessage: "Device ID and temperature value are required.",
        }),
      );
      return;
    }

    // Validate temperature value is a valid number
    const tempValue = parseFloat(temperatureLog.temperatureValue);
    if (isNaN(tempValue)) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.envMonitoring.invalidTemp",
          defaultMessage: "Temperature value must be a valid number.",
        }),
      );
      return;
    }

    setIsLoggingTemp(true);
    setError(null);

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
          setSuccessMessage(
            intl.formatMessage({
              id: "notebook.mntd.envMonitoring.logSuccess",
              defaultMessage: "Temperature logged successfully.",
            }),
          );
          setTempMonitoringModalOpen(false);
          loadTemperatureLogs();
          // Reset form but keep device ID for convenience
          setTemperatureLog((prev) => ({
            freezerId: prev.freezerId,
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
              id: "notebook.mntd.envMonitoring.logError",
              defaultMessage: "Failed to log temperature. Please try again.",
            }),
          );
        }
      },
    );
  }, [temperatureLog, entryId, intl, loadTemperatureLogs]);

  // Temperature ranges for status checking
  const temperatureRanges = {
    Refrigerator: { min: 2, max: 8 },
    "Freezer-20": { min: -25, max: -15 },
    "Freezer-80": { min: -85, max: -75 },
    LN2Tank: { min: -200, max: -180 },
    Incubator: { min: 35, max: 38 },
    ColdRoom: { min: 2, max: 8 },
  };

  // Check if temperature is in range
  const isTemperatureInRange = (log) => {
    const range = temperatureRanges[log.deviceType] || { min: -999, max: 999 };
    const temp = log.temperatureValue;
    return temp >= range.min && temp <= range.max;
  };

  // Get temperature status tag
  const getTemperatureStatusTag = (log) => {
    const inRange = isTemperatureInRange(log);
    return inRange ? (
      <Tag type="green" size="sm">
        {log.temperatureValue}°{log.temperatureUnit}
      </Tag>
    ) : (
      <Tag type="red" size="sm" renderIcon={Warning}>
        {log.temperatureValue}°{log.temperatureUnit}
      </Tag>
    );
  };

  // Format date for display
  const formatDateTime = (dateString) => {
    if (!dateString) return "-";
    try {
      const date = new Date(dateString);
      return date.toLocaleString();
    } catch {
      return dateString;
    }
  };

  // Data table headers
  const headers = [
    {
      key: "freezerId",
      header: intl.formatMessage({
        id: "notebook.mntd.envMonitoring.column.deviceId",
        defaultMessage: "Device ID",
      }),
    },
    {
      key: "temperature",
      header: intl.formatMessage({
        id: "notebook.mntd.envMonitoring.column.temperature",
        defaultMessage: "Temperature",
      }),
    },
    {
      key: "checkTime",
      header: intl.formatMessage({
        id: "notebook.mntd.envMonitoring.column.checkTime",
        defaultMessage: "Check Time",
      }),
    },
    {
      key: "checkedBy",
      header: intl.formatMessage({
        id: "notebook.mntd.envMonitoring.column.checkedBy",
        defaultMessage: "Checked By",
      }),
    },
    {
      key: "checkedDateTime",
      header: intl.formatMessage({
        id: "notebook.mntd.envMonitoring.column.dateTime",
        defaultMessage: "Date/Time",
      }),
    },
    {
      key: "notes",
      header: intl.formatMessage({
        id: "notebook.mntd.envMonitoring.column.notes",
        defaultMessage: "Notes",
      }),
    },
  ];

  // Transform logs for DataTable
  const getTableRows = () => {
    let filteredLogs = temperatureLogs;

    // Apply search filter
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filteredLogs = temperatureLogs.filter(
        (log) =>
          (log.freezerId && log.freezerId.toLowerCase().includes(search)) ||
          (log.checkedBy && log.checkedBy.toLowerCase().includes(search)) ||
          (log.notes && log.notes.toLowerCase().includes(search)),
      );
    }

    return filteredLogs.map((log, index) => ({
      id: String(log.id || index),
      freezerId: log.freezerId || "-",
      temperature: getTemperatureStatusTag(log),
      checkTime: log.checkTime || "-",
      checkedBy: log.checkedBy || "-",
      checkedDateTime: formatDateTime(log.checkedDateTime || log.loggedAt),
      notes: log.notes || "-",
    }));
  };

  // Calculate summary stats
  const totalLogs = temperatureLogs.length;
  const outOfRangeLogs = temperatureLogs.filter(
    (log) => !isTemperatureInRange(log),
  ).length;
  const todayLogs = temperatureLogs.filter((log) => {
    const logDate = new Date(log.checkedDateTime || log.loggedAt);
    const today = new Date();
    return logDate.toDateString() === today.toDateString();
  }).length;

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading
          withOverlay={false}
          description={intl.formatMessage({
            id: "notebook.mntd.envMonitoring.loading",
            defaultMessage: "Loading temperature logs...",
          })}
        />
      </div>
    );
  }

  return (
    <div className="mntd-environmental-monitoring-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <Temperature size={20} style={{ marginRight: "0.5rem" }} />
          <FormattedMessage
            id="notebook.page.mntd.envMonitoring.title"
            defaultMessage="Environmental Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.envMonitoring.description"
            defaultMessage="Record and view temperature logs for laboratory environmental compliance. Out-of-range temperatures are automatically flagged."
          />
        </p>
      </div>

      {/* Summary Tiles */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.envMonitoring.totalLogs"
                  defaultMessage="Total Logs"
                />
              </span>
              <span className="progress-value">{totalLogs}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.envMonitoring.todayLogs"
                  defaultMessage="Today's Logs"
                />
              </span>
              <span className="progress-value">{todayLogs}</span>
            </Tile>
            {outOfRangeLogs > 0 && (
              <Tile className="progress-tile pending">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.mntd.envMonitoring.outOfRange"
                    defaultMessage="Out of Range"
                  />
                </span>
                <span className="progress-value" style={{ color: "#da1e28" }}>
                  {outOfRangeLogs}
                </span>
              </Tile>
            )}
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Add}
          onClick={() => {
            setTemperatureLog((prev) => ({
              ...prev,
              checkedDateTime: new Date().toISOString().slice(0, 16),
            }));
            setTempMonitoringModalOpen(true);
          }}
        >
          <FormattedMessage
            id="notebook.mntd.envMonitoring.logTemperature"
            defaultMessage="Log Temperature"
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadTemperatureLogs}
        >
          <FormattedMessage
            id="notebook.mntd.envMonitoring.refresh"
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
          style={{ marginBottom: "1rem" }}
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Temperature Logs Data Table */}
      <div style={{ marginTop: "1.5rem" }}>
        <DataTable rows={getTableRows()} headers={headers} isSortable>
          {({
            rows,
            headers,
            getHeaderProps,
            getRowProps,
            getTableProps,
            getTableContainerProps,
            onInputChange,
          }) => (
            <TableContainer
              title={intl.formatMessage({
                id: "notebook.mntd.envMonitoring.tableTitle",
                defaultMessage: "Temperature Logs",
              })}
              description={intl.formatMessage(
                {
                  id: "notebook.mntd.envMonitoring.tableDescription",
                  defaultMessage:
                    "Showing {count} temperature log(s) for this entry",
                },
                { count: rows.length },
              )}
              {...getTableContainerProps()}
            >
              <TableToolbar>
                <TableToolbarContent>
                  <TableToolbarSearch
                    onChange={(e) => setSearchTerm(e.target.value)}
                    placeholder={intl.formatMessage({
                      id: "notebook.mntd.envMonitoring.search",
                      defaultMessage: "Search by device ID, checked by, notes",
                    })}
                  />
                </TableToolbarContent>
              </TableToolbar>
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
                  {rows.length > 0 ? (
                    rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={headers.length}>
                        <div
                          style={{
                            textAlign: "center",
                            padding: "2rem",
                            color: "#525252",
                          }}
                        >
                          <Temperature
                            size={32}
                            style={{ marginBottom: "0.5rem" }}
                          />
                          <p>
                            <FormattedMessage
                              id="notebook.mntd.envMonitoring.noLogs"
                              defaultMessage="No temperature logs recorded yet. Click 'Log Temperature' to add the first entry."
                            />
                          </p>
                        </div>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      </div>

      {/* Temperature Logging Modal */}
      <Modal
        open={tempMonitoringModalOpen}
        onRequestClose={() => setTempMonitoringModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.envMonitoring.modal.title",
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
            id="notebook.mntd.envMonitoring.modal.description"
            defaultMessage="Record temperature monitoring data for MNTD laboratory environmental compliance."
          />
        </p>

        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="freezerId"
              labelText={intl.formatMessage({
                id: "notebook.mntd.envMonitoring.deviceId",
                defaultMessage: "Device/Equipment ID *",
              })}
              value={temperatureLog.freezerId}
              onChange={(e) =>
                setTemperatureLog((prev) => ({
                  ...prev,
                  freezerId: e.target.value,
                }))
              }
              placeholder="e.g., FRZ-001, LN2-002"
              required
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.mntd.envMonitoring.checkTime",
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
                id: "notebook.mntd.envMonitoring.temperatureValue",
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
                id: "notebook.mntd.envMonitoring.temperatureUnit",
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
                id: "notebook.mntd.envMonitoring.checkedBy",
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
                  id="notebook.mntd.envMonitoring.checkedDateTime"
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
                id: "notebook.mntd.envMonitoring.notes",
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
    </div>
  );
}

export default MNTDEnvironmentalMonitoringPage;
