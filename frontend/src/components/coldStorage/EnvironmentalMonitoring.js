import React, {
  useState,
  useMemo,
  useEffect,
  useContext,
  useCallback,
} from "react";
import {
  Button,
  Dropdown,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  Pagination,
  Modal,
  Form,
  Stack,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  NumberInput,
  DatePicker,
  DatePickerInput,
  RadioButtonGroup,
  RadioButton,
  ExpandableTile,
  TileAboveTheFoldContent,
  TileBelowTheFoldContent,
} from "@carbon/react";
import {
  Add,
  Edit,
  Temperature,
  Warning,
  Snowflake,
  Building,
  Settings,
  VirtualPrivateCloud,
} from "@carbon/react/icons";
import {
  fetchEnvironmentalLogs,
  fetchDashboardStatistics,
  createEnvironmentalLog,
  fetchStorageUnitTypes,
  fetchSampleTypes,
  fetchProjects,
  searchSampleIds,
} from "./environmentalApi";
import { fetchDevices, fetchLocations } from "./api";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { FormattedMessage, useIntl } from "react-intl";
import "./EnvironmentalMonitoring.scss";

/**
 * EnvironmentalMonitoring - Tab component for Cold Storage feature.
 *
 * Lab-wide environmental monitoring integrated as a tab in the Cold Storage dashboard.
 * Supports different storage unit types with dynamic fields for Movable Fridge context.
 */
export default function EnvironmentalMonitoring() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const userSessionDetails = useContext(UserSessionDetailsContext);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // State for environmental logs
  const [environmentalLogs, setEnvironmentalLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dashboardStats, setDashboardStats] = useState({});

  // Modal and form state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [environmentalLog, setEnvironmentalLog] = useState({
    storageUnitType: "ROOM",
    storageUnitId: "",
    intervalType: "AM",
    temperatureValue: "",
    temperatureUnit: "C",
    humidityValue: "",
    checkedBy: "",
    checkedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
    // Movable Fridge specific fields
    sampleType: "",
    projectName: "",
    sampleId: "",
    additionalDetails: "",
  });
  const [isLoggingEnv, setIsLoggingEnv] = useState(false);

  // Table and pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Search and filtering state
  const [searchTerm, setSearchTerm] = useState("");
  const [storageTypeFilter, setStorageTypeFilter] = useState("");

  // Dropdown data state
  const [storageUnitTypes, setStorageUnitTypes] = useState([]);
  const [storageUnitOptions, setStorageUnitOptions] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [projects, setProjects] = useState([]);
  const [sampleIds, setSampleIds] = useState([]);

  // Static storage unit type icons mapping
  const storageUnitTypeIcons = {
    ROOM: Building,
    FREEZER: Snowflake,
    EQUIPMENT_ANALYZER: Settings,
    MOVABLE_FRIDGE: VirtualPrivateCloud,
  };

  // Static storage unit type labels mapping
  const storageUnitTypeLabels = {
    ROOM: "Room",
    FREEZER: "Freezer",
    EQUIPMENT_ANALYZER: "Equipments / Analyzer",
    MOVABLE_FRIDGE: "Movable Fridge",
  };

  const asArray = (response, keys = []) => {
    if (Array.isArray(response)) {
      return response;
    }
    for (const key of keys) {
      if (Array.isArray(response?.[key])) {
        return response[key];
      }
    }
    return [];
  };

  // Load environmental logs and dashboard stats on mount
  useEffect(() => {
    loadEnvironmentalLogs();
    loadDashboardStatistics();
    loadDropdownData();
  }, []);

  // Load dropdown data
  const loadDropdownData = async () => {
    try {
      // Load storage unit types
      console.log("Loading storage unit types...");
      const storageTypesResponse = await fetchStorageUnitTypes();
      console.log("Storage types response:", storageTypesResponse);
      if (storageTypesResponse.success) {
        const formattedStorageTypes = storageTypesResponse.storageUnitTypes.map(
          (type) => ({
            id: type,
            text: storageUnitTypeLabels[type] || type,
            value: type,
          }),
        );
        console.log("Formatted storage types:", formattedStorageTypes);
        setStorageUnitTypes(formattedStorageTypes);
      } else {
        // Fallback to static data if API fails
        console.log("API failed, using fallback storage types");
        const fallbackTypes = [
          "ROOM",
          "FREEZER",
          "EQUIPMENT_ANALYZER",
          "MOVABLE_FRIDGE",
        ].map((type) => ({
          id: type,
          text: storageUnitTypeLabels[type] || type,
          value: type,
        }));
        setStorageUnitTypes(fallbackTypes);
      }

      // Load sample types
      console.log("Loading sample types...");
      const sampleTypesResponse = await fetchSampleTypes();
      console.log("Sample types response:", sampleTypesResponse);
      if (sampleTypesResponse.success) {
        setSampleTypes(sampleTypesResponse.sampleTypes); // Already in {id, value} format
      }

      // Load projects
      console.log("Loading projects...");
      const projectsResponse = await fetchProjects();
      console.log("Projects response:", projectsResponse);
      if (projectsResponse.success) {
        const formattedProjects = projectsResponse.projects.map((project) => ({
          id: project.id,
          text: project.title || project.name || "Unknown Project",
          value: project.title || project.name,
        }));
        console.log("Formatted projects:", formattedProjects);
        setProjects(formattedProjects);
      }

      const [roomsResponse, devicesResponse] = await Promise.all([
        fetchLocations().catch(() => []),
        fetchDevices().catch(() => []),
      ]);
      const rooms = asArray(roomsResponse, [
        "rooms",
        "items",
        "data",
        "results",
        "content",
      ]);
      const devices = asArray(devicesResponse, [
        "devices",
        "items",
        "data",
        "results",
        "content",
      ]);
      setStorageUnitOptions([
        ...rooms.map((room) => ({
          id: `ROOM-${room.id}`,
          text: room.name || room.code || `Room ${room.id}`,
          value: String(room.id),
          type: "ROOM",
        })),
        ...devices.map((device) => ({
          id: `FREEZER-${device.id}`,
          text: device.name || `Freezer ${device.id}`,
          value: String(device.id),
          type: "FREEZER",
        })),
      ]);
    } catch (error) {
      console.error("Error loading dropdown data:", error);
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to load dropdown data",
      });
    }
  };

  const loadEnvironmentalLogs = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetchEnvironmentalLogs({ limit: 50 });

      if (response.success) {
        // Convert numeric IDs to strings for DataTable compatibility
        const logsWithStringIds = response.logs.map((log) => ({
          ...log,
          id: String(log.id),
        }));
        setEnvironmentalLogs(logsWithStringIds);
        setError(null);
      } else {
        setError("Failed to load environmental logs");
        notify({
          kind: NotificationKinds.error,
          title: "Error",
          subtitle: response.error || "Failed to load environmental logs",
        });
      }
    } catch (err) {
      console.error("Error loading environmental logs:", err);
      setError("Failed to load environmental logs");
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to load environmental logs",
      });
    } finally {
      setLoading(false);
    }
  }, [notify]);

  const loadDashboardStatistics = useCallback(async () => {
    try {
      const response = await fetchDashboardStatistics();

      if (response.success) {
        setDashboardStats(response);
      }
    } catch (err) {
      console.error("Error loading dashboard statistics:", err);
    }
  }, []);

  const handleInputChange = (field, value) => {
    console.log(`Field changed: ${field} = ${value} (${typeof value})`);
    setEnvironmentalLog((prev) => {
      const updated = { ...prev, [field]: value };

      // Clear Movable Fridge specific fields when changing storage unit type
      if (field === "storageUnitType") {
        updated.storageUnitId = "";
        if (value !== "MOVABLE_FRIDGE") {
          updated.sampleType = "";
          updated.projectName = "";
          updated.sampleId = "";
          updated.additionalDetails = "";
        }
      }

      console.log("Updated environmental log:", updated);
      return updated;
    });
  };

  const selectedStorageUnitOptions = useMemo(
    () =>
      storageUnitOptions.filter(
        (option) => option.type === environmentalLog.storageUnitType,
      ),
    [environmentalLog.storageUnitType, storageUnitOptions],
  );

  const openLogModalForUnit = (unit) => {
    resetEnvironmentalLogForm();
    setEnvironmentalLog((prev) => ({
      ...prev,
      storageUnitType: unit.type,
      storageUnitId: unit.value,
    }));
    setIsModalOpen(true);
  };

  const validateEnvironmentalLog = () => {
    console.log("Validating environmental log:", environmentalLog);
    const errors = [];

    if (!environmentalLog.storageUnitType) {
      console.log("Storage unit type missing");
      errors.push(
        intl.formatMessage({
          id: "environmental.validation.storageUnitType",
        }) || "Storage unit type is required",
      );
    }
    if (!environmentalLog.storageUnitId?.trim()) {
      console.log("Storage unit ID missing");
      errors.push(
        intl.formatMessage({ id: "environmental.validation.storageUnitId" }) ||
          "Storage unit ID is required",
      );
    }
    if (
      environmentalLog.temperatureValue === null ||
      environmentalLog.temperatureValue === undefined ||
      environmentalLog.temperatureValue === "" ||
      environmentalLog.temperatureValue === " "
    ) {
      console.log(
        "Temperature value missing or invalid:",
        environmentalLog.temperatureValue,
        typeof environmentalLog.temperatureValue,
      );
      errors.push(
        intl.formatMessage({ id: "environmental.validation.temperature" }) ||
          "Temperature is required",
      );
    }
    if (!environmentalLog.checkedBy?.trim()) {
      console.log("Checked by missing");
      errors.push(
        intl.formatMessage({ id: "environmental.validation.checkedBy" }) ||
          "Checked by is required",
      );
    }

    if (environmentalLog.storageUnitType === "MOVABLE_FRIDGE") {
      if (!environmentalLog.sampleType?.trim()) {
        errors.push(
          intl.formatMessage({ id: "environmental.validation.sampleType" }),
        );
      }
      if (!environmentalLog.projectName?.trim()) {
        errors.push(
          intl.formatMessage({ id: "environmental.validation.projectName" }),
        );
      }
      if (!environmentalLog.sampleId?.trim()) {
        errors.push(
          intl.formatMessage({ id: "environmental.validation.sampleId" }),
        );
      }
    }

    return errors;
  };

  const handleSaveEnvironmentalLog = async () => {
    const validationErrors = validateEnvironmentalLog();
    if (validationErrors.length > 0) {
      notify({
        kind: NotificationKinds.error,
        title: "Validation Error",
        subtitle: validationErrors.join(", "),
      });
      return;
    }

    setIsLoggingEnv(true);
    try {
      const response = await createEnvironmentalLog(environmentalLog);

      if (response.success) {
        notify({
          kind: NotificationKinds.success,
          title: "Success",
          subtitle: intl.formatMessage({ id: "environmental.success" }),
        });
        resetEnvironmentalLogForm();
        setIsModalOpen(false);
        await loadEnvironmentalLogs();
      } else {
        notify({
          kind: NotificationKinds.error,
          title: "Error",
          subtitle:
            response.message ||
            intl.formatMessage({ id: "environmental.error" }),
        });
      }
    } catch (err) {
      console.error("Error saving environmental log:", err);
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle:
          err.message || intl.formatMessage({ id: "environmental.error" }),
      });
    } finally {
      setIsLoggingEnv(false);
    }
  };

  const resetEnvironmentalLogForm = () => {
    setEnvironmentalLog({
      storageUnitType: "ROOM",
      storageUnitId: "",
      intervalType: "AM",
      temperatureValue: "",
      temperatureUnit: "C",
      humidityValue: "",
      checkedBy: "",
      checkedDateTime: new Date().toISOString().slice(0, 16),
      notes: "",
      sampleType: "",
      projectName: "",
      sampleId: "",
      additionalDetails: "",
    });
  };

  const getTemperatureRangeForType = (storageUnitType) => {
    const ranges = {
      FREEZER: { min: -25, max: -15 },
      ROOM: { min: 15, max: 25 },
      EQUIPMENT_ANALYZER: { min: 2, max: 8 },
      MOVABLE_FRIDGE: { min: 2, max: 8 },
    };
    return ranges[storageUnitType] || { min: 0, max: 100 };
  };

  const isTemperatureOutOfRange = (log) => {
    const range = getTemperatureRangeForType(log.storageUnitType);
    if (!range) return false;
    return log.temperatureValue < range.min || log.temperatureValue > range.max;
  };

  const getStorageUnitIcon = (type) => {
    return storageUnitTypeIcons[type] || Settings;
  };

  const latestLogByUnit = useMemo(() => {
    return environmentalLogs.reduce((latest, log) => {
      const key = `${log.storageUnitType}-${log.storageUnitId}`;
      const previous = latest[key];
      if (
        !previous ||
        new Date(log.checkedDateTime) > new Date(previous.checkedDateTime)
      ) {
        latest[key] = log;
      }
      return latest;
    }, {});
  }, [environmentalLogs]);

  const filteredUnits = storageUnitOptions.filter((unit) => {
    const latestLog = latestLogByUnit[`${unit.type}-${unit.value}`];
    const matchesSearch =
      !searchTerm ||
      unit.text.toLowerCase().includes(searchTerm.toLowerCase()) ||
      unit.value.toLowerCase().includes(searchTerm.toLowerCase()) ||
      latestLog?.checkedBy?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      latestLog?.notes?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesFilter = !storageTypeFilter || unit.type === storageTypeFilter;

    return matchesSearch && matchesFilter;
  });

  // Table configuration
  const tableHeaders = [
    { key: "storageUnitType", header: "Storage Unit Type" },
    { key: "storageUnitId", header: "Unit ID" },
    { key: "temperatureValue", header: "Temperature" },
    { key: "humidityValue", header: "Humidity" },
    { key: "checkedBy", header: "Checked By" },
    { key: "checkedDateTime", header: "Date/Time" },
    { key: "notes", header: "Notes" },
  ];

  const tableRows = filteredUnits.map((unit) => {
    const latestLog = latestLogByUnit[`${unit.type}-${unit.value}`];
    return {
      id: unit.id,
      storageUnitType: (
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          {React.createElement(getStorageUnitIcon(unit.type), {
            size: 16,
          })}
          {unit.type.replace("_", " ")}
        </div>
      ),
      storageUnitId: unit.text,
      temperatureValue: (
        <span
          style={{
            color:
              latestLog && isTemperatureOutOfRange(latestLog)
                ? "#da1e28"
                : "inherit",
            fontWeight:
              latestLog && isTemperatureOutOfRange(latestLog)
                ? "bold"
                : "normal",
          }}
        >
          {latestLog
            ? `${latestLog.temperatureValue}°${latestLog.temperatureUnit || "C"}`
            : "—"}
          {latestLog && isTemperatureOutOfRange(latestLog) && (
            <Warning
              size={16}
              style={{ marginLeft: "4px", color: "#da1e28" }}
            />
          )}
        </span>
      ),
      humidityValue: latestLog?.humidityValue
        ? `${latestLog.humidityValue}%`
        : "—",
      checkedBy: latestLog?.checkedBy || "—",
      checkedDateTime: latestLog?.checkedDateTime
        ? new Date(latestLog.checkedDateTime).toLocaleString()
        : "No reading yet",
      notes: (
        <Button
          kind="ghost"
          size="sm"
          onClick={() => openLogModalForUnit(unit)}
        >
          Log Reading
        </Button>
      ),
    };
  });

  // Pagination
  const paginatedRows = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return tableRows.slice(startIndex, endIndex);
  }, [tableRows, currentPage, pageSize]);

  if (loading) {
    return (
      <div style={{ padding: "1rem 0", textAlign: "center" }}>
        <FormattedMessage
          id="common.loading"
          defaultMessage="Loading environmental monitoring data..."
        />
      </div>
    );
  }

  return (
    <div style={{ padding: "1rem 0" }}>
      {notificationVisible === true ? <AlertDialog /> : ""}

      {/* Environmental Logs Data Table */}
      <DataTable rows={paginatedRows} headers={tableHeaders}>
        {({
          rows,
          headers,
          getHeaderProps,
          getRowProps,
          getTableProps,
          getTableContainerProps,
        }) => (
          <TableContainer
            title={intl.formatMessage({ id: "environmental.title" })}
            description={intl.formatMessage({ id: "environmental.subtitle" })}
            {...getTableContainerProps()}
          >
            <TableToolbar>
              <TableToolbarContent>
                <TableToolbarSearch
                  placeholder="Search environmental logs..."
                  onChange={(event) => setSearchTerm(event.target.value)}
                />
                <Dropdown
                  id="storage-unit-filter"
                  name="storageUnitFilter"
                  label={
                    storageTypeFilter
                      ? storageUnitTypes.find(
                          (type) => type.value === storageTypeFilter,
                        )?.text || "All Storage Units"
                      : "All Storage Units"
                  }
                  initialSelectedItem={
                    storageTypeFilter
                      ? [
                          ...[{ id: "", text: "All Storage Units" }],
                          ...storageUnitTypes,
                        ].find((type) => type.value === storageTypeFilter)
                      : { id: "", text: "All Storage Units" }
                  }
                  items={[
                    { id: "", text: "All Storage Units", value: "" },
                    ...storageUnitTypes.map((type) => ({
                      id: type.value,
                      text: type.text,
                      value: type.value,
                    })),
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  onChange={(event) => {
                    setStorageTypeFilter(event.selectedItem?.value || "");
                  }}
                />
                <Button
                  kind="primary"
                  renderIcon={Add}
                  onClick={() => {
                    resetEnvironmentalLogForm();
                    setIsModalOpen(true);
                  }}
                >
                  <FormattedMessage
                    id="environmental.submit"
                    defaultMessage="Log Reading"
                  />
                </Button>
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
                {rows.map((row) => (
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {tableRows.length > pageSize && (
              <Pagination
                backwardText="Previous page"
                forwardText="Next page"
                itemsPerPageText="Items per page:"
                page={currentPage}
                pageSize={pageSize}
                pageSizes={[10, 20, 50]}
                totalItems={tableRows.length}
                onChange={({ page, pageSize: newPageSize }) => {
                  setCurrentPage(page);
                  setPageSize(newPageSize);
                }}
              />
            )}
          </TableContainer>
        )}
      </DataTable>

      {/* No data state */}
      {filteredUnits.length === 0 && !loading && (
        <div
          style={{ textAlign: "center", padding: "3rem 0", color: "#6f6f6f" }}
        >
          <Temperature size={48} style={{ opacity: 0.3 }} />
          <p style={{ marginTop: "1rem" }}>
            <FormattedMessage
              id="environmental.nologs"
              defaultMessage="No storage units found."
            />
          </p>
        </div>
      )}

      {/* Environmental Logging Modal */}
      <Modal
        open={isModalOpen}
        onRequestClose={() => setIsModalOpen(false)}
        onRequestSubmit={handleSaveEnvironmentalLog}
        modalHeading={intl.formatMessage({ id: "environmental.title" })}
        primaryButtonText={intl.formatMessage({ id: "environmental.submit" })}
        secondaryButtonText={intl.formatMessage({ id: "environmental.cancel" })}
        primaryButtonDisabled={isLoggingEnv}
        size="md"
      >
        <Form>
          <Stack gap={6}>
            {/* Storage Unit Type */}
            <Select
              id="storage-unit-type"
              labelText={<FormattedMessage id="environmental.storageUnit" />}
              value={environmentalLog.storageUnitType}
              onChange={(event) =>
                handleInputChange("storageUnitType", event.target.value)
              }
            >
              {storageUnitTypes.map((type) => (
                <SelectItem
                  key={type.value}
                  value={type.value}
                  text={type.text}
                />
              ))}
            </Select>

            {selectedStorageUnitOptions.length > 0 ? (
              <Select
                id="storage-unit-id"
                labelText={<FormattedMessage id="environmental.unitId" />}
                value={environmentalLog.storageUnitId}
                onChange={(event) =>
                  handleInputChange("storageUnitId", event.target.value)
                }
                required
              >
                <SelectItem value="" text="Select a unit" />
                {selectedStorageUnitOptions.map((unit) => (
                  <SelectItem
                    key={unit.id}
                    value={unit.value}
                    text={unit.text}
                  />
                ))}
              </Select>
            ) : (
              <TextInput
                id="storage-unit-id"
                labelText={<FormattedMessage id="environmental.unitId" />}
                placeholder={intl.formatMessage({
                  id: "environmental.unitId.placeholder",
                })}
                value={environmentalLog.storageUnitId}
                onChange={(event) =>
                  handleInputChange("storageUnitId", event.target.value)
                }
                required
              />
            )}

            {/* Temperature */}
            <NumberInput
              id="temperature-value"
              label={<FormattedMessage id="environmental.temperature" />}
              placeholder={intl.formatMessage({
                id: "environmental.temperature.placeholder",
              })}
              value={environmentalLog.temperatureValue}
              onChange={(event, { value }) => {
                console.log("Temperature change:", value);
                handleInputChange("temperatureValue", value);
              }}
              step={0.1}
              required
            />

            {/* Temperature Unit */}
            <RadioButtonGroup
              name="temperature-unit"
              value={environmentalLog.temperatureUnit}
              onChange={(value) => handleInputChange("temperatureUnit", value)}
              legendText={
                <FormattedMessage id="environmental.temperatureUnit" />
              }
            >
              <RadioButton labelText="Celsius (°C)" value="C" id="celsius" />
              <RadioButton
                labelText="Fahrenheit (°F)"
                value="F"
                id="fahrenheit"
              />
            </RadioButtonGroup>

            {/* Humidity */}
            <NumberInput
              id="humidity-value"
              label={<FormattedMessage id="environmental.humidity" />}
              placeholder={intl.formatMessage({
                id: "environmental.humidity.placeholder",
              })}
              value={environmentalLog.humidityValue}
              onChange={(event, { value }) =>
                handleInputChange("humidityValue", value)
              }
              min={0}
              max={100}
            />

            {/* Interval Type */}
            <Select
              id="interval-type"
              labelText={<FormattedMessage id="environmental.interval" />}
              value={environmentalLog.intervalType}
              onChange={(event) =>
                handleInputChange("intervalType", event.target.value)
              }
            >
              <SelectItem value="AM" text="AM" />
              <SelectItem value="PM" text="PM" />
            </Select>

            {/* Checked By */}
            <TextInput
              id="checked-by"
              labelText={<FormattedMessage id="environmental.checkedBy" />}
              placeholder={intl.formatMessage({
                id: "environmental.checkedBy.placeholder",
              })}
              value={environmentalLog.checkedBy}
              onChange={(event) =>
                handleInputChange("checkedBy", event.target.value)
              }
              required
            />

            {/* Checked Date/Time */}
            <TextInput
              id="checked-datetime"
              labelText={
                <FormattedMessage id="environmental.checkedDateTime" />
              }
              type="datetime-local"
              value={environmentalLog.checkedDateTime}
              onChange={(event) =>
                handleInputChange("checkedDateTime", event.target.value)
              }
            />

            {/* Movable Fridge Specific Fields */}
            {environmentalLog.storageUnitType === "MOVABLE_FRIDGE" && (
              <ExpandableTile className="movable-fridge-section">
                <TileAboveTheFoldContent>
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "8px",
                    }}
                  >
                    <VirtualPrivateCloud size={20} />
                    <strong>
                      <FormattedMessage
                        id="environmental.movableFridge.title"
                        defaultMessage="Movable Fridge Sample Information"
                      />
                    </strong>
                  </div>
                </TileAboveTheFoldContent>
                <TileBelowTheFoldContent>
                  <Stack gap={4}>
                    {/* Sample Type Dropdown */}
                    <Select
                      id="sample-type"
                      labelText={
                        <FormattedMessage id="environmental.movableFridge.sampleType" />
                      }
                      value={environmentalLog.sampleType}
                      onChange={(event) =>
                        handleInputChange("sampleType", event.target.value)
                      }
                      required
                    >
                      <SelectItem value="" text="Select a sample type" />
                      {sampleTypes.map((type) => (
                        <SelectItem
                          key={type.id}
                          value={type.id}
                          text={type.value}
                        />
                      ))}
                    </Select>

                    {/* Project Name Dropdown */}
                    <Select
                      id="project-name"
                      labelText={
                        <FormattedMessage id="environmental.movableFridge.projectName" />
                      }
                      value={environmentalLog.projectName}
                      onChange={(event) =>
                        handleInputChange("projectName", event.target.value)
                      }
                      required
                    >
                      <SelectItem value="" text="Select a project" />
                      {projects.map((project) => (
                        <SelectItem
                          key={project.id}
                          value={project.value}
                          text={project.text}
                        />
                      ))}
                    </Select>

                    {/* Sample ID - keep as text input for now, can add lookup later */}
                    <TextInput
                      id="sample-id"
                      labelText={
                        <FormattedMessage id="environmental.movableFridge.sampleId" />
                      }
                      placeholder={intl.formatMessage({
                        id: "environmental.movableFridge.sampleId.placeholder",
                      })}
                      value={environmentalLog.sampleId}
                      onChange={(event) =>
                        handleInputChange("sampleId", event.target.value)
                      }
                      required
                    />

                    <TextArea
                      id="additional-details"
                      labelText={
                        <FormattedMessage id="environmental.movableFridge.additionalDetails" />
                      }
                      placeholder={intl.formatMessage({
                        id: "environmental.movableFridge.additionalDetails.placeholder",
                      })}
                      value={environmentalLog.additionalDetails}
                      onChange={(event) =>
                        handleInputChange(
                          "additionalDetails",
                          event.target.value,
                        )
                      }
                      rows={3}
                    />
                  </Stack>
                </TileBelowTheFoldContent>
              </ExpandableTile>
            )}

            {/* Notes */}
            <TextArea
              id="notes"
              labelText={<FormattedMessage id="environmental.notes" />}
              placeholder={intl.formatMessage({
                id: "environmental.notes.placeholder",
              })}
              value={environmentalLog.notes}
              onChange={(event) =>
                handleInputChange("notes", event.target.value)
              }
              rows={3}
            />
          </Stack>
        </Form>
      </Modal>
    </div>
  );
}
